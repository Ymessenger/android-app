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

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.google.gson.Gson
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.stfalcon.imageviewer.StfalconImageViewer
import kotlinx.android.synthetic.main.activity_user_profile.*
import kotlinx.android.synthetic.main.dialog_qr_user_profile.view.*
import org.ymessenger.app.R
import org.ymessenger.app.data.local.qr.entities.QRUserProfile
import org.ymessenger.app.databinding.ActivityUserProfileBinding
import org.ymessenger.app.di.Injection
import org.ymessenger.app.viewmodels.UserProfileViewModel

class UserProfileActivity : BaseActivity() {

    private lateinit var viewModel: UserProfileViewModel

    companion object {
        private const val USER_ID = "userId"

        fun startActivity(context: Context, userId: Long) {
            val intent = Intent(context, UserProfileActivity::class.java)
            intent.putExtra(USER_ID, userId)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val userId = intent.extras?.getLong(USER_ID) ?: throw Exception("User id is null")
        val factory = Injection.provideUserProfileViewModelFactory(appBase, userId)
        viewModel = ViewModelProviders.of(this, factory).get(UserProfileViewModel::class.java)

        val binding: ActivityUserProfileBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_user_profile)
        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        initToolbar()

        binding.ivUserAvatar.setOnClickListener {
            if (!canClick()) return@setOnClickListener
            viewModel.openPhoto()
        }

        btnSendMessage.setOnClickListener {
            if (!canClick()) return@setOnClickListener
            clickStartDialog(false)
        }
        btnStartSecretChat.setOnClickListener {
            if (!canClick()) return@setOnClickListener
            clickStartDialog(true)
        }

        btnQrProfile.setOnClickListener {
            if (!canClick()) return@setOnClickListener
            showQRUserProfile(userId)
        }

        subscribeUi(viewModel)
    }

    override fun onResume() {
        super.onResume()
        viewModel.initTimer()
    }

    override fun onPause() {
        super.onPause()
        viewModel.cancelTimer()
    }

    private fun subscribeUi(viewModel: UserProfileViewModel) {
        viewModel.subscribeOnEvents(this)

        viewModel.openDialogEvent.observe(this, Observer { userId ->
            openDialog(userId)
        })

        viewModel.openPhotoEvent.observe(this, Observer {
            openPhoto(it)
        })

        viewModel.openEditContactEvent.observe(this, Observer {
            EditContactActivity.start(this, it)
        })

        viewModel.copyTagEvent.observe(this, Observer {
            copyTag(it)
        })

        viewModel.copyEmailEvent.observe(this, Observer {
            copyEmail(it)
        })

        viewModel.copyPhoneEvent.observe(this, Observer {
            copyPhone(it)
        })
    }

    private fun openPhoto(photoUrl: String) {
        StfalconImageViewer.Builder(this, listOf(photoUrl)) { imageView, image ->
            if (image != null) {
                Glide.with(this)
                    .load(image)
                    .thumbnail(0.1F)
                    .apply(RequestOptions().placeholder(R.drawable.no_avatar).override(Target.SIZE_ORIGINAL))
                    .into(imageView)
            } else {
                imageView.setImageResource(R.drawable.no_avatar)
            }
        }.withHiddenStatusBar(false)
            .show()
    }

    private fun clickStartDialog(isProtected: Boolean) {
        viewModel.openDialog()
    }

    private fun openDialog(userId: Long) {
        DialogActivity.start(this, userId)
    }

    private fun copyTag(tag: String) {
        copySomething("Tag", tag)
        showToast(R.string.tag_is_copied)
    }

    private fun copyEmail(email: String) {
        copySomething("Email", email)
        showToast(R.string.email_is_copied)
    }

    private fun copyPhone(phone: String) {
        copySomething("Phone", phone)
        showToast(R.string.phone_is_copied)
    }

    private fun copySomething(label: String, text: String) {
        val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboardManager.primaryClip =
            ClipData.newPlainText(label, text)
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
        dialogView.tvDescription.setText(R.string.scan_qr_code_to_find_this_user)
        dialogView.ivQRCode.setImageBitmap(bitmap)
        dialogBuilder.setNeutralButton(R.string.close) { dialogInterface, i ->
            // what
        }
        dialogBuilder.create().show()
    }

}