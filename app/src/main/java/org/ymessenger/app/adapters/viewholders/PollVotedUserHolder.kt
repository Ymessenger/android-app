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

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_poll_voted_user.view.*
import org.ymessenger.app.data.local.db.entities.User
import org.ymessenger.app.databinding.ItemPollVotedUserBinding
import org.ymessenger.app.models.PollVotedUser
import org.ymessenger.app.viewmodels.VotedUserListViewModel

class PollVotedUserHolder(
    val binding: ItemPollVotedUserBinding,
    private val pollVotedUserActions: IPollVotedUserActions
) : RecyclerView.ViewHolder(binding.root) {

    private var item: PollVotedUser? = null

    init {
        itemView.setOnClickListener {
            item?.let {
                pollVotedUserActions.onItemClick(it.user)
            }
        }

        itemView.ivVerificationStatus.setOnClickListener {
            item?.let {
                pollVotedUserActions.onVerificationStatusClick(it.signVerified)
            }
        }
    }

    fun bind(pollVotedUser: PollVotedUser) {
        item = pollVotedUser
        binding.pollVotedUser = pollVotedUser
        binding.isVerified = pollVotedUser.signVerified
        binding.isSignRequired = pollVotedUser.signRequired
        binding.executePendingBindings()

        if (pollVotedUser.signRequired && !pollVotedUser.signVerified) {
            binding.isLoading = true
            binding.executePendingBindings()
            pollVotedUserActions.verifyVote(
                pollVotedUser,
                object : VotedUserListViewModel.IVerifyVoteSign {
                    override fun verified() {
                        pollVotedUser.signVerified = true
                        item = pollVotedUser
                        updateVerifySignStatus(true)
                    }

                    override fun error() {
                        pollVotedUser.signVerified = false
                        item = pollVotedUser
                        updateVerifySignStatus(false)
                    }
                })
        }
    }

    private fun updateVerifySignStatus(verified: Boolean) {
        binding.isLoading = false
        binding.isVerified = verified
        binding.executePendingBindings()
    }

    interface IPollVotedUserActions {
        fun onItemClick(user: User)
        fun onVerificationStatusClick(verified: Boolean)

        fun verifyVote(
            pollVotedUser: PollVotedUser,
            verifyVoteSign: VotedUserListViewModel.IVerifyVoteSign
        )
    }

    companion object {
        fun create(
            parent: ViewGroup,
            pollVotedUserActions: IPollVotedUserActions
        ): PollVotedUserHolder {
            val binding = ItemPollVotedUserBinding
                .inflate(LayoutInflater.from(parent.context), parent, false)
            return PollVotedUserHolder(binding, pollVotedUserActions)
        }
    }

}