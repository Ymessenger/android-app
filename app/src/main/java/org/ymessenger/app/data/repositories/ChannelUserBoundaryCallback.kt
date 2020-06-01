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
import org.ymessenger.app.data.local.db.models.ChannelUserModel
import org.ymessenger.app.data.remote.WebSocketService
import org.ymessenger.app.data.remote.requests.GetChannelUsers
import org.ymessenger.app.data.remote.responses.ChannelUsers
import org.ymessenger.app.data.remote.responses.ResultResponse
import org.ymessenger.app.utils.AppExecutors

class ChannelUserBoundaryCallback(
    private val channelId: Long,
    private val executors: AppExecutors,
    private val webSocketService: WebSocketService,
    private val handleResponse: (ChannelUsers) -> Unit
) : PagedList.BoundaryCallback<ChannelUserModel>() {

    private val helper = PagingRequestHelper(executors.networkIO)

    override fun onZeroItemsLoaded() {
        Log.d(TAG, "onZeroItemsLoaded")
        super.onZeroItemsLoaded()
        helper.runIfNotRunning(PagingRequestHelper.RequestType.INITIAL) { helperCallback ->
            val getChannelUsers = GetChannelUsers(channelId)
            webSocketService.getChannelUsers(
                getChannelUsers,
                createWebserviceCallback(helperCallback)
            )
        }
    }

    override fun onItemAtEndLoaded(itemAtEnd: ChannelUserModel) {
        Log.d(TAG, "onItemAtEndLoaded")
        super.onItemAtEndLoaded(itemAtEnd)
        helper.runIfNotRunning(PagingRequestHelper.RequestType.AFTER) { helperCallback ->
            val getChannelUsers =
                GetChannelUsers(channelId, navigationUserId = itemAtEnd.channelUser.userId)
            webSocketService.getChannelUsers(
                getChannelUsers,
                createWebserviceCallback(helperCallback)
            )
        }
    }

    private fun insertItemsIntoDb(
        response: ChannelUsers,
        helperCallback: PagingRequestHelper.Request.Callback
    ) {
        executors.diskIO.execute {
            handleResponse.invoke(response)
            helperCallback.recordSuccess()
        }
    }

    private fun createWebserviceCallback(helperCallback: PagingRequestHelper.Request.Callback):
            WebSocketService.ResponseCallback<ChannelUsers> {
        return object : WebSocketService.ResponseCallback<ChannelUsers> {
            override fun onResponse(response: ChannelUsers) {
                insertItemsIntoDb(response, helperCallback)
            }

            override fun onError(error: ResultResponse) {
                Log.e(TAG, "Failed to fetch data")
                helperCallback.recordFailure(Throwable("Failed to fetch data"))
            }
        }
    }

    companion object {
        private const val TAG = "ChannelUserBoundary"
    }
}