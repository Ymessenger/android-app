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
import org.ymessenger.app.data.remote.LicensorWSService
import org.ymessenger.app.data.remote.NetworkState
import org.ymessenger.app.data.remote.entities.Node
import org.ymessenger.app.data.remote.licensorRequests.GetNodes
import org.ymessenger.app.data.remote.licensorResponses.Nodes
import org.ymessenger.app.data.remote.responses.LicensorResultResponse

class NodeDataSource(
    private val searchQuery: String?,
    private val licensorWSService: LicensorWSService
) : ItemKeyedDataSource<Long, Node>() {

    companion object {
        private const val TAG = "NodeDataSource"
    }

    val initialLoad = MutableLiveData<NetworkState>()

    override fun loadInitial(params: LoadInitialParams<Long>, callback: LoadInitialCallback<Node>) {
        initialLoad.postValue(NetworkState.LOADING)
        val getNodes =
            GetNodes(searchQuery = searchQuery)
        licensorWSService.getNodes(
            getNodes,
            object : LicensorWSService.ResponseCallback<Nodes> {
                override fun onResponse(response: Nodes) {
                    initialLoad.postValue(NetworkState.LOADED)
                    callback.onResult(response.nodes)
                }

                override fun onError(error: LicensorResultResponse) {
                    Log.e(TAG, "Failed to get nodes")
                    val networkState = NetworkState.error(error.errorMessage ?: "unknown error")
                    initialLoad.postValue(networkState)
                }
            })
    }

    override fun loadAfter(params: LoadParams<Long>, callback: LoadCallback<Node>) {
        val getNodes = GetNodes(
            searchQuery = searchQuery,
            nodeId = params.key
        )
        licensorWSService.getNodes(
            getNodes,
            object : LicensorWSService.ResponseCallback<Nodes> {
                override fun onResponse(response: Nodes) {
                    callback.onResult(response.nodes)
                }

                override fun onError(error: LicensorResultResponse) {
                    Log.e(TAG, "Failed to get nodes")
                }
            })
    }

    override fun loadBefore(params: LoadParams<Long>, callback: LoadCallback<Node>) {
        // ignored
    }

    override fun getKey(item: Node): Long {
        return item.id
    }
}