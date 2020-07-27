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
import org.ymessenger.app.helpers.NodeManager
import org.ymessenger.app.helpers.SettingsHelper
import org.ymessenger.app.utils.SingleLiveEvent

class EnterViewModel(
    private val settingsHelper: SettingsHelper,
    private val nodeRepository: NodeRepository,
    private val nodeManager: NodeManager
) : BaseViewModel() {

    private val getNodesEvent = MutableLiveData<Void>()
    private val nodes = Transformations.switchMap(getNodesEvent) {
        nodeRepository.getFirstNodes()
    }

    val hasTokenEvent = SingleLiveEvent<Void>()
    val authorizationErrorEvent = SingleLiveEvent<String>()
    val showIntroEvent = SingleLiveEvent<Void>()
    val openSetPassphraseEvent = SingleLiveEvent<Void>()

    init {
        if (settingsHelper.isFirstLaunch() || settingsHelper.getYEncryptPass() == null && settingsHelper.getSavePassphrase()) {
            openSetPassphraseEvent.call()
            settingsHelper.setAskedOldUsersToSetPassphrase()
        }

        checkAuthorization()

        nodes.observeForever {
            if (!nodeManager.hasNode()) {
                autoSelectNode(it)
            }
        }
    }

    private fun autoSelectNode(nodes: List<Node>) {
        nodeManager.autoSelectNode(nodes)
    }

    private fun checkAuthorization() {
        val token = settingsHelper.getToken()
        if (token != null) {
            hasTokenEvent.call()
        } else {
            if (settingsHelper.isFirstLaunch()) {
                settingsHelper.setFirstLaunch()
                showIntroEvent.call()
            }
            if (!nodeManager.hasNode()) {
                getNodesEvent.value = null
            }
        }
    }

    class Factory(
        private val settingsHelper: SettingsHelper,
        private val nodeRepository: NodeRepository,
        private val nodeManager: NodeManager
    ) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return EnterViewModel(settingsHelper, nodeRepository, nodeManager) as T
        }
    }

}