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
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_contact_group_edit.*
import org.ymessenger.app.R
import org.ymessenger.app.adapters.ContactsAdapter
import org.ymessenger.app.databinding.ActivityContactGroupEditBinding
import org.ymessenger.app.di.Injection
import org.ymessenger.app.viewmodels.ContactGroupEditViewModel

class ContactGroupEditActivity : BaseActivity() {

    private lateinit var viewModel: ContactGroupEditViewModel

    private lateinit var contactsAdapter: ContactsAdapter

    companion object {
        private const val KEY_CONTACT_GROUP_ID = "contactGroupId"

        private const val REQUEST_CODE_SELECT_CONTACTS = 100

        fun start(context: Context, contactGroupId: String?) {
            val intent = Intent(context, ContactGroupEditActivity::class.java)
            contactGroupId?.let {
                intent.putExtra(KEY_CONTACT_GROUP_ID, it)
            }
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var contactGroupId: String? = null
        if (intent.hasExtra(KEY_CONTACT_GROUP_ID)) {
            contactGroupId = intent.getStringExtra(KEY_CONTACT_GROUP_ID)
        }

        val factory = Injection.provideContactGroupEditViewModelFactory(appBase, contactGroupId)
        viewModel = ViewModelProviders.of(this, factory).get(ContactGroupEditViewModel::class.java)

        val binding: ActivityContactGroupEditBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_contact_group_edit)
        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        initToolbar()
        if (intent.hasExtra(KEY_CONTACT_GROUP_ID)) {
            supportActionBar?.setTitle(R.string.edit_contact_group)
        }

        contactsAdapter = ContactsAdapter {
            viewModel.addRemoveSelectedContact(it.contact)
        }

        val layoutManager = LinearLayoutManager(this)
        binding.rvContacts.layoutManager = layoutManager
        binding.rvContacts.addItemDecoration(DividerItemDecoration(this, layoutManager.orientation))
        binding.rvContacts.adapter = contactsAdapter
        contactsAdapter.setType(ContactsAdapter.TYPE_SELECTION)

        fabAdd.setOnClickListener {
            if (!canClick()) return@setOnClickListener
            addMembers()
        }

        fabRemove.setOnClickListener {
            if (!canClick()) return@setOnClickListener
            deleteMembers()
        }

        val settingsHelper = Injection.provideSettingsHelper(appBase)
        btnPrivacySettings.visibility =
            if (settingsHelper.getSyncContacts()) View.VISIBLE else View.GONE

        subscribeUi(viewModel)
    }

    private fun subscribeUi(viewModel: ContactGroupEditViewModel) {
        viewModel.subscribeOnEvents(this)

        viewModel.contactGroup.observe(this, Observer { })
        viewModel.groupContactList.observe(this, Observer { contacts ->
            contactsAdapter.submitList(contacts)
        })

        viewModel.selectedContactsId.observe(this, Observer {
            contactsAdapter.setSelectedContactsId(it)

            if (it.isNotEmpty()) {
                fabAdd.hide()
                fabRemove.show()
            } else {
                fabAdd.show()
                fabRemove.hide()
            }
        })

        viewModel.savedEvent.observe(this, Observer {
            finish()
        })
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            R.id.mi_delete -> {
                deleteContactGroup()
                true
            }
            R.id.mi_save -> {
                if (canClick()) {
                    save()
                    true
                } else false
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.edit_contact_group_menu, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        if (!intent.hasExtra(KEY_CONTACT_GROUP_ID)) {
            menu?.findItem(R.id.mi_delete)?.isVisible = false
        }
        return super.onPrepareOptionsMenu(menu)
    }

    private fun addMembers() {
        val intent = Intent(this, ContactsActivity::class.java)
        intent.putExtra(
            ContactsActivity.REQUEST_CODE,
            ContactsActivity.REQUEST_CODE_PICK_CONTACT_LIST
        )
        startActivityForResult(intent, REQUEST_CODE_SELECT_CONTACTS)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CODE_SELECT_CONTACTS -> {
                if (resultCode == Activity.RESULT_OK) {
                    if (data != null) {
                        @Suppress("UNCHECKED_CAST") val usersId =
                            data.getSerializableExtra(ContactsActivity.ARRAY_USERS_ID) as ArrayList<Long>
                        viewModel.addUsers(usersId)
                    }
                }
            }
        }
    }

    private fun deleteMembers() {
        viewModel.removeUsers()
    }

    private fun save() {
        // TODO: move valid check to viewModel
        if (!isFormValid()) {
            return
        }

        viewModel.saveContactGroup()
    }

    private fun isFormValid(): Boolean {
        var isValid = true

        contact_group_name_layout.error = null
        if (etGroupName.text.toString().trim().isEmpty()) {
            contact_group_name_layout.error = getString(R.string.field_can_not_be_empty)
            isValid = false
        }

        return isValid
    }

    private fun deleteContactGroup() {
        AlertDialog.Builder(this)
            .setTitle(R.string.attention)
            .setMessage(R.string.do_you_want_delete_this_contact_group)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.yes) { _, _ ->
                viewModel.removeContactGroup()
            }
            .show()
    }
}