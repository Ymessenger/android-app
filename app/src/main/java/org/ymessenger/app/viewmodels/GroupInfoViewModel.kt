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

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.ymessenger.app.R
import org.ymessenger.app.data.ConversationType
import org.ymessenger.app.data.local.db.entities.ChatUser
import org.ymessenger.app.data.local.db.entities.FavoriteConversation
import org.ymessenger.app.data.remote.requests.AddUsersChats
import org.ymessenger.app.data.remote.requests.ChangeChatUsers
import org.ymessenger.app.data.remote.responses.ResultResponse
import org.ymessenger.app.data.repositories.*
import org.ymessenger.app.interfaces.SuccessErrorCallback
import org.ymessenger.app.utils.SingleLiveEvent

class GroupInfoViewModel(
    private val chatId: Long,
    private val currentUserId: Long,
    private val chatRepository: ChatRepository,
    private val chatUserRepository: ChatUserRepository,
    private val contactRepository: ContactRepository,
    private val chatPreviewRepository: ChatPreviewRepository,
    private val favoriteConversationRepository: FavoriteConversationRepository
) : BaseViewModel() {
    val chat = chatRepository.getChat(chatId)

    val favoriteConversation =
        favoriteConversationRepository.getFavoriteConversation(chatId, ConversationType.CHAT)

    private val updateChatUsers = SingleLiveEvent<Void>()
    val chatUserModels = Transformations.switchMap(updateChatUsers) {
        chatUserRepository.getChatUserModels(chatId)
    }
    val currentChatUser = chatUserRepository.getChatUser(chatId, currentUserId)

    private val contactsIdToAdd = MutableLiveData<List<String>>()
    val contactsToAdd = Transformations.switchMap(contactsIdToAdd) {
        contactRepository.getContactModelsById(it)
    }

    val openUserProfileEvent = SingleLiveEvent<Long>()
    val openChatEditEvent = SingleLiveEvent<Long>()
    val chatDeletedEvent = SingleLiveEvent<Void>()

    private var connected: Boolean = false

    init {
        updateChatUsers.call()
    }

    fun openUserProfile(userId: Long) {
        openUserProfileEvent.postValue(userId)
    }

    fun switchFavorite() {
        favoriteConversation.value?.let {
            favoriteConversationRepository.delete(it)
        } ?: favoriteConversationRepository.insert(
            FavoriteConversation(
                chatId,
                ConversationType.CHAT
            )
        )
    }

    fun openChatEdit() {
        openChatEditEvent.postValue(chatId)
    }

    fun addContactsToGroup(contactsId: List<String>) {
        contactsIdToAdd.postValue(contactsId)
    }

    fun addUsersToGroup(usersId: List<Long>) {
        startLoading()
        val addUsersChats = AddUsersChats(listOf(chatId), usersId)
        chatUserRepository.addUsersToChats(addUsersChats, object : SuccessErrorCallback {
            override fun success() {
                endLoading()
            }

            override fun error(error: ResultResponse) {
                endLoading()
                showErrorFromCode(error.errorCode)
            }
        })
    }

    fun removeUsersFromChat(usersId: List<Long>) {
        val chatUsers = arrayListOf<org.ymessenger.app.data.remote.entities.ChatUser>()
        for (userId in usersId) {
            chatUsers.add(
                org.ymessenger.app.data.remote.entities.ChatUser(
                    chatId,
                    userId,
                    true,
                    null,
                    null,
                    null
                )
            )
        }

        changeChatUsers(chatUsers)
    }

    fun deleteChat() {
        if (!hasConnection()) return

        startLoading(R.string.deleting)
        chatRepository.deleteChat(chatId, object : SuccessErrorCallback {
            override fun success() {
                chatPreviewRepository.deleteChatPreview(chatId, ConversationType.CHAT)
                endLoading()
                chatDeletedEvent.call()
            }

            override fun error(error: ResultResponse) {
                endLoading()
                showErrorFromCode(error.errorCode)
            }
        })
    }

    fun setUserRole(userId: Long, userRole: Int) {
        val chatUser = org.ymessenger.app.data.remote.entities.ChatUser(
            chatId,
            userId,
            false,
            null,
            userRole,
            null
        )

        changeChatUsers(listOf(chatUser))
    }

    fun isMember(): Boolean {
        return currentChatUser.value != null
    }

    private fun changeChatUsers(chatUsers: List<org.ymessenger.app.data.remote.entities.ChatUser>) {
        if (!hasConnection()) return

        startLoading(R.string.updating)
        val changeChatUsers = ChangeChatUsers(chatUsers, chatId)
        chatUserRepository.changeChatUsers(
            changeChatUsers,
            object : ChatUserRepository.ChangeChatUsersCallback {
                override fun success(editedChatUsers: List<ChatUser>) {
                    endLoading()
                    updateChatUsers.call()
                }

                override fun error(error: ResultResponse) {
                    endLoading()
                    showErrorFromCode(error.errorCode)
                }

            })
    }

    fun setConnected(status: Boolean) {
        connected = status
    }

    private fun hasConnection(): Boolean {
        if (!connected) {
            showError(R.string.connection_is_lost)
        }

        return connected
    }

    class Factory(
        private val chatId: Long,
        private val currentUserId: Long,
        private val chatRepository: ChatRepository,
        private val chatUserRepository: ChatUserRepository,
        private val contactRepository: ContactRepository,
        private val chatPreviewRepository: ChatPreviewRepository,
        private val favoriteConversationRepository: FavoriteConversationRepository
    ) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return GroupInfoViewModel(
                chatId,
                currentUserId,
                chatRepository,
                chatUserRepository,
                contactRepository,
                chatPreviewRepository,
                favoriteConversationRepository
            ) as T
        }
    }

    companion object {
        private const val TAG = "GroupInfoViewModel"
    }

}