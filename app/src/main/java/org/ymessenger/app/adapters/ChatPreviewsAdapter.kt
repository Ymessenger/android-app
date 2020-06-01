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

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import kotlinx.android.synthetic.main.item_chat_preview_header.view.*
import org.ymessenger.app.R
import org.ymessenger.app.adapters.viewholders.BaseClickListener
import org.ymessenger.app.adapters.viewholders.ChatPreviewHolder
import org.ymessenger.app.data.local.db.models.ChatPreviewModel
import org.ymessenger.app.data.local.db.models.UserActionModel
import org.ymessenger.app.utils.ListAdapterWithHeader
import org.ymessenger.app.utils.SingleLiveEvent

class ChatPreviewsAdapter(
    private val currentUserId: Long,
    private val glide: RequestManager,
    private val itemClickListeners: BaseClickListener<ChatPreviewModel>,
    private val favoriteConversationsAdapter: FavoriteConversationsAdapter,
    private val onSearchClick: (() -> Unit),
    private val selectMode: Boolean = false
) : ListAdapterWithHeader<ChatPreviewModel, RecyclerView.ViewHolder>(ChatPreviewsDiffCallback()) {

    companion object {
        private const val TAG = "ChatPreviewsAdapter"

        private const val HEADER_ITEM = 0
        private const val CHAT_PREVIEW_ITEM = 1
    }

    private val userActionsLiveData = SingleLiveEvent<List<UserActionModel>>()

    fun setLastUserActionList(userActionModels: List<UserActionModel>) {
        userActionsLiveData.postValue(userActionModels)
    }

    override fun getItemViewType(position: Int): Int {
        if (position == 0)
            return HEADER_ITEM

        return CHAT_PREVIEW_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, type: Int): RecyclerView.ViewHolder {
        when (type) {
            HEADER_ITEM -> return HeaderHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.item_chat_preview_header,
                    parent,
                    false
                )
            )

            else -> return ChatPreviewHolder.create(
                parent,
                currentUserId,
                glide,
                itemClickListeners,
                userActionsLiveData
            )
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position)
        } else {
            Log.d(TAG, "Payloads size ${payloads.size}")
            val payloadObject = payloads[0] as ChatPreviewsDiffCallback.Payloads
            val chatPreviewModel = getItem(position)
            val chatPreviewHolder = holder as ChatPreviewHolder

            if (payloadObject.hasChanges()) {
                chatPreviewHolder.setItem(chatPreviewModel)

                if (payloadObject.lastMessageChanged) {
                    Log.d(TAG, "Payload: lastMessageChanged")
                    chatPreviewHolder.updatePreviewText(chatPreviewModel)
                    chatPreviewHolder.updateRead(chatPreviewModel)
                }

                if (payloadObject.unreadChanged) {
                    Log.d(TAG, "Payload: unread")
                    chatPreviewHolder.updateUnreadCount(chatPreviewModel)
                }

                if (payloadObject.chatNameChanged) {
                    Log.d(TAG, "Payload: chatName")
                    chatPreviewHolder.updateChatName(chatPreviewModel)
                }

                if (payloadObject.textChanged || payloadObject.senderNameChanged) {
                    Log.d(TAG, "Payload: text || senderName")
                    chatPreviewHolder.updatePreviewText(chatPreviewModel)
                }

                if (payloadObject.photoChanged) {
                    Log.d(TAG, "Payload: photo")
                    chatPreviewHolder.updatePhoto(chatPreviewModel)
                }

                if (payloadObject.readStatusChanged) {
                    Log.d(TAG, "Payload: readStatus")
                    chatPreviewHolder.updateRead(chatPreviewModel)
                }

                if (payloadObject.userOnlineChanged) {
                    Log.d(TAG, "Payload: userOnline")
                    chatPreviewHolder.updateOnline(chatPreviewModel)
                }

                if (payloadObject.favouriteChanged) {
                    Log.d(TAG, "Payload: favorite")
                    chatPreviewHolder.updateFavourite(chatPreviewModel)
                }

                if (payloadObject.protectedChanged) {
                    Log.d(TAG, "Payload: protected")
                    chatPreviewHolder.updateProtected(chatPreviewModel)
                }

                if (payloadObject.mutedChanged) {
                    Log.d(TAG, "Payload: muted")
                    chatPreviewHolder.updateMuted(chatPreviewModel)
                }
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is HeaderHolder) {
            // nothing to do
        } else if (holder is ChatPreviewHolder) {
            holder.bind(getItem(position))
        }
    }

    inner class HeaderHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        init {
            // TODO: move this shit out of here
            val linearLayoutManager =
                LinearLayoutManager(itemView.context, LinearLayoutManager.HORIZONTAL, false)
            itemView.rvFavourites.layoutManager = linearLayoutManager
            itemView.rvFavourites.adapter = favoriteConversationsAdapter

            if (selectMode) {
                itemView.etSearch.visibility = View.GONE
            }

            val callback =
                FavouriteConversationItemTouchHelperCallback(favoriteConversationsAdapter)
            val touchHelper = ItemTouchHelper(callback)
            touchHelper.attachToRecyclerView(itemView.rvFavourites)

            val itemOffsetDecoration =
                FavouriteUserOffsetDecoration(itemView.context.resources.getDimensionPixelSize(R.dimen.gapMedium))
            itemView.rvFavourites.addItemDecoration(itemOffsetDecoration)

            // scroll to insert position
            favoriteConversationsAdapter.registerAdapterDataObserver(object :
                RecyclerView.AdapterDataObserver() {
                override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                    linearLayoutManager.scrollToPosition(positionStart)
                }
            })

            setupSearch(itemView.etSearch)
        }

        private fun setupSearch(etSearch: EditText) {
            etSearch.setOnEditorActionListener { v, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    // TODO: search
                    hideKeyboard(v)
                }

                return@setOnEditorActionListener true
            }

            etSearch.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
                if (!hasFocus) {
                    hideKeyboard(v)
                }
            }

            etSearch.setOnClickListener {
                onSearchClick.invoke()
            }
        }

        private fun hideKeyboard(v: View) {
            (v.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                .hideSoftInputFromWindow(v.windowToken, 0)
            v.clearFocus()
        }
    }

    class ChatPreviewsDiffCallback : DiffUtil.ItemCallback<ChatPreviewModel>() {

        data class Payloads(
            var lastMessageChanged: Boolean = false,
            var unreadChanged: Boolean = false,
            var chatNameChanged: Boolean = false,
            var textChanged: Boolean = false,
            var photoChanged: Boolean = false,
            var senderNameChanged: Boolean = false,
            var readStatusChanged: Boolean = false,
            var userOnlineChanged: Boolean = false,
            var favouriteChanged: Boolean = false,
            var protectedChanged: Boolean = false,
            var mutedChanged: Boolean = false
        ) {
            fun hasChanges(): Boolean {
                return lastMessageChanged ||
                        unreadChanged ||
                        chatNameChanged ||
                        textChanged ||
                        photoChanged ||
                        senderNameChanged ||
                        readStatusChanged ||
                        userOnlineChanged ||
                        favouriteChanged ||
                        protectedChanged ||
                        mutedChanged
            }
        }

        override fun areItemsTheSame(
            oldItem: ChatPreviewModel,
            newItem: ChatPreviewModel
        ): Boolean {
            return oldItem.chatPreview.conversationId == newItem.chatPreview.conversationId &&
                    oldItem.chatPreview.conversationType == newItem.chatPreview.conversationType
        }

        override fun areContentsTheSame(
            oldItem: ChatPreviewModel,
            newItem: ChatPreviewModel
        ): Boolean {
            return oldItem.chatPreview.lastMessageId == newItem.chatPreview.lastMessageId &&
                    oldItem.chatPreview.unreadCount == newItem.chatPreview.unreadCount &&
                    oldItem.chatPreview.photo == newItem.chatPreview.photo &&
                    oldItem.chatPreview.chatName == newItem.chatPreview.chatName &&
                    oldItem.chatPreview.previewText == newItem.chatPreview.previewText &&
                    oldItem.chatPreview.userId == newItem.chatPreview.userId &&
                    oldItem.chatPreview.read == newItem.chatPreview.read &&
                    oldItem.online == newItem.isOnline() &&
                    oldItem.isFavourite() == newItem.isFavourite() &&
                    oldItem.isProtected() == newItem.isProtected() &&
                    oldItem.getDraft() == newItem.getDraft() &&
                    oldItem.isMuted() == newItem.isMuted() &&
                    oldItem.getDisplayChatName() == newItem.getDisplayChatName()
        }

        override fun getChangePayload(oldItem: ChatPreviewModel, newItem: ChatPreviewModel): Any? {
            val payloads = Payloads()

            if (oldItem.chatPreview.lastMessageId != newItem.chatPreview.lastMessageId)
                payloads.lastMessageChanged = true

            if (oldItem.chatPreview.unreadCount != newItem.chatPreview.unreadCount)
                payloads.unreadChanged = true

            if (oldItem.chatPreview.chatName != newItem.chatPreview.chatName ||
                oldItem.getDisplayChatName() != newItem.getDisplayChatName() // This doesn't work for some reason
            )
                payloads.chatNameChanged = true

            if (oldItem.chatPreview.previewText != newItem.chatPreview.previewText ||
                oldItem.getDraft() != newItem.getDraft()
            )
                payloads.textChanged = true

            if (oldItem.chatPreview.photo != newItem.chatPreview.photo ||
                oldItem.getDisplayChatName() != newItem.getDisplayChatName() // This doesn't work for some reason
            )
                payloads.photoChanged = true

            if (oldItem.chatPreview.userName != newItem.chatPreview.userName)
                payloads.senderNameChanged = true

            if (oldItem.chatPreview.read != newItem.chatPreview.read ||
                oldItem.getDraft() != newItem.getDraft()
            )
                payloads.readStatusChanged = true

            if (oldItem.online != newItem.isOnline())
                payloads.userOnlineChanged = true

            if (oldItem.isFavourite() != newItem.isFavourite()) {
                payloads.favouriteChanged = true
            }

            if (oldItem.isProtected() != newItem.isProtected()) {
                payloads.protectedChanged = true
            }

            if (oldItem.isMuted() != newItem.isMuted()) {
                payloads.mutedChanged = true
            }

            return if (payloads.hasChanges())
                payloads
            else
                null
        }
    }

}