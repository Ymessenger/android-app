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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.gson.Gson
import org.ymessenger.app.R
import org.ymessenger.app.data.remote.WSResponse
import org.ymessenger.app.data.remote.entities.QRCode
import org.ymessenger.app.data.remote.responses.ResultResponse
import org.ymessenger.app.helpers.AuthorizationManager
import org.ymessenger.app.helpers.NodeManager
import org.ymessenger.app.utils.SingleLiveEvent

class QRLoginViewModel(
    private val authorizationManager: AuthorizationManager,
    private val nodeManager: NodeManager
) : BaseViewModel() {

    val loading = MutableLiveData<Boolean>()

    private var isConnected: Boolean = false

    val authorizedEvent = SingleLiveEvent<Void>()

    fun processQRCode(data: String) {
        val gson = Gson()
        try {
            val qrCode: QRCode = gson.fromJson(data, QRCode::class.java)
            checkNode(qrCode)
        } catch (e: Exception) {
            showError(R.string.wrong_qr_code)
        }
    }

    private fun checkNode(qrCode: QRCode) {
        if (nodeManager.getCurrentNode().value?.id == qrCode.nodeId) {
            authorize(qrCode)
        } else {
            switchToRequiredNode(qrCode)
        }
    }

    private fun switchToRequiredNode(qrCode: QRCode) {
        startLoading(R.string.connecting_to_required_server)
        nodeManager.switchToNode(qrCode.nodeId, {
            endLoading()
            authorize(qrCode)
        }, {
            endLoading()
            showError(R.string.required_node_is_not_available)
        })
    }

    private fun authorize(qrCode: QRCode) {
        if (!hasConnection()) return

        startLoading(R.string.signing_in)
        authorizationManager.authorizeWithQR(qrCode, object : AuthorizationManager.Callback {
            override fun authorized() {
                endLoading()
                authorizedEvent.call()
            }

            override fun error(error: ResultResponse) {
                endLoading()
                if (WSResponse.ErrorCode.CHECK_QR_PROBLEM == error.errorCode) {
                    showError(R.string.qr_code_was_not_found)
                } else {
                    showError(R.string.authorization_error)
                }
            }
        })
    }

    fun setConnectionStatus(isConnected: Boolean) {
        this.isConnected = isConnected
    }

    private fun hasConnection(): Boolean {
        if (!isConnected) {
            showError(R.string.connection_is_lost_try_later)
        }

        return isConnected
    }

    class Factory(
        private val authorizationManager: AuthorizationManager,
        private val nodeManager: NodeManager
    ) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return QRLoginViewModel(
                authorizationManager,
                nodeManager
            ) as T
        }
    }

}