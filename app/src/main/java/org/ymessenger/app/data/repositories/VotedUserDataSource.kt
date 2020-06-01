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
import androidx.lifecycle.MutableLiveData
import androidx.paging.ItemKeyedDataSource
import org.ymessenger.app.data.local.db.dao.ContactDao
import org.ymessenger.app.data.mappers.UserMapper
import org.ymessenger.app.data.remote.NetworkState
import org.ymessenger.app.data.remote.WebSocketService
import org.ymessenger.app.data.remote.entities.PollResults
import org.ymessenger.app.data.remote.requests.GetPollVotedUsers
import org.ymessenger.app.data.remote.responses.PollResult
import org.ymessenger.app.data.remote.responses.ResultResponse
import org.ymessenger.app.models.PollVotedUser
import org.ymessenger.app.utils.AppExecutors

class VotedUserDataSource(
    private val pollId: String,
    private val optionId: Int,
    private val conversationId: Long,
    private val conversationType: Int,
    private val signRequired: Boolean,
    private val webSocketService: WebSocketService,
    private val userMapper: UserMapper,
    private val contactDao: ContactDao
) : ItemKeyedDataSource<Long, PollVotedUser>() {

    companion object {
        private const val TAG = "VotedUserDataSource"
    }

    val initialLoad = MutableLiveData<NetworkState>()

    override fun loadInitial(
        params: LoadInitialParams<Long>,
        callback: LoadInitialCallback<PollVotedUser>
    ) {
        initialLoad.postValue(NetworkState.LOADING)
        val getPollVotedUsers =
            GetPollVotedUsers(pollId, optionId, conversationId, conversationType)
        webSocketService.getPollVotedUsers(
            getPollVotedUsers,
            object : WebSocketService.ResponseCallback<PollResult> {
                override fun onResponse(response: PollResult) {
                    val data = getConvertedData(response.pollResults)

                    AppExecutors.getInstance().diskIO.execute {
                        for (item in data) {
                            item.contact = contactDao.getContactByUserId(item.user.id)
                        }
                        initialLoad.postValue(NetworkState.LOADED)
                        callback.onResult(data)
                    }
                }

                override fun onError(error: ResultResponse) {
                    Log.e(TAG, "Failed to get voted users")
                    val networkState = NetworkState.error(error.message ?: "unknown error")
                    initialLoad.postValue(networkState)
                }
            })
    }

    override fun loadAfter(params: LoadParams<Long>, callback: LoadCallback<PollVotedUser>) {
        val getPollVotedUsers =
            GetPollVotedUsers(pollId, optionId, conversationId, conversationType, params.key)
        webSocketService.getPollVotedUsers(
            getPollVotedUsers,
            object : WebSocketService.ResponseCallback<PollResult> {
                override fun onResponse(response: PollResult) {
                    val data = getConvertedData(response.pollResults)
                    AppExecutors.getInstance().diskIO.execute {
                        for (item in data) {
                            item.contact = contactDao.getContactByUserId(item.user.id)
                        }

                        callback.onResult(data)
                    }
                }

                override fun onError(error: ResultResponse) {
                    Log.e(TAG, "Failed to get voted users")
                }
            })
    }

    override fun loadBefore(params: LoadParams<Long>, callback: LoadCallback<PollVotedUser>) {
        // ignored
    }

    override fun getKey(item: PollVotedUser): Long {
        return item.user.id
    }

    private fun getConvertedData(pollResults: List<PollResults>): List<PollVotedUser> {
        val result = arrayListOf<PollVotedUser>()

        result.addAll(pollResults.map {
            PollVotedUser(
                userMapper.toDb(it.user),
                it.sign,
                signRequired = signRequired,
                signVerified = false
            )
        })

        return result
    }
}