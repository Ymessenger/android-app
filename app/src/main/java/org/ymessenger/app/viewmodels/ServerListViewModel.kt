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
import org.ymessenger.app.data.remote.entities.Node
import org.ymessenger.app.data.repositories.NodeRepository

class ServerListViewModel(
    private val nodeRepository: NodeRepository
) : ViewModel() {

    private val searchQuery = MutableLiveData<String?>(null)

    private val nodeListResult = Transformations.map(searchQuery) {
        nodeRepository.getNodes(it)
    }

    private val nodeList = Transformations.switchMap(nodeListResult) {
        it.pagedList
    }

    val nodeRefreshState = Transformations.switchMap(nodeListResult) {
        it.refreshState
    }

    private val currentNode = MutableLiveData<Node>()

    fun getNodes() = nodeList

    fun getCurrentNode() = currentNode

    fun setCurrentNode(node: Node) {
        currentNode.value = node
    }

    fun search(query: String?) {
        searchQuery.postValue(query)
    }

    fun refresh() {
        nodeListResult.value?.refresh?.invoke()
    }

    class Factory(
        private val nodeRepository: NodeRepository
    ) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return ServerListViewModel(nodeRepository) as T
        }
    }
}