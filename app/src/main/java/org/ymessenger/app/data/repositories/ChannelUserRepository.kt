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
import androidx.lifecycle.Transformations
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import org.ymessenger.app.data.local.db.dao.ChannelUserDao
import org.ymessenger.app.data.local.db.dao.UserDao
import org.ymessenger.app.data.local.db.entities.ChannelUser
import org.ymessenger.app.data.local.db.models.ChannelUserModel
import org.ymessenger.app.data.mappers.ChannelUserMapper
import org.ymessenger.app.data.remote.Listing
import org.ymessenger.app.data.remote.NetworkState
import org.ymessenger.app.data.remote.WebSocketService
import org.ymessenger.app.data.remote.requests.AddUsersToChannels
import org.ymessenger.app.data.remote.requests.EditChannelUsers
import org.ymessenger.app.data.remote.requests.GetChannelUsers
import org.ymessenger.app.data.remote.responses.ChannelUsers
import org.ymessenger.app.data.remote.responses.ResultResponse
import org.ymessenger.app.utils.AppExecutors

class ChannelUserRepository private constructor(
    private val executors: AppExecutors,
    private val channelUserDao: ChannelUserDao,
    private val userDao: UserDao,
    private val webSocketService: WebSocketService,
    private val channelUserMapper: ChannelUserMapper
) {

    fun getChannelUsers(channelId: Long): Listing<ChannelUserModel> {
        val boundaryCallback = ChannelUserBoundaryCallback(
            channelId,
            executors,
            webSocketService,
            this::insertChannelUsersToDatabase
        )

        val factory = channelUserDao.getChannelUserModels(channelId)

        val config = PagedList.Config.Builder()
            .setEnablePlaceholders(false)
            .setPageSize(PAGE_SIZE)
            .build()

        val livePagedListBuilder = LivePagedListBuilder<Int, ChannelUserModel>(factory, config)
            .setBoundaryCallback(boundaryCallback)
            .setFetchExecutor(executors.networkIO)
            .build()

        val refreshTrigger = MutableLiveData<Unit>()
        val refreshState = Transformations.switchMap(refreshTrigger) { refresh(channelId) }

        return Listing(
            livePagedListBuilder,
            refresh = {
                refreshTrigger.value = null
            },
            refreshState = refreshState
        )
    }

    /**
     * Refresh all channel users. Firstly it sends request for first page. When result is come it clears database and insert
     * new channel users.
     */
    private fun refresh(channelId: Long): LiveData<NetworkState> {
        val networkState = MutableLiveData<NetworkState>().apply {
            postValue(NetworkState.LOADING)
        }

        deleteChannelUsersFrom(channelId)

        val getChannelUsers = GetChannelUsers(channelId)
        webSocketService.getChannelUsers(
            getChannelUsers,
            object : WebSocketService.ResponseCallback<ChannelUsers> {
                override fun onResponse(response: ChannelUsers) {
                    executors.diskIO.execute {
                        channelUserDao.deleteAllChannelUsers(channelId)
                        insertChannelUsersToDatabase(response)
                        networkState.postValue(NetworkState.LOADED)
                    }
                }

                override fun onError(error: ResultResponse) {
                    Log.e(TAG, "Failed to get data")
                    networkState.postValue(NetworkState.error(error.message))
                }
            })
        return networkState
    }

    fun getChannelUsersByChannel(channelId: Long): LiveData<List<ChannelUser>> {
        val getChannelUsers = GetChannelUsers(channelId)
        webSocketService.getChannelUsers(
            getChannelUsers,
            object : WebSocketService.ResponseCallback<ChannelUsers> {
                override fun onResponse(response: ChannelUsers) {
                    // FIXME: it should work with pagination
                    insertChannelUsersToDatabase(response)
                }

                override fun onError(error: ResultResponse) {
                    // nothing?
                }

            })

        return channelUserDao.getChannelUsersByChannel(channelId)
    }

    fun addUsersToChannels(
        usersId: List<Long>,
        channelsId: List<Long>,
        callback: WebSocketService.ResponseCallback<ChannelUsers>
    ) {
        val addUsersToChannels = AddUsersToChannels(usersId, channelsId)
        webSocketService.addUsersToChannels(
            addUsersToChannels,
            object : WebSocketService.ResponseCallback<ChannelUsers> {
                override fun onResponse(response: ChannelUsers) {
                    // TODO: save channel users to DB
                    callback.onResponse(response)
                }

                override fun onError(error: ResultResponse) {
                    Log.e(TAG, "Failed to add users to channels")
                    callback.onError(error)
                }
            })
    }

    fun editChannelUsers(
        channelUsers: List<org.ymessenger.app.data.remote.entities.ChannelUser>,
        channelId: Long,
        callback: WebSocketService.ResponseCallback<ChannelUsers>
    ) {
        val editChannelUsers = EditChannelUsers(channelUsers, channelId)
        webSocketService.editChannelUsers(
            editChannelUsers,
            object : WebSocketService.ResponseCallback<ChannelUsers> {
                override fun onResponse(response: ChannelUsers) {
                    insertChannelUsersToDatabase(response)
                    callback.onResponse(response)
                }

                override fun onError(error: ResultResponse) {
                    Log.e(TAG, "Failed to edit channel users")
                    callback.onError(error)
                }
            })
    }

    private fun insertChannelUsersToDatabase(channelUsers: ChannelUsers) {
        val channelUsersDb = arrayListOf<ChannelUser>()

        channelUsers.administration?.let {
            for (admin in it) {
                channelUsersDb.add(channelUserMapper.toDb(admin))
            }
        }

        channelUsers.subscribers?.let {
            for (subscriber in it) {
                channelUsersDb.add(channelUserMapper.toDb(subscriber))
            }
        }

        channelUsers.blockedUsers?.let {
            for (blocked in it) {
                channelUsersDb.add(channelUserMapper.toDb(blocked))
            }
        }

        executors.diskIO.execute {
            channelUserDao.insert(channelUsersDb)
        }
    }

    fun deleteChannelUsersFrom(channelId: Long) {
        executors.diskIO.execute {
            channelUserDao.deleteAllChannelUsers(channelId)
        }
    }

    fun deleteChannelUser(channelUser: ChannelUser) {
        executors.diskIO.execute {
            channelUserDao.delete(channelUser)
        }
    }

    companion object {
        private const val TAG = "ChannelUserRepository"
        private var instance: ChannelUserRepository? = null
        private const val PAGE_SIZE = 100

        fun getInstance(
            executors: AppExecutors,
            channelUserDao: ChannelUserDao,
            userDao: UserDao,
            webSocketService: WebSocketService,
            channelUserMapper: ChannelUserMapper
        ) =
            instance ?: synchronized(this) {
                instance
                    ?: ChannelUserRepository(
                        executors,
                        channelUserDao,
                        userDao,
                        webSocketService,
                        channelUserMapper
                    ).also { instance = it }
            }
    }

}