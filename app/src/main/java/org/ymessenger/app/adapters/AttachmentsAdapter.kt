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
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.bumptech.glide.RequestManager
import org.ymessenger.app.adapters.viewholders.AttachmentViewHolder
import org.ymessenger.app.models.AttachmentModel

class AttachmentsAdapter(
    private val glide: RequestManager,
    private val onDeleteClick: (AttachmentModel) -> Unit,
    private val onItemMoved: (fromPosition: Int, toPosition: Int) -> Unit
) :
    ListAdapter<AttachmentModel, AttachmentViewHolder>(AttachmentDiffCallback()),
    ItemTouchHelperAdapter {

    override fun onCreateViewHolder(parent: ViewGroup, p1: Int): AttachmentViewHolder {
        return AttachmentViewHolder.create(parent, glide, onDeleteClick)
    }

    override fun onBindViewHolder(holder: AttachmentViewHolder, position: Int) {
        holder.bind(getItem(position))
    }


    class AttachmentDiffCallback : DiffUtil.ItemCallback<AttachmentModel>() {
        override fun areItemsTheSame(oldItem: AttachmentModel, newItem: AttachmentModel): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: AttachmentModel,
            newItem: AttachmentModel
        ): Boolean {
            return oldItem.type == newItem.type
        }
    }

    override fun getItemViewType(position: Int): Int {
        return getItem(position).type
    }

    override fun onItemMove(fromPosition: Int, toPosition: Int) {
        onItemMoved.invoke(fromPosition, toPosition)
    }

    override fun onItemDismiss(position: Int) {
        // nothing
    }
}