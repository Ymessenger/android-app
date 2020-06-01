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
import org.ymessenger.app.data.remote.WebSocketService
import org.ymessenger.app.data.remote.requests.VerificationUser
import org.ymessenger.app.data.remote.responses.ResultResponse
import org.ymessenger.app.interfaces.SuccessErrorCallback

class VerificationRepository(
    private val webSocketService: WebSocketService
) {

    fun sendVCodeToPhone(phone: String, isRegistration: Boolean, callback: SuccessErrorCallback) {
        val verificationUser = VerificationUser(VerificationUser.TYPE_PHONE, phone, isRegistration)
        sendVCode(verificationUser, callback)
    }

    fun sendVCodeToEmail(email: String, isRegistration: Boolean, callback: SuccessErrorCallback) {
        val verificationUser = VerificationUser(VerificationUser.TYPE_EMAIL, email, isRegistration)
        sendVCode(verificationUser, callback)
    }

    private fun sendVCode(verificationUser: VerificationUser, callback: SuccessErrorCallback) {
        webSocketService.verificationUser(
            verificationUser,
            object : WebSocketService.ResponseCallback<ResultResponse> {
                override fun onResponse(response: ResultResponse) {
                    // Verification code was sent
                    callback.success()
                }

                override fun onError(error: ResultResponse) {
                    Log.e(TAG, "Failed to send verification code")
                    callback.error(error)
                }
            })
    }

    companion object {
        private const val TAG = "VerificationRepository"
        private var instance: VerificationRepository? = null

        fun getInstance(
            webSocketService: WebSocketService
        ): VerificationRepository {
            return instance ?: synchronized(this) {
                instance
                    ?: VerificationRepository(
                        webSocketService
                    ).also { instance = it }
            }
        }
    }

}