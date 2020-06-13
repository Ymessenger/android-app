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

import android.util.Log
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.ymessenger.app.R
import org.ymessenger.app.data.remote.entities.Node
import org.ymessenger.app.data.remote.responses.ResultResponse
import org.ymessenger.app.data.repositories.ChatPreviewRepository
import org.ymessenger.app.data.repositories.NodeRepository
import org.ymessenger.app.interfaces.SimpleResultCallback
import org.ymessenger.app.interfaces.TypedSuccessErrorCallback
import org.ymessenger.app.utils.SingleLiveEvent

class NodeInfoViewModel(
    private val nodeRepository: NodeRepository,
    private val chatPreviewRepository: ChatPreviewRepository
) : BaseViewModel() {

    private val refreshTrigger = SingleLiveEvent<Void>()

    val nodeResource = Transformations.switchMap(refreshTrigger) {
        nodeRepository.getCurrentNodeInformation()
    }

    private var nodeToSwitchOn: Node? = null

    val serverIsChangedEvent = SingleLiveEvent<Long>()

    init {
        load()
    }

    private fun load() {
        refreshTrigger.call()
    }

    fun refresh() {
        load()
    }

    fun changeNode(nodeUrl: String, nodeId: Long, userNodeId: Long) {
        startLoading(R.string.changing_server_process)
        nodeRepository.changeNodeGetOperationId(nodeId, object : TypedSuccessErrorCallback<String> {
            override fun success(result: String) {
                changeNodeMoveData(nodeId, nodeUrl, result, userNodeId)
            }

            override fun error(error: ResultResponse) {
                endLoading()
                showErrorFromCode(error.errorCode)
            }
        })
    }

    private fun changeNodeMoveData(nodeId: Long, nodeUrl: String, operationId: String, userNodeId: Long) {
        nodeRepository.switchToOtherNode(nodeUrl, operationId, userNodeId, object : SimpleResultCallback {
            override fun success() {
                endLoading()
                // Logout and login on new server
                serverIsChangedEvent.postValue(nodeId)
                showToast(R.string.server_has_been_changed)
            }

            override fun error() {
                endLoading()
                showError(R.string.failed_to_change_server)
            }
        })
    }

    fun deleteAllChatPreviews() {
        chatPreviewRepository.deleteAllChatPreviews()
    }

    class Factory(
        private val nodeRepository: NodeRepository,
        private val chatPreviewRepository: ChatPreviewRepository
    ) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return NodeInfoViewModel(nodeRepository, chatPreviewRepository) as T
        }
    }

    companion object {
        private const val TAG = "NodeInfoViewModel"
    }
}