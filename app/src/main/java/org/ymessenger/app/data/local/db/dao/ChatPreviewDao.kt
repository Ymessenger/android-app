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
import org.ymessenger.app.data.local.db.entities.ChatPreview
import org.ymessenger.app.data.local.db.models.ChatPreviewModel

@Dao
interface ChatPreviewDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(chatPreview: ChatPreview)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(chatPreviews: List<ChatPreview>)

    @Query("SELECT * FROM chat_previews ORDER BY last_message_time DESC")
    fun getChatPreviews(): LiveData<List<ChatPreview>>

    @Transaction
    @Query("SELECT * FROM chat_previews ORDER BY last_message_time DESC")
    fun getChatPreviewModels(): LiveData<List<ChatPreviewModel>>

    @Query("SELECT * FROM chat_previews WHERE conversation_id = :conversationId AND conversation_type = :conversationType")
    fun getChatPreviewByChat(conversationId: Long, conversationType: Int): LiveData<ChatPreview>

    @Transaction
    @Query("SELECT * FROM chat_previews WHERE conversation_id = :conversationId AND conversation_type = :conversationType")
    fun getChatPreviewByChatSync(conversationId: Long, conversationType: Int): ChatPreviewModel?

    @Update
    fun update(chatPreview: ChatPreview)

    @Query("DELETE FROM chat_previews WHERE conversation_id = :conversationId AND conversation_type = :conversationType")
    fun delete(conversationId: Long, conversationType: Int)

    @Query("DELETE FROM chat_previews")
    fun deleteAll()
}