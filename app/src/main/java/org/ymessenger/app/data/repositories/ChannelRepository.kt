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
import androidx.lifecycle.Transformations
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import org.ymessenger.app.data.ConversationType
import org.ymessenger.app.data.local.db.dao.ChannelDao
import org.ymessenger.app.data.local.db.entities.Channel
import org.ymessenger.app.data.mappers.ChannelMapper
import org.ymessenger.app.data.remote.Listing
import org.ymessenger.app.data.remote.WSResponse
import org.ymessenger.app.data.remote.WebSocketService
import org.ymessenger.app.data.remote.requests.CreateChannel
import org.ymessenger.app.data.remote.requests.DeleteConversation
import org.ymessenger.app.data.remote.requests.EditChannel
import org.ymessenger.app.data.remote.requests.GetChannels
import org.ymessenger.app.data.remote.responses.Channels
import org.ymessenger.app.data.remote.responses.ResultResponse
import org.ymessenger.app.utils.AppExecutors

class ChannelRepository private constructor(
    private val executors: AppExecutors,
    private val channelDao: ChannelDao,
    private val webSocketService: WebSocketService,
    private val channelMapper: ChannelMapper
) {

    fun getChannels() = channelDao.getChannels()

    fun getChannel(channelId: Long): LiveData<Channel> {
        getChannelsFromServer(listOf(channelId))

        return channelDao.getChannel(channelId)
    }

    fun getChannelSync(channelId: Long) = channelDao.getChannelSync(channelId)

    fun insert(channel: Channel) {
        executors.diskIO.execute {
            channelDao.insert(channel)
        }
    }

    fun update(channel: Channel) {
        executors.diskIO.execute {
            channelDao.update(channel)
        }
    }

    fun delete(channel: Channel) {
        executors.diskIO.execute {
            channelDao.delete(channel)
        }
    }

    fun createChannel(createChannel: CreateChannel, createChannelCallback: CreateChannelCallback) {
        webSocketService.createChannel(
            createChannel,
            object : WebSocketService.ResponseCallback<Channels> {
                override fun onResponse(response: Channels) {
                    val dbChannels = response.channels.map { channelMapper.toDb(it) }

                    executors.diskIO.execute {
                        channelDao.insert(dbChannels)
                    }

                    if (dbChannels.isNotEmpty()) {
                        createChannelCallback.created(dbChannels.first().id)
                    }
                }

                override fun onError(error: ResultResponse) {
                    Log.e(TAG, "error while creating channel. Code ${error.errorCode}")
                    createChannelCallback.error()
                }

            })
    }

    fun getChannelsFromServer(channelsId: List<Long>) {
        val getChannels = GetChannels(channelsId)
        webSocketService.getChannels(
            getChannels,
            object : WebSocketService.ResponseCallback<Channels> {
                override fun onResponse(response: Channels) {
                    val dbChannels = response.channels.map { channelMapper.toDb(it) }
                    executors.diskIO.execute {
                        if (dbChannels.isEmpty()) {
                            channelDao.setDeleted(channelsId)
                        } else {
                            channelDao.insert(dbChannels)
                        }
                    }
                }

                override fun onError(error: ResultResponse) {
                    Log.e(TAG, "Failed to get channels from server")

                    if (error.errorCode != WSResponse.ErrorCode.USER_NOT_AUTHORIZED) {
                        executors.diskIO.execute {
                            channelDao.setDeleted(channelsId)
                        }
                    }
                }
            })
    }

    fun deleteChannel(channelId: Long, deleteChannelCallback: DeleteChannelCallback) {
        val deleteConversation = DeleteConversation(channelId, ConversationType.CHANNEL)
        webSocketService.deleteConversation(
            deleteConversation,
            object : WebSocketService.ResponseCallback<WSResponse> {
                override fun onResponse(response: WSResponse) {
                    Log.d(TAG, "Channel has been successfully deleted")
                    executors.diskIO.execute {
                        channelDao.deleteChannel(channelId)
                    }
                    deleteChannelCallback.deleted()
                }

                override fun onError(error: ResultResponse) {
                    Log.e(TAG, "Failed to delete channel")
                    deleteChannelCallback.error(error.errorCode)
                }
            })
    }

    interface CreateChannelCallback {
        fun created(channelId: Long)
        fun error()
    }

    interface DeleteChannelCallback {
        fun deleted()
        fun error(errorCode: Int)
    }

    interface EditChannelCallback {
        fun edited()
        fun error(error: ResultResponse)
    }

    fun editChannel(editChannel: EditChannel, editChannelCallback: EditChannelCallback) {
        webSocketService.editChannel(
            editChannel,
            object : WebSocketService.ResponseCallback<Channels> {
                override fun onResponse(response: Channels) {
                    val dbChannels = response.channels.map { channelMapper.toDb(it) }
                    executors.diskIO.execute {
                        channelDao.insert(dbChannels)
                    }
                    editChannelCallback.edited()
                }

                override fun onError(error: ResultResponse) {
                    Log.e(TAG, "Failed to edit channel")
                    editChannelCallback.error(error)
                }
            })
    }

    fun search(searchQuery: String): Listing<Channel> {
        val sourceFactory =
            SearchChannelDataSourceFactory(searchQuery, webSocketService, channelMapper)

        val config = PagedList.Config.Builder()
            .setEnablePlaceholders(false)
            .setPageSize(PAGE_SIZE)
            .build()

        val livePagedListBuilder = LivePagedListBuilder<Long, Channel>(sourceFactory, config)
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

    fun deleteAllLocally() {
        executors.diskIO.execute {
            channelDao.deleteAll()
        }
    }

    companion object {
        private const val TAG = "ChannelRepository"
        private var instance: ChannelRepository? = null
        private const val PAGE_SIZE = 100

        fun getInstance(
            executors: AppExecutors,
            channelDao: ChannelDao,
            webSocketService: WebSocketService,
            channelMapper: ChannelMapper
        ) =
            instance ?: synchronized(this) {
                instance
                    ?: ChannelRepository(
                        executors,
                        channelDao,
                        webSocketService,
                        channelMapper
                    ).also { instance = it }
            }
    }

}