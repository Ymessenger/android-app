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
import org.ymessenger.app.data.repositories.ChatPreviewRepository
import org.ymessenger.app.data.repositories.MessageRepository
import org.ymessenger.app.utils.SingleLiveEvent

class SearchMessagesViewModel(
    private val messageRepository: MessageRepository,
    private val chatPreviewRepository: ChatPreviewRepository
) : BaseViewModel() {

    private val searchQuery = MutableLiveData<String>()

    private val foundMessagesResult = Transformations.map(searchQuery) {
        messageRepository.searchMessages(it, chatPreviewRepository = chatPreviewRepository)
    }
    val foundMessages = Transformations.switchMap(foundMessagesResult) { it.pagedList }
    val foundMessagesRefreshState =
        Transformations.switchMap(foundMessagesResult) { it.refreshState }

    val openUserEvent = SingleLiveEvent<Long>()
    val openChatEvent = SingleLiveEvent<Long>()
    val openChannelEvent = SingleLiveEvent<Long>()

    fun search(query: String) {
        searchQuery.postValue(query)
    }

    fun openUser(userId: Long) {
        openUserEvent.postValue(userId)
    }

    fun openChat(chatId: Long) {
        openChatEvent.postValue(chatId)
    }

    fun openChannel(channelId: Long) {
        openChannelEvent.postValue(channelId)
    }

    fun refreshMessages() {
        foundMessagesResult.value?.refresh?.invoke()
    }

    class Factory(
        private val messageRepository: MessageRepository,
        private val chatPreviewRepository: ChatPreviewRepository
    ) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return SearchMessagesViewModel(
                messageRepository,
                chatPreviewRepository
            ) as T
        }
    }

}