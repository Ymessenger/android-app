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

package org.ymessenger.app.data.local.db.dao

import androidx.room.*

interface BaseDao<T> {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(item: T): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(items: List<T>): List<Long>

    @Update
    fun update(item: T)

    @Update
    fun update(items: List<T>)

    @Delete
    fun delete(item: T)

    @Transaction
    fun upsert(item: T) {
        val id = insert(item)
        if (id == -1L) {
            update(item)
        }
    }

    @Transaction
    @JvmSuppressWildcards
    fun upsert(items: List<T>) {
        val insertResult = insert(items)
        val updateList = arrayListOf<T>()

        for ((index, result) in insertResult.withIndex()) {
            if (result == -1L) {
                updateList.add(items[index])
            }
        }

        if (updateList.isNotEmpty()) {
            update(updateList)
        }
    }

}