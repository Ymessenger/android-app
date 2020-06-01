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
import org.ymessenger.app.data.remote.entities.Session
import org.ymessenger.app.data.remote.requests.GetSessions
import org.ymessenger.app.data.remote.responses.ResultResponse
import org.ymessenger.app.data.remote.responses.Sessions

class SessionRepository private constructor(
    private val webSocketService: WebSocketService
) {

    fun getSessions(): LiveData<Resource<List<Session>>> {
        val sessionsResult = MutableLiveData<Resource<List<Session>>>()

        sessionsResult.postValue(Resource.loading(null))
        val getSessions = GetSessions()
        webSocketService.getSessions(
            getSessions,
            object : WebSocketService.ResponseCallback<Sessions> {
                override fun onResponse(response: Sessions) {
                    sessionsResult.postValue(Resource.success(response.sessions))
                }

                override fun onError(error: ResultResponse) {
                    Log.e(TAG, "Failed to get sessions. Message: ${error.message}")
                    sessionsResult.postValue(
                        Resource.error(
                            "Failed to get sessions: ${error.message}",
                            null
                        )
                    )
                }
            })

        return sessionsResult
    }

    companion object {
        private const val TAG = "SessionRepository"

        private var instance: SessionRepository? = null

        fun getInstance(
            webSocketService: WebSocketService
        ): SessionRepository {
            return instance ?: synchronized(this) {
                instance
                    ?: SessionRepository(
                        webSocketService
                    ).also { instance = it }
            }
        }
    }

}