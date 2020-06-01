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

package org.ymessenger.app.singleton

import android.os.Environment
import org.ymessenger.app.models.FileInfo
import java.io.File

object Data {
    var files = createFiles()

    private fun createFiles(): List<FileInfo> {
        val files = arrayListOf<FileInfo>()

        files.add(
            FileInfo(
                "Document.pdf",
                "",
                false,
                false
            )
        )

        files.add(
            FileInfo(
                "Video.3gp",
                "",
                false,
                false
            )
        )

        files.add(
            FileInfo(
                "New document.pdf",
                "",
                false,
                false
            )
        )

        files.add(
            FileInfo(
                "Music.mp3",
                "",
                false,
                false
            )
        )

        files.add(
            FileInfo(
                "App.apk",
                "",
                false,
                false
            )
        )

        files.add(
            FileInfo(
                "Archive.zip",
                "",
                false,
                false
            )
        )

        files.add(
            FileInfo(
                "One more archive.tar",
                "",
                false,
                false
            )
        )

        val file = File(
            Environment.getExternalStorageDirectory(),
            "Pictures/YMessenger/IMG_20181211_163705.jpg"
        )

        files.add(
            FileInfo(
                file
            )
        )

        files.add(
            FileInfo(
                "New video.mp4",
                "",
                false,
                false
            )
        )

        return files
    }
}