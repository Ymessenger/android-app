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
import androidx.paging.PagedList
import org.ymessenger.app.data.remote.WebSocketService
import org.ymessenger.app.data.remote.entities.User
import org.ymessenger.app.data.remote.requests.GetAllUsersInformationNode
import org.ymessenger.app.data.remote.responses.ResultResponse
import org.ymessenger.app.data.remote.responses.Users
import org.ymessenger.app.utils.AppExecutors

class UserBoundaryCallback(
    private val executors: AppExecutors,
    private val webSocketService: WebSocketService,
    private val handleResponse: (List<User>) -> Unit
) : PagedList.BoundaryCallback<org.ymessenger.app.data.local.db.entities.User>() {

    private val helper = PagingRequestHelper(executors.networkIO)

    override fun onZeroItemsLoaded() {
        Log.d(TAG, "onZeroItemsLoaded")
        super.onZeroItemsLoaded()
        helper.runIfNotRunning(PagingRequestHelper.RequestType.INITIAL) { helperCallback ->
            val getAllUsersInformationNode = GetAllUsersInformationNode()
            webSocketService.getAllUsersInformationNode(
                getAllUsersInformationNode,
                createWebserviceCallback(helperCallback)
            )
        }
    }

    override fun onItemAtEndLoaded(itemAtEnd: org.ymessenger.app.data.local.db.entities.User) {
        Log.d(TAG, "onItemAtEndLoaded")
        super.onItemAtEndLoaded(itemAtEnd)
        helper.runIfNotRunning(PagingRequestHelper.RequestType.AFTER) { helperCallback ->
            val getAllUsersInformationNode =
                GetAllUsersInformationNode(navigationUserId = itemAtEnd.id)
            webSocketService.getAllUsersInformationNode(
                getAllUsersInformationNode,
                createWebserviceCallback(helperCallback)
            )
        }
    }

    private fun insertItemsIntoDb(
        items: List<User>,
        helperCallback: PagingRequestHelper.Request.Callback
    ) {
        executors.diskIO.execute {
            handleResponse.invoke(items)
            helperCallback.recordSuccess()
        }
    }

    private fun createWebserviceCallback(helperCallback: PagingRequestHelper.Request.Callback):
            WebSocketService.ResponseCallback<Users> {
        return object : WebSocketService.ResponseCallback<Users> {
            override fun onResponse(response: Users) {
                insertItemsIntoDb(response.users, helperCallback)
            }

            override fun onError(error: ResultResponse) {
                Log.e(TAG, "Failed to fetch data")
                helperCallback.recordFailure(Throwable("Failed to fetch data"))
            }
        }
    }

    companion object {
        private const val TAG = "UserBoundaryCallback"
    }
}