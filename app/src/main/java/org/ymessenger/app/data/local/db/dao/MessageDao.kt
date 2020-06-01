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
import androidx.paging.DataSource
import androidx.room.*
import org.ymessenger.app.data.local.db.entities.Message
import org.ymessenger.app.data.local.db.models.MessageModel
import org.ymessenger.app.data.local.db.models.MessageWithUser
import org.ymessenger.app.data.local.db.models.MessageWithUserModel

@Dao
interface MessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(messages: List<Message>)

    @Insert
    fun saveMessage(message: Message)

    @Query("SELECT * FROM messages")
    fun getMessages(): LiveData<List<Message>>

    @Query(
        """SELECT * FROM messages
                    WHERE conversation_id = :conversationId
                    AND conversation_type = :conversationType
                    ORDER BY sent_at DESC, global_id"""
    )
    fun getMessagesByConversation(
        conversationId: Long,
        conversationType: Int
    ): LiveData<List<Message>>

    @Query(
        """SELECT messages.*, users.first_name, users.photo FROM messages
                    INNER JOIN users ON messages.sender_id = users.id
                    WHERE conversation_id = :conversationId
                    AND conversation_type = :conversationType
                    ORDER BY sent_at DESC, global_id"""
    )
    fun getMessagesByConversationWithUser(
        conversationId: Long,
        conversationType: Int
    ): LiveData<List<MessageWithUser>>

    @Transaction
    @Query(
        """SELECT * FROM messages
                    WHERE conversation_id = :conversationId
                    AND conversation_type = :conversationType
                    AND global_id = :globalId"""
    )
    fun getMessageWithUserModel(
        conversationId: Long,
        conversationType: Int,
        globalId: String
    ): LiveData<MessageWithUserModel>

    @Transaction
    @Query(
        """SELECT * FROM messages
                    WHERE conversation_id = :conversationId
                    AND conversation_type = :conversationType
                    ORDER BY sent_at DESC, global_id"""
    )
    fun getMessageModelsByConversation(
        conversationId: Long,
        conversationType: Int
    ): DataSource.Factory<Int, MessageModel>

    @Transaction
    @Query("SELECT * FROM messages WHERE conversation_id = :conversationId AND conversation_type = :conversationType ORDER BY sent_at DESC, global_id LIMIT 1")
    fun getLastMessage(conversationId: Long, conversationType: Int): LiveData<MessageModel>

    @Query("SELECT * FROM messages WHERE conversation_id = :conversationId AND conversation_type = :conversationType AND read = 1 ORDER BY sent_at DESC, global_id LIMIT 1")
    fun getLastReadMessageSync(conversationId: Long, conversationType: Int): Message?


    // PositionalDataSource

    @Transaction
    @Query(
        """SELECT * FROM messages
                    WHERE conversation_id = :conversationId
                    AND conversation_type = :conversationType
                    ORDER BY sent_at DESC, global_id 
                    LIMIT :requestedLoadSize OFFSET :requestedStartPosition"""
    )
    fun getMessagesByConversation(
        conversationId: Long,
        conversationType: Int,
        requestedStartPosition: Int,
        requestedLoadSize: Int
    ): List<MessageModel>


    @Update
    fun update(message: Message)

    @Query(
        """UPDATE messages SET read = 1
                    WHERE global_id IN (:messagesId)
                    AND conversation_id = :conversationId AND conversation_type = :conversationType"""
    )
    fun messagesRead(messagesId: List<String>, conversationId: Long, conversationType: Int)

    @Query(
        """UPDATE messages SET read = 1
                    WHERE sent_at <= (SELECT sent_at FROM messages WHERE global_id = :messageId
                                        AND conversation_id = :conversationId
                                        AND conversation_type = :conversationType)
                    AND conversation_id = :conversationId
                    AND conversation_type = :conversationType
                    AND read = 0"""
    )
    fun readAllMessagesBefore(messageId: String, conversationId: Long, conversationType: Int)

    @Query("DELETE FROM messages WHERE sent_at >= (SELECT sent_at FROM messages WHERE global_id = :messageId)")
    fun deleteAfter(messageId: String)

    @Query("DELETE FROM messages WHERE conversation_id = :conversationId AND conversation_type = :conversationType AND global_id IN (:messageIds)")
    fun deleteMessages(conversationId: Long, conversationType: Int, messageIds: List<String>)

    /**
     * For testing purposes
     */
    @Query("DELETE FROM messages WHERE conversation_type = :conversationType AND conversation_id = :conversationId")
    fun clearMessages(conversationType: Int, conversationId: Long)
}