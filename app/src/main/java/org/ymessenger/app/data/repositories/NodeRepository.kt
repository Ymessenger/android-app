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
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import org.ymessenger.app.data.remote.LicensorWSService
import org.ymessenger.app.data.remote.Listing
import org.ymessenger.app.data.remote.Resource
import org.ymessenger.app.data.remote.WebSocketService
import org.ymessenger.app.data.remote.entities.Node
import org.ymessenger.app.data.remote.licensorRequests.GetNodes
import org.ymessenger.app.data.remote.licensorResponses.Nodes
import org.ymessenger.app.data.remote.requests.GetInformationNode
import org.ymessenger.app.data.remote.requests.SetConnectionEncrypted
import org.ymessenger.app.data.remote.responses.EncryptedKey
import org.ymessenger.app.data.remote.responses.LicensorResultResponse
import org.ymessenger.app.data.remote.responses.ResultResponse
import org.ymessenger.app.utils.AppExecutors

class NodeRepository private constructor(
    private val executors: AppExecutors,
    private val webSocketService: WebSocketService,
    private val licensorWSService: LicensorWSService
) {

    fun getNodes(searchQuery: String?): Listing<Node> {
        val sourceFactory = NodeDataSourceFactory(searchQuery, licensorWSService)

        val config = PagedList.Config.Builder()
            .setEnablePlaceholders(false)
            .setPageSize(30)
            .build()

        val livePagedListBuilder = LivePagedListBuilder<Long, Node>(sourceFactory, config)
            .setFetchExecutor(executors.networkIO)
            .build()

        val refreshState = Transformations.switchMap(sourceFactory.sourceLiveData) {
            it.initialLoad
        }

        return Listing(
            livePagedListBuilder,
            refresh = {
                sourceFactory.sourceLiveData.value?.invalidate()
            },
            refreshState = refreshState
        )
    }

    fun getFirstNodes(): LiveData<List<Node>> {
        val result = MutableLiveData<List<Node>>()

        val getNodes = GetNodes()
        licensorWSService.getNodes(getNodes, object : LicensorWSService.ResponseCallback<Nodes> {
            override fun onResponse(response: Nodes) {
                result.postValue(response.nodes)
            }

            override fun onError(error: LicensorResultResponse) {
                Log.e(TAG, "Failed to get node list")
            }
        })

        return result
    }

    fun getInformationCurrentNode(callback: (Node) -> Unit, errorCallback: () -> Unit) {
        val getInformationNode = GetInformationNode()
        webSocketService.getInformationNode(
            getInformationNode,
            object :
                WebSocketService.ResponseCallback<org.ymessenger.app.data.remote.responses.Nodes> {
                override fun onResponse(response: org.ymessenger.app.data.remote.responses.Nodes) {
                    if (response.nodes.isNotEmpty()) {
                        callback.invoke(response.nodes.first())
                    } else {
                        errorCallback.invoke()
                    }
                }

                override fun onError(error: ResultResponse) {
                    Log.e(TAG, "Failed to get current node information")
                    errorCallback.invoke()
                }
            })
    }

    fun getCurrentNodeInformation(): LiveData<Resource<Node>> {
        val result = MutableLiveData<Resource<Node>>(Resource.loading(null))

        val getInformationNode = GetInformationNode()
        webSocketService.getInformationNode(
            getInformationNode,
            object :
                WebSocketService.ResponseCallback<org.ymessenger.app.data.remote.responses.Nodes> {
                override fun onResponse(response: org.ymessenger.app.data.remote.responses.Nodes) {
                    if (response.nodes.isNotEmpty()) {
                        result.postValue(Resource.success(response.nodes.first()))
                    } else {
                        result.postValue(Resource.error("Nodes list is empty", null))
                    }
                }

                override fun onError(error: ResultResponse) {
                    result.postValue(
                        Resource.error(
                            error.message ?: "Failed to get node information", null
                        )
                    )
                }
            })

        return result
    }

    fun getInformationNode(
        nodesId: List<Long>? = null,
        callback: (List<Node>) -> Unit,
        errorCallback: () -> Unit
    ) {
        val getInformationNode = GetInformationNode(nodesId)
        webSocketService.getInformationNode(
            getInformationNode,
            object :
                WebSocketService.ResponseCallback<org.ymessenger.app.data.remote.responses.Nodes> {
                override fun onResponse(response: org.ymessenger.app.data.remote.responses.Nodes) {
                    callback.invoke(response.nodes)
                }

                override fun onError(error: ResultResponse) {
                    Log.e(TAG, "Failed to get node information")
                    errorCallback.invoke()
                }
            })
    }

    fun setConnectionEncrypted(
        setConnectionEncrypted: SetConnectionEncrypted,
        callback: (encryptedData: String) -> Unit,
        errorCallback: () -> Unit
    ) {
        webSocketService.setConnectionEncrypted(
            setConnectionEncrypted,
            object : WebSocketService.ResponseCallback<EncryptedKey> {
                override fun onResponse(response: EncryptedKey) {
                    callback.invoke(response.encryptedData)
                }

                override fun onError(error: ResultResponse) {
                    Log.e(TAG, "Failed to set encrypted connection")
                    errorCallback.invoke()
                }
            })
    }

    companion object {
        private const val TAG = "NodeRepository2"
        private var instance: NodeRepository? = null

        fun getInstance(
            executors: AppExecutors,
            webSocketService: WebSocketService,
            licensorWSService: LicensorWSService
        ) =
            instance ?: synchronized(this) {
                instance
                    ?: NodeRepository(
                        executors,
                        webSocketService,
                        licensorWSService
                    ).also { instance = it }
            }
    }

}