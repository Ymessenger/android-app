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
import org.ymessenger.app.data.remote.requests.GetRandomSequence
import org.ymessenger.app.data.remote.responses.ResultResponse
import org.ymessenger.app.data.remote.responses.Sequence

class RandomSequenceRepository private constructor(
    private val webSocketService: WebSocketService
) {

    fun getRandomSequence(length: Int, callback: GetSequenceCallback) {
        val getRandomSequence = GetRandomSequence(length)
        webSocketService.getRandomSequence(
            getRandomSequence,
            object : WebSocketService.ResponseCallback<Sequence> {
                override fun onResponse(response: Sequence) {
                    Log.d(TAG, "Get random sequence: ${response.sequence}")
                    callback.success(response.sequence)
                }

                override fun onError(error: ResultResponse) {
                    Log.e(TAG, "Failed to get random sequence: ${error.message}")
                    callback.error(error)
                }
            })
    }

    interface GetSequenceCallback {
        fun success(sequence: String)
        fun error(error: ResultResponse)
    }

    companion object {
        private const val TAG = "SequenceRepository"

        private var instance: RandomSequenceRepository? = null

        fun getInstance(
            webSocketService: WebSocketService
        ): RandomSequenceRepository {
            return instance ?: synchronized(this) {
                instance
                    ?: RandomSequenceRepository(
                        webSocketService
                    ).also { instance = it }
            }
        }
    }

}