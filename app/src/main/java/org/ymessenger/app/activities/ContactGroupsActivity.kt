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
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_contact_groups.*
import org.ymessenger.app.R
import org.ymessenger.app.adapters.ContactGroupsAdapter
import org.ymessenger.app.adapters.SimpleItemTouchHelperCallback
import org.ymessenger.app.di.Injection
import org.ymessenger.app.viewmodels.ContactGroupListViewModel

class ContactGroupsActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contact_groups)

        val factory = Injection.provideContactGroupListViewModelFactory(appBase)
        val viewModel =
            ViewModelProviders.of(this, factory).get(ContactGroupListViewModel::class.java)

        initToolbar()

        val linearLayoutManager = LinearLayoutManager(this)
        rvContactGroups.layoutManager = linearLayoutManager
        rvContactGroups.addItemDecoration(
            DividerItemDecoration(
                this,
                linearLayoutManager.orientation
            )
        )

        val contactGroupsAdapter = ContactGroupsAdapter { contactGroups ->
            viewModel.updateSort(contactGroups)
        }

        rvContactGroups.adapter = contactGroupsAdapter
        val callback = SimpleItemTouchHelperCallback(contactGroupsAdapter)
        val touchHelper = ItemTouchHelper(callback)
        touchHelper.attachToRecyclerView(rvContactGroups)

        contactGroupsAdapter.onItemClick = {
            if (canClick()) {
                ContactGroupEditActivity.start(this, it.id)
            }
        }

        fabAddContactGroup.setOnClickListener {
            if (!canClick()) return@setOnClickListener
            addNewContactGroup()
        }

        subscribeUi(viewModel, contactGroupsAdapter)
    }

    private fun subscribeUi(
        viewModel: ContactGroupListViewModel,
        contactGroupsAdapter: ContactGroupsAdapter
    ) {
        viewModel.contactGroupList.observe(this, Observer { contactGroupsList ->
            contactGroupsAdapter.submitList(contactGroupsList)
        })
    }

    private fun addNewContactGroup() {
        ContactGroupEditActivity.start(this, null)
    }
}