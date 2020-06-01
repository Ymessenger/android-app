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
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import org.ymessenger.app.R
import org.ymessenger.app.data.ConversationType
import org.ymessenger.app.fragments.addfile.AddAttachmentFragment
import org.ymessenger.app.fragments.addfile.CreatePollFragment
import org.ymessenger.app.fragments.addfile.FilesFragment
import org.ymessenger.app.fragments.addfile.GalleryFragment

class AddAttachmentActivity : BaseActivity() {

    private val addAttachmentFragment = AddAttachmentFragment()

    private lateinit var fragmentToOpen: Fragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_attachment)

        val args = Bundle()
        args.putInt(
            AddAttachmentFragment.ARG_CONVERSATION_TYPE,
            intent.getIntExtra(AddAttachmentFragment.ARG_CONVERSATION_TYPE, ConversationType.DIALOG)
        )
        addAttachmentFragment.arguments = args

        initToolbar()

        addAttachmentFragment.onClickListener = { view ->
            when (view.id) {
                R.id.btnGallery -> {
                    fragmentToOpen = GalleryFragment()
                    checkStoragePermission()
                }
                R.id.btnDocuments -> {
                    fragmentToOpen = FilesFragment()
                    checkStoragePermission()
                }
                R.id.btnPoll -> {
                    if (intent.getBooleanExtra(AddAttachmentFragment.ARG_HAS_POLL, false)) {
                        showToast(R.string.you_can_add_only_one_poll_per_message)
                    } else {
                        openFragment(CreatePollFragment())
                    }
                }
            }
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.container_layout, addAttachmentFragment)
            .commit()
    }

    private fun openFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.container_layout, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun checkStoragePermission() {
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED
        ) {

            // Permission is not granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            ) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                AlertDialog.Builder(this)
                    .setTitle(R.string.permission_required)
                    .setMessage(R.string.app_needs_storage_permission)
                    .setNegativeButton(R.string.cancel) { _, _ ->

                    }
                    .setPositiveButton(R.string.ok) { _, _ ->
                        ActivityCompat.requestPermissions(
                            this,
                            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                            MY_PERMISSIONS_REQUEST_STORAGE_RATIONALE
                        )
                    }
                    .show()
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    MY_PERMISSIONS_REQUEST_STORAGE
                )

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        } else {
            // Permission has already been granted
            openFragment(fragmentToOpen)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {

            MY_PERMISSIONS_REQUEST_STORAGE -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    openFragment(fragmentToOpen)
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    askToOpenSettings()
                }
                return
            }

            MY_PERMISSIONS_REQUEST_STORAGE_RATIONALE -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    openFragment(fragmentToOpen)
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
        private const val MY_PERMISSIONS_REQUEST_STORAGE = 400
        private const val MY_PERMISSIONS_REQUEST_STORAGE_RATIONALE = 401

        fun getIntent(
            context: Context,
            conversationType: Int,
            hasPoll: Boolean,
            availableAttachments: Int
        ): Intent {
            val intent = Intent(context, AddAttachmentActivity::class.java)
            intent.putExtra(AddAttachmentFragment.ARG_CONVERSATION_TYPE, conversationType)
            intent.putExtra(AddAttachmentFragment.ARG_HAS_POLL, hasPoll)
            intent.putExtra(AddAttachmentFragment.ARG_AVAILABLE_ATTACHMENTS, availableAttachments)

            return intent
        }
    }

}