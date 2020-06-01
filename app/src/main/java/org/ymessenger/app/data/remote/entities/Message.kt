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
import org.ymessenger.app.data.ConversationType

data class Message(
    @SerializedName("Id")
    val id: Long?,
    @SerializedName("SendingTime")
    val sendingTime: Long?,
    @SerializedName("SenderId")
    val senderId: Long?,
    @SerializedName("ReceiverId")
    val receiverId: Long?,
    @SerializedName("ConversationId")
    val conversationId: Long?,
    @SerializedName("ConversationType")
    val conversationType: Int?,
    @SerializedName("GlobalId")
    val globalId: String?,
    @SerializedName("Read")
    val read: Boolean,
    @SerializedName("NodesId")
    val nodesId: List<Long>?,
    @SerializedName("ReplyTo")
    var replyTo: String?,
    @SerializedName("Text")
    val text: String?,
    @SerializedName("Attachments")
    val attachments: List<Attachment>?
) {
    class Builder(private val conversationType: Int, identifier: Long) {

        private var receiverId: Long? = null
        private var conversationId: Long? = null
        private var replyTo: String? = null
        private var text: String? = null
        private var attachments: List<Attachment>? = null

        init {
            if (conversationType == ConversationType.DIALOG) {
                receiverId = identifier
            } else {
                conversationId = identifier
            }
        }

        /**
         * Builds remote message object for sending to chat
         *
         * @param text Text of message
         * @param chatId Identifier of chat where to send message
         */
        @Deprecated(
            "This method is deprecated",
            ReplaceWith("Pass ConversationType to Builder constructor")
        )
        fun forChat(text: String, chatId: Long) = Message(
            null,
            null,
            null,
            null,
            chatId,
            ConversationType.CHAT,
            null,
            false,
            null,
            null,
            text,
            null
        )

        /**
         * Builds remote message object for sending to dialog
         *
         * @param text Text of message
         * @param receiverId Identifier of receiver
         */
        @Deprecated(
            "This method is deprecated",
            ReplaceWith("Pass ConversationType to Builder constructor")
        )
        fun forDialog(text: String, receiverId: Long) = Message(
            null,
            null,
            null,
            receiverId,
            null,
            null,
            null,
            false,
            null,
            null,
            text,
            null
        )

        fun setText(text: String?): Builder {
            this.text = text

            return this
        }

        fun setAttachments(attachments: List<Attachment>?): Builder {
            this.attachments = attachments

            return this
        }

        fun setReplyTo(replyTo: String?): Builder {
            this.replyTo = replyTo

            return this
        }

        fun build() = Message(
            null,
            null,
            null,
            receiverId,
            conversationId,
            conversationType,
            null,
            false,
            null,
            replyTo,
            text,
            attachments
        )
    }
}