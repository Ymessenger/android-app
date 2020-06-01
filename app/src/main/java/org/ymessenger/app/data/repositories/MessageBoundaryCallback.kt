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
import org.ymessenger.app.data.local.db.dao.MessageDao
import org.ymessenger.app.data.local.db.models.MessageModel
import org.ymessenger.app.data.remote.WebSocketService
import org.ymessenger.app.data.remote.entities.Message
import org.ymessenger.app.data.remote.requests.GetMessages
import org.ymessenger.app.data.remote.responses.Messages
import org.ymessenger.app.data.remote.responses.ResultResponse
import org.ymessenger.app.utils.AppExecutors

class MessageBoundaryCallback(
    private val conversationType: Int,
    private val conversationId: Long,
    private val webSocketService: WebSocketService,
    private val messageDao: MessageDao,
    private val handleResponse: (List<Message>) -> Unit
) : PagedList.BoundaryCallback<MessageModel>() {

    private val executors = AppExecutors.getInstance()
    private val helper = PagingRequestHelper(executors.networkIO)

    override fun onZeroItemsLoaded() {
        Log.d(TAG, "onZeroItemsLoaded")
        super.onZeroItemsLoaded()
        helper.runIfNotRunning(PagingRequestHelper.RequestType.INITIAL) { helperCallback ->
            val getMessages = GetMessages(conversationType, conversationId, direction = true)
            webSocketService.getMessages(getMessages, createWebserviceCallback(helperCallback))
        }
    }

    override fun onItemAtEndLoaded(itemAtEnd: MessageModel) {
        Log.d(TAG, "onItemAtEndLoaded")
        super.onItemAtEndLoaded(itemAtEnd)
        helper.runIfNotRunning(PagingRequestHelper.RequestType.AFTER) { helperCallback ->
            val getMessages =
                GetMessages(conversationType, conversationId, itemAtEnd.getMessage().globalId, true)
            webSocketService.getMessages(getMessages, createWebserviceCallback(helperCallback))
        }
    }

    override fun onItemAtFrontLoaded(itemAtFront: MessageModel) {
        Log.d(TAG, "onItemAtFrontLoaded")
        super.onItemAtFrontLoaded(itemAtFront)
        helper.runIfNotRunning(PagingRequestHelper.RequestType.BEFORE) { helperCallback ->
            var globalId = itemAtFront.getMessage().globalId
            if (itemAtFront.hasAttachments() &&
                itemAtFront.getAttachment().isEncryptedMessage() &&
                itemAtFront.getAttachment().getPayloadAsEncryptedMessage().saveFlag == 0
            ) {
                executors.diskIO.execute {
                    Log.d(
                        TAG,
                        "This is local encrypted message. Getting last read message from database..."
                    )
                    val lastReadMessage =
                        messageDao.getLastReadMessageSync(conversationId, conversationType)
                    lastReadMessage?.let {
                        Log.d(TAG, "Last read message found")
                        globalId = it.globalId
                    } ?: Log.d(TAG, "Last read message NOT found")

                    val getMessages = GetMessages(conversationType, conversationId, globalId, false)
                    webSocketService.getMessages(
                        getMessages,
                        createWebserviceCallback(helperCallback)
                    )
                }
            } else {
                val getMessages = GetMessages(conversationType, conversationId, globalId, false)
                webSocketService.getMessages(getMessages, createWebserviceCallback(helperCallback))
            }
        }
    }

    private fun insertItemsIntoDb(
        messages: List<Message>,
        helperCallback: PagingRequestHelper.Request.Callback
    ) {
        executors.diskIO.execute {
            handleResponse.invoke(messages)
            helperCallback.recordSuccess()
        }
    }

    private fun createWebserviceCallback(helperCallback: PagingRequestHelper.Request.Callback):
            WebSocketService.ResponseCallback<Messages> {
        return object : WebSocketService.ResponseCallback<Messages> {
            override fun onResponse(response: Messages) {
                insertItemsIntoDb(response.messages, helperCallback)
            }

            override fun onError(error: ResultResponse) {
                Log.e(TAG, "Failed to fetch data")
                helperCallback.recordFailure(Throwable("Failed to fetch data"))
            }
        }
    }

    companion object {
        private const val TAG = "MessageBoundaryCallback"
    }
}