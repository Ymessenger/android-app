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

package org.ymessenger.app.adapters.viewholders

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import kotlinx.android.synthetic.main.item_found_message.view.*
import kotlinx.android.synthetic.main.item_user_avatar.view.*
import org.ymessenger.app.R
import org.ymessenger.app.models.FoundMessage
import org.ymessenger.app.utils.DateUtils

class FoundMessageHolder(
    itemView: View,
    private val currentUserId: Long,
    private val glide: RequestManager,
    itemClickListeners: BaseClickListener<FoundMessage>
) : RecyclerView.ViewHolder(itemView) {

    private var item: FoundMessage? = null

    init {
        itemView.setOnClickListener {
            item?.let {
                itemClickListeners.onClick(it)
            }
        }
        itemView.setOnLongClickListener {
            item?.let {
                itemClickListeners.onLongClick(it)
                true
            } ?: false
        }
    }

    fun bind(foundMessage: FoundMessage) {
        this.item = foundMessage

        // Hide unread count
        itemView.tvUnreadMessages.visibility = View.INVISIBLE
        // Hide online
        itemView.ivOnline.visibility = View.INVISIBLE
        // Hide favorite
        itemView.ivFavourite.visibility = View.INVISIBLE

        // Display photo
        updatePhoto(foundMessage)

        // Display chat name
        updateChatName(foundMessage)

        updateRead(foundMessage)

        // Show preview text
        updatePreviewText(foundMessage)

        // Show date
        updateDate(foundMessage)
    }

    fun updateChatName(foundMessage: FoundMessage) {
        val chatName = foundMessage.chatPreviewModel?.chatPreview?.chatName

        itemView.tvUserName.text = chatName ?: itemView.context.getString(R.string.name_is_hidden)
    }

    fun updateRead(foundMessage: FoundMessage) {
        if (foundMessage.message.senderId == currentUserId) {
            itemView.ivMessageStatus.visibility = View.VISIBLE
            val imgRes =
                if (foundMessage.message.read) R.drawable.ic_done_all else R.drawable.ic_done
            itemView.ivMessageStatus.setImageResource(imgRes)
        } else {
            itemView.ivMessageStatus.visibility = View.GONE
        }
    }

    fun updatePreviewText(foundMessage: FoundMessage) {
        val context = itemView.context

        var lastMessage = foundMessage.message.text

        if (foundMessage.isChat()) {
            itemView.tvAuthorNameLabel.text = when {
                foundMessage.message.senderId == currentUserId -> {
                    context.getString(R.string.you)
                }
                else -> {
                    foundMessage.chatPreviewModel?.getUser()?.firstName
                        ?: context.getString(R.string.name_is_hidden)
                }
            }

            showAuthorLabel()
        } else {
            hideAuthorLabel()
        }

        itemView.tvMessageText.text = lastMessage
    }

    fun updateDate(foundMessage: FoundMessage) {
        val date = DateUtils.smartFormat(foundMessage.message.sentAt)
        itemView.tvDate.text = date
    }

    private fun showAuthorLabel() {
        itemView.tvAuthorNameLabel.visibility = View.VISIBLE
        itemView.tvMessageText.maxLines = 1
    }

    private fun hideAuthorLabel() {
        itemView.tvAuthorNameLabel.visibility = View.GONE
        itemView.tvMessageText.maxLines = 2
    }

    fun updatePhoto(foundMessage: FoundMessage) {
        val photoUrl: String? = foundMessage.chatPreviewModel?.chatPreview?.getPhotoUrl()
        val photoLabel: String? = foundMessage.chatPreviewModel?.getPhotoLabel()

        itemView.tvPhotoLabel.text = photoLabel
        if (photoUrl != null) {
            glide.load(photoUrl)
                .thumbnail(0.1F)
                .into(itemView.ivUserAvatar)
            itemView.ivUserAvatar.visibility = View.VISIBLE
        } else {
            itemView.ivUserAvatar.visibility = View.INVISIBLE
        }
    }

    companion object {
        fun create(
            parent: ViewGroup,
            currentUserId: Long,
            glide: RequestManager,
            itemClickListeners: BaseClickListener<FoundMessage>
        ): FoundMessageHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_found_message, parent, false)
            return FoundMessageHolder(view, currentUserId, glide, itemClickListeners)
        }
    }
}