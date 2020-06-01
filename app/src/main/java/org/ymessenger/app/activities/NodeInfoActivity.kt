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
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.activity_node_info.*
import kotlinx.android.synthetic.main.activity_sessions.swipeRefreshLayout
import org.ymessenger.app.R
import org.ymessenger.app.data.remote.Resource
import org.ymessenger.app.data.remote.entities.Node
import org.ymessenger.app.di.Injection
import org.ymessenger.app.viewmodels.NodeInfoViewModel

class NodeInfoActivity : BaseActivity() {

    private lateinit var viewModel: NodeInfoViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_node_info)
        initToolbar()

        val factory = Injection.provideNodeInfoViewModelFactory(appBase)
        viewModel = ViewModelProviders.of(this, factory).get(NodeInfoViewModel::class.java)

        swipeRefreshLayout.setOnRefreshListener {
            viewModel.refresh()
        }

        subscribeUi(viewModel)
    }

    private fun subscribeUi(viewModel: NodeInfoViewModel) {
        viewModel.subscribeOnEvents(this)

        viewModel.nodeResource.observe(this, Observer {
            when (it.status) {
                Resource.Status.SUCCESS -> {
                    // stop progress bar
                    swipeRefreshLayout.isRefreshing = false
                    setNode(it.data!!)
                }

                Resource.Status.LOADING -> {
                    // show progress bar
                    swipeRefreshLayout.isRefreshing = true
                }

                Resource.Status.ERROR -> {
                    // stop progress bar
                    swipeRefreshLayout.isRefreshing = false
                    showError(R.string.failed_to_load_data)
                }
            }
        })
    }

    private fun setNode(node: Node) {
        tvServerName.text = node.getDisplayName()
        tvIdentifier.text = node.tag

        tvPhotoLabel.text = node.getPhotoLabel()
        node.getPhotoUrl()?.let {
            Glide.with(this)
                .load(it)
                .into(ivServerImage)
        }

        tvDescription.text = node.about
        tvCountry.text = node.country ?: getString(R.string.no_info)

        tvEncryptionType.text = node.getEncryptionTypeLabel(this)
        tvPermanentlyDeleting.text = node.getPermanentlyDeletingLabel(this)
        tvRegistrationMethod.text = node.getRegistrationMethodLabel(this)
        tvSupportEmail.text = node.supportEmail ?: getString(R.string.not_specified)
        tvAdminEmail.text = node.adminEmail ?: getString(R.string.not_specified)
    }

}