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
import org.ymessenger.app.data.local.db.dao.ChatDao
import org.ymessenger.app.data.local.db.entities.Chat
import org.ymessenger.app.data.mappers.ChatMapper
import org.ymessenger.app.data.remote.Listing
import org.ymessenger.app.data.remote.WSResponse
import org.ymessenger.app.data.remote.WebSocketService
import org.ymessenger.app.data.remote.requests.DeleteConversation
import org.ymessenger.app.data.remote.requests.EditChats
import org.ymessenger.app.data.remote.requests.GetChats
import org.ymessenger.app.data.remote.requests.NewChats
import org.ymessenger.app.data.remote.responses.Chats
import org.ymessenger.app.data.remote.responses.ResultResponse
import org.ymessenger.app.interfaces.SuccessErrorCallback
import org.ymessenger.app.utils.AppExecutors

class ChatRepository private constructor(
    private val executors: AppExecutors,
    private val chatDao: ChatDao,
    private val webSocketService: WebSocketService,
    private val chatMapper: ChatMapper
) {

    fun getChats() = chatDao.getChats()

    fun getChat(chatId: Long): LiveData<Chat> {
        getChatsFromServer(listOf(chatId))

        return chatDao.getChat(chatId)
    }

    fun getChatSync(chatId: Long) = chatDao.getChatSync(chatId)

    fun insertChat(chat: Chat) {
        executors.diskIO.execute {
            chatDao.insert(chat)
        }
    }

    fun updateChat(chat: Chat) {
        executors.diskIO.execute {
            chatDao.update(chat)
        }
    }

    fun createChat(newChats: NewChats, createChatCallback: CreateChatCallback) {
        webSocketService.newChats(newChats, object : WebSocketService.ResponseCallback<Chats> {
            override fun onResponse(response: Chats) {
                val dbChats = response.chats.map { chatMapper.toDb(it) }

                executors.diskIO.execute {
                    chatDao.insert(dbChats)
                }

                if (dbChats.isNotEmpty()) {
                    createChatCallback.created(dbChats.first().id)
                }
            }

            override fun onError(error: ResultResponse) {
                Log.e(TAG, "error while creating chat. Code ${error.errorCode}")
                createChatCallback.error()
            }

        })
    }

    fun getChatsFromServer(chatsId: List<Long>) {
        val getChats = GetChats(chatsId)
        webSocketService.getChats(getChats, object : WebSocketService.ResponseCallback<Chats> {
            override fun onResponse(response: Chats) {
                val dbChats = response.chats.map { chatMapper.toDb(it) }

                executors.diskIO.execute {
                    if (dbChats.isEmpty()) {
                        chatDao.setDeleted(chatsId)
                    } else {
                        chatDao.insert(dbChats)
                    }
                }
            }

            override fun onError(error: ResultResponse) {
                Log.e(TAG, "Failed to get chats from server")
            }
        })
    }

    interface CreateChatCallback {
        fun created(chatId: Long)
        fun error()
    }

    interface EditChatCallback {
        fun edited()
        fun error(error: ResultResponse)
    }

    fun editChats(editChats: EditChats, editChatCallback: EditChatCallback) {
        webSocketService.editChats(editChats, object : WebSocketService.ResponseCallback<Chats> {
            override fun onResponse(response: Chats) {
                val dbChats = response.chats.map { chatMapper.toDb(it) }
                executors.diskIO.execute {
                    chatDao.insert(dbChats)
                }
                editChatCallback.edited()
            }

            override fun onError(error: ResultResponse) {
                Log.e(TAG, "Failed to edit chat")
                editChatCallback.error(error)
            }
        })
    }

    fun deleteChat(chatId: Long, callback: SuccessErrorCallback) {
        val deleteConversation = DeleteConversation(chatId, ConversationType.CHAT)
        webSocketService.deleteConversation(
            deleteConversation,
            object : WebSocketService.ResponseCallback<WSResponse> {
                override fun onResponse(response: WSResponse) {
                    executors.diskIO.execute {
                        chatDao.delete(chatId)
                    }
                    callback.success()
                }

                override fun onError(error: ResultResponse) {
                    Log.e(TAG, "Failed to remove chat, error code = ${error.errorCode}")
                    callback.error(error)
                }
            })
    }

    fun search(searchQuery: String): Listing<Chat> {
        val sourceFactory = SearchChatDataSourceFactory(searchQuery, webSocketService, chatMapper)

        val config = PagedList.Config.Builder()
            .setEnablePlaceholders(false)
            .setPageSize(PAGE_SIZE)
            .build()

        val livePagedListBuilder = LivePagedListBuilder<Long, Chat>(sourceFactory, config)
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
            chatDao.deleteAll()
        }
    }

    companion object {
        private const val TAG = "ChatRepository"
        private var instance: ChatRepository? = null

        private const val PAGE_SIZE = 100

        fun getInstance(
            executors: AppExecutors,
            chatDao: ChatDao,
            webSocketService: WebSocketService,
            chatMapper: ChatMapper
        ) =
            instance ?: synchronized(this) {
                instance
                    ?: ChatRepository(
                        executors,
                        chatDao,
                        webSocketService,
                        chatMapper
                    ).also { instance = it }
            }
    }

}