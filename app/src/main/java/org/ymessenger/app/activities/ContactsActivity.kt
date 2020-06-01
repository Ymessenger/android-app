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
import android.text.Editable
import android.text.TextWatcher
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import kotlinx.android.synthetic.main.activity_contacts.*
import org.ymessenger.app.R
import org.ymessenger.app.adapters.ContactsPagerAdapter
import org.ymessenger.app.di.Injection
import org.ymessenger.app.viewmodels.ContactListViewModel

class ContactsActivity : BaseActivity() {

    private var requestCode = NO_REQUEST_CODE
    private lateinit var contactsPagerAdapter: ContactsPagerAdapter

    companion object {
        const val REQUEST_CODE = "REQUEST_CODE"
        const val USER_ID = "USER_ID"
        const val ARRAY_CONTACTS_ID = "ARRAY_CONTACTS_ID"
        const val ARRAY_USERS_ID = "ARRAY_USERS_ID"

        const val NO_REQUEST_CODE = -1
        const val REQUEST_CODE_CHOOSE_CONTACT = 0
        const val REQUEST_CODE_PICK_CONTACT_LIST = 1

        fun getIntent(context: Context, requestCode: Int = NO_REQUEST_CODE): Intent {
            val intent = Intent(context, ContactsActivity::class.java)
            intent.putExtra(REQUEST_CODE, requestCode)
            return intent
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contacts)

        val factory = Injection.provideContactListViewModelFactory(appBase)
        val viewModel = ViewModelProviders.of(this, factory).get(ContactListViewModel::class.java)

        getRequestCode()
        viewModel.requestCode = requestCode

        initToolbar()

        contactsPagerAdapter = ContactsPagerAdapter(this, supportFragmentManager)

        viewPager.adapter = contactsPagerAdapter
        tabLayout.setupWithViewPager(viewPager)

        fabAction.setOnClickListener {
            if (!canClick()) return@setOnClickListener
            when (requestCode) {
                NO_REQUEST_CODE -> goToSearchUsers()
                REQUEST_CODE_PICK_CONTACT_LIST -> selectContacts(viewModel)
            }
        }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val filter = s.toString().trim().replace(" +".toRegex(), " ")
                viewModel.setFilter(filter)
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // nothing
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // nothing
            }
        })

        subscribeUi(viewModel)
    }

    private fun subscribeUi(viewModel: ContactListViewModel) {
        viewModel.contactGroupList.observe(this, Observer { contactGroupList ->
            if (contactGroupList != null) {
                contactsPagerAdapter.setData(contactGroupList)
            }
        })

        if (requestCode == REQUEST_CODE_PICK_CONTACT_LIST) {
            viewModel.selectedContactsId.observe(this, Observer { selectedContactsId ->
                if (selectedContactsId.isNotEmpty()) {
                    fabAction.show()
                    supportActionBar?.title =
                        getString(R.string.selected_placeholder, selectedContactsId.size)
                } else {
                    fabAction.hide()
                    supportActionBar?.setTitle(R.string.contacts)
                }
            })
        }
    }

    private fun getRequestCode() {
        if (intent.hasExtra(REQUEST_CODE)) {
            requestCode = intent.getIntExtra(REQUEST_CODE, NO_REQUEST_CODE)
        }
    }

    override fun onResume() {
        super.onResume()
        when (requestCode) {
            REQUEST_CODE_CHOOSE_CONTACT -> fabAction.hide()
            REQUEST_CODE_PICK_CONTACT_LIST -> {
                fabAction.setImageResource(R.drawable.ic_arrow_right)
            }
        }
    }

    private fun goToSearchUsers() {
        startActivity(Intent(this, GlobalSearchActivity::class.java))
    }

    private fun selectContacts(viewModel: ContactListViewModel) {
        val contactsId = (viewModel.selectedContactsId.value ?: listOf()) as ArrayList
        val usersId = (viewModel.selectedContacts.map { it.userId }) as ArrayList

        val intent = Intent()
        intent.putExtra(ARRAY_CONTACTS_ID, contactsId)
        intent.putExtra(ARRAY_USERS_ID, usersId)
        setResult(Activity.RESULT_OK, intent)

        finish()
    }
}