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

package org.ymessenger.app.helpers

import android.util.Log
import androidx.lifecycle.MutableLiveData
import org.ymessenger.app.data.local.db.entities.User
import org.ymessenger.app.data.mappers.UserMapper
import org.ymessenger.app.data.remote.WSResponse
import org.ymessenger.app.data.remote.WebSocketService
import org.ymessenger.app.data.remote.entities.QRCode
import org.ymessenger.app.data.remote.requests.CheckQRCode
import org.ymessenger.app.data.remote.requests.Login
import org.ymessenger.app.data.remote.requests.Logout
import org.ymessenger.app.data.remote.responses.ResultResponse
import org.ymessenger.app.data.remote.responses.Tokens
import org.ymessenger.app.data.repositories.UserRepository
import org.ymessenger.app.utils.SingleLiveEvent

class AuthorizationManager(
    private val webSocketService: WebSocketService,
    private val messageUpdater: MessageUpdater,
    private val settingsHelper: SettingsHelper,
    private val localDataManager: LocalDataManager,
    private val userRepository: UserRepository,
    private val userMapper: UserMapper
) {

    var authorizedUser: User? = null

    var isAuthorized = false
        private set

    val fileAccessTokenLiveData = MutableLiveData<String>()

    var onNeedLoginEvent: (() -> Unit)? = null

    val authorizedEvent = SingleLiveEvent<Void>()

    val userIsBannedEvent = SingleLiveEvent<Void>()

    init {
        webSocketService.onReconnected = {
            tryAuthorize()
        }
    }

    fun tryAuthorize() {
        val token = settingsHelper.getToken()
        if (token != null /*&& !isAuthorized*/) {
            val loginRequest = Login(token)
            authorize(loginRequest, object : Callback {
                override fun authorized() {
                    // We don't need it now, since we're getting user from tokens
//                    getSelf()
                }

                override fun error(error: ResultResponse) {
                    // nothing
                }
            })
        } else {
            Log.w(TAG, "Your token is null")
        }
    }

    private fun getSelf() {
        userRepository.getSelf({
            authorizedUser = it
        }, { error ->
            if (error.errorCode == WSResponse.ErrorCode.PERMISSION_DENIED) {
                Log.e(TAG, "SHOW USER IS BAAAAAAAAAAAAANNEEEEEEEEEED!!!!!!!")
            }
        })
    }

    fun authorize(loginRequest: Login, callback: Callback) {
        loginRequest.deviceTokenId = settingsHelper.getFirebaseToken()
        webSocketService.authorize(
            loginRequest,
            object : WebSocketService.ResponseCallback<Tokens> {
                override fun onResponse(response: Tokens) {
                    Log.d(TAG, "Authorized successfully")
                    isAuthorized = true
                    settingsHelper.setToken(response.token)
                    response.user?.let {
                        userRepository.insert(it)
                        updateSyncContact(it.syncContacts)
                        authorizedUser = userMapper.toDb(it)
                        if (authorizedUser!!.banned) {
                            Log.e(TAG, "SHOW USER IS BAAAAAAAAAAAAANNEEEEEEEEEED!!!!!!!")
                            userIsBannedEvent.call()
                        }
                    }
                    fileAccessTokenLiveData.postValue(response.fileAccessToken)
                    messageUpdater.getMessagesUpdates()
                    authorizedEvent.call()
                    callback.authorized()
                }

                override fun onError(error: ResultResponse) {
                    Log.e(TAG, "Authorization error: ${error.message}")
                    isAuthorized = false
                    if (WSResponse.ErrorCode.INVALID_ACCESS_TOKEN == error.errorCode) {
                        localDataManager.clearAllData()
                        onNeedLoginEvent?.invoke()
                    }
                    callback.error(error)
                }
            })
    }

    fun authorizeWithQR(qrCode: QRCode, callback: Callback) {
        val checkQRCode = CheckQRCode(qrCode, settingsHelper.getFirebaseToken())
        webSocketService.checkQRCode(
            checkQRCode,
            object : WebSocketService.ResponseCallback<Tokens> {
                override fun onResponse(response: Tokens) {
                    Log.d(TAG, "Authorized successfully")
                    isAuthorized = true
                    settingsHelper.setToken(response.token)
                    response.user?.let {
                        userRepository.insert(it)
                        updateSyncContact(it.syncContacts)
                        authorizedUser = userMapper.toDb(it)
                        if (authorizedUser!!.banned) {
                            Log.e(TAG, "SHOW USER IS BAAAAAAAAAAAAANNEEEEEEEEEED!!!!!!!")
                            userIsBannedEvent.call()
                        }
                    }
                    fileAccessTokenLiveData.postValue(response.fileAccessToken)
                    messageUpdater.getMessagesUpdates()
                    authorizedEvent.call()
                    callback.authorized()
                }

                override fun onError(error: ResultResponse) {
                    Log.e(TAG, "Authorization error: ${error.message}")
                    isAuthorized = false
                    if (WSResponse.ErrorCode.INVALID_ACCESS_TOKEN == error.errorCode) {
                        localDataManager.clearAllData()
                        onNeedLoginEvent?.invoke()
                    }
                    callback.error(error)
                }
            })
    }

    private fun updateSyncContact(userSyncContacts: Boolean) {
        Log.d(TAG, "User sync contacts - $userSyncContacts")
        settingsHelper.setSyncContacts(userSyncContacts)
    }

    fun logout(success: () -> Unit, error: () -> Unit) {
        // If we aren't connected to server, just logout
        if (webSocketService.getConnectionStatus().value != true) {
            forceLogout()
            success()
            return
        }

        // Request to logout
        val token = settingsHelper.getToken()
        if (token != null) {
            val logout = Logout(token.accessToken)
            webSocketService.logout(
                logout,
                object : WebSocketService.ResponseCallback<ResultResponse> {
                    override fun onResponse(response: ResultResponse) {
                        isAuthorized = false
                        localDataManager.clearAllData()
                        success()
                    }

                    override fun onError(error: ResultResponse) {
                        Log.e(TAG, "Logout error")
                        error()
                    }
                })
        }
    }

    fun forceLogout() {
        isAuthorized = false
        localDataManager.clearAllData()
    }

    fun getAuthorizedUserId(): Long? {
        return if (isAuthorized) {
            authorizedUser?.id ?: getSavedTokenUserId()
        } else {
            getSavedTokenUserId()
        }
    }

    private fun getSavedTokenUserId(): Long? {
        return settingsHelper.getToken()?.userId
    }

    fun closeOtherSession(tokenIds: List<Long>, callback: ClosingSessionCallback) {
        val logout = Logout(tokensIds = tokenIds)
        webSocketService.logout(logout, object : WebSocketService.ResponseCallback<ResultResponse> {
            override fun onResponse(response: ResultResponse) {
                Log.d(TAG, "Sessions closed")
                callback.closed()
            }

            override fun onError(error: ResultResponse) {
                Log.e(TAG, "Failed to close sessions")
                callback.error()
            }
        })
    }

    interface Callback {
        fun authorized()
        fun error(error: ResultResponse)
    }

    interface ClosingSessionCallback {
        fun closed()
        fun error()
    }

    companion object {
        private const val TAG = "AuthorizationManager"
    }

}