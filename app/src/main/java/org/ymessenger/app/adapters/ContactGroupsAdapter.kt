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
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_contact_group.view.*
import org.ymessenger.app.R
import org.ymessenger.app.data.local.db.entities.ContactGroup

class ContactGroupsAdapter(private val onItemMoved: ((List<ContactGroup>) -> Unit)) :
    ListAdapter<ContactGroup, ContactGroupsAdapter.Holder>(ContactGroupDiffCallback()),
    ItemTouchHelperAdapter {

    var onItemClick: ((ContactGroup) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, type: Int): Holder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_contact_group, parent, false)

        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onItemMove(fromPosition: Int, toPosition: Int) {
        // We take currentList, make movement and update all rows in database
        val currentListImmutable = ArrayList(currentList)
        val contactGroup = getItem(fromPosition)
        currentListImmutable.removeAt(fromPosition)
        currentListImmutable.add(toPosition, contactGroup)
        onItemMoved.invoke(currentListImmutable)
    }

    override fun onItemDismiss(position: Int) {
        // nothing
    }

    inner class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        init {
            itemView.setOnClickListener { onItemClick?.invoke(getItem(adapterPosition)) }
        }

        fun bind(contactGroup: ContactGroup) {
            itemView.tvContactGroupName.text = contactGroup.name
        }
    }

    class ContactGroupDiffCallback : DiffUtil.ItemCallback<ContactGroup>() {
        override fun areItemsTheSame(oldItem: ContactGroup, newItem: ContactGroup): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ContactGroup, newItem: ContactGroup): Boolean {
            return oldItem.name == newItem.name
        }
    }

}