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
import org.ymessenger.app.adapters.viewholders.BaseClickListener
import org.ymessenger.app.adapters.viewholders.FoundMessageHolder
import org.ymessenger.app.models.FoundMessage

class FoundMessagesPagedAdapter(
    private val currentUserId: Long,
    private val glide: RequestManager,
    private val itemClickListeners: BaseClickListener<FoundMessage>
) : PagedListAdapter<FoundMessage, FoundMessageHolder>(FoundMessageDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FoundMessageHolder {
        return FoundMessageHolder.create(parent, currentUserId, glide, itemClickListeners)
    }

    override fun onBindViewHolder(holder: FoundMessageHolder, position: Int) {
        getItem(position)?.let {
            holder.bind(it)
        }
    }

    class FoundMessageDiffCallback : DiffUtil.ItemCallback<FoundMessage>() {
        override fun areItemsTheSame(oldItem: FoundMessage, newItem: FoundMessage): Boolean {
            return oldItem.message.globalId == newItem.message.globalId
        }

        override fun areContentsTheSame(oldItem: FoundMessage, newItem: FoundMessage): Boolean {
            return oldItem.message.conversationId == newItem.message.conversationId &&
                    oldItem.message.conversationType == newItem.message.conversationType
        }
    }
}