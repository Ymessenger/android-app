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

package org.ymessenger.app.data.remote.entities

import com.google.gson.annotations.SerializedName

data class ChatPreview(
    @SerializedName("ConversationType")
    val conversationType: Int,
    @SerializedName("ConversationId")
    val conversationId: Long,
    @SerializedName("ChatType")
    val chatType: Int?,
    @SerializedName("Title")
    val title: String,
    @SerializedName("Photo")
    val photo: String?,
    @SerializedName("PreviewText")
    val previewText: String?,
    @SerializedName("UnreadedCount")
    val unreadedCount: Int,
    @SerializedName("LastMessageSenderId")
    val lastMessageSenderId: Long?,
    @SerializedName("LastMessageSenderName")
    val lastMessageSenderName: String?,
    @SerializedName("LastMessageTime")
    val lastMessageTime: Long,
    @SerializedName("SecondUid")
    val secondUid: Long?,
    @SerializedName("LastMessageId")
    val lastMessageId: String?,
    @SerializedName("AttachmentType")
    val attachmentType: Int?,
    @SerializedName("AttachmentTypes")
    val attachmentTypes: List<Int>,
    @SerializedName("Read")
    val read: Boolean?,
    @SerializedName("IsMuted")
    val isMuted: Boolean
) {
}