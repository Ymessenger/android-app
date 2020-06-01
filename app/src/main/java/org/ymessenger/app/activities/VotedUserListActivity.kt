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

package org.ymessenger.app.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_voted_user_list.*
import org.ymessenger.app.R
import org.ymessenger.app.adapters.PollVotedUsersPagedAdapter
import org.ymessenger.app.adapters.viewholders.PollVotedUserHolder
import org.ymessenger.app.data.local.db.entities.User
import org.ymessenger.app.data.remote.NetworkState
import org.ymessenger.app.di.Injection
import org.ymessenger.app.models.PollVotedUser
import org.ymessenger.app.viewmodels.VotedUserListViewModel

class VotedUserListActivity : BaseActivity() {

    companion object {
        private const val ARG_POLL_ID = "pollId"
        private const val ARG_OPTION_ID = "optionId"
        private const val ARG_OPTION_TEXT = "optionText"
        private const val ARG_CONVERSATION_ID = "conversationId"
        private const val ARG_CONVERSATION_TYPE = "conversationType"
        private const val ARG_SIGN_REQUIRED = "signRequired"

        fun open(
            context: Context,
            pollId: String,
            optionId: Int,
            optionText: String,
            conversationId: Long,
            conversationType: Int,
            signRequired: Boolean
        ) {
            val intent = Intent(context, VotedUserListActivity::class.java)
            intent.putExtra(ARG_POLL_ID, pollId)
            intent.putExtra(ARG_OPTION_ID, optionId)
            intent.putExtra(ARG_OPTION_TEXT, optionText)
            intent.putExtra(ARG_CONVERSATION_ID, conversationId)
            intent.putExtra(ARG_CONVERSATION_TYPE, conversationType)
            intent.putExtra(ARG_SIGN_REQUIRED, signRequired)

            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_voted_user_list)
        initToolbar()

        if (!intent.hasExtra(ARG_POLL_ID) || !intent.hasExtra(ARG_OPTION_ID) || !intent.hasExtra(
                ARG_CONVERSATION_ID
            ) || !intent.hasExtra(ARG_CONVERSATION_TYPE)
        ) {
            throw IllegalArgumentException("You must start this activity using method 'open'")
        }

        val pollId = intent.getStringExtra(ARG_POLL_ID)
        val optionId = intent.getIntExtra(ARG_OPTION_ID, 0)
        title = intent.getStringExtra(ARG_OPTION_TEXT) // Setting up the toolbar title
        val conversationId = intent.getLongExtra(ARG_CONVERSATION_ID, 0)
        val conversationType = intent.getIntExtra(ARG_CONVERSATION_TYPE, 0)
        val signRequired = intent.getBooleanExtra(ARG_SIGN_REQUIRED, false)

        val factory = Injection.provideVotedUserListViewModelFactory(
            appBase,
            pollId!!,
            optionId,
            conversationId,
            conversationType,
            signRequired
        )

        val viewModel = ViewModelProviders.of(this, factory).get(VotedUserListViewModel::class.java)

        val adapter =
            PollVotedUsersPagedAdapter(object : PollVotedUserHolder.IPollVotedUserActions {
                override fun onItemClick(user: User) {
                    UserProfileActivity.startActivity(this@VotedUserListActivity, user.id)
                }

                override fun onVerificationStatusClick(verified: Boolean) {
                    if (verified) {
                        showToast(R.string.vote_is_confirmed)
                    } else {
                        showToast(R.string.vote_is_not_confirmed)
                    }
                }

                override fun verifyVote(
                    pollVotedUser: PollVotedUser,
                    verifyVoteSign: VotedUserListViewModel.IVerifyVoteSign
                ) {
                    viewModel.verifyVoteSign(pollVotedUser, verifyVoteSign)
                }
            })
        val layoutManager = LinearLayoutManager(this)
        rvUsers.layoutManager = layoutManager
        rvUsers.addItemDecoration(DividerItemDecoration(this, layoutManager.orientation))
        rvUsers.adapter = adapter

        swipeRefreshLayout.setOnRefreshListener {
            viewModel.refresh()
        }

        subscribeUi(viewModel, adapter)
    }

    private fun subscribeUi(
        viewModel: VotedUserListViewModel,
        adapter: PollVotedUsersPagedAdapter
    ) {
        viewModel.pollVotedUsers.observe(this, Observer {
            adapter.submitList(it)
        })

        viewModel.refreshState.observe(this, Observer {
            swipeRefreshLayout.isRefreshing = it == NetworkState.LOADING
        })
    }
}