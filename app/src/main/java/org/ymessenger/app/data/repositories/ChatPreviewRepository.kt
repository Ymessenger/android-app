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
import org.ymessenger.app.data.local.db.dao.ChatPreviewDao
import org.ymessenger.app.data.local.db.entities.ChatPreview
import org.ymessenger.app.data.local.db.models.ChatPreviewModel
import org.ymessenger.app.data.mappers.ChatPreviewMapper
import org.ymessenger.app.data.remote.NetworkState
import org.ymessenger.app.data.remote.SimpleListing
import org.ymessenger.app.data.remote.WebSocketService
import org.ymessenger.app.data.remote.requests.GetAllUserConversations
import org.ymessenger.app.data.remote.requests.MuteConversation
import org.ymessenger.app.data.remote.responses.Conversations
import org.ymessenger.app.data.remote.responses.ResultResponse
import org.ymessenger.app.interfaces.SuccessErrorCallback
import org.ymessenger.app.utils.AppExecutors

class ChatPreviewRepository private constructor(
    private val executors: AppExecutors,
    private val chatPreviewDao: ChatPreviewDao,
    private val webSocketService: WebSocketService,
    private val chatPreviewMapper: ChatPreviewMapper
) {

    fun getChatPreviewModels(): SimpleListing<ChatPreviewModel> {
        val refreshTrigger = MutableLiveData<Void>()
        val networkState =
            Transformations.switchMap(refreshTrigger) { loadChatPreviewsFromServer() }
        refreshTrigger.value = null

        return SimpleListing(
            chatPreviewDao.getChatPreviewModels(),
            networkState,
            refresh = {
                refreshTrigger.value = null
            }
        )
    }

    fun updateChatPreviews() {
        loadChatPreviewsFromServer()
    }

    private fun loadChatPreviewsFromServer(): LiveData<NetworkState> {
        val networkState = MutableLiveData<NetworkState>(NetworkState.LOADING)

        val getAllUserConversations = GetAllUserConversations()
        webSocketService.getAllUserConversations(
            getAllUserConversations,
            object : WebSocketService.ResponseCallback<Conversations> {
                override fun onResponse(response: Conversations) {
                    networkState.postValue(NetworkState.LOADED)

                    val dbChatPreviews = response.conversations.map { chatPreviewMapper.toDb(it) }

                    executors.diskIO.execute {
                        chatPreviewDao.insertAll(dbChatPreviews)
                    }
                }

                override fun onError(error: ResultResponse) {
                    Log.e(TAG, "Get chat previews errors with code ${error.errorCode}")
                    networkState.postValue(NetworkState.error(error.message))
                }

            })

        return networkState
    }

    fun getChatPreviewByChat(chatId: Long, chatType: Int) =
        chatPreviewDao.getChatPreviewByChat(chatId, chatType)

    fun getChatPreviewByChatSync(conversationId: Long, conversationType: Int): ChatPreviewModel? {
        return chatPreviewDao.getChatPreviewByChatSync(conversationId, conversationType)
    }

    fun insertChatPreview(chatPreview: ChatPreview) {
        executors.diskIO.execute {
            chatPreviewDao.insert(chatPreview)
        }
    }

    fun updateChatPreview(chatPreview: ChatPreview) {
        executors.diskIO.execute {
            chatPreviewDao.update(chatPreview)
        }
    }

    fun deleteChatPreview(chatId: Long, chatType: Int) {
        executors.diskIO.execute {
            chatPreviewDao.delete(chatId, chatType)
        }
    }

    fun deleteAllChatPreviews() {
        executors.diskIO.execute {
            chatPreviewDao.deleteAll()
        }
    }

    fun muteConversation(chatPreview: ChatPreview, callback: SuccessErrorCallback) {
        val unStr = if (chatPreview.isMuted) "un" else ""
        val muteConversation =
            MuteConversation(chatPreview.conversationId, chatPreview.conversationType)
        webSocketService.muteConversation(
            muteConversation,
            object : WebSocketService.ResponseCallback<ResultResponse> {
                override fun onResponse(response: ResultResponse) {
                    Log.d(TAG, "Conversation is ${unStr}muted")
                    val updatedChatPreview = chatPreview.copy(isMuted = !chatPreview.isMuted)
                    executors.diskIO.execute {
                        chatPreviewDao.update(updatedChatPreview)
                    }

                    callback.success()
                }

                override fun onError(error: ResultResponse) {
                    Log.e(TAG, "Failed to ${unStr}mute conversation")
                    callback.error(error)
                }
            })
    }

    companion object {
        private const val TAG = "ChatPreviewRepository"
        private var instance: ChatPreviewRepository? = null

        fun getInstance(
            executors: AppExecutors,
            chatPreviewDao: ChatPreviewDao,
            webSocketService: WebSocketService,
            chatPreviewMapper: ChatPreviewMapper
        ) =
            instance ?: synchronized(this) {
                instance
                    ?: ChatPreviewRepository(
                        executors,
                        chatPreviewDao,
                        webSocketService,
                        chatPreviewMapper
                    ).also { instance = it }
            }
    }

}