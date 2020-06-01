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
import org.ymessenger.app.data.repositories.ChannelRepository
import org.ymessenger.app.data.repositories.ChatRepository
import org.ymessenger.app.data.repositories.UserRepository
import org.ymessenger.app.utils.SingleLiveEvent

class GlobalSearchViewModel(
    private val userRepository: UserRepository,
    private val chatRepository: ChatRepository,
    private val channelRepository: ChannelRepository
) : BaseViewModel() {

    private val searchQuery = MutableLiveData<String>()

    // People
    private val peopleResult = Transformations.map(searchQuery) {
        userRepository.searchUsers(it)
    }
    val people = Transformations.switchMap(peopleResult) { it.pagedList }
    val peopleRefreshState = Transformations.switchMap(peopleResult) { it.refreshState }

    // Chats
    private val chatsResult = Transformations.map(searchQuery) {
        chatRepository.search(it)
    }
    val chats = Transformations.switchMap(chatsResult) { it.pagedList }
    val chatsRefreshState = Transformations.switchMap(chatsResult) { it.refreshState }

    // Channels
    private val channelsResult = Transformations.map(searchQuery) {
        channelRepository.search(it)
    }
    val channels = Transformations.switchMap(channelsResult) { it.pagedList }
    val channelsRefreshState = Transformations.switchMap(channelsResult) { it.refreshState }

    val openDialogEvent = SingleLiveEvent<Long>()
    val openChatEvent = SingleLiveEvent<Long>()
    val openChannelEvent = SingleLiveEvent<Long>()

    val searchTextChangedEvent = MutableLiveData<String>()

    fun search(query: String) {
        searchQuery.postValue(query)
        searchTextChangedEvent.postValue(query)
    }

    fun openDialog(userId: Long) {
        openDialogEvent.postValue(userId)
    }

    fun openChat(chatId: Long) {
        openChatEvent.postValue(chatId)
    }

    fun openChannel(channelId: Long) {
        openChannelEvent.postValue(channelId)
    }

    fun refreshPeople() {
        peopleResult.value?.refresh?.invoke()
    }

    fun refreshChats() {
        chatsResult.value?.refresh?.invoke()
    }

    fun refreshChannels() {
        channelsResult.value?.refresh?.invoke()
    }

    class Factory(
        private val userRepository: UserRepository,
        private val chatRepository: ChatRepository,
        private val channelRepository: ChannelRepository
    ) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return GlobalSearchViewModel(userRepository, chatRepository, channelRepository) as T
        }
    }

}