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
import org.ymessenger.app.data.local.db.entities.FavoriteConversation
import org.ymessenger.app.data.remote.WebSocketService
import org.ymessenger.app.data.remote.entities.ChannelUser
import org.ymessenger.app.data.remote.responses.ChannelUsers
import org.ymessenger.app.data.remote.responses.ResultResponse
import org.ymessenger.app.data.repositories.*
import org.ymessenger.app.utils.SingleLiveEvent

class ChannelInfoViewModel(
    private val channelId: Long,
    private val currentUserId: Long,
    private val channelRepository: ChannelRepository,
    private val channelUserRepository: ChannelUserRepository,
    private val contactRepository: ContactRepository,
    private val chatPreviewRepository: ChatPreviewRepository,
    private val favoriteConversationRepository: FavoriteConversationRepository
) : BaseViewModel() {
    private val updateChannel = SingleLiveEvent<Void>()
    val channel = Transformations.switchMap(updateChannel) {
        channelRepository.getChannel(channelId)
    }

    val favoriteConversation =
        favoriteConversationRepository.getFavoriteConversation(channelId, ConversationType.CHANNEL)

    private val contactsIdToAdd = MutableLiveData<List<String>>()
    val contactsToAdd = Transformations.switchMap(contactsIdToAdd) {
        contactRepository.getContactModelsById(it)
    }

    val openUserProfileEvent = SingleLiveEvent<Long>()
    val openChannelEditEvent = SingleLiveEvent<Long>()
    val openSubscribersEvent = SingleLiveEvent<Long>()
    val openPhotoEvent = SingleLiveEvent<String?>()
    val channelDeletedEvent = SingleLiveEvent<Void>()

    private var connected: Boolean = false

    init {
        updateChannel.call()
    }

    fun openUserProfile(userId: Long) {
        openUserProfileEvent.postValue(userId)
    }

    fun switchFavorite() {
        favoriteConversation.value?.let {
            favoriteConversationRepository.delete(it)
        } ?: favoriteConversationRepository.insert(
            FavoriteConversation(
                channelId,
                ConversationType.CHANNEL
            )
        )
    }

    fun openChannelEdit() {
        openChannelEditEvent.postValue(channelId)
    }

    fun openPhoto() {
        openPhotoEvent.postValue(channel.value?.getPhotoUrl())
    }

    fun openSubscribers() {
        openSubscribersEvent.postValue(channelId)
    }

    fun joinChannel() {
        if (!hasConnection()) return

        channelUserRepository.addUsersToChannels(
            listOf(currentUserId),
            listOf(channelId),
            object : WebSocketService.ResponseCallback<ChannelUsers> {
                override fun onResponse(response: ChannelUsers) {
                    // Update channel user role
                    updateChannel.call()
                }

                override fun onError(error: ResultResponse) {
                    showErrorFromCode(error.errorCode)
                }
            })
    }

    fun leaveChannel() {
        if (!hasConnection()) return

        startLoading()
        val channelUser = ChannelUser(channelId, currentUserId, null, true, null)
        channelUserRepository.editChannelUsers(
            listOf(channelUser),
            channelId,
            object : WebSocketService.ResponseCallback<ChannelUsers> {
                override fun onResponse(response: ChannelUsers) {
                    endLoading()
                    // Update channel user role
                    updateChannel.call()
                }

                override fun onError(error: ResultResponse) {
                    endLoading()
                    showErrorFromCode(error.errorCode)
                }
            })
    }

    fun deleteChannel() {
        if (!hasConnection()) return

        startLoading(R.string.deleting)
        channelRepository.deleteChannel(
            channelId,
            object : ChannelRepository.DeleteChannelCallback {
                override fun deleted() {
                    endLoading()
                    chatPreviewRepository.deleteChatPreview(channelId, ConversationType.CHANNEL)
                    channelDeletedEvent.call()
                }

                override fun error(errorCode: Int) {
                    endLoading()
                    showErrorFromCode(errorCode)
                }
            })
    }

    fun addContactsToGroup(contactsId: List<String>) {
        contactsIdToAdd.postValue(contactsId)
    }

    fun removeUsersFromChannel(usersId: List<Long>) {
        TODO()
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
        private val channelId: Long,
        private val currentUserId: Long,
        private val channelRepository: ChannelRepository,
        private val channelUserRepository: ChannelUserRepository,
        private val contactRepository: ContactRepository,
        private val chatPreviewRepository: ChatPreviewRepository,
        private val favoriteConversationRepository: FavoriteConversationRepository
    ) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return ChannelInfoViewModel(
                channelId,
                currentUserId,
                channelRepository,
                channelUserRepository,
                contactRepository,
                chatPreviewRepository,
                favoriteConversationRepository
            ) as T
        }
    }

    companion object {
        private const val TAG = "ChannelInfoViewModel"
    }

}