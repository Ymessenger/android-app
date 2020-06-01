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
import org.ymessenger.app.adapters.viewholders.PollVotedUserHolder
import org.ymessenger.app.models.PollVotedUser

class PollVotedUsersPagedAdapter(
    private val pollVotedUserActions: PollVotedUserHolder.IPollVotedUserActions
) : PagedListAdapter<PollVotedUser, PollVotedUserHolder>(PollVotedUserDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, type: Int): PollVotedUserHolder {
        return PollVotedUserHolder.create(parent, pollVotedUserActions)
    }

    override fun onBindViewHolder(holder: PollVotedUserHolder, position: Int) {
        holder.bind(getItem(position)!!) // may be an error
    }

    class PollVotedUserDiffCallback : DiffUtil.ItemCallback<PollVotedUser>() {
        override fun areItemsTheSame(oldItem: PollVotedUser, newItem: PollVotedUser): Boolean {
            return oldItem.user.id == newItem.user.id
        }

        override fun areContentsTheSame(oldItem: PollVotedUser, newItem: PollVotedUser): Boolean {
            return oldItem.user.firstName == newItem.user.firstName &&
                    oldItem.user.lastName == newItem.user.lastName &&
                    oldItem.user.photo == newItem.user.photo &&
                    oldItem.signVerified == newItem.signVerified
        }

    }
}