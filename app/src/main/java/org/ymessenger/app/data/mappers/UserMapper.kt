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

package org.ymessenger.app.data.mappers

import com.google.gson.Gson
import org.ymessenger.app.data.remote.entities.User

class UserMapper {
    fun toDb(from: User) = org.ymessenger.app.data.local.db.entities.User(
        from.id!!,
        from.nameFirst,
        from.nameSecond,
        from.about,
        if (!from.emails.isNullOrEmpty()) from.emails.first() else null,
        if (!from.phones.isNullOrEmpty()) Gson().toJson(from.phones.first()) else null,
        from.photo,
        from.online,
        from.tag,
        PrivacyConverter.toString(from.privacy),
        from.confirmed,
        from.banned
    )
}