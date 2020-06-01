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
import org.ymessenger.app.data.local.db.entities.Attachment

@Dao
interface AttachmentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(attachment: Attachment)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(attachments: List<Attachment>)

    @Query("SELECT * FROM attachments")
    fun getAttachments(): LiveData<List<Attachment>>

    @Query("SELECT * FROM attachments WHERE message_id = :messageId")
    fun getAttachmentsByMessage(messageId: Long): LiveData<List<Attachment>>

    @Query("SELECT * FROM attachments WHERE id = :attachmentId")
    fun getAttachment(attachmentId: Long): LiveData<List<Attachment>>

    @Update
    fun update(attachment: Attachment)

    @Update
    fun update(attachments: List<Attachment>)

    @Query("UPDATE attachments SET saved_at = :savedAt WHERE id = :attachmentId")
    fun updateSavedAt(attachmentId: Long, savedAt: String?)

    @Delete
    fun delete(attachment: Attachment)

    @Query(
        """SELECT attachments.* FROM attachments, messages 
        WHERE attachments.conversation_id = :conversationId 
        AND attachments.conversation_type = :conversationType 
        AND type = 2 
        AND attachments.message_id = messages.global_id
        ORDER BY messages.sent_at DESC"""
    )
    fun getPhotosByConversation(
        conversationId: Long,
        conversationType: Int
    ): LiveData<List<Attachment>>
}