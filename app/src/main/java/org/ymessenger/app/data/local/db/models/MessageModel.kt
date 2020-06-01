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

package org.ymessenger.app.data.local.db.models

import androidx.room.Embedded
import androidx.room.Ignore
import androidx.room.Relation
import com.google.gson.Gson
import org.ymessenger.app.data.ConversationType
import org.ymessenger.app.data.local.db.entities.Attachment
import org.ymessenger.app.data.local.db.entities.Channel
import org.ymessenger.app.data.local.db.entities.ForwardedMessageInfo
import org.ymessenger.app.data.local.db.entities.RepliedMessage
import org.ymessenger.app.data.remote.entities.FileInfo
import y.encrypt.DecrypteMsg

class MessageModel {
    @Embedded
    lateinit var messageWithUserModel: MessageWithUserModel

    @Relation(parentColumn = "reply_to", entityColumn = "global_id", entity = RepliedMessage::class)
    lateinit var replyToMessages: List<MessageWithUserModel>

    @Relation(parentColumn = "global_id", entityColumn = "message_id", entity = Attachment::class)
    lateinit var attachments: List<Attachment>

    @Relation(
        parentColumn = "global_id",
        entityColumn = "message_id",
        entity = ForwardedMessageInfo::class
    )
    lateinit var forwardedMessageInfoList: List<ForwardedMessageInfoModel>

    @Relation(parentColumn = "conversation_id", entityColumn = "id", entity = Channel::class)
    lateinit var channelList: List<Channel>

    @Ignore
    private var decryptedMsg: DecrypteMsg? = null

    fun getReplyToMessage() = replyToMessages.firstOrNull()

    fun getAuthor() = messageWithUserModel.getAuthor()

    fun getAuthorUserModel() = messageWithUserModel.getUserModel()

    fun getMessage() = messageWithUserModel.message

    fun hasAttachments() = !attachments.isNullOrEmpty()

    fun getAttachment() = attachments.first()

    fun isForwarded() = !forwardedMessageInfoList.isNullOrEmpty()

    fun getForwardedMessageInfo() = forwardedMessageInfoList.firstOrNull()

    fun getChannel() = if (isChannelMessage()) channelList.firstOrNull() else null

    fun isChannelMessage() =
        messageWithUserModel.message.conversationType == ConversationType.CHANNEL

    fun isProtected(): Boolean {
        var protected = false

        if (attachments.isNotEmpty()) {
            val attachment = attachments.first()
            if (attachment.isExchangeKeyMessage() || attachment.isEncryptedMessage()) {
                protected = true
            }
        }

        return protected
    }

    fun setDecryptedMessage(decryptedMsg: DecrypteMsg) {
        this.decryptedMsg = decryptedMsg
    }

    fun getDecryptedMessageType() = decryptedMsg?.type

    fun getDecryptedMessageContent() = decryptedMsg?.msg

    fun getDecryptedAsText() = String(decryptedMsg!!.msg)

    fun getDecryptedAsFileInfo(): FileInfo {
        val gson = Gson()
        val json = String(decryptedMsg!!.msg)

        val fileInfo = gson.fromJson(json, FileInfo::class.java)

        return fileInfo
    }

    fun canBeForwarded() = !isProtected()

    fun canBeReplied() = !isProtected()

}