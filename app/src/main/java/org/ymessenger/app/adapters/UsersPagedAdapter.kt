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
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import org.ymessenger.app.adapters.viewholders.UserHolder
import org.ymessenger.app.data.local.db.entities.User
import org.ymessenger.app.databinding.ItemUserSimpleListBinding

class UsersPagedAdapter(private val onUserClick: (User) -> Unit) :
    PagedListAdapter<User, UserHolder>(UserDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, type: Int): UserHolder {
        val binding =
            ItemUserSimpleListBinding.inflate(LayoutInflater.from(parent.context), parent, false)

        return UserHolder(binding)
    }

    override fun onBindViewHolder(holder: UserHolder, position: Int) {
        holder.bind(getItem(position)!!, onUserClick, onUserClick) // may be an error
    }

    class UserDiffCallback : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem.firstName == newItem.firstName &&
                    oldItem.lastName == newItem.lastName &&
                    oldItem.photo == newItem.photo
        }

    }
}