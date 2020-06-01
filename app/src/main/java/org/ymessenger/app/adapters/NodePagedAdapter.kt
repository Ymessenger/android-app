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
import androidx.databinding.DataBindingUtil
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import org.ymessenger.app.R
import org.ymessenger.app.adapters.viewholders.NodeHolder
import org.ymessenger.app.data.remote.entities.Node
import org.ymessenger.app.databinding.ItemServerBinding

class NodePagedAdapter(private val onItemClick: (Node) -> Unit) :
    PagedListAdapter<Node, NodeHolder>(NodeDiffCallback()) {

    private val expandedItems = hashSetOf<Long>()

    override fun onCreateViewHolder(parent: ViewGroup, type: Int): NodeHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = DataBindingUtil.inflate<ItemServerBinding>(
            inflater,
            R.layout.item_server,
            parent,
            false
        )

        return NodeHolder(expandedItems, binding, onItemClick)
    }

    override fun onBindViewHolder(holder: NodeHolder, position: Int) {
        holder.bind(getItem(position)!!) // may be an error
    }

    class NodeDiffCallback : DiffUtil.ItemCallback<Node>() {
        override fun areItemsTheSame(oldItem: Node, newItem: Node): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Node, newItem: Node): Boolean {
            return oldItem.getDisplayName() == newItem.getDisplayName() &&
                    oldItem.about == newItem.about &&
                    oldItem.photo == newItem.photo
        }
    }
}