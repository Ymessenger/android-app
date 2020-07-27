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

package org.ymessenger.app.adapters

import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import com.bumptech.glide.RequestManager
import org.ymessenger.app.adapters.viewholders.messages.AbstractMessageHolder
import org.ymessenger.app.adapters.viewholders.messages.MessageHolder
import org.ymessenger.app.adapters.viewholders.messages.MyMessageHolder
import org.ymessenger.app.data.ConversationType
import org.ymessenger.app.data.local.db.entities.Attachment
import org.ymessenger.app.data.local.db.entities.Message
import org.ymessenger.app.data.local.db.models.MessageModel
import org.ymessenger.app.data.remote.entities.FileInfo
import org.ymessenger.app.data.remote.entities.Poll
import org.ymessenger.app.viewmodels.DialogViewModel
import java.io.File
import java.util.*

class MessagesPagedAdapter(
    private val readMessageCallback: (Message) -> Unit,
    private val currentUserId: Long,
    private val conversationType: Int,
    private val glide: RequestManager,
    private val itemClickListeners: ItemClickListeners,
    private val encryptedMessageCallbacks: EncryptedMessageCallbacks?
) : PagedListAdapter<MessageModel, AbstractMessageHolder>(MessagesDiffCallback()),
    IMessagesAdapter {

    companion object {
        private const val TAG = "MessagesPagedAdapter"

        private const val TYPE_LOADING = 0
        private const val TYPE_MESSAGE = 1
        private const val TYPE_MY_MESSAGE = 2
    }

    var onRenderEncryptedMessage: (() -> Unit)? = null

    interface ItemClickListeners {
        fun onMessageClick(messageModel: MessageModel)
        fun onUserClick(userId: Long)
        fun onChannelClick(channelId: Long)
        fun onImageClick(imageUrl: String?)
        fun openFile(fileInfo: FileInfo, attachment: Attachment)
        fun downloadFile(fileInfo: FileInfo, attachmentId: Long)
        fun votePoll(optionId: Int, poll: Poll, callback: (() -> Unit))
        fun showVotedUsers(optionId: Int, poll: Poll)
        fun updateMessage(messageId: String)
        fun playVoice(filePath: String, callback: () -> Unit)
        fun pauseVoice()
    }

    interface EncryptedMessageCallbacks {
        fun decryptFile(
            file: File,
            senderId: Long,
            keyId: Long,
            signKeyId: Long,
            decryptFileCallback: DialogViewModel.DecryptFileCallback
        )

        fun onEncryptedImageClick(image: ByteArray)
        fun openEncryptedFile(file: File, senderId: Long, keyId: Long, signKeyId: Long)
    }

    override fun getItemViewType(position: Int): Int {
        val messageModel = getItem(position)!! // FIXME: may be an error

        return if (messageModel.getMessage().senderId == currentUserId && conversationType != ConversationType.CHANNEL) {
            TYPE_MY_MESSAGE
        } else {
            TYPE_MESSAGE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, type: Int): AbstractMessageHolder {
        return when (type) {
            TYPE_MY_MESSAGE -> MyMessageHolder.create(
                parent,
                glide,
                itemClickListeners,
                encryptedMessageCallbacks
            )

            else -> MessageHolder.create(
                parent,
                readMessageCallback,
                conversationType == ConversationType.CHAT || conversationType == ConversationType.CHANNEL,
                glide,
                itemClickListeners,
                encryptedMessageCallbacks
            )
        }
    }

    override fun onBindViewHolder(holder: AbstractMessageHolder, position: Int) {
        val messageModel = getItem(position)
        if (messageModel != null) {
            onRenderEncryptedMessage?.let {
                if (messageModel.getDecryptedMessageType() != null) it.invoke()
            }
            val prevMessageModel = if (position == itemCount - 1) null else getItem(position + 1)
            val displayDateDivider = displayDateDivider(messageModel, prevMessageModel)
            holder.bind(messageModel, displayDateDivider)
        } else {
            // TODO: clear holder?
        }
    }

    private fun displayDateDivider(
        messageModel: MessageModel,
        prevMessageModel: MessageModel?
    ): Boolean {
        var shouldDisplay = true

        if (prevMessageModel != null) {
            val date = Calendar.getInstance()
                .apply { timeInMillis = messageModel.getMessage().sentAt * 1000 }
            val datePrev = Calendar.getInstance()
                .apply { timeInMillis = prevMessageModel.getMessage().sentAt * 1000 }

            if (date[Calendar.YEAR] == datePrev[Calendar.YEAR] &&
                date[Calendar.MONTH] == datePrev[Calendar.MONTH] &&
                date[Calendar.DAY_OF_MONTH] == datePrev[Calendar.DAY_OF_MONTH]
            ) {
                shouldDisplay = false
            }
        }

        return shouldDisplay
    }

    override fun onBindViewHolder(
        holder: AbstractMessageHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position)
        } else {
            val item = getItem(position)!!// FIXME: may be an error

            val payloadsObject = payloads[0] as MessagesDiffCallback.Payloads

            if (payloadsObject.readStatusChanged) {
                if (holder is MyMessageHolder) {
                    holder.updateReadStatus(item)
                }
            }

            if (payloadsObject.repliedMessageChanged) {
                holder.updateRepliedMessage(item)
            }

            if (payloadsObject.attachmentsChanged) {
                if (item.getDecryptedMessageType() != null) onRenderEncryptedMessage?.invoke()
                holder.updateAttachments(item)
            }
        }
    }


    override fun getMessage(position: Int) =
        getItem(position)!!.getMessage() // FIXME: may be an error

    override fun showPhoto(): Boolean {
        return conversationType == ConversationType.CHAT || conversationType == ConversationType.CHANNEL
    }

    class MessagesDiffCallback : DiffUtil.ItemCallback<MessageModel>() {

        data class Payloads(
            var readStatusChanged: Boolean = false,
            var repliedMessageChanged: Boolean = false,
            var attachmentsChanged: Boolean = false
        ) {
            fun hasChanges(): Boolean {
                return readStatusChanged || repliedMessageChanged || attachmentsChanged
            }
        }

        override fun areItemsTheSame(oldItem: MessageModel, newItem: MessageModel): Boolean {
            return oldItem.getMessage().globalId == newItem.getMessage().globalId
        }

        override fun areContentsTheSame(oldItem: MessageModel, newItem: MessageModel): Boolean {
            return oldItem.getMessage().globalId == newItem.getMessage().globalId &&
                    oldItem.getMessage().text == newItem.getMessage().text &&
                    oldItem.getMessage().read == newItem.getMessage().read &&
                    oldItem.getAuthor()?.firstName == newItem.getAuthor()?.firstName &&
                    oldItem.getAuthor()?.photo == newItem.getAuthor()?.photo &&
                    oldItem.getReplyToMessage()?.message?.globalId == newItem.getReplyToMessage()?.message?.globalId &&
                    oldItem.attachments == newItem.attachments &&
                    oldItem.getChannel() == newItem.getChannel() &&
                    oldItem.getDecryptedMessageType() == newItem.getDecryptedMessageType()
        }

        override fun getChangePayload(oldItem: MessageModel, newItem: MessageModel): Any? {
            val payloads = Payloads()
            if (oldItem.getMessage().read != newItem.getMessage().read) {
                payloads.readStatusChanged = true
            }

            if (oldItem.getReplyToMessage() != newItem.getReplyToMessage()) {
                payloads.repliedMessageChanged = true
            }

            if (oldItem.attachments != newItem.attachments ||
                oldItem.getDecryptedMessageType() != newItem.getDecryptedMessageType()
            ) {
                payloads.attachmentsChanged = true
            }

            return if (payloads.hasChanges())
                payloads
            else
                null
        }
    }

    fun showEncrypted(fromPosition: Int, toPosition: Int): Boolean {
        if (itemCount == 0) return false

        for (pos in fromPosition..toPosition) {
            val message = getItem(pos)
            if (message?.getDecryptedMessageType() != null) return true
        }

        return false
    }

}