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
import org.ymessenger.app.adapters.viewholders.ChannelUserViewHolder
import org.ymessenger.app.data.local.db.models.ChannelUserModel

class ChannelUsersPagedAdapter(
    private val glide: RequestManager,
    private val channelUserClickListeners: ChannelUserClickListeners
) : PagedListAdapter<ChannelUserModel, ChannelUserViewHolder>(ChannelUserModelDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, type: Int): ChannelUserViewHolder {
        return ChannelUserViewHolder.create(parent, glide, channelUserClickListeners)
    }

    override fun onBindViewHolder(holder: ChannelUserViewHolder, position: Int) {
        holder.bind(getItem(position)!!) // may be an error
    }

    class ChannelUserModelDiffCallback : DiffUtil.ItemCallback<ChannelUserModel>() {
        override fun areItemsTheSame(
            oldItem: ChannelUserModel,
            newItem: ChannelUserModel
        ): Boolean {
            return oldItem.channelUser.userId == newItem.channelUser.userId
        }

        override fun areContentsTheSame(
            oldItem: ChannelUserModel,
            newItem: ChannelUserModel
        ): Boolean {
            return oldItem.getUser()?.photo == newItem.getUser()?.photo &&
                    oldItem.getUser()?.fullName == newItem.getUser()?.fullName &&
                    oldItem.channelUser.channelUserRole == newItem.channelUser.channelUserRole
        }

    }

    interface ChannelUserClickListeners {
        fun onItemClick(item: ChannelUserModel)
        fun onLongItemClick(item: ChannelUserModel)
    }
}