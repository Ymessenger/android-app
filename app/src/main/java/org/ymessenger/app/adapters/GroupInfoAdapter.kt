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

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import org.ymessenger.app.R
import org.ymessenger.app.adapters.viewholders.ChatUserHolder
import org.ymessenger.app.adapters.viewholders.GroupInfoHeaderHolder
import org.ymessenger.app.data.local.db.entities.Chat
import org.ymessenger.app.data.local.db.models.ChatUserModel
import org.ymessenger.app.databinding.ItemChatUserListBinding
import org.ymessenger.app.utils.ListAdapterWithHeader

class GroupInfoAdapter(
    private val clickListener: (ChatUserModel) -> Unit,
    private val longClickListener: (ChatUserModel) -> Unit,
    private val onGroupPhotoClick: (url: String) -> Unit,
    private val onFavoriteClick: () -> Unit,
    private val onTagClick: (String) -> Unit
) :
    ListAdapterWithHeader<ChatUserModel, RecyclerView.ViewHolder>(ChatUserModelDiffCallback()) {

    private var chat: Chat? = null
    private var isFavorite: Boolean = false

    fun setChat(chat: Chat) {
        this.chat = chat
        notifyItemChanged(0)
    }

    fun updateFavorite(isFavorite: Boolean) {
        this.isFavorite = isFavorite
        notifyItemChanged(0)
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) HEADER_ITEM else CHAT_USER_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return if (viewType == HEADER_ITEM) {
            GroupInfoHeaderHolder(
                layoutInflater.inflate(R.layout.group_info_header, parent, false),
                onGroupPhotoClick,
                onFavoriteClick,
                onTagClick
            )
        } else {
            val binding =
                ItemChatUserListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            ChatUserHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is GroupInfoHeaderHolder) {
            chat?.let {
                holder.bind(it)
            }
            holder.updateFavorite(isFavorite)
        } else if (holder is ChatUserHolder) {
            holder.bind(getItem(position), clickListener, longClickListener)
        }
    }

    companion object {
        private const val HEADER_ITEM = 1
        private const val CHAT_USER_ITEM = 2
    }

    class ChatUserModelDiffCallback : DiffUtil.ItemCallback<ChatUserModel>() {
        override fun areItemsTheSame(oldItem: ChatUserModel, newItem: ChatUserModel): Boolean {
            return oldItem.chatUser.userId == newItem.chatUser.userId
        }

        override fun areContentsTheSame(oldItem: ChatUserModel, newItem: ChatUserModel): Boolean {
            return oldItem.getUser()?.firstName == newItem.getUser()?.firstName &&
                    oldItem.getUser()?.lastName == newItem.getUser()?.lastName &&
                    oldItem.getUser()?.photo == newItem.getUser()?.photo &&
                    oldItem.chatUser.userRole == newItem.chatUser.userRole
        }

    }

}