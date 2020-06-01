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

package org.ymessenger.app.fragments.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.fragment_results_page.*
import kotlinx.android.synthetic.main.fragment_results_page.view.*
import org.ymessenger.app.R
import org.ymessenger.app.adapters.FoundMessagesPagedAdapter
import org.ymessenger.app.adapters.viewholders.BaseClickListener
import org.ymessenger.app.data.remote.NetworkState
import org.ymessenger.app.di.Injection
import org.ymessenger.app.models.FoundMessage
import org.ymessenger.app.viewmodels.GlobalSearchViewModel
import org.ymessenger.app.viewmodels.SearchMessagesViewModel

class SearchMessagesResultFragment : BaseSearchTabFragment() {

    private lateinit var sharedViewModel: GlobalSearchViewModel

    private lateinit var viewModel: SearchMessagesViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sharedViewModel = activity?.run {
            ViewModelProviders.of(this).get(GlobalSearchViewModel::class.java)
        } ?: throw Exception("Invalid Activity")

        val factory = Injection.provideSearchMessagesViewModelFactory(appBase)
        viewModel = ViewModelProviders.of(this, factory).get(SearchMessagesViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_results_page, container, false)

        val linearLayoutManager = LinearLayoutManager(activity)
        view.recyclerView.layoutManager = linearLayoutManager
        view.recyclerView.addItemDecoration(
            DividerItemDecoration(
                activity,
                linearLayoutManager.orientation
            )
        )

        val currentUserId = appBase.authorizationManager.getAuthorizedUserId()
            ?: throw Exception("Current user id is null")
        val glide = Glide.with(this)

        val adapter = FoundMessagesPagedAdapter(
            currentUserId,
            glide,
            object : BaseClickListener<FoundMessage> {
                override fun onClick(item: FoundMessage) {
                    if (!canClick()) return

                    when {
                        item.isDialog() -> {
                            val dialogUserId = item.message.senderId?.let { senderId ->
                                if (senderId == currentUserId) {
                                    item.message.receiverId?.let { receiverId ->
                                        receiverId
                                    }
                                } else {
                                    senderId
                                }
                            }

                            dialogUserId?.let {
                                sharedViewModel.openDialog(it)
                            }
                        }

                        item.isChat() -> {
                            sharedViewModel.openChat(item.message.conversationId)
                        }

                        item.isChannel() -> {
                            sharedViewModel.openChannel(item.message.conversationId)
                        }
                    }

                    showToast("For now it does not really show specific message, just opens conversation")
                }

                override fun onLongClick(item: FoundMessage) {
                    // nothing
                }
            })

        view.recyclerView.adapter = adapter

        view.swipeRefreshLayout.setOnRefreshListener {
            viewModel.refreshMessages()
        }

        subscribeUi(adapter)
        return view
    }

    private fun subscribeUi(adapter: FoundMessagesPagedAdapter) {
        sharedViewModel.searchTextChangedEvent.observe(this, Observer {
            viewModel.search(it)
        })

        viewModel.foundMessages.observe(this, Observer {
            adapter.submitList(it)
        })

        viewModel.foundMessagesRefreshState.observe(this, Observer {
            swipeRefreshLayout.isRefreshing = it == NetworkState.LOADING
        })
    }

    override fun getTitle(): Int {
        return R.string.messages
    }
}