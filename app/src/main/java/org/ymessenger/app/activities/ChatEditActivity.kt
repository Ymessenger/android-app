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
import android.database.DatabaseUtils
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.PopupMenu
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.github.rubensousa.bottomsheetbuilder.BottomSheetBuilder
import kotlinx.android.synthetic.main.activity_edit_chat.*
import org.ymessenger.app.R
import org.ymessenger.app.data.local.db.entities.Chat
import org.ymessenger.app.di.Injection
import org.ymessenger.app.utils.ImageUtils
import org.ymessenger.app.viewmodels.ChatEditViewModel
import java.io.File
import java.io.IOException

class ChatEditActivity : BaseActivity() {

    private lateinit var viewModel: ChatEditViewModel

    companion object {
        private const val ARG_CHAT_ID = "chat_id"
        private const val REQUEST_CODE_PICK_IMAGE = 100
        private const val MY_PERMISSIONS_REQUEST_STORAGE = 400
        private const val MY_PERMISSIONS_REQUEST_STORAGE_RATIONALE = 401
        private const val TAG = "ChatEditActivity"

        fun start(context: Context, chatId: Long) {
            val intent = Intent(context, ChatEditActivity::class.java).apply {
                putExtra(ARG_CHAT_ID, chatId)
            }
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_chat)

        val chatId = intent.extras?.getLong(ARG_CHAT_ID) ?: throw Exception("Chat id is null")
        val factory = Injection.provideChatEditViewModelFactory(appBase, chatId)
        viewModel = ViewModelProviders.of(this, factory).get(ChatEditViewModel::class.java)

        initToolbar()

        fabSave.setOnClickListener {
            if (!canClick()) return@setOnClickListener
            saveChat()
        }
        ivChatPhoto.setOnClickListener {
            if (!canClick()) return@setOnClickListener
            onPhotoClicked()
        }

        subscribeUi(viewModel)
    }

    private fun subscribeUi(viewModel: ChatEditViewModel) {
        viewModel.chat.observe(this, Observer {
            // FIXME: it can return null
            setChat(it)
        })

        viewModel.subscribeOnEvents(this)

        viewModel.editedEvent.observe(this, Observer {
            finish()
        })

        appBase.getWebSocketService().getConnectionStatus().observe(this, Observer {
            viewModel.setConnected(it)
        })
    }

    private fun setChat(chat: Chat) {
        Glide.with(this)
            .load(chat.getPhotoUrl())
            .apply(RequestOptions().placeholder(R.drawable.no_chat_photo))
            .into(ivChatPhoto)

        etChatName.setText(chat.name)
        etAbout.setText(chat.about)
    }

    private fun saveChat() {
        val chatName = etChatName.text.toString().trim()
        val about = etAbout.text.toString().trim()

        viewModel.saveChat(chatName, about)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
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
            updateChatPhoto(fileId)
        }
    }

    private fun updateChatPhoto(fileId: String) {
        viewModel.updatePhoto(fileId)
    }

    private fun onPhotoClicked() {
        val popupMenu = PopupMenu(this, null)
        val menu = popupMenu.menu
        popupMenu.menuInflater.inflate(R.menu.set_photo_menu, menu)

        // Display copy option
        menu.findItem(R.id.mi_remove_photo).isVisible = viewModel.hasPhoto()

        val dialog = BottomSheetBuilder(this)
            .setMode(BottomSheetBuilder.MODE_LIST)
            .setMenu(menu)
            .setItemClickListener {
                val id = it.itemId
                when (id) {
                    R.id.mi_upload_photo -> setPhoto()
                    R.id.mi_remove_photo -> removePhoto()
                    else -> showToast(R.string.this_feature_unavailable)
                }
            }.createDialog()

        dialog.show()
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

    private fun removePhoto() {
        AlertDialog.Builder(this)
            .setTitle(R.string.attention)
            .setMessage(R.string.do_you_want_to_remove_photo)
            .setNegativeButton(R.string.cancel) { _, _ ->

            }
            .setPositiveButton(R.string.yes) { _, _ ->
                viewModel.removePhoto()
            }
            .show()
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