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
import org.ymessenger.app.data.remote.Resource
import org.ymessenger.app.data.remote.WebSocketService
import org.ymessenger.app.data.remote.entities.QRCode
import org.ymessenger.app.data.remote.requests.GetQRCode
import org.ymessenger.app.data.remote.responses.ResultResponse

class QRCodeRepository(
    private val webSocketService: WebSocketService
) {

    fun getQRCode(): LiveData<Resource<QRCode>> {
        val result = MutableLiveData<Resource<QRCode>>(Resource.loading(null))

        val getQRCode = GetQRCode()
        webSocketService.getQRCode(
            getQRCode,
            object :
                WebSocketService.ResponseCallback<org.ymessenger.app.data.remote.responses.QRCode> {
                override fun onResponse(response: org.ymessenger.app.data.remote.responses.QRCode) {
                    result.postValue(Resource.success(response.qrCode))
                }

                override fun onError(error: ResultResponse) {
                    Log.e(TAG, "Failed to get QR code")
                    result.postValue(Resource.error(error.message ?: "Failed to get QR code", null))
                }
            })

        return result
    }

    companion object {
        private const val TAG = "QRCodeRepository"
        private var instance: QRCodeRepository? = null

        fun getInstance(
            webSocketService: WebSocketService
        ): QRCodeRepository {
            return instance ?: synchronized(this) {
                instance
                    ?: QRCodeRepository(
                        webSocketService
                    ).also { instance = it }
            }
        }
    }

}