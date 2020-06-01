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

package org.ymessenger.app.utils

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.AttributeSet
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.EditText
import androidx.core.view.inputmethod.EditorInfoCompat
import androidx.core.view.inputmethod.InputConnectionCompat
import androidx.core.view.inputmethod.InputContentInfoCompat

class RichEditText(context: Context, attributeSet: AttributeSet) : EditText(context, attributeSet) {

    private var keyboardInputCallbackListener: KeyboardInputCallbackListener? = null

    private val imageTypes = arrayOf("image/png", "image/gif")

    override fun onCreateInputConnection(editorInfo: EditorInfo?): InputConnection {
        if (editorInfo == null) {
            return super.onCreateInputConnection(editorInfo)
        } else {
            val ic: InputConnection = super.onCreateInputConnection(editorInfo)
            EditorInfoCompat.setContentMimeTypes(editorInfo, imageTypes)

            val callback =
                InputConnectionCompat.OnCommitContentListener { inputContentInfo, flags, opts ->
                    val lacksPermission = (flags and
                            InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION) != 0
                    // read and display inputContentInfo asynchronously
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1 && lacksPermission) {
                        try {
                            inputContentInfo.requestPermission()
                        } catch (e: Exception) {
                            return@OnCommitContentListener false // return false if failed
                        }
                    }

                    // read and display inputContentInfo asynchronously.
                    // call inputContentInfo.releasePermission() as needed.
                    keyboardInputCallbackListener?.onCommitContent(inputContentInfo, flags, opts)

                    true  // return true if succeeded
                }
            return InputConnectionCompat.createWrapper(ic, editorInfo, callback)
        }
    }

    interface KeyboardInputCallbackListener {
        fun onCommitContent(inputContentInfo: InputContentInfoCompat, flags: Int, opts: Bundle?)
    }

    fun setKeyboardInputCallbackListener(keyboardInputCallbackListener: KeyboardInputCallbackListener?) {
        this.keyboardInputCallbackListener = keyboardInputCallbackListener
    }

    companion object {
        private const val TAG = "RichEditText"
    }
}