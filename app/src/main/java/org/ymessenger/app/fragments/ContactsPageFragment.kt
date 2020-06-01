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

package org.ymessenger.app.fragments

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_contacts_page.view.*
import org.ymessenger.app.R
import org.ymessenger.app.activities.ContactsActivity
import org.ymessenger.app.activities.UserProfileActivity
import org.ymessenger.app.adapters.ContactsAdapter
import org.ymessenger.app.di.Injection
import org.ymessenger.app.viewmodels.ContactGroupPageViewModel
import org.ymessenger.app.viewmodels.ContactListViewModel

class ContactsPageFragment : BaseFragment() {

    private var contactGroupId: String? = null

    private lateinit var sharedViewModel: ContactListViewModel
    private lateinit var viewModel: ContactGroupPageViewModel

    companion object {
        private const val ARG_CONTACT_GROUP_ID = "contact_group_id"

        fun newInstance(
            contactGroupId: String?
        ): ContactsPageFragment {
            val contactsPageFragment = ContactsPageFragment()
            if (contactGroupId != null) {
                val bundle = Bundle()
                bundle.putString(ARG_CONTACT_GROUP_ID, contactGroupId)
                contactsPageFragment.arguments = bundle
            }

            return contactsPageFragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Obtaining contact group id
        arguments?.let {
            if (it.containsKey(ARG_CONTACT_GROUP_ID)) {
                contactGroupId = it.getString(ARG_CONTACT_GROUP_ID)
            }
        }

        sharedViewModel = activity?.run {
            ViewModelProviders.of(this).get(ContactListViewModel::class.java)
        } ?: throw Exception("Invalid Activity")

        viewModel = activity?.run {
            val factory = Injection.provideContactGroupPageViewModelFactory(appBase, contactGroupId)
            ViewModelProviders.of(this, factory)
                .get(contactGroupId ?: "null", ContactGroupPageViewModel::class.java)
        } ?: throw Exception("Invalid Activity")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_contacts_page, container, false)

        view?.tvNothingFound?.setText(R.string.you_did_not_add_anybody_to_your_contacts)

        val linearLayoutManager = LinearLayoutManager(activity)
        view.rvContacts.layoutManager = linearLayoutManager
        view.rvContacts.addItemDecoration(
            DividerItemDecoration(
                activity,
                linearLayoutManager.orientation
            )
        )
        val contactsAdapter = ContactsAdapter {
            when (sharedViewModel.requestCode) {
                ContactsActivity.NO_REQUEST_CODE -> {
                    if (canClick()) {
                        UserProfileActivity.startActivity(activity!!, it.contact.userId)
                    }
                }

                ContactsActivity.REQUEST_CODE_CHOOSE_CONTACT -> {
                    if (canClick()) {
                        val finishIntent = Intent()
                        finishIntent.putExtra(ContactsActivity.USER_ID, it.getUser()!!.id)
                        activity!!.setResult(Activity.RESULT_OK, finishIntent)
                        activity!!.finish()
                    }
                }

                ContactsActivity.REQUEST_CODE_PICK_CONTACT_LIST -> {
                    sharedViewModel.addSelectedContact(it.contact)
                }
            }
        }

        view.rvContacts.adapter = contactsAdapter
        if (sharedViewModel.requestCode == ContactsActivity.REQUEST_CODE_PICK_CONTACT_LIST) {
            contactsAdapter.setType(ContactsAdapter.TYPE_SELECTION)
        }

        subscribeUi(sharedViewModel, viewModel, contactsAdapter)
        return view
    }

    private fun subscribeUi(
        sharedViewModel: ContactListViewModel,
        viewModel: ContactGroupPageViewModel,
        contactsAdapter: ContactsAdapter
    ) {
        viewModel.contactList.observe(viewLifecycleOwner, Observer {
            contactsAdapter.submitList(it)
            if (it.isEmpty()) {
                view?.tvNothingFound?.visibility = View.VISIBLE
            } else {
                view?.tvNothingFound?.visibility = View.GONE
            }
        })

        sharedViewModel.searchQuery.observe(viewLifecycleOwner, Observer {
            viewModel.setFilter(it)
        })

        sharedViewModel.selectedContactsId.observe(viewLifecycleOwner, Observer {
            contactsAdapter.setSelectedContactsId(it)
        })
    }
}