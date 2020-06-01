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

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.integration.android.IntentIntegrator
import kotlinx.android.synthetic.main.activity_qr_code_enter.*
import org.ymessenger.app.R
import org.ymessenger.app.di.Injection
import org.ymessenger.app.viewmodels.QRLoginViewModel
import java.io.IOException


class QRCodeEnterActivity : BaseActivity() {

    private lateinit var viewModel: QRLoginViewModel

    companion object {
        private const val TAG = "QRCodeEnterActivity"

        private const val REQUEST_CODE_PICK_IMAGE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_code_enter)

        initToolbar()

        val factory = Injection.provideQRLoginViewModelFactory(appBase)
        viewModel = ViewModelProviders.of(this, factory).get(QRLoginViewModel::class.java)

        btnScanQRCode.setOnClickListener {
            openScanner()
        }

        btnGetQRFromGallery.setOnClickListener {
            openChooseImageActivity()
        }

        subscribeUi()

        openScanner()
    }

    private fun subscribeUi() {
        viewModel.subscribeOnEvents(this)

        appBase.getWebSocketService().getConnectionStatus().observe(this, Observer {
            viewModel.setConnectionStatus(it)
        })

        viewModel.authorizedEvent.observe(this, Observer {
            goToMainActivity()
        })
    }

    private fun openScanner() {
        IntentIntegrator(this)
            .setPrompt(getString(R.string.scan_code_to_sign_in))
            .initiateScan()
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
                checkQRCode(result.contents)
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
            }
        }
    }

    private fun processFileUri(uri: Uri) {
        try {
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
            checkQRCode(result.text)
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

    private fun checkQRCode(data: String) {
        viewModel.processQRCode(data)
    }

    private fun goToMainActivity() {
        val intent = MainActivity.getIntentToGenerateQR(this)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

}