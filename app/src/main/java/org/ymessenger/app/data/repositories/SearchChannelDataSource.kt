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
import org.ymessenger.app.data.local.db.entities.Channel
import org.ymessenger.app.data.mappers.ChannelMapper
import org.ymessenger.app.data.remote.NetworkState
import org.ymessenger.app.data.remote.WebSocketService
import org.ymessenger.app.data.remote.requests.Search
import org.ymessenger.app.data.remote.responses.ResultResponse
import org.ymessenger.app.data.remote.responses.SearchResult

class SearchChannelDataSource(
    private val searchQuery: String,
    private val webSocketService: WebSocketService,
    private val channelMapper: ChannelMapper
) : ItemKeyedDataSource<Long, Channel>() {

    companion object {
        private const val TAG = "SearchChannelDataSource"
    }

    val initialLoad = MutableLiveData<NetworkState>()

    override fun loadInitial(
        params: LoadInitialParams<Long>,
        callback: LoadInitialCallback<Channel>
    ) {
        initialLoad.postValue(NetworkState.LOADING)
        val searchUsers = Search(searchQuery, searchTypes = listOf(Search.CHANNELS))
        webSocketService.search(
            searchUsers,
            object : WebSocketService.ResponseCallback<SearchResult> {
                override fun onResponse(response: SearchResult) {
                    val dbChannels = response.channels.map { channelMapper.toDb(it) }
                    initialLoad.postValue(NetworkState.LOADED)
                    callback.onResult(dbChannels)
                }

                override fun onError(error: ResultResponse) {
                    Log.e(TAG, "Failed to search channels")
                    val networkState = NetworkState.error(error.message ?: "unknown error")
                    initialLoad.postValue(networkState)
                }
            })
    }

    override fun loadAfter(params: LoadParams<Long>, callback: LoadCallback<Channel>) {
        val searchUsers = Search(searchQuery, params.key, searchTypes = listOf(Search.CHANNELS))
        webSocketService.search(
            searchUsers,
            object : WebSocketService.ResponseCallback<SearchResult> {
                override fun onResponse(response: SearchResult) {
                    val dbChannels = response.channels.map { channelMapper.toDb(it) }
                    callback.onResult(dbChannels)
                }

                override fun onError(error: ResultResponse) {
                    Log.e(TAG, "Failed to search channels")
                }
            })
    }

    override fun loadBefore(params: LoadParams<Long>, callback: LoadCallback<Channel>) {
        // ignored
    }

    override fun getKey(item: Channel): Long {
        return item.id
    }
}