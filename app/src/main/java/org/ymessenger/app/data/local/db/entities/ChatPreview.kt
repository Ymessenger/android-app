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

package org.ymessenger.app.data.local.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import org.ymessenger.app.data.ConversationType
import org.ymessenger.app.data.remote.UrlGenerator

@Entity(
    tableName = "chat_previews",
    primaryKeys = ["conversation_id", "conversation_type"],
    indices = [Index(value = ["conversation_id", "conversation_type"], unique = true)]
)
data class ChatPreview(
    @ColumnInfo(name = "conversation_id") val conversationId: Long,
    @ColumnInfo(name = "conversation_type") val conversationType: Int,
    @ColumnInfo(name = "chat_name") var chatName: String?,
    @ColumnInfo(name = "preview_text") var previewText: String?,
    @ColumnInfo(name = "user_id") var userId: Long?,
    @ColumnInfo(name = "user_name") var userName: String?,
    @ColumnInfo(name = "photo") var photo: String?,
    @ColumnInfo(name = "last_message_time") var lastMessageTime: Long,
    @ColumnInfo(name = "last_message_sender_id") var lastMessageSenderId: Long?,
    @ColumnInfo(name = "unread_count") var unreadCount: Int,
    @ColumnInfo(name = "chat_type") var chatType: Int?,
    @ColumnInfo(name = "last_message_id") var lastMessageId: String?,
    @ColumnInfo(name = "attachment_type") var attachmentType: Int?,
    @ColumnInfo(name = "attachment_types") var attachmentTypes: String,
    @ColumnInfo(name = "read") var read: Boolean?,
    @ColumnInfo(name = "is_muted") var isMuted: Boolean
) {
    fun getPhotoUrl() = UrlGenerator.getFileUrl(photo)

    fun isDialog() = conversationType == ConversationType.DIALOG

    fun isChat() = conversationType == ConversationType.CHAT

    fun isChannel() = conversationType == ConversationType.CHANNEL
}