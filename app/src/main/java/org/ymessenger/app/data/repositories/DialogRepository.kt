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

package org.ymessenger.app.data.repositories

import android.util.Log
import androidx.lifecycle.LiveData
import org.ymessenger.app.data.local.db.dao.DialogDao
import org.ymessenger.app.data.local.db.entities.Dialog
import org.ymessenger.app.data.mappers.DialogMapper
import org.ymessenger.app.data.remote.WebSocketService
import org.ymessenger.app.data.remote.requests.GetDialogs
import org.ymessenger.app.data.remote.responses.Conversations
import org.ymessenger.app.data.remote.responses.ResultResponse
import org.ymessenger.app.utils.AppExecutors

class DialogRepository(
    private val executors: AppExecutors,
    private val dialogDao: DialogDao,
    private val webSocketService: WebSocketService,
    private val dialogMapper: DialogMapper
) {

    fun getDialogs() = dialogDao.getDialogs()

    fun getDialog(dialogId: Long) = dialogDao.getDialog(dialogId)

    fun getDialogByUser(userId: Long): LiveData<Dialog> {
        getDialogsFromServer(listOf(userId))
        return dialogDao.getDialogByUser(userId)
    }

    private fun getDialogsFromServer(usersId: List<Long>) {
        val getDialogs = GetDialogs(usersId)
        webSocketService.getDialogs(
            getDialogs,
            object : WebSocketService.ResponseCallback<Conversations> {
                override fun onResponse(response: Conversations) {
                    val dbDialogs = response.conversations.map { dialogMapper.toDb(it) }

                    executors.diskIO.execute {
                        dialogDao.insert(dbDialogs)
                    }
                }

                override fun onError(error: ResultResponse) {
                    Log.d(TAG, "Failed to get data")
                }
            })
    }

    fun createDialog(dialog: Dialog) {
        executors.diskIO.execute {
            dialogDao.addDialog(dialog)
        }
    }

    fun deleteDialog(dialog: Dialog) {
        executors.diskIO.execute {
            dialogDao.removeDialog(dialog)
        }
    }

    fun updateDialog(dialog: Dialog) {
        executors.diskIO.execute {
            dialogDao.updateDialog(dialog)
        }
    }

    companion object {
        private const val TAG = "DialogRepository"
        private var instance: DialogRepository? = null

        fun getInstance(
            executors: AppExecutors,
            dialogDao: DialogDao,
            webSocketService: WebSocketService,
            dialogMapper: DialogMapper
        ): DialogRepository {
            return instance ?: synchronized(this) {
                instance
                    ?: DialogRepository(
                        executors,
                        dialogDao,
                        webSocketService,
                        dialogMapper
                    ).also { instance = it }
            }
        }
    }

}