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

package org.ymessenger.app.data.local.db.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import org.ymessenger.app.data.local.db.entities.ChatUser
import org.ymessenger.app.data.local.db.models.ChatUserModel

@Dao
interface ChatUserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(chatUser: ChatUser)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(chatUsers: List<ChatUser>)

    @Query("SELECT * FROM chat_users")
    fun getChatUsers(): LiveData<List<ChatUser>>

    @Query("SELECT * FROM chat_users WHERE chat_id = :chatId")
    fun getChatUsersByChat(chatId: Long): LiveData<List<ChatUser>>

    @Transaction
//    @Query("SELECT chat_users.* FROM chat_users, users WHERE chat_id = :chatId AND chat_users.user_id = users.id ORDER BY users.online DESC")
    @Query(
        """SELECT chat_users.*, users.online 
        FROM chat_users
        LEFT JOIN users ON chat_users.user_id = users.id
        WHERE chat_id = :chatId
        ORDER BY users.online DESC"""
    )
    fun getChatUserModels(chatId: Long): LiveData<List<ChatUserModel>>

    @Transaction
    @Query("SELECT * FROM chat_users WHERE chat_id = :chatId AND user_id = :userId")
    fun getChatUserModel(chatId: Long, userId: Long): LiveData<ChatUserModel>

    @Update
    fun update(chatUser: ChatUser)

    @Delete
    fun delete(chatUser: ChatUser)

    @Delete
    fun delete(chatUsers: List<ChatUser>)

    @Update
    fun update(chatUsers: List<ChatUser>)

    @Query("DELETE FROM chat_users WHERE chat_id = :chatId")
    fun deleteAllChatUsers(chatId: Long)
}