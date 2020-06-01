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

import androidx.recyclerview.widget.RecyclerView
import org.ymessenger.app.data.remote.entities.Node
import org.ymessenger.app.databinding.ItemServerBinding
import org.ymessenger.app.utils.AnimationUtils

class NodeHolder(
    private val expandedItems: HashSet<Long>,
    var binding: ItemServerBinding,
    onItemClick: (Node) -> Unit
) :
    RecyclerView.ViewHolder(binding.root) {

    private var node: Node? = null

    init {
        binding.root.setOnClickListener {
            node?.let {
                onItemClick(it)
            }
        }

        binding.ivExpand.setOnClickListener { expand() }

        binding.tvServerName.setHorizontallyScrolling(true)
        binding.tvServerName.isSelected = true
    }

    fun bind(node: Node) {
        this.node = node
        binding.node = node
        val expanded = isExpanded()
        AnimationUtils.toggleArrow(binding.ivExpand, expanded, 0)
        binding.isExpanded = expanded
        binding.executePendingBindings()
    }

    private fun expand() {
        if (isExpanded()) {
            expandedItems.remove(node!!.id)
        } else {
            expandedItems.add(node!!.id)
        }

        val expanded = isExpanded()
        AnimationUtils.toggleArrow(binding.ivExpand, expanded)
        binding.isExpanded = expanded
    }

    private fun isExpanded(): Boolean {
        return expandedItems.contains(node!!.id)
    }
}