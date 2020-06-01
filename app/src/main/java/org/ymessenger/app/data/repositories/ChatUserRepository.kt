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
import org.ymessenger.app.data.local.db.dao.ChatUserDao
import org.ymessenger.app.data.local.db.entities.ChatUser
import org.ymessenger.app.data.local.db.models.ChatUserModel
import org.ymessenger.app.data.mappers.ChatUserMapper
import org.ymessenger.app.data.remote.WSResponse
import org.ymessenger.app.data.remote.WebSocketService
import org.ymessenger.app.data.remote.requests.AddUsersChats
import org.ymessenger.app.data.remote.requests.ChangeChatUsers
import org.ymessenger.app.data.remote.requests.GetChatUsers
import org.ymessenger.app.data.remote.responses.ChatUsers
import org.ymessenger.app.data.remote.responses.ResultResponse
import org.ymessenger.app.interfaces.SuccessErrorCallback
import org.ymessenger.app.utils.AppExecutors

class ChatUserRepository private constructor(
    private val executors: AppExecutors,
    private val chatUserDao: ChatUserDao,
    private val webSocketService: WebSocketService,
    private val chatUserMapper: ChatUserMapper
) {

    fun getChatUsersByChat(chatId: Long): LiveData<List<ChatUser>> {
        val getChatUsers = GetChatUsers(chatId)
        webSocketService.getChatUsers(
            getChatUsers,
            object : WebSocketService.ResponseCallback<ChatUsers> {
                override fun onResponse(response: ChatUsers) {
                    val dbChatUsers = response.chatUsers.map { chatUserMapper.toDb(it) }

                    executors.diskIO.execute {
                        chatUserDao.deleteAllChatUsers(chatId)
                        chatUserDao.insert(dbChatUsers)
                    }
                }

                override fun onError(error: ResultResponse) {
                    if (error.errorCode == WSResponse.ErrorCode.PERMISSION_DENIED) {
                        executors.diskIO.execute {
                            chatUserDao.deleteAllChatUsers(chatId)
                        }
                    }
                }

            })

        return chatUserDao.getChatUsersByChat(chatId)
    }

    fun getChatUserModels(chatId: Long): LiveData<List<ChatUserModel>> {
        val getChatUsers = GetChatUsers(chatId)
        webSocketService.getChatUsers(
            getChatUsers,
            object : WebSocketService.ResponseCallback<ChatUsers> {
                override fun onResponse(response: ChatUsers) {
                    val dbChatUsers = response.chatUsers.map { chatUserMapper.toDb(it) }

                    executors.diskIO.execute {
                        chatUserDao.deleteAllChatUsers(chatId)
                        chatUserDao.insert(dbChatUsers)
                    }
                }

                override fun onError(error: ResultResponse) {
                    if (error.errorCode == WSResponse.ErrorCode.PERMISSION_DENIED) {
                        executors.diskIO.execute {
                            chatUserDao.deleteAllChatUsers(chatId)
                        }
                    }
                }

            })

        return chatUserDao.getChatUserModels(chatId)
    }

    fun getChatUser(chatId: Long, userId: Long): LiveData<ChatUserModel> {
        return chatUserDao.getChatUserModel(chatId, userId)
    }

    fun changeChatUsers(
        changeChatUsers: ChangeChatUsers,
        changeChatUsersCallback: ChangeChatUsersCallback?
    ) {
        webSocketService.changeChatUsers(
            changeChatUsers,
            object : WebSocketService.ResponseCallback<ChatUsers> {
                override fun onResponse(response: ChatUsers) {
                    val dbChatUsers = response.chatUsers.map { chatUserMapper.toDb(it) }
                    executors.diskIO.execute {
                        chatUserDao.insert(dbChatUsers)
                    }
                    changeChatUsersCallback?.success(dbChatUsers)
                }

                override fun onError(error: ResultResponse) {
                    Log.e(TAG, "error change chat users. Code ${error.errorCode}")
                    changeChatUsersCallback?.error(error)
                }

            })
    }

    fun addUsersToChats(addUsersChats: AddUsersChats, callback: SuccessErrorCallback) {
        webSocketService.addUsersChats(
            addUsersChats,
            object : WebSocketService.ResponseCallback<ChatUsers> {
                override fun onResponse(response: ChatUsers) {
                    val dbChatUsers = response.chatUsers.map { chatUserMapper.toDb(it) }
                    executors.diskIO.execute {
                        chatUserDao.insert(dbChatUsers)
                    }
                    Log.d(TAG, "Added ${dbChatUsers.size} new users")
                    callback.success()
                }

                override fun onError(error: ResultResponse) {
                    Log.e(TAG, "Error while add users to chats")
                    callback.error(error)
                }
            })
    }

    interface ChangeChatUsersCallback {
        fun success(editedChatUsers: List<ChatUser>)
        fun error(error: ResultResponse)
    }

    fun insertChatUser(chatUser: ChatUser) {
        executors.diskIO.execute {
            chatUserDao.insert(chatUser)
        }
    }

    fun insertChatUsers(chatUsers: List<ChatUser>) {
        executors.diskIO.execute {
            chatUserDao.insert(chatUsers)
        }
    }

    fun updateChatPreview(chatUser: ChatUser) {
        executors.diskIO.execute {
            chatUserDao.update(chatUser)
        }
    }

    fun deleteChatUser(chatUser: ChatUser) {
        executors.diskIO.execute {
            chatUserDao.delete(chatUser)
        }
    }

    fun deleteChatUsers(chatUsers: List<ChatUser>) {
        executors.diskIO.execute {
            chatUserDao.delete(chatUsers)
        }
    }

    fun updateChatUsers(chatUsers: List<ChatUser>) {
        executors.diskIO.execute {
            chatUserDao.update(chatUsers)
        }
    }

    companion object {
        private const val TAG = "ChatUserRepository"
        private var instance: ChatUserRepository? = null

        fun getInstance(
            executors: AppExecutors,
            chatUserDao: ChatUserDao,
            webSocketService: WebSocketService,
            chatUserMapper: ChatUserMapper
        ) =
            instance ?: synchronized(this) {
                instance
                    ?: ChatUserRepository(
                        executors,
                        chatUserDao,
                        webSocketService,
                        chatUserMapper
                    ).also { instance = it }
            }
    }

}