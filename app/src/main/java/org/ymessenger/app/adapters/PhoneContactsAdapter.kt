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
import org.ymessenger.app.adapters.viewholders.PhoneContactViewHolder
import org.ymessenger.app.interfaces.SimpleItemClickListener
import org.ymessenger.app.models.PhoneContact

class PhoneContactsAdapter(private val itemClickListener: SimpleItemClickListener<PhoneContact>) :
    ListAdapter<PhoneContact, PhoneContactViewHolder>(PhoneContactDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, p1: Int): PhoneContactViewHolder {
        return PhoneContactViewHolder.create(parent, itemClickListener)
    }

    override fun onBindViewHolder(holder: PhoneContactViewHolder, position: Int) {
        holder.bind(getItem(position))
    }


    class PhoneContactDiffCallback : DiffUtil.ItemCallback<PhoneContact>() {
        override fun areItemsTheSame(oldItem: PhoneContact, newItem: PhoneContact): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: PhoneContact, newItem: PhoneContact): Boolean {
            return oldItem.name == newItem.name
        }
    }
}