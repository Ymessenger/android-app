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
import android.os.Bundle
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle
import kotlinx.android.synthetic.main.activity_pdf_viewer.*
import org.ymessenger.app.R
import java.io.IOException

class PDFViewerActivity : BaseSecureViewerActivity() {

    companion object {
        fun open(
            context: Context,
            fileLocation: String,
            senderId: Long,
            keyId: Long,
            signKeyId: Long
        ) = open(context, fileLocation, senderId, keyId, signKeyId, PDFViewerActivity::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_pdf_viewer)
        super.onCreate(savedInstanceState)
    }

    override fun onDataReady(data: ByteArray) {
        pdfView.fromBytes(data)
            .scrollHandle(DefaultScrollHandle(this))
            .onError {
                when (it) {
                    is IOException -> {
                        showToast(R.string.file_not_in_pdf_format_or_corrupted)
                        finish()
                    }

                    else -> {
                        showToast(R.string.unknown_error)
                        finish()
                    }
                }
            }
            .load()
    }

}