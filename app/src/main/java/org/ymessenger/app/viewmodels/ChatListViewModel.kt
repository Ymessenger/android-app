/*
 * This file is part of Y messenger.
 *
 * Y messenger is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Y messenger is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Y messenger.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.ymessenger.app.viewmodels

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.ymessenger.app.R
import org.ymessenger.app.data.ConversationType
import org.ymessenger.app.data.local.db.entities.ChatPreview
import org.ymessenger.app.data.local.db.entities.FavoriteConversation
import org.ymessenger.app.data.local.db.entities.Keys
import org.ymessenger.app.data.local.db.entities.User
import org.ymessenger.app.data.local.db.models.ChatPreviewModel
import org.ymessenger.app.data.remote.Config
import org.ymessenger.app.data.remote.WSResponse
import org.ymessenger.app.data.remote.WebSocketService
import org.ymessenger.app.data.remote.entities.Token
import org.ymessenger.app.data.remote.requests.DeleteConversation
import org.ymessenger.app.data.remote.requests.Login
import org.ymessenger.app.data.remote.responses.ResultResponse
import org.ymessenger.app.data.repositories.*
import org.ymessenger.app.helpers.AuthorizationManager
import org.ymessenger.app.helpers.EncryptHelper
import org.ymessenger.app.helpers.SettingsHelper
import org.ymessenger.app.interfaces.SuccessErrorCallback
import org.ymessenger.app.utils.SingleLiveEvent
import java.util.*

class ChatListViewModel(
    private val userRepository: UserRepository,
    private val chatPreviewRepository: ChatPreviewRepository,
    private val messageRepository: MessageRepository,
    private val webSocketService: WebSocketService,
    private val authorizationManager: AuthorizationManager,
    private val keysRepository: KeysRepository,
    private val settingsHelper: SettingsHelper,
    private val favoriteConversationRepository: FavoriteConversationRepository,
    private val userActionRepository: UserActionRepository,
    private val contactRepository: ContactRepository
) : BaseViewModel() {

    val currentUser: LiveData<User>
    val favoriteConversations = favoriteConversationRepository.getFavoriteConversations()

    private val updateChatPreviewEvent = SingleLiveEvent<Void>()
    private val chatPreviewsResult = chatPreviewRepository.getChatPreviewModels()

    val chatPreviews: LiveData<List<ChatPreviewModel>> =
        Transformations.map(chatPreviewsResult.itemList) {
            updateUsersFromChatPreviews(it ?: listOf())
            it
        }
    val chatPreviewsNetworkState = chatPreviewsResult.networkState

    val logoutEvent = SingleLiveEvent<Void>()
    val openChatEvent = SingleLiveEvent<Long>()
    val openDialogEvent = SingleLiveEvent<Long>()
    val openChannelEvent = SingleLiveEvent<Long>()

    val generateShortKeysEvent = SingleLiveEvent<Void>()

    val askToSetPassphraseEvent = SingleLiveEvent<Void>()

    var userId: Long = 0

    val authorizedEvent = authorizationManager.authorizedEvent

    val setPrivateEncryptKeyToNotificationHandlerEvent = SingleLiveEvent<ByteArray>()

    /**
     * Workaround to restrict recursively chatPreviews - users update
     */
    private var lastUpdateUsersTime = 0L

    private lateinit var timer: Timer

    val lastUserActionList =
        userActionRepository.getLastUserActionModels(System.currentTimeMillis() / 1000L)

    init {
        checkAuthorization()
        currentUser = userRepository.getUser(userId)
        updateChatPreviewEvent.call()
        // Load all contacts
        contactRepository.getContactModels()

        if (!settingsHelper.getAskedOldUsersToSetPassphrase()) {
            askToSetPassphraseEvent.call()
        }
    }

    fun initTimer() {
        timer = Timer()
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                Log.d(TAG, "Timer tick")
                Handler(Looper.getMainLooper()).post {
                    updateUsersFromChatPreviews(chatPreviews.value ?: listOf())
                }
            }
        }, Config.TIMER_UPDATE_PERIOD, Config.TIMER_UPDATE_PERIOD)
        Log.d(TAG, "Timer started")
    }

    fun cancelTimer() {
        timer.cancel()
        Log.d(TAG, "Timer was cancelled")
    }

    fun addUserToFavourites(userId: Long) {
        favoriteConversationRepository.insert(FavoriteConversation(userId, ConversationType.DIALOG))
    }

    fun openChat(chatId: Long) {
        openChatEvent.postValue(chatId)
    }

    fun openChannel(channelId: Long) {
        openChannelEvent.postValue(channelId)
    }

    fun openDialog(userId: Long) {
        openDialogEvent.postValue(userId)
    }

    // FIXME: delegate this to chatRepository
    fun deleteChat(chatId: Long) {
        val deleteChats = DeleteConversation(chatId, ConversationType.CHAT)
        webSocketService.deleteConversation(
            deleteChats,
            object : WebSocketService.ResponseCallback<WSResponse> {
                override fun onResponse(response: WSResponse) {
                    chatPreviewRepository.deleteChatPreview(chatId, ConversationType.CHAT)
                    Log.d(TAG, "Chat deleted")
                }

                override fun onError(error: ResultResponse) {
                    Log.e(TAG, "Error while deleting chat")
                }
            })
    }

    fun deleteAllConversationMessages(conversationId: Long, conversationType: Int) {
        messageRepository.clearMessagesLocally(conversationType, conversationId)
    }

    fun deleteChatPreview(conversationId: Long, conversationType: Int) {
        chatPreviewRepository.deleteChatPreview(conversationId, conversationType)
    }

    private fun checkAuthorization() {
        // FIXME: move this to AuthorizationManager or to AppBase
        val token = settingsHelper.getToken() ?: throw Exception("Authorization token not found")
        userId = token.userId
        if (!authorizationManager.isAuthorized) {
            authorizeWithToken(token)
        } else {
            // Check for keys. If there is no keys then generate short keys and send a public key to server
            checkForKeys()
        }
    }

    // FIXME: Delegate to authorization manager
    private fun authorizeWithToken(token: Token) {
        val loginRequest = Login(token)
        authorizationManager.authorize(loginRequest, object : AuthorizationManager.Callback {
            override fun authorized() {
                updateChatPreviewEvent.call()
                userRepository.getSelf(token.userId)
                // Check for keys. If there is no keys then generate short keys and send a public key to server
                checkForKeys()
            }

            override fun error(error: ResultResponse) {
                // nothing
            }
        })
    }

    // TODO: Maybe I should move this check to AuthorizationManager
    private fun checkForKeys() {
        // We are looking for ENCRYPTION (not sign) keys, because if we don't have encryption key
        // nobody can send us an encrypted message
        val isSign = false
        keysRepository.getMyLastKey(userId, isSign, object : KeysRepository.GetKeyResult {
            override fun result(keys: Keys) {
                Log.d(TAG, "There is my asymmetric keys: keyId = ${keys.id}")

                // Checking for keys lifetime and if it is expired it's need to generate new keys
                if (EncryptHelper.isExpired(keys)) {
                    Log.d(TAG, "My asymmetric keys are expired. Generating new one...")
                    generateShortKeys()
                } else {
                    keysRepository.getKeysFromOtherDevices(
                        EncryptHelper.bytesToBase64(keys.publicKey),
                        object : SuccessErrorCallback {
                            override fun success() {
//                            showToast("Request 'GetDevicePrivateKeys' was sent")
                            }

                            override fun error(error: ResultResponse) {
//                            showToast("Failed to send request 'GetDevicePrivateKeys'. Message: ${error.message}")
                            }
                        })
                    setPrivateEncryptKeyToNotificationHandlerEvent.postValue(keys.privateKey)
                }
            }

            override fun error() {
                Log.e(TAG, "Failed to get my asymmetric keys. Generating new one...")
                generateShortKeys()
            }
        })
    }

    private fun generateShortKeys() {
        generateShortKeysEvent.call()
    }

    fun logout() {
        startLoading(R.string.logging_out)
        authorizationManager.logout({
            endLoading()
            logoutEvent.call()
        }, {
            endLoading()
            showError(R.string.failed_to_logout)
        })
    }

    fun updateChatPreviews() {
        chatPreviewsResult.refresh.invoke()
    }

    private fun updateUsersFromChatPreviews(chatPreviewModels: List<ChatPreviewModel>) {
        val timeout = 5000L // 5 sec
        if (System.currentTimeMillis() - lastUpdateUsersTime < timeout) {
            Log.d(TAG, "Too many users updates, skip")
            return
        }

        lastUpdateUsersTime = System.currentTimeMillis()

        Log.d(TAG, "chatPreviewModels size ${chatPreviewModels.size}")
        val usersId = hashSetOf<Long>()

        for (chatPreviewModel in chatPreviewModels) {
            if (chatPreviewModel.chatPreview.isDialog()) {
                chatPreviewModel.chatPreview.userId?.let {
                    usersId.add(it)
                }
            }
        }

        if (usersId.isNotEmpty()) {
            userRepository.getUsers(usersId.toList())
        }
    }

    fun reorderFavoriteConversations(favoriteConversations: List<FavoriteConversation>) {
        for ((index, favoriteConversation) in favoriteConversations.withIndex()) {
            favoriteConversation.sort = index
        }
        favoriteConversationRepository.update(favoriteConversations)
    }

    fun muteNotifications(chatPreview: ChatPreview) {
        chatPreviewRepository.muteConversation(chatPreview, object : SuccessErrorCallback {
            override fun success() {
                // nothing
            }

            override fun error(error: ResultResponse) {
                showToast(R.string.failed_to_perform_operation)
            }
        })
    }

    fun addToFavorites(identifier: Long, conversationType: Int) {
        val favoriteConversation = FavoriteConversation(identifier, conversationType)
        favoriteConversationRepository.insert(favoriteConversation)
    }

    fun removeFromFavorites(identifier: Long, conversationType: Int) {
        favoriteConversationRepository.delete(identifier, conversationType)
    }

    fun getAlwaysOpenMainActivityAsAfterRegister() =
        settingsHelper.getAlwaysOpenMainActivityAsAfterRegister()

    fun setAskedToSetPassphrase() {
        settingsHelper.setAskedOldUsersToSetPassphrase()
    }

    class Factory(
        private val userRepository: UserRepository,
        private val chatPreviewRepository: ChatPreviewRepository,
        private val messageRepository: MessageRepository,
        private val webSocketService: WebSocketService,
        private val authorizationManager: AuthorizationManager,
        private val keysRepository: KeysRepository,
        private val settingsHelper: SettingsHelper,
        private val favoriteConversationRepository: FavoriteConversationRepository,
        private val userActionRepository: UserActionRepository,
        private val contactRepository: ContactRepository
    ) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return ChatListViewModel(
                userRepository,
                chatPreviewRepository,
                messageRepository,
                webSocketService,
                authorizationManager,
                keysRepository,
                settingsHelper,
                favoriteConversationRepository,
                userActionRepository,
                contactRepository
            ) as T
        }
    }

    companion object {
        private const val TAG = "ChatListViewModel"
    }
}