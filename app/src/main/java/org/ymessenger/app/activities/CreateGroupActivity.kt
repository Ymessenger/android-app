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
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.database.DatabaseUtils
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_create_group.*
import org.ymessenger.app.R
import org.ymessenger.app.adapters.ContactsAdapter
import org.ymessenger.app.databinding.ActivityCreateGroupBinding
import org.ymessenger.app.di.Injection
import org.ymessenger.app.utils.ImageUtils
import org.ymessenger.app.viewmodels.CreateGroupViewModel
import java.io.File
import java.io.IOException

class CreateGroupActivity : BaseActivity() {

    private lateinit var viewModel: CreateGroupViewModel

    companion object {
        private const val TAG = "CreateGroupActivity"
        private const val REQUEST_CODE_SELECT_USERS = 100
        private const val REQUEST_CODE_PICK_IMAGE = 200
        private const val MY_PERMISSIONS_REQUEST_STORAGE = 400
        private const val MY_PERMISSIONS_REQUEST_STORAGE_RATIONALE = 401

        const val CONVERSATION_ID = "chat_id"
        const val CONVERSATION_TYPE = "conversation_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_group)

        val factory = Injection.provideCreateGroupViewModelFactory(appBase)
        viewModel = ViewModelProviders.of(this, factory).get(CreateGroupViewModel::class.java)

        val binding: ActivityCreateGroupBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_create_group)
        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        initToolbar()

        val contactsAdapter = ContactsAdapter {
            viewModel.addRemoveSelectedContact(it.contact.id)
        }

        val layoutManager = LinearLayoutManager(this)
        binding.rvGroupUsers.layoutManager = layoutManager
        binding.rvGroupUsers.addItemDecoration(
            DividerItemDecoration(
                this,
                layoutManager.orientation
            )
        )
        binding.rvGroupUsers.adapter = contactsAdapter
        contactsAdapter.setType(ContactsAdapter.TYPE_SELECTION)

        fabDone.setOnClickListener {
            if (!canClick()) return@setOnClickListener
            done()
        }

        btnAddMembers.setOnClickListener {
            if (!canClick()) return@setOnClickListener
            addMembers()
        }

        fabRemove.setOnClickListener {
            if (!canClick()) return@setOnClickListener
            deleteMembers()
        }

        ivGroupAvatar.setOnClickListener {
            if (!canClick()) return@setOnClickListener
            setPhoto()
        }

        subscribeUi(viewModel, contactsAdapter)
    }

    private fun subscribeUi(viewModel: CreateGroupViewModel, contactsAdapter: ContactsAdapter) {
        viewModel.contactList.observe(this, Observer {
            contactsAdapter.submitList(it)
        })

        viewModel.selectedContactsId.observe(this, Observer {
            contactsAdapter.setSelectedContactsId(it)

            if (it.isNotEmpty()) {
                fabDone.hide()
                fabRemove.show()
            } else {
                fabDone.show()
                fabRemove.hide()
            }
        })

        viewModel.createdEvent.observe(this, Observer { pair ->
            val resultIntent = Intent()
            resultIntent.putExtra(CONVERSATION_ID, pair.first)
            resultIntent.putExtra(CONVERSATION_TYPE, pair.second)
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        })

        viewModel.subscribeOnEvents(this)

        appBase.getWebSocketService().getConnectionStatus().observe(this, Observer {
            viewModel.setConnected(it)
        })
    }

    private fun addMembers() {
        val intent = Intent(this, ContactsActivity::class.java)
        intent.putExtra(
            ContactsActivity.REQUEST_CODE,
            ContactsActivity.REQUEST_CODE_PICK_CONTACT_LIST
        )
        startActivityForResult(intent, REQUEST_CODE_SELECT_USERS)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CODE_SELECT_USERS -> {
                if (resultCode == Activity.RESULT_OK) {
                    if (data != null) {
                        @Suppress("UNCHECKED_CAST") val contactsId =
                            data.getSerializableExtra(ContactsActivity.ARRAY_CONTACTS_ID) as ArrayList<String>
                        viewModel.submitGroupContacts(contactsId)
                    }
                }
            }

            REQUEST_CODE_PICK_IMAGE -> {
                appBase.isLocked = false // We don't need to lock app when picking image
                if (resultCode == RESULT_OK && data?.data != null) {
                    val uri = data.data!!
                    processFileUri(uri)
                }
            }
        }
    }

    private fun processFileUri(uri: Uri) {
        try {
            val path = getPicturePathFromUri(uri)
            val file = File(path)
            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
            uploadPhoto(bitmap, file)
        } catch (e: IOException) {
            e.printStackTrace()
            showError(R.string.storage_permission_denied)
        } catch (e: Exception) {
            e.printStackTrace()
            showError(R.string.try_to_save_image_locally)
        }
    }

    private fun getPicturePathFromUri(uri: Uri): String {
        val projection = arrayOf(MediaStore.Images.Media.DATA)

        val cursor = contentResolver.query(uri, projection, null, null, null)
            ?: throw Exception("Something went wrong")
        cursor.moveToFirst()

        Log.d(TAG, DatabaseUtils.dumpCursorToString(cursor))

        val columnIndex = cursor.getColumnIndex(projection[0])
        val picturePath = cursor.getString(columnIndex)
        cursor.close()
        return picturePath
    }

    private fun uploadPhoto(bitmap: Bitmap, file: File) {
        val dimension = 256

        // Generate file to send
        val fileTmp = ImageUtils.compress(this, file, dimension)

        viewModel.uploadFile(fileTmp) { fileId ->
            viewModel.setPhotoId(fileId)
            ivGroupAvatar.setImageBitmap(bitmap)
        }
    }

    private fun done() {
        if (!validation())
            return

        viewModel.saveGroup()
    }

    private fun validation(): Boolean {
        var validationResult = true
        group_name_layout.error = null

        val groupName = etGroupName.text.toString().trim()
        if (groupName.isEmpty()) {
            group_name_layout.error = getString(R.string.field_can_not_be_empty)
            validationResult = false
        }

        return validationResult
    }

    private fun deleteMembers() {
        viewModel.deleteMembers()
    }

    private fun setPhoto() {
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
            openChooseImageActivity()
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
                    openChooseImageActivity()
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
                    openChooseImageActivity()
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

    private fun openChooseImageActivity() {
        val intent = Intent().apply {
            type = "image/*"
            action = Intent.ACTION_PICK
        }
        startActivityForResult(
            Intent.createChooser(intent, "Select picture"),
            REQUEST_CODE_PICK_IMAGE
        )
    }
}