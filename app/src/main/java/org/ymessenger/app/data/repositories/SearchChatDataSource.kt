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

package org.ymessenger.app.data.repositories

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.paging.ItemKeyedDataSource
import org.ymessenger.app.data.local.db.entities.Chat
import org.ymessenger.app.data.mappers.ChatMapper
import org.ymessenger.app.data.remote.NetworkState
import org.ymessenger.app.data.remote.WebSocketService
import org.ymessenger.app.data.remote.requests.Search
import org.ymessenger.app.data.remote.responses.ResultResponse
import org.ymessenger.app.data.remote.responses.SearchResult

class SearchChatDataSource(
    private val searchQuery: String,
    private val webSocketService: WebSocketService,
    private val chatMapper: ChatMapper
) : ItemKeyedDataSource<Long, Chat>() {

    companion object {
        private const val TAG = "SearchChatDataSource"
    }

    val initialLoad = MutableLiveData<NetworkState>()

    override fun loadInitial(params: LoadInitialParams<Long>, callback: LoadInitialCallback<Chat>) {
        initialLoad.postValue(NetworkState.LOADING)
        val searchUsers = Search(searchQuery, searchTypes = listOf(Search.CHATS))
        webSocketService.search(
            searchUsers,
            object : WebSocketService.ResponseCallback<SearchResult> {
                override fun onResponse(response: SearchResult) {
                    val dbChats = response.chats.map { chatMapper.toDb(it) }
                    initialLoad.postValue(NetworkState.LOADED)
                    callback.onResult(dbChats)
                }

                override fun onError(error: ResultResponse) {
                    Log.e(TAG, "Failed to search chats")
                    val networkState = NetworkState.error(error.message ?: "unknown error")
                    initialLoad.postValue(networkState)
                }
            })
    }

    override fun loadAfter(params: LoadParams<Long>, callback: LoadCallback<Chat>) {
        val searchUsers = Search(searchQuery, params.key, searchTypes = listOf(Search.CHATS))
        webSocketService.search(
            searchUsers,
            object : WebSocketService.ResponseCallback<SearchResult> {
                override fun onResponse(response: SearchResult) {
                    val dbChats = response.chats.map { chatMapper.toDb(it) }
                    callback.onResult(dbChats)
                }

                override fun onError(error: ResultResponse) {
                    Log.e(TAG, "Failed to search chats")
                }
            })
    }

    override fun loadBefore(params: LoadParams<Long>, callback: LoadCallback<Chat>) {
        // ignored
    }

    override fun getKey(item: Chat): Long {
        return item.id
    }
}