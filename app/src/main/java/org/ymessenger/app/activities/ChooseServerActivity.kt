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
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_choose_server.*
import kotlinx.android.synthetic.main.item_server.*
import org.ymessenger.app.R
import org.ymessenger.app.adapters.NodePagedAdapter
import org.ymessenger.app.data.remote.NetworkState
import org.ymessenger.app.data.remote.entities.Node
import org.ymessenger.app.databinding.ActivityChooseServerBinding
import org.ymessenger.app.di.Injection
import org.ymessenger.app.utils.AndroidUtils
import org.ymessenger.app.utils.SimpleTextWatcher
import org.ymessenger.app.viewmodels.ServerListViewModel

class ChooseServerActivity : BaseActivity() {

    private lateinit var viewModel: ServerListViewModel

    companion object {
        private const val TAG = "ChooseServerActivity"

        fun startActivity(context: Context) {
            context.startActivity(Intent(context, ChooseServerActivity::class.java))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val factory = Injection.provideServerListViewModelFactory(appBase)
        viewModel = ViewModelProviders.of(this, factory).get(ServerListViewModel::class.java)
        val binding: ActivityChooseServerBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_choose_server)

        binding.viewModel = viewModel
        binding.setLifecycleOwner(this) // This enables MutableLiveData to be update on UI

        initToolbar()
        val nodePagedAdapter = NodePagedAdapter { node ->
            // Skip if we clicked on selected server
            if (node != viewModel.getCurrentNode().value) {
                selectNode(node)
            } else {
                showToast(R.string.this_server_is_already_selected)
            }
        }
        initRecyclerView(nodePagedAdapter)
        initSelectedServer()

        // Add scroll to top when data is changed
        nodePagedAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)
                rvServers.scrollToPosition(0)
            }
        })

        binding.etSearch.addTextChangedListener(object : SimpleTextWatcher() {
            override fun onChanged(text: String) {
                if (text.trim().isEmpty()) {
                    viewModel.search(null)
                }
            }
        })

        binding.etSearch.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = if (v.text.toString().trim().isNotBlank()) {
                    v.text.toString().trim()
                } else {
                    null
                }
                viewModel.search(query)
                AndroidUtils.hideKeyboard(v)
                true
            } else false
        }

        swipeRefreshLayout.setOnRefreshListener {
            viewModel.refresh()
        }

        fabDone.setOnClickListener {
            onBackPressed()
        }

        subscribeUi(viewModel, nodePagedAdapter)
    }

    private fun initRecyclerView(nodePagedAdapter: NodePagedAdapter) {
        val linearLayoutManager = LinearLayoutManager(this)
        rvServers.layoutManager = linearLayoutManager
        rvServers.addItemDecoration(DividerItemDecoration(this, linearLayoutManager.orientation))
        rvServers.adapter = nodePagedAdapter
    }

    private fun subscribeUi(viewModel: ServerListViewModel, nodePagedAdapter: NodePagedAdapter) {
        viewModel.getNodes().observe(this, Observer {
            nodePagedAdapter.submitList(it)
        })

        viewModel.nodeRefreshState.observe(this, Observer {
            swipeRefreshLayout.isRefreshing = it == NetworkState.LOADING
        })

        viewModel.getCurrentNode().observe(this, Observer {
            updateSelectedNode(it)
        })

        appBase.nodeManager.getCurrentNode().observe(this, Observer {
            viewModel.setCurrentNode(it)
        })
    }

    private fun selectNode(node: Node) {
        AlertDialog.Builder(this@ChooseServerActivity)
            .setTitle(R.string.changing_server)
            .setMessage(
                getString(
                    R.string.do_you_really_want_use_server_name,
                    node.getDisplayName()
                )
            )
            .setPositiveButton(R.string.yes) { _, _ ->
                appBase.nodeManager.setCurrentNode(node)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun initSelectedServer() {
        current_server_layout.visibility = View.GONE
        current_server_label.visibility = View.VISIBLE
        ivExpand.visibility = View.INVISIBLE

        tvServerName.setHorizontallyScrolling(true)
        tvServerName.isSelected = true
    }

    private fun updateSelectedNode(node: Node?) {
        if (node == null) {
            current_server_layout.visibility = View.GONE
        } else {
            current_server_layout.visibility = View.VISIBLE
        }
    }
}