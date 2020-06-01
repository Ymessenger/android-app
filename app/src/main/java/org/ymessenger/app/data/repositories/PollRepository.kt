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
import androidx.lifecycle.Transformations
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import org.ymessenger.app.data.local.db.dao.ContactDao
import org.ymessenger.app.data.mappers.UserMapper
import org.ymessenger.app.data.remote.Listing
import org.ymessenger.app.data.remote.WebSocketService
import org.ymessenger.app.data.remote.requests.Polling
import org.ymessenger.app.data.remote.responses.Polls
import org.ymessenger.app.data.remote.responses.ResultResponse
import org.ymessenger.app.interfaces.SuccessErrorCallback
import org.ymessenger.app.models.PollVotedUser
import org.ymessenger.app.utils.AppExecutors

class PollRepository(
    private val executors: AppExecutors,
    private val webSocketService: WebSocketService,
    private val userMapper: UserMapper,
    private val contactDao: ContactDao
) {

    fun votePoll(polling: Polling, callback: SuccessErrorCallback) {
        webSocketService.polling(polling, object : WebSocketService.ResponseCallback<Polls> {
            override fun onResponse(response: Polls) {
                Log.d(TAG, "voted successfully")
                callback.success()
                // TODO: save a poll as Room object?
            }

            override fun onError(error: ResultResponse) {
                Log.e(TAG, "failed to vote a poll")
                callback.error(error)
            }
        })
    }

    fun getVotedUserList(
        pollId: String,
        optionId: Int,
        conversationId: Long,
        conversationType: Int,
        signRequired: Boolean
    ): Listing<PollVotedUser> {
        val sourceFactory = VotedUserDataSourceFactory(
            pollId,
            optionId,
            conversationId,
            conversationType,
            signRequired,
            webSocketService,
            userMapper,
            contactDao
        )

        val config = PagedList.Config.Builder()
            .setEnablePlaceholders(false)
            .setPageSize(PAGE_SIZE)
            .build()

        val livePagedListBuilder = LivePagedListBuilder<Long, PollVotedUser>(sourceFactory, config)
            .setFetchExecutor(executors.networkIO)
            .build()

        val refreshState = Transformations.switchMap(sourceFactory.sourceLiveData) {
            it.initialLoad
        }

        return Listing(
            livePagedListBuilder,
            refresh = {
                sourceFactory.sourceLiveData.value?.invalidate()
            },
            refreshState = refreshState
        )
    }

    companion object {
        private const val TAG = "PollRepository"
        private var instance: PollRepository? = null
        private const val PAGE_SIZE = 100

        fun getInstance(
            executors: AppExecutors,
            webSocketService: WebSocketService,
            userMapper: UserMapper,
            contactDao: ContactDao
        ): PollRepository {
            return instance ?: synchronized(this) {
                instance
                    ?: PollRepository(
                        executors,
                        webSocketService,
                        userMapper,
                        contactDao
                    ).also { instance = it }
            }
        }
    }

}