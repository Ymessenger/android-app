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

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_get_phone_contacts.*
import org.ymessenger.app.R
import org.ymessenger.app.adapters.PhoneContactsAdapter
import org.ymessenger.app.di.Injection
import org.ymessenger.app.interfaces.SimpleItemClickListener
import org.ymessenger.app.models.PhoneContact
import org.ymessenger.app.viewmodels.PhoneContactsViewModel

class GetPhoneContactsActivity : BaseActivity() {

    private lateinit var viewModel: PhoneContactsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_get_phone_contacts)

        initToolbar()

        val factory = Injection.providePhoneContactViewModelFactory(appBase)
        viewModel = ViewModelProviders.of(this, factory).get(PhoneContactsViewModel::class.java)

        val adapter = PhoneContactsAdapter(object : SimpleItemClickListener<PhoneContact> {
            override fun onClick(item: PhoneContact) {
                item.number?.let {
                    copyNumber(it)
                }
            }
        })
        val layoutManager = LinearLayoutManager(this)
        rvPhoneContacts.layoutManager = layoutManager
        rvPhoneContacts.addItemDecoration(DividerItemDecoration(this, layoutManager.orientation))
        rvPhoneContacts.adapter = adapter

        subscribeUi(viewModel, adapter)

        checkReadContactPermission()
    }

    private fun getPhoneContacts() {
        viewModel.loadContacts()
    }

    private fun subscribeUi(viewModel: PhoneContactsViewModel, adapter: PhoneContactsAdapter) {
        viewModel.contactList.observe(this, Observer {
            adapter.submitList(it)
        })
    }

    private fun copyNumber(number: String) {
        val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboardManager.primaryClip =
            ClipData.newPlainText("number", number)
        showToast(R.string.copied)
    }

    private fun checkReadContactPermission() {
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED
        ) {

            // Permission is not granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.READ_CONTACTS
                )
            ) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                AlertDialog.Builder(this)
                    .setTitle(R.string.permission_required)
                    .setMessage(R.string.app_needs_read_contacts_permission)
                    .setNegativeButton(R.string.cancel) { _, _ ->

                    }
                    .setPositiveButton(R.string.ok) { _, _ ->
                        ActivityCompat.requestPermissions(
                            this,
                            arrayOf(Manifest.permission.READ_CONTACTS),
                            MY_PERMISSIONS_REQUEST_CONTACTS_RATIONALE
                        )
                    }
                    .show()
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_CONTACTS),
                    MY_PERMISSIONS_REQUEST_CONTACTS
                )

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        } else {
            // Permission has already been granted
            getPhoneContacts()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {

            MY_PERMISSIONS_REQUEST_CONTACTS -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    getPhoneContacts()
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    askToOpenSettings()
                }
                return
            }

            MY_PERMISSIONS_REQUEST_CONTACTS_RATIONALE -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    getPhoneContacts()
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
            }

            // Add other 'when' lines to check for other
            // permissions this app might request.
            else -> {
                // Ignore all other requests.
            }
        }
    }

    companion object {
        private const val MY_PERMISSIONS_REQUEST_CONTACTS = 100
        private const val MY_PERMISSIONS_REQUEST_CONTACTS_RATIONALE = 101
    }
}