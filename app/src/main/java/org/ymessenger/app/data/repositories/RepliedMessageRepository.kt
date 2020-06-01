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
import org.ymessenger.app.data.local.db.dao.RepliedMessageDao
import org.ymessenger.app.data.local.db.models.MessageModel
import org.ymessenger.app.data.mappers.RepliedMessageMapper
import org.ymessenger.app.data.remote.WebSocketService
import org.ymessenger.app.data.remote.requests.GetMessages
import org.ymessenger.app.data.remote.responses.Messages
import org.ymessenger.app.data.remote.responses.ResultResponse
import org.ymessenger.app.utils.AppExecutors

class RepliedMessageRepository private constructor(
    private val executors: AppExecutors,
    private val repliedMessageDao: RepliedMessageDao,
    private val webSocketService: WebSocketService,
    private val repliedMessageMapper: RepliedMessageMapper
) {

    fun getRepliedMessage(
        conversationId: Long,
        conversationType: Int,
        globalId: String
    ): LiveData<MessageModel> {
        getRepliedMessagesFromServer(conversationId, conversationType, listOf(globalId))

        return repliedMessageDao.getRepliedMessageModel(globalId, conversationId, conversationType)
    }

    fun getRepliedMessagesFromServer(
        conversationId: Long,
        conversationType: Int,
        globalIds: List<String>
    ) {
        val getMessages = GetMessages(conversationType, conversationId, messagesId = globalIds)
        webSocketService.getMessages(
            getMessages,
            object : WebSocketService.ResponseCallback<Messages> {
                override fun onResponse(response: Messages) {
                    val dbMessages = response.messages.map { repliedMessageMapper.toDb(it) }
                    executors.diskIO.execute {
                        repliedMessageDao.insertAll(dbMessages)
                    }
                }

                override fun onError(error: ResultResponse) {
                    Log.e(TAG, "Get message errors with code ${error.errorCode}")
                }

            })
    }

    fun deleteAllLocally() {
        executors.diskIO.execute {
            repliedMessageDao.deleteAll()
        }
    }

    companion object {
        private const val TAG = "MessageRepository"
        const val MESSAGES_PAGE_SIZE = 30

        private var instance: RepliedMessageRepository? = null

        fun getInstance(
            executors: AppExecutors,
            repliedMessageDao: RepliedMessageDao,
            webSocketService: WebSocketService,
            repliedMessageMapper: RepliedMessageMapper
        ): RepliedMessageRepository {
            return instance ?: synchronized(this) {
                instance
                    ?: RepliedMessageRepository(
                        executors,
                        repliedMessageDao,
                        webSocketService,
                        repliedMessageMapper
                    ).also { instance = it }
            }
        }
    }

}