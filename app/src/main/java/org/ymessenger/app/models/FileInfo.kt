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

package org.ymessenger.app.models

import org.ymessenger.app.R
import java.io.File

class FileInfo(val name: String, val path: String, val isFolder: Boolean, val isParent: Boolean) :
    Comparable<FileInfo> {

    constructor(file: File, isParent: Boolean = false) : this(
        if (isParent) ".." else file.name,
        file.absolutePath,
        file.isDirectory,
        isParent
    )

    override fun compareTo(other: FileInfo): Int {
        return compareValuesBy(this, other, { !it.isFolder }, { it.name.toLowerCase() })
    }

    enum class Extension(val extension: String, val drawable: Int) {
        JPG(".jpg", R.drawable.file),
        JPEG(".jpeg", R.drawable.file),
        PNG(".png", R.drawable.file),
        V3GP(".3gp", R.drawable.file),
        MP4(".mp4", R.drawable.file),
        ZIP(".zip", R.drawable.file_zip),
        A7Z(".7z", R.drawable.file_zip),
        TAR(".tar", R.drawable.file_zip),
        APK(".apk", R.drawable.file_apk),
        PDF(".pdf", R.drawable.file_pdf);

        companion object {
            fun getTypesWithImage(): List<Extension> = arrayListOf(JPG, JPEG, PNG)
            fun getVideoTypes(): List<Extension> = arrayListOf(V3GP, MP4)
        }
    }

}