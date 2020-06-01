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

package org.ymessenger.app.adapters.viewholders.messages

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.RequestManager
import kotlinx.android.synthetic.main.item_message.view.*
import org.ymessenger.app.R
import org.ymessenger.app.adapters.MessagesPagedAdapter
import org.ymessenger.app.data.local.db.entities.Message
import org.ymessenger.app.data.local.db.models.MessageModel

class MessageHolder(
    itemView: View,
    private val readMessageCallback: (Message) -> Unit,
    private val groupChat: Boolean,
    glide: RequestManager,
    itemClickListeners: MessagesPagedAdapter.ItemClickListeners,
    encryptedMessageCallbacks: MessagesPagedAdapter.EncryptedMessageCallbacks?
) : AbstractMessageHolder(itemView, glide, itemClickListeners, encryptedMessageCallbacks) {

    init {
        val clickListener = View.OnClickListener {
            messageModel?.let {
                if (it.isChannelMessage()) {
                    itemClickListeners.onChannelClick(it.getMessage().conversationId)
                } else {
                    it.getMessage().senderId?.let { senderId ->
                        itemClickListeners.onUserClick(senderId)
                    }
                }
            }
        }

        itemView.ivPlaceholder?.setOnClickListener(clickListener)
        itemView.tvUserName?.setOnClickListener(clickListener)
    }

    override fun initializeColors(messageModel: MessageModel) {
        messageCloudColor = if (messageModel.isProtected()) {
            R.color.colorGrayLighten
        } else {
            R.color.backgroundGray
        }
        messageReplyColor = R.color.colorDark
        messageTextColor = R.color.colorDark
        messageFileSizeColor = R.color.colorGray
    }

    override fun bind(messageModel: MessageModel, displayDateDivider: Boolean) {
        super.bind(messageModel, displayDateDivider)

        // Display avatar and name
        if (groupChat) {
            itemView.tvPhotoLabel.text = if (messageModel.isChannelMessage()) {
                messageModel.getChannel()?.getPhotoLabel()
            } else {
                messageModel.getAuthorUserModel()?.getPhotoLabel()
            }

            val photoUrl = if (messageModel.isChannelMessage()) {
                messageModel.getChannel()?.getPhotoUrl()
            } else {
                messageModel.getAuthor()?.getPhotoUrl()
            }

            if (photoUrl != null) {
                glide.load(photoUrl)
                    .into(itemView.ivUserAvatar)

                itemView.ivUserAvatar.visibility = View.VISIBLE
            } else {
                itemView.ivUserAvatar.visibility = View.INVISIBLE
            }

            itemView.tvUserName.text = if (messageModel.isChannelMessage()) {
                messageModel.getChannel()?.name ?: itemView.context.getString(R.string.loading)
            } else {
                messageModel.getAuthorUserModel()?.getDisplayName()
                    ?: itemView.context.getString(R.string.name_is_hidden)
            }
        } else {
            itemView.ivUserAvatar.visibility = View.GONE
            itemView.tvUserName.visibility = View.GONE
        }

        readMessage()
    }

    private fun readMessage() {
        messageModel?.let {
            if (!it.getMessage().read) {
                if (it.hasAttachments()) {
                    if (it.getAttachment().isEncryptedMessage()) {
                        if (it.getAttachment().getPayloadAsEncryptedMessage().saveFlag == 1) {
                            readMessageCallback(it.getMessage())
                        }
                    } else {
                        readMessageCallback(it.getMessage())
                    }
                } else {
                    readMessageCallback(it.getMessage())
                }
            }
        }
    }

    override fun getSymmetricKeyMessage(): Int {
        return R.string.interlocutor_turned_on_protected_mode
    }

    companion object {
        fun create(
            parent: ViewGroup,
            readMessageCallback: (Message) -> Unit,
            groupChat: Boolean,
            glide: RequestManager,
            itemClickListeners: MessagesPagedAdapter.ItemClickListeners,
            encryptedMessageCallbacks: MessagesPagedAdapter.EncryptedMessageCallbacks?
        ): MessageHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_message, parent, false)
            return MessageHolder(
                view,
                readMessageCallback,
                groupChat,
                glide,
                itemClickListeners,
                encryptedMessageCallbacks
            )
        }
    }
}