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
import org.ymessenger.app.data.local.db.dao.UserActionDao
import org.ymessenger.app.data.local.db.entities.UserAction
import org.ymessenger.app.data.local.db.models.UserActionModel
import org.ymessenger.app.data.remote.WebSocketService
import org.ymessenger.app.data.remote.requests.SendUserAction
import org.ymessenger.app.data.remote.responses.ResultResponse

class UserActionRepository private constructor(
    private val userActionDao: UserActionDao,
    private val webSocketService: WebSocketService
) {

    fun insert(userAction: UserAction) {
        userActionDao.insert(userAction)
    }

    fun getLastUserActions(
        conversationId: Long,
        conversationType: Int,
        currentTime: Long
    ): LiveData<List<UserAction>> {
        return userActionDao.getLastUserActions(conversationId, conversationType, currentTime)
    }

    fun getLastUserActionModels(
        conversationId: Long,
        conversationType: Int,
        currentTime: Long
    ): LiveData<List<UserActionModel>> {
        return userActionDao.getLastUserActionModels(conversationId, conversationType, currentTime)
    }

    fun getLastUserActionModels(
        currentTime: Long
    ): LiveData<List<UserActionModel>> {
        return userActionDao.getLastUserActionModels(currentTime)
    }

    fun sendUserAction(
        conversationId: Long,
        conversationType: Int,
        action: Int,
        success: () -> Unit
    ) {
        val sendUserAction = SendUserAction(conversationId, conversationType, action)
        webSocketService.sendUserAction(
            sendUserAction,
            object : WebSocketService.ResponseCallback<ResultResponse> {
                override fun onResponse(response: ResultResponse) {
                    Log.d(TAG, "User action was sent")
                    success.invoke()
                }

                override fun onError(error: ResultResponse) {
                    Log.e(TAG, "Failed to send user action")
                }
            })
    }

    companion object {
        private const val TAG = "UserActionRepository"

        private var instance: UserActionRepository? = null

        fun getInstance(
            userActionDao: UserActionDao,
            webSocketService: WebSocketService
        ): UserActionRepository {
            return instance ?: synchronized(this) {
                instance
                    ?: UserActionRepository(
                        userActionDao,
                        webSocketService
                    ).also { instance = it }
            }
        }
    }

}