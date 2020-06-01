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
import androidx.recyclerview.widget.ListAdapter
import org.ymessenger.app.adapters.viewholders.ChatUserMiniHolder
import org.ymessenger.app.data.local.db.models.ChatUserModel
import org.ymessenger.app.databinding.ItemChatUserMiniBinding

class ChatUsersMiniAdapter(
    private val clickListener: (ChatUserModel) -> Unit
) : ListAdapter<ChatUserModel, ChatUserMiniHolder>(ChatUserModelDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatUserMiniHolder {
        val binding =
            ItemChatUserMiniBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChatUserMiniHolder(binding, clickListener)
    }

    override fun onBindViewHolder(holder: ChatUserMiniHolder, position: Int) {
        holder.bind(getItem(position))
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