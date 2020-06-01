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
import android.content.ClipData
import android.content.ClipboardManager
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
import com.github.rubensousa.bottomsheetbuilder.BottomSheetBuilder
import com.google.gson.Gson
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.journeyapps.barcodescanner.BarcodeEncoder
import kotlinx.android.synthetic.main.activity_settings.*
import kotlinx.android.synthetic.main.dialog_qr_user_profile.view.*
import org.ymessenger.app.BuildConfig
import org.ymessenger.app.R
import org.ymessenger.app.data.local.db.entities.User
import org.ymessenger.app.data.local.qr.entities.QRUserProfile
import org.ymessenger.app.data.remote.entities.UserPhone
import org.ymessenger.app.di.Injection
import org.ymessenger.app.fragments.PrefsFragment
import org.ymessenger.app.utils.ImageUtils
import org.ymessenger.app.viewmodels.SettingsViewModel
import java.io.File
import java.io.IOException

class SettingsActivity : BaseActivity() {

    private lateinit var viewModel: SettingsViewModel

    private lateinit var prefsFragment: PrefsFragment

    private var clicksToUnlock = 0
    private var lastClickTime = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val settingsHelper2 = Injection.provideSettingsHelper(appBase)
        val userId =
            settingsHelper2.getToken()?.userId ?: throw Exception("There is no userId. Abort")

        val factory = Injection.provideSettingsViewModelFactory(userId, appBase)
        viewModel = ViewModelProviders.of(this, factory).get(SettingsViewModel::class.java)

        initToolbar()
        initPreferences()

        ivUserAvatar.setOnClickListener {
            if (!canClick()) return@setOnClickListener
            onPhotoClicked()
        }

        layoutPhotoAndName.setOnClickListener {
            if (!canClick()) return@setOnClickListener

            startActivity(Intent(this@SettingsActivity, ChangeNameActivity::class.java))
        }

        layoutEmail.setOnClickListener {
            if (!canClick()) return@setOnClickListener

            startActivity(Intent(this@SettingsActivity, ChangeEmailActivity::class.java))
        }

        layoutPhoneNumber.setOnClickListener {
            if (!canClick()) return@setOnClickListener

            startActivity(Intent(this@SettingsActivity, ChangePhoneActivity::class.java))
        }

        layoutTag.setOnClickListener {
            if (!canClick()) return@setOnClickListener

            val tagText = tvTag.text.toString().trim()
            if (tagText.isNotBlank()) {
                copyTag(tagText)
            }
        }

        layoutAbout.setOnClickListener {
            if (!canClick()) return@setOnClickListener

            startActivity(Intent(this@SettingsActivity, ChangeAboutActivity::class.java))
        }

        tvVersion.text = getString(R.string.version_label, BuildConfig.VERSION_NAME)
        tvVersion.setOnClickListener {
            clickToUnlock()
        }

        btnShowQR.setOnClickListener {
            if (!canClick()) return@setOnClickListener
            showQRUserProfile(userId)
        }

        subscribeUi(viewModel)
    }

    private fun initPreferences() {
        prefsFragment = PrefsFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.preferences_container, prefsFragment)
            .commit()
    }

    private fun subscribeUi(viewModel: SettingsViewModel) {
        viewModel.user.observe(this, Observer { user ->
            if (user != null) {
                setUser(user)
            }
        })

        viewModel.subscribeOnEvents(this)

        appBase.getWebSocketService().getConnectionStatus().observe(this, Observer {
            viewModel.setConnected(it)
        })
    }

    private fun setUser(user: User) {
        Glide.with(this)
            .load(user.getPhotoUrl())
            .into(ivUserAvatar)

        tvPhotoLabel.text = user.getPhotoLabel()

        tvName.text = user.fullName

        val userPhone = Gson().fromJson(user.phone, UserPhone::class.java)
        if (userPhone == null) {
            tvPhoneNumber.setText(R.string.phone)
            tvPhoneTapLabel.setText(R.string.tap_to_add)
        } else {
            tvPhoneNumber.text = userPhone.fullNumber
            tvPhoneTapLabel.setText(R.string.tap_to_change)
        }

        if (user.email == null) {
            tvEmail.setText(R.string.email)
            tvEmailTapLabel.setText(R.string.tap_to_add)
        } else {
            tvEmail.text = user.email
            tvEmailTapLabel.setText(R.string.tap_to_change)
        }


        if (user.about.isNullOrBlank()) {
            tvAbout.setText(R.string.about)
            tvAboutTapLabel.setText(R.string.tap_to_add)
        } else {
            tvAbout.text = user.about
            tvAboutTapLabel.setText(R.string.tap_to_change_about)
        }

        tvTag.text = user.tag
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_CODE_PICK_IMAGE -> {
                appBase.isLocked = false // We don't need to lock app when picking image
                if (resultCode == Activity.RESULT_OK && data?.data != null) {
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
            updateUserPhoto(fileId)
        }
    }

    private fun updateUserPhoto(fileId: String) {
        Log.d(TAG, fileId)
        viewModel.updateUserPhoto(fileId)
    }

    private fun clickToUnlock() {
        if (appBase.settingsHelper.isDevOptionsUnlocked()) {
            return
        }

        if (System.currentTimeMillis() - lastClickTime > 1000) {
            clicksToUnlock = 0
        }

        clicksToUnlock++
        lastClickTime = System.currentTimeMillis()

        if (clicksToUnlock > 3) {
            val clicksLeft = CLICKS_TO_UNLOCK_DEV_OPTIONS - clicksToUnlock
            val text = resources.getQuantityString(R.plurals.clicks_left, clicksLeft, clicksLeft)
            showToast(text)
        }

        if (clicksToUnlock == CLICKS_TO_UNLOCK_DEV_OPTIONS) {
            appBase.settingsHelper.setDevOptionsUnlocked(true)
            prefsFragment.showDeveloperOptions(true)
            showToast(R.string.you_have_unlocked_developer_options)
        }
    }

    private fun copyTag(tag: String) {
        val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboardManager.primaryClip =
            ClipData.newPlainText("Tag", tag)
        showToast(R.string.copied)
    }

    private fun showQRUserProfile(userId: Long) {
        val qrUserProfile = QRUserProfile(userId, 1)
        val gson = Gson()
        val qrUserProfileJson = gson.toJson(qrUserProfile)
        val bitmap = generateQRCode(qrUserProfileJson)
        if (bitmap != null) {
            showDialogWithQR(bitmap)
        } else {
            showError(R.string.unknown_error)
        }
    }

    private fun generateQRCode(text: String): Bitmap? {
        val multiFormatWriter = MultiFormatWriter()
        val size = 300
        try {
            val bitMatrix = multiFormatWriter.encode(text, BarcodeFormat.QR_CODE, size, size)
            val barcodeEncoder = BarcodeEncoder()
            val bitmap = barcodeEncoder.createBitmap(bitMatrix)

            return bitmap
        } catch (e: WriterException) {
            e.printStackTrace()
        }

        return null
    }

    private fun showDialogWithQR(bitmap: Bitmap) {
        val dialogBuilder = AlertDialog.Builder(this)
        val dialogView = layoutInflater.inflate(R.layout.dialog_qr_user_profile, null)
        dialogBuilder.setView(dialogView)
        dialogView.ivQRCode.setImageBitmap(bitmap)
        dialogBuilder.setNeutralButton(R.string.close) { dialogInterface, i ->
            // what
        }
        dialogBuilder.create().show()
    }

    companion object {
        private const val TAG = "SettingsActivity"
        private const val REQUEST_CODE_PICK_IMAGE = 100
        private const val MY_PERMISSIONS_REQUEST_STORAGE = 400
        private const val MY_PERMISSIONS_REQUEST_STORAGE_RATIONALE = 401

        private const val CLICKS_TO_UNLOCK_DEV_OPTIONS = 10
    }

}