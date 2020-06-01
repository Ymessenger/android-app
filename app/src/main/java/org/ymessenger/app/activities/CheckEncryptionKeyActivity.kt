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
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.DatabaseUtils
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.integration.android.IntentIntegrator
import com.journeyapps.barcodescanner.BarcodeEncoder
import kotlinx.android.synthetic.main.activity_check_encryption_key.*
import org.ymessenger.app.R
import org.ymessenger.app.data.ConversationType
import org.ymessenger.app.di.Injection
import org.ymessenger.app.viewmodels.CheckEncryptionKeyViewModel
import java.io.File
import java.io.IOException


class CheckEncryptionKeyActivity : BaseActivity() {

    private lateinit var viewModel: CheckEncryptionKeyViewModel

    companion object {
        private const val TAG = "CheckEncryptionKey"

        private const val ARG_CONVERSATION_ID = "conversationId"

        private const val REQUEST_CODE_PICK_IMAGE = 100
        private const val REQUEST_CODE_CONVERSATION_GALLERY = 200
        private const val MY_PERMISSIONS_REQUEST_STORAGE = 400
        private const val MY_PERMISSIONS_REQUEST_STORAGE_RATIONALE = 401

        const val ARG_HASH = "ARG_HASH"

        fun getIntent(context: Context, conversationId: Long): Intent {
            val intent = Intent(context, CheckEncryptionKeyActivity::class.java)
            intent.putExtra(ARG_CONVERSATION_ID, conversationId)

            return intent
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_check_encryption_key)
        initToolbar()

        if (!intent.hasExtra(ARG_CONVERSATION_ID)) {
            throw IllegalArgumentException("You should start this activity using `getIntent` method")
        }

        val conversationId = intent.getLongExtra(ARG_CONVERSATION_ID, 0)

        val factory = Injection.provideCheckEncryptionKeyViewModelFactory(conversationId, appBase)
        viewModel =
            ViewModelProviders.of(this, factory).get(CheckEncryptionKeyViewModel::class.java)

        btnSendQRCode.setOnClickListener {
            viewModel.sendQRCode()
        }

        btnScanQRCode.setOnClickListener {
            viewModel.scanQRCode()
        }

        btnGetQRFromMessages.setOnClickListener {
            viewModel.getQRCodeFromMessages()
        }

        btnGetQRFromGallery.setOnClickListener {
            viewModel.getQRCodeFromGallery()
        }

        subscribeUi(viewModel)
    }

    private fun subscribeUi(viewModel: CheckEncryptionKeyViewModel) {
        viewModel.subscribeOnEvents(this)

        viewModel.symmetricKeyHash.observe(this, Observer {
            generateQRCode(it)
            setHashText(it)
        })

        viewModel.sendQRCodeEvent.observe(this, Observer {
            sendMyQRCode(it)
        })

        viewModel.openScannerEvent.observe(this, Observer {
            openScanner()
        })

        viewModel.openGalleryEvent.observe(this, Observer {
            openGallery()
        })

        viewModel.openConversationGalleryEvent.observe(this, Observer {
            openConversationGallery(it, ConversationType.DIALOG)
        })

        viewModel.encryptionKeyIsVerified.observe(this, Observer {
            AlertDialog.Builder(this)
                .setTitle(R.string.verified)
                .setMessage(R.string.encryption_key_is_verified)
                .setNeutralButton(R.string.ok, null)
                .show()
        })
    }

    private fun generateQRCode(text: String) {
        val multiFormatWriter = MultiFormatWriter()
        val size = 256
        try {
            val bitMatrix = multiFormatWriter.encode(text, BarcodeFormat.QR_CODE, size, size)
            val barcodeEncoder = BarcodeEncoder()
            val bitmap = barcodeEncoder.createBitmap(bitMatrix)
            ivQRCode.setImageBitmap(bitmap)
        } catch (e: WriterException) {
            e.printStackTrace()
        }
    }

    private fun setHashText(hash: String) {
        tvKeyHash.text = hash
    }

    private fun openScanner() {
        IntentIntegrator(this)
//            .setPrompt(getString(R.string.scan_code_to_sign_in))
            .initiateScan()
    }

    private fun openGallery() {
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents == null) {
                // nothing
            } else {
                viewModel.checkHash(result.contents)
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
            when (requestCode) {
                REQUEST_CODE_PICK_IMAGE -> {
                    appBase.isLocked = false // We don't need to lock app when picking image
                    if (resultCode == RESULT_OK && data?.data != null) {
                        val uri = data.data!!
                        processFileUri(uri)
                    }
                }

                REQUEST_CODE_CONVERSATION_GALLERY -> {
                    if (resultCode == Activity.RESULT_OK) {
                        val url = data!!.getStringExtra(ConversationGalleryActivity.DATA_PHOTO_URL)
                        getPhotoFromUrl(url)
                    }
                }
            }
        }
    }

    private fun processFileUri(uri: Uri) {
        try {
            val path = getPicturePathFromUri(uri)
            val file = File(path)
            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
            scanQRCodeFromImage(bitmap)
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

    private fun scanQRCodeFromImage(bitmap: Bitmap) {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val source = RGBLuminanceSource(width, height, pixels)
        val binaryBitmap = BinaryBitmap(HybridBinarizer(source))

        val reader = MultiFormatReader()

        try {
            val result = reader.decode(binaryBitmap)
            Log.d(TAG, "Image scanned, content: ${result.text}")
            viewModel.checkHash(result.text)
        } catch (e: NotFoundException) {
            e.printStackTrace()
            showError(R.string.failed_to_recognize_qr_code)
        } catch (e: ChecksumException) {
            e.printStackTrace()
            showError(R.string.unknown_error)
        } catch (e: FormatException) {
            e.printStackTrace()
            showError(R.string.unknown_error)
        }
    }

    private fun sendMyQRCode(hash: String) {
        val intent = Intent()
        intent.putExtra(ARG_HASH, hash)

        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    private fun openConversationGallery(conversationId: Long, conversationType: Int) {
        val intent =
            ConversationGalleryActivity.getIntent(this, conversationId, conversationType, true)
        startActivityForResult(intent, REQUEST_CODE_CONVERSATION_GALLERY)
    }

    private fun getPhotoFromUrl(url: String) {
        Glide.with(this)
            .asBitmap()
            .load(url)
            .into(object : CustomTarget<Bitmap>() {
                override fun onLoadCleared(placeholder: Drawable?) {
                    // this is called when imageView is cleared on lifecycle call or for
                    // some other reason.
                    // if you are referencing the bitmap somewhere else too other than this imageView
                    // clear it here as you can no longer have the bitmap
                }

                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    scanQRCodeFromImage(resource)
                }
            })
    }
}