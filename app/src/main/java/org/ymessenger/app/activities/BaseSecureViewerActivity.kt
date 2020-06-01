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

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import org.ymessenger.app.R
import org.ymessenger.app.di.Injection
import org.ymessenger.app.viewmodels.BaseSecureViewerViewModel
import java.io.File

abstract class BaseSecureViewerActivity : BaseActivity() {

    private lateinit var viewModel: BaseSecureViewerViewModel

    companion object {
        private const val ARG_FILE_PATH = "FILE_PATH"
        private const val ARG_SENDER_ID = "SENDER_ID"
        private const val ARG_KEY_ID = "KEY_ID"
        private const val ARG_SIGN_KEY_ID = "SIGN_KEY_ID"

        fun open(
            context: Context,
            fileLocation: String,
            senderId: Long,
            keyId: Long,
            signKeyId: Long,
            activityClass: Class<*>
        ) {
            val intent = Intent(context, activityClass)
            intent.putExtra(ARG_FILE_PATH, fileLocation)
            intent.putExtra(ARG_SENDER_ID, senderId)
            intent.putExtra(ARG_KEY_ID, keyId)
            intent.putExtra(ARG_SIGN_KEY_ID, signKeyId)

            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // disable screenshots, hide content
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        initToolbar()

        val factory = Injection.provideBaseSecureViewerViewModelFactory(appBase)
        viewModel = ViewModelProviders.of(this, factory).get(BaseSecureViewerViewModel::class.java)

        val filePath =
            intent.getStringExtra(ARG_FILE_PATH) ?: throw Exception("File location is empty")
        val senderId = intent.getLongExtra(ARG_SENDER_ID, 0)
        val keyId = intent.getLongExtra(ARG_KEY_ID, 0)
        val signKeyId = intent.getLongExtra(ARG_SIGN_KEY_ID, 0)

        val file = File(filePath)
        if (!file.exists()) {
            showError(R.string.file_does_not_exist)
        } else {
            viewModel.decryptFile(file, senderId, keyId, signKeyId)
        }

        subscribeUi(viewModel)
    }

    private fun subscribeUi(viewModel: BaseSecureViewerViewModel) {
        viewModel.subscribeOnEvents(this)

        viewModel.decryptedBytes.observe(this, Observer {
            it?.let {
                onDataReady(it)
            }
        })
    }

    protected abstract fun onDataReady(data: ByteArray)

}