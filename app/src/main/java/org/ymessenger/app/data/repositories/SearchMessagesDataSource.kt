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
import org.ymessenger.app.data.mappers.MessageMapper
import org.ymessenger.app.data.remote.NetworkState
import org.ymessenger.app.data.remote.WebSocketService
import org.ymessenger.app.data.remote.entities.Message
import org.ymessenger.app.data.remote.requests.SearchMessages
import org.ymessenger.app.data.remote.responses.Messages
import org.ymessenger.app.data.remote.responses.ResultResponse
import org.ymessenger.app.models.FoundMessage
import org.ymessenger.app.utils.AppExecutors

class SearchMessagesDataSource(
    private val query: String,
    private val conversationId: Long?,
    private val conversationType: Int?,
    private val webSocketService: WebSocketService,
    private val chatPreviewRepository: ChatPreviewRepository,
    private val executors: AppExecutors,
    private val messageMapper: MessageMapper
) : ItemKeyedDataSource<SearchMessagesDataSource.SearchKey, FoundMessage>() {

    companion object {
        private const val TAG = "SearchMessagesDS"
    }

    val initialLoad = MutableLiveData<NetworkState>()

    override fun loadInitial(
        params: LoadInitialParams<SearchKey>,
        callback: LoadInitialCallback<FoundMessage>
    ) {
        initialLoad.postValue(NetworkState.LOADING)
        val searchMessages = SearchMessages(query, conversationId, conversationType)
        webSocketService.searchMessages(
            searchMessages,
            object : WebSocketService.ResponseCallback<Messages> {
                override fun onResponse(response: Messages) {
                    executors.diskIO.execute {
                        val foundMessages = convertToFoundMessages(response.messages)

                        initialLoad.postValue(NetworkState.LOADED)
                        callback.onResult(foundMessages)
                    }
                }

                override fun onError(error: ResultResponse) {
                    Log.e(TAG, "Failed to search messages")
                    val networkState = NetworkState.error(error.message ?: "unknown error")
                    initialLoad.postValue(networkState)
                }
            })
    }

    override fun loadAfter(params: LoadParams<SearchKey>, callback: LoadCallback<FoundMessage>) {
        val searchMessages = SearchMessages(
            query,
            conversationId,
            conversationType,
            navigationMessageId = params.key.navigationId,
            navigationConversationId = params.key.navigationConversationId,
            navigationConversationType = params.key.navigationConversationType
        )
        webSocketService.searchMessages(
            searchMessages,
            object : WebSocketService.ResponseCallback<Messages> {
                override fun onResponse(response: Messages) {
                    executors.diskIO.execute {
                        val foundMessages = convertToFoundMessages(response.messages)
                        callback.onResult(foundMessages)
                    }
                }

                override fun onError(error: ResultResponse) {
                    Log.e(TAG, "Failed to search messages")
                }
            })
    }

    override fun loadBefore(params: LoadParams<SearchKey>, callback: LoadCallback<FoundMessage>) {
        // ignored
    }

    override fun getKey(item: FoundMessage): SearchKey {
        return SearchKey(
            item.message.globalId,
            item.message.conversationId,
            item.message.conversationType
        )
    }

    private fun convertToFoundMessages(remoteMessages: List<Message>): List<FoundMessage> {
        val dbMessages = remoteMessages.map { messageMapper.toDb(it) }

        val foundMessages = arrayListOf<FoundMessage>()
        for (message in dbMessages) {
            val foundMessage = FoundMessage(message)

            val chatPreviewModel = chatPreviewRepository.getChatPreviewByChatSync(
                message.conversationId,
                message.conversationType
            )
            foundMessage.chatPreviewModel = chatPreviewModel

            foundMessages.add(foundMessage)
        }

        return foundMessages
    }

    data class SearchKey(
        val navigationId: String,
        val navigationConversationId: Long,
        val navigationConversationType: Int
    )
}