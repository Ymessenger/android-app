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
import android.graphics.Bitmap
import android.util.Log
import androidx.annotation.IntRange
import id.zelory.compressor.Compressor
import org.ymessenger.app.helpers.SequenceGenerator
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*


class ImageUtils {

    companion object {
        private const val TAG = "ImageUtils"

        private const val PICTURES_FOLDER_NAME = "photos"
        private const val MAX_IMAGE_SIZE = 1280

        fun compress(
            context: Context,
            originalFile: File, @IntRange(from = 1) dimension: Int = MAX_IMAGE_SIZE
        ): File {
            val path = context.getExternalFilesDir(null)
            val folder = File(path, PICTURES_FOLDER_NAME)
            if (!folder.exists()) {
                folder.mkdirs()
            }
            val filename = getDateFileName()
            Log.d(TAG, "Compressed filename: $filename")

            return Compressor(context)
                .setMaxWidth(dimension)
                .setMaxHeight(dimension)
                .setDestinationDirectoryPath(folder.absolutePath)
                .compressToFile(originalFile, filename)
        }

        private fun getDateFileName(): String {
            return "IMG_" + getDateTimeStamp() + "_" + SequenceGenerator.getString(6) + ".jpg"
        }

        private fun getDateTimeStamp(): String {
            val calendar = Calendar.getInstance()
            val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

            return dateFormat.format(calendar.time)
        }

        fun qrCodeToFile(context: Context, bitmap: Bitmap): File {
            val path = context.getExternalFilesDir(null)
            val folder = File(path, PICTURES_FOLDER_NAME)
            if (!folder.exists()) {
                folder.mkdirs()
            }
            val filename = getDateFileName()

            val file = File(folder, filename)

            val fOut = FileOutputStream(file)

            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fOut)
            fOut.flush()
            fOut.close()

            return file
        }
    }

}