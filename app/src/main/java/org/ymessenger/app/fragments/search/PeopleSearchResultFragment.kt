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
import kotlinx.android.synthetic.main.fragment_results_page.*
import kotlinx.android.synthetic.main.fragment_results_page.view.*
import org.ymessenger.app.R
import org.ymessenger.app.adapters.UsersPagedAdapter
import org.ymessenger.app.data.remote.NetworkState
import org.ymessenger.app.viewmodels.GlobalSearchViewModel

class PeopleSearchResultFragment : BaseSearchTabFragment() {

    private lateinit var viewModel: GlobalSearchViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = activity?.run {
            ViewModelProviders.of(this).get(GlobalSearchViewModel::class.java)
        } ?: throw Exception("Invalid Activity")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_results_page, container, false)

        view?.tvNothingFound?.setText(R.string.nothing_found_people_label)

        val linearLayoutManager = LinearLayoutManager(activity)
        view.recyclerView.layoutManager = linearLayoutManager
        view.recyclerView.addItemDecoration(
            DividerItemDecoration(
                activity,
                linearLayoutManager.orientation
            )
        )

        val userAdapter = UsersPagedAdapter {
            if (!canClick()) return@UsersPagedAdapter
            viewModel.openDialog(it.id)
        }
        view.recyclerView.adapter = userAdapter

        view.swipeRefreshLayout.setOnRefreshListener {
            viewModel.refreshPeople()
        }

        subscribeUi(viewModel, userAdapter)
        return view
    }

    private fun subscribeUi(
        viewModel: GlobalSearchViewModel,
        usersPagedAdapter: UsersPagedAdapter
    ) {
        viewModel.people.observe(this, Observer {
            usersPagedAdapter.submitList(it)
        })

        viewModel.peopleRefreshState.observe(this, Observer {
            swipeRefreshLayout.isRefreshing = it == NetworkState.LOADING

            if (it == NetworkState.EMPTY) {
                view?.tvNothingFound?.visibility = View.VISIBLE
            } else {
                view?.tvNothingFound?.visibility = View.GONE
            }
        })
    }

    override fun getTitle(): Int {
        return R.string.people
    }
}