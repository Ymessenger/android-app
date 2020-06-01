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

import org.ymessenger.app.data.remote.entities.Attachment
import kotlin.random.Random

class AttachmentModel(
    val type: Int,
    val filePath: String?
) {
    val id: Long = Random.nextLong(Long.MAX_VALUE)
    var readyToSend: Boolean = false
    var fileId: String? = null
    var pollJson: String? = null

    val sortType: Int
        get() {
            return when (type) {
                Attachment.Type.PICTURE -> 0
                Attachment.Type.FILE -> 1
                Attachment.Type.POLL -> 2
                else -> Int.MAX_VALUE
            }
        }
}