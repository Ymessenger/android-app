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

import android.content.res.ColorStateList
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import kotlinx.android.synthetic.main.item_chat_preview.view.*
import kotlinx.android.synthetic.main.item_user_avatar.view.*
import org.ymessenger.app.R
import org.ymessenger.app.data.local.db.models.ChatPreviewModel
import org.ymessenger.app.data.local.db.models.UserActionModel
import org.ymessenger.app.data.remote.Config
import org.ymessenger.app.data.remote.entities.Attachment
import java.util.*

class ChatPreviewHolder(
    itemView: View,
    private val currentUserId: Long,
    private val glide: RequestManager,
    itemClickListeners: BaseClickListener<ChatPreviewModel>,
    userActionsLiveData: LiveData<List<UserActionModel>>
) : RecyclerView.ViewHolder(itemView) {

    private var item: ChatPreviewModel? = null

    private var displayUserActionTimer: Timer? = null

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

        userActionsLiveData.observeForever {
            item?.let { chatPreviewModel ->
                if (it.isNullOrEmpty()) {
                    hideUserAction()
                } else {
                    for (userActionModel in it) {
                        if (userActionModel.userAction.conversationId == chatPreviewModel.chatPreview.conversationId &&
                            userActionModel.userAction.conversationType == chatPreviewModel.chatPreview.conversationType
                        ) {
                            // This liveData gets updates every time when this view holder is created.
                            // To fix this I introduce a stupid hack but it should work
                            // WTF, this is so shitty
                            val now = System.currentTimeMillis() / 1000L
                            if (now - userActionModel.userAction.time < Config.USER_ACTION_DISPLAY_PERIOD) {
                                updateUserAction(userActionModel)
                            }
                            break
                        }
                    }
                }
            }
        }
    }

    fun bind(chatPreviewModel: ChatPreviewModel) {
        this.item = chatPreviewModel

        // Display photo
        updatePhoto(chatPreviewModel)

        // Display unread count
        updateUnreadCount(chatPreviewModel)

        // Display chat name
        updateChatName(chatPreviewModel)

        // Show preview text
        updatePreviewText(chatPreviewModel)

        // Display lock icon
        updateProtected(chatPreviewModel)

        // Display message status: [sent, read]
        updateRead(chatPreviewModel)

        // Display online
        updateOnline(chatPreviewModel)

        // Display favourite
        updateFavourite(chatPreviewModel)

        updateMuted(chatPreviewModel)
    }

    fun setItem(chatPreviewModel: ChatPreviewModel) {
        this.item = chatPreviewModel
    }

    fun updateUnreadCount(chatPreviewModel: ChatPreviewModel) {
        val unreadCount = chatPreviewModel.chatPreview.unreadCount
        var textColor = R.color.textSecondary
        if (unreadCount > 0) {
            itemView.tvUnreadMessages.text = unreadCount.toString()
            itemView.tvUnreadMessages.visibility = View.VISIBLE
            textColor = R.color.colorPrimary
        } else {
            itemView.tvUnreadMessages.visibility = View.INVISIBLE
        }
        itemView.tvMessageText.setTextColor(
            ColorStateList.valueOf(
                ContextCompat.getColor(
                    itemView.context,
                    textColor
                )
            )
        )
    }

    fun updateChatName(chatPreviewModel: ChatPreviewModel) {
        val chatName = chatPreviewModel.getDisplayChatName()
        itemView.tvUserName.text = chatName ?: itemView.context.getString(R.string.name_is_hidden)
    }

    fun updatePreviewText(chatPreviewModel: ChatPreviewModel) {
        val context = itemView.context

        var lastMessage = chatPreviewModel.chatPreview.previewText

        if (!chatPreviewModel.hasAttachments() && lastMessage == null) {
            lastMessage = context.getString(R.string.no_messages_yet)
            hideAuthorLabel()
        } else {
            if (chatPreviewModel.hasAttachments() && lastMessage == null) {
                lastMessage =
                    if (chatPreviewModel.chatPreview.attachmentType == Attachment.Type.KEY_EXCHANGE_MESSAGE) {
                        if (chatPreviewModel.chatPreview.lastMessageSenderId == currentUserId) {
                            context.getString(R.string.you_turned_on_protected_mode)
                        } else {
                            context.getString(R.string.interlocutor_turned_on_protected_mode)
                        }
                    } else {
                        chatPreviewModel.getAttachmentDescription(context)
                    }
            }

            var isDraft = false
            chatPreviewModel.getDraft()?.let {
                if (it.savedAt > chatPreviewModel.chatPreview.lastMessageTime) {
                    lastMessage = it.text
                    isDraft = true
                }
            }

            if (chatPreviewModel.chatPreview.isChat() || isDraft) {
                itemView.tvAuthorNameLabel.text = when {
                    isDraft -> {
                        context.getString(R.string.draft)
                    }
                    chatPreviewModel.chatPreview.lastMessageSenderId == currentUserId -> {
                        context.getString(R.string.you)
                    }
                    else -> {
                        chatPreviewModel.getLastMessageSenderName()
                            ?: context.getString(R.string.name_is_hidden)
                    }
                }

                val color = if (isDraft) R.color.colorMaterialRed500 else R.color.colorDark
                itemView.tvAuthorNameLabel.setTextColor(
                    ColorStateList.valueOf(
                        ContextCompat.getColor(
                            itemView.context,
                            color
                        )
                    )
                )

                showAuthorLabel()
            } else {
                hideAuthorLabel()
            }
        }

        itemView.tvMessageText.text = lastMessage
    }

    private fun showAuthorLabel() {
        itemView.tvAuthorNameLabel.visibility = View.VISIBLE
        itemView.tvMessageText.maxLines = 1
    }

    private fun hideAuthorLabel() {
        itemView.tvAuthorNameLabel.visibility = View.GONE
        itemView.tvMessageText.maxLines = 2
    }

    fun updateUserAction(userActionModel: UserActionModel) {
        displayUserActionTimer?.cancel()
        displayUserActionTimer = Timer()
        displayUserActionTimer?.schedule(object : TimerTask() {
            override fun run() {
                Handler(Looper.getMainLooper()).post {
                    hideUserAction()
                }
            }
        }, Config.USER_ACTION_DISPLAY_PERIOD * 1000L)
        showUserAction(userActionModel, item?.isDialog() == true)
    }

    private fun showUserAction(userActionModel: UserActionModel, isDialog: Boolean) {
        val context = itemView.context

        val userActionLabel = if (isDialog) {
            context.getString(userActionModel.getActionLabelRes())
        } else {
            val userName =
                userActionModel.getDisplayName() ?: context.getString(R.string.name_is_hidden)
            context.getString(userActionModel.getActionPlaceholderLabelRes(), userName)
        }
        itemView.tvUserAction.text = userActionLabel

        itemView.tvUserAction.visibility = View.VISIBLE
        itemView.tvMessageText.visibility = View.INVISIBLE
    }

    private fun hideUserAction() {
        itemView.tvUserAction.visibility = View.INVISIBLE
        itemView.tvMessageText.visibility = View.VISIBLE
    }

    fun updateProtected(chatPreviewModel: ChatPreviewModel) {
        if (chatPreviewModel.isProtected()) {
            itemView.ivSecureChat.visibility = View.VISIBLE
        } else {
            itemView.ivSecureChat.visibility = View.GONE
        }
    }

    fun updateRead(chatPreviewModel: ChatPreviewModel) {
        if (chatPreviewModel.chatPreview.lastMessageSenderId == currentUserId && chatPreviewModel.chatPreview.read != null && chatPreviewModel.getDraft() == null) {
            itemView.ivMessageStatus.visibility = View.VISIBLE
            val imgRes =
                if (chatPreviewModel.chatPreview.read!!) R.drawable.ic_done_all else R.drawable.ic_done
            itemView.ivMessageStatus.setImageResource(imgRes)
        } else {
            itemView.ivMessageStatus.visibility = View.GONE
        }
    }

    fun updateOnline(chatPreviewModel: ChatPreviewModel) {
        if (chatPreviewModel.isOnline()) {
            itemView.ivOnline.visibility = View.VISIBLE
        } else {
            itemView.ivOnline.visibility = View.INVISIBLE
        }
    }

    fun updatePhoto(chatPreviewModel: ChatPreviewModel) {
        itemView.tvPhotoLabel.text = chatPreviewModel.getPhotoLabel()
        val photoUrl = chatPreviewModel.chatPreview.getPhotoUrl()
        if (photoUrl != null) {
            glide.load(photoUrl)
                .thumbnail(0.1F)
                .into(itemView.ivUserAvatar)
            itemView.ivUserAvatar.visibility = View.VISIBLE
        } else {
            itemView.ivUserAvatar.visibility = View.INVISIBLE
        }
    }

    fun updateFavourite(chatPreviewModel: ChatPreviewModel) {
        if (chatPreviewModel.isFavourite()) {
            itemView.ivFavourite.visibility = View.VISIBLE
        } else {
            itemView.ivFavourite.visibility = View.INVISIBLE
        }
    }

    fun updateMuted(chatPreviewModel: ChatPreviewModel) {
        if (chatPreviewModel.isMuted()) {
            itemView.ivMuted.visibility = View.VISIBLE
        } else {
            itemView.ivMuted.visibility = View.GONE
        }
    }

    companion object {
        fun create(
            parent: ViewGroup,
            currentUserId: Long,
            glide: RequestManager,
            itemClickListeners: BaseClickListener<ChatPreviewModel>,
            userActionsLiveData: LiveData<List<UserActionModel>>
        ): ChatPreviewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_chat_preview, parent, false)
            return ChatPreviewHolder(
                view,
                currentUserId,
                glide,
                itemClickListeners,
                userActionsLiveData
            )
        }
    }
}