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
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Environment
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.google.gson.Gson
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.journeyapps.barcodescanner.BarcodeEncoder
import kotlinx.android.synthetic.main.activity_get_qr_code_for_login.*
import org.ymessenger.app.R
import org.ymessenger.app.data.remote.Resource
import org.ymessenger.app.data.remote.entities.QRCode
import org.ymessenger.app.di.Injection
import org.ymessenger.app.viewmodels.GetQRCodeForLoginViewModel
import java.io.File

class GetQRCodeForLoginActivity : BaseActivity() {

    private lateinit var viewModel: GetQRCodeForLoginViewModel

    private var bitmapToSave: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_get_qr_code_for_login)
        initToolbar()

        val factory = Injection.provideGetQRCodeForLoginViewModelFactory(appBase)
        viewModel = ViewModelProviders.of(this, factory).get(GetQRCodeForLoginViewModel::class.java)

        btnGetQRCode.setOnClickListener {
            if (!canClick()) return@setOnClickListener
            viewModel.getQRCode()
        }

        btnNotNow.setOnClickListener {
            if (!canClick()) return@setOnClickListener
            finish()
        }

        btnSave.setOnClickListener {
            if (!canClick()) return@setOnClickListener
            if (hasStoragePermission()) {
                saveQRCode()
            } else {
                showToast(R.string.you_dont_have_permissions_for_this_operation)
            }
        }

        subscribeUi()
    }

    private fun subscribeUi() {
        viewModel.qrCodeResource.observe(this, Observer {
            when (it.status) {
                Resource.Status.LOADING -> {
                    progressBar.visibility = View.VISIBLE
                    btnGetQRCode.isEnabled = false
                }

                Resource.Status.SUCCESS -> {
                    progressBar.visibility = View.INVISIBLE

                    it.data?.let { qrCode ->
                        showQRCode(qrCode)
                    }
                }

                Resource.Status.ERROR -> {
                    btnGetQRCode.isEnabled = true
                    progressBar.visibility = View.INVISIBLE
                    showError(R.string.unknown_error)
                }
            }
        })
    }

    private fun showQRCode(qrCode: QRCode) {
        val gson = Gson()
        val qrCodeString = gson.toJson(qrCode)

        bitmapToSave = generateQRCode(qrCodeString)
        if (bitmapToSave != null) {
            ivQRCode.setImageBitmap(bitmapToSave)
            btnSave.visibility = View.VISIBLE
            ivQRCode.visibility = View.VISIBLE
            tvSaveQrLabel.visibility = View.VISIBLE
        } else {
            showToast("Failed to generate QR code")
        }
    }

    private fun generateQRCode(text: String): Bitmap? {
        val multiFormatWriter = MultiFormatWriter()
        val size = 256
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

    private fun hasStoragePermission(): Boolean {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ), 0
            )
            return false
        }
        return true
    }

    private fun saveQRCode() {
        val folderToSave = File(
            Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES
            ), "Y messenger"
        )

        if (!folderToSave.exists()) {
            folderToSave.mkdirs()
        }

        bitmapToSave?.let {
            File(folderToSave, getFilename()).writeBitmap(it, Bitmap.CompressFormat.JPEG, 100)
            val savedTo = getString(R.string.qr_code_is_saved_to, folderToSave.path)
            showToast(savedTo)
            finish()
        }
    }

    private fun getFilename(): String {
        val time = System.currentTimeMillis() / 1000L
        return "YMQR_$time.jpg"
    }

    private fun File.writeBitmap(bitmap: Bitmap, format: Bitmap.CompressFormat, quality: Int) {
        outputStream().use { out ->
            bitmap.compress(format, quality, out)
            out.flush()
            out.close()
        }
    }
}