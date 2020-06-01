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

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.PopupMenu
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.github.rubensousa.bottomsheetbuilder.BottomSheetBuilder
import kotlinx.android.synthetic.main.activity_subscribers.*
import org.ymessenger.app.R
import org.ymessenger.app.adapters.ChannelUsersPagedAdapter
import org.ymessenger.app.data.local.db.models.ChannelUserModel
import org.ymessenger.app.data.remote.NetworkState
import org.ymessenger.app.data.remote.entities.ChannelUser
import org.ymessenger.app.di.Injection
import org.ymessenger.app.viewmodels.ChannelUsersViewModel

class ChannelUsersActivity : BaseActivity() {

    private lateinit var viewModel: ChannelUsersViewModel
    private var userRole: Int? = null

    companion object {
        private const val ARG_CHANNEL_ID = "channel_id"

        private const val REQUEST_CODE_PICK_USERS_TO_ADD = 100

        fun start(context: Context, channelId: Long) {
            val intent = Intent(context, ChannelUsersActivity::class.java)
            intent.putExtra(ARG_CHANNEL_ID, channelId)

            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_subscribers)

        initToolbar()

        val channelId = getChannelId()
        val factory = Injection.provideChannelUsersViewModelFactory(appBase, channelId)
        viewModel = ViewModelProviders.of(this, factory).get(ChannelUsersViewModel::class.java)

        val adapter =
            ChannelUsersPagedAdapter(
                Glide.with(this),
                object : ChannelUsersPagedAdapter.ChannelUserClickListeners {
                    override fun onItemClick(item: ChannelUserModel) {
                        if (!canClick()) return
                        UserProfileActivity.startActivity(
                            this@ChannelUsersActivity,
                            item.channelUser.userId
                        )
                    }

                    override fun onLongItemClick(item: ChannelUserModel) {
                        if (item.isCreator() || item.channelUser.userId == appBase.authorizationManager.getAuthorizedUserId()) return
                        showChannelUserMenu(item)
                    }
                })

        val layoutManager = LinearLayoutManager(this)
        rvSubscribers.layoutManager = layoutManager
        rvSubscribers.adapter = adapter
        rvSubscribers.addItemDecoration(DividerItemDecoration(this, layoutManager.orientation))

        swipeRefreshLayout.setOnRefreshListener {
            viewModel.refresh()
        }

        fabAddChannelUsers.setOnClickListener {
            if (!canClick()) return@setOnClickListener
            pickUsersToAdd()
        }

        subscribeUi(viewModel, adapter)
    }

    private fun subscribeUi(viewModel: ChannelUsersViewModel, adapter: ChannelUsersPagedAdapter) {
        viewModel.channel.observe(this, Observer {
            userRole = it.userRole
            supportActionBar?.title = getString(R.string.subscribers_count, it.subscribersCount)
        })

        viewModel.channelUsers.observe(this, Observer {
            adapter.submitList(it)
        })

        viewModel.refreshState.observe(this, Observer {
            swipeRefreshLayout.isRefreshing = it == NetworkState.LOADING
        })

        viewModel.subscribeOnEvents(this)

        viewModel.contactsToAdd.observe(this, Observer {
            val usersId = arrayListOf<Long>()
            for (contactModel in it) {
                usersId.add(contactModel.contact.userId)
            }
            viewModel.addUsersToChannel(usersId)
        })
    }

    private fun getChannelId() =
        intent.extras?.getLong(ARG_CHANNEL_ID) ?: throw NullPointerException("Empty channel_id")

    private fun showChannelUserMenu(item: ChannelUserModel) {
        val popupMenu = PopupMenu(this, null)
        val menu = popupMenu.menu
        popupMenu.menuInflater.inflate(R.menu.channel_user_menu, menu)

        menu.findItem(R.id.mi_promote_to_admin).isVisible = item.isSubscriber()
        menu.findItem(R.id.mi_dismiss_admin_rights).isVisible = item.isAdmin()

        if (userRole == ChannelUser.ChannelUserRole.ADMINISTRATOR) {
            menu.findItem(R.id.mi_promote_to_admin).isVisible = false
            menu.findItem(R.id.mi_dismiss_admin_rights).isVisible = false
            menu.findItem(R.id.mi_remove_from_channel).isVisible = item.isSubscriber()
        }

        if (!menu.hasVisibleItems()) return

        val dialog = BottomSheetBuilder(this)
            .setMode(BottomSheetBuilder.MODE_LIST)
            .setMenu(menu)
            .setItemClickListener {
                when (it.itemId) {
                    R.id.mi_promote_to_admin -> promoteToAdmin(item)
                    R.id.mi_dismiss_admin_rights -> dismissAdminRights(item)
                    R.id.mi_remove_from_channel -> removeUserFromChannel(item)
                    else -> showToast(R.string.this_feature_unavailable)
                }
            }.createDialog()

        dialog.show()
    }

    private fun promoteToAdmin(channelUserModel: ChannelUserModel) {
        viewModel.promoteToAdmin(channelUserModel)
    }

    private fun dismissAdminRights(channelUserModel: ChannelUserModel) {
        viewModel.dismissAdminRights(channelUserModel)
    }

    private fun removeUserFromChannel(channelUserModel: ChannelUserModel) {
        viewModel.removeUserFromChannel(channelUserModel)
    }

    private fun pickUsersToAdd() {
        val intent =
            ContactsActivity.getIntent(this, ContactsActivity.REQUEST_CODE_PICK_CONTACT_LIST)
        startActivityForResult(intent, REQUEST_CODE_PICK_USERS_TO_ADD)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CODE_PICK_USERS_TO_ADD -> {
                if (resultCode == Activity.RESULT_OK) {
                    if (data != null) {
                        @Suppress("UNCHECKED_CAST") val contactsId =
                            data.getSerializableExtra(ContactsActivity.ARRAY_CONTACTS_ID) as ArrayList<String>
                        viewModel.addContactsToChannel(contactsId)
                    }
                }
            }
        }
    }
}