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
import kotlinx.android.synthetic.main.item_message_my.view.*
import org.ymessenger.app.R
import org.ymessenger.app.adapters.MessagesPagedAdapter
import org.ymessenger.app.data.local.db.models.MessageModel

class MyMessageHolder(
    itemView: View,
    glide: RequestManager,
    itemClickListeners: MessagesPagedAdapter.ItemClickListeners,
    encryptedMessageCallbacks: MessagesPagedAdapter.EncryptedMessageCallbacks?
) :
    AbstractMessageHolder(itemView, glide, itemClickListeners, encryptedMessageCallbacks) {

    override fun initializeColors(messageModel: MessageModel) {
        if (messageModel.isProtected()) {
            messageCloudColor = R.color.colorDark
            messageReplyColor = R.color.colorWhite
        } else {
            messageCloudColor = R.color.colorPrimary
            messageReplyColor = R.color.colorDark
        }
        messageTextColor = R.color.colorWhite
        messageFileSizeColor = R.color.colorGrayLighten
    }

    override fun bind(messageModel: MessageModel, displayDateDivider: Boolean) {
        super.bind(messageModel, displayDateDivider)
        updateReadStatus(messageModel)
//        itemView.tvMessageText.text = messageModel.getMessage().text
    }

    fun updateReadStatus(messageModel: MessageModel) {
        this.messageModel = messageModel
        itemView.ivMessageStatus.setImageResource(if (messageModel.getMessage().read) R.drawable.ic_done_all else R.drawable.ic_done)
    }

    override fun getSymmetricKeyMessage(): Int {
        return R.string.you_turned_on_protected_mode
    }

    companion object {
        fun create(
            parent: ViewGroup,
            glide: RequestManager,
            itemClickListeners: MessagesPagedAdapter.ItemClickListeners,
            encryptedMessageCallbacks: MessagesPagedAdapter.EncryptedMessageCallbacks?
        ): MyMessageHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_message_my, parent, false)
            return MyMessageHolder(
                view,
                glide,
                itemClickListeners,
                encryptedMessageCallbacks
            )
        }
    }
}