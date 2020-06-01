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

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_sessions.*
import org.ymessenger.app.R
import org.ymessenger.app.adapters.SessionsAdapter
import org.ymessenger.app.data.remote.Resource
import org.ymessenger.app.di.Injection
import org.ymessenger.app.viewmodels.SessionsViewModel

class SessionsActivity : BaseActivity() {

    private lateinit var viewModel: SessionsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sessions)
        initToolbar()

        val factory = Injection.provideSessionsViewModelFactory(appBase)
        viewModel = ViewModelProviders.of(this, factory).get(SessionsViewModel::class.java)

        val adapter = SessionsAdapter {
            if (!it.isCurrent) {
                AlertDialog.Builder(this)
                    .setTitle(R.string.attention)
                    .setMessage(R.string.do_you_really_want_to_close_this_session)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.yes) { _, _ ->
                        viewModel.closeSession(it)
                    }.show()
            }
        }

        val layoutManager = LinearLayoutManager(this)
        rvSessions.layoutManager = layoutManager
        rvSessions.addItemDecoration(DividerItemDecoration(this, layoutManager.orientation))
        rvSessions.adapter = adapter

        swipeRefreshLayout.setOnRefreshListener {
            viewModel.refresh()
        }

        subscribeUi(viewModel, adapter)
    }

    private fun subscribeUi(viewModel: SessionsViewModel, adapter: SessionsAdapter) {
        viewModel.subscribeOnEvents(this)

        viewModel.sessions.observe(this, Observer {
            when (it.status) {
                Resource.Status.SUCCESS -> {
                    adapter.submitList(it.data)
                    swipeRefreshLayout.isRefreshing = false
                }

                Resource.Status.LOADING -> {
                    swipeRefreshLayout.isRefreshing = true
                }

                Resource.Status.ERROR -> {
                    swipeRefreshLayout.isRefreshing = false
                    showToast(R.string.failed_to_load_data)
                }
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.sessions_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            R.id.mi_close_all_sessions -> {
                closeAllSessions()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun closeAllSessions() {
        AlertDialog.Builder(this)
            .setTitle(R.string.attention)
            .setMessage(R.string.do_you_want_to_close_all_sessions)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.yes) { _, _ ->
                viewModel.closeAllSessions()
            }.show()
    }

}