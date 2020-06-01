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

package org.ymessenger.app.helpers

object FileExtensionHelper {

    private val PDF_EXT = hashSetOf("pdf")

    private val IMAGE_EXT = hashSetOf("jpg", "png", "bmp")

    private const val GIF_EXT = "gif"

    enum class FileType {
        Image,
        PDF,
        GIF,
        Unsupported
    }

    fun isPDF(ext: String): Boolean {
        return PDF_EXT.contains(ext.toLowerCase())
    }

    fun isGIF(ext: String): Boolean {
        return GIF_EXT == ext.toLowerCase()
    }

    fun isImage(ext: String): Boolean {
        return IMAGE_EXT.contains(ext.toLowerCase())
    }

    fun getFileType(ext: String): FileType {
        return when {
            isPDF(ext) -> FileType.PDF

            isImage(ext) -> FileType.Image

            isGIF(ext) -> FileType.GIF

            else -> FileType.Unsupported
        }
    }
}