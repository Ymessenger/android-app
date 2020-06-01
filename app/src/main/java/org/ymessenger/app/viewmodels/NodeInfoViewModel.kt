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

import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.ymessenger.app.data.repositories.NodeRepository
import org.ymessenger.app.utils.SingleLiveEvent

class NodeInfoViewModel(
    private val nodeRepository: NodeRepository
) : BaseViewModel() {

    private val refreshTrigger = SingleLiveEvent<Void>()

    val nodeResource = Transformations.switchMap(refreshTrigger) {
        nodeRepository.getCurrentNodeInformation()
    }

    init {
        load()
    }

    private fun load() {
        refreshTrigger.call()
    }

    fun refresh() {
        load()
    }

    class Factory(
        private val nodeRepository: NodeRepository
    ) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return NodeInfoViewModel(nodeRepository) as T
        }
    }
}