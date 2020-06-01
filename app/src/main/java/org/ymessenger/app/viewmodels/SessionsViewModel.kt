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

package org.ymessenger.app.viewmodels

import android.util.Log
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.ymessenger.app.R
import org.ymessenger.app.data.remote.entities.Session
import org.ymessenger.app.data.repositories.SessionRepository
import org.ymessenger.app.helpers.AuthorizationManager
import org.ymessenger.app.utils.SingleLiveEvent

class SessionsViewModel(
    private val sessionRepository: SessionRepository,
    private val authorizationManager: AuthorizationManager
) : BaseViewModel() {

    private val updateSessions = SingleLiveEvent<Void>()
    val sessions = Transformations.switchMap(updateSessions) {
        sessionRepository.getSessions()
    }

    init {
        updateSessions.call()
    }

    fun refresh() {
        updateSessions.call()
    }

    fun closeSession(session: Session) {
        startLoading()
        authorizationManager.closeOtherSession(
            listOf(session.tokenId),
            object : AuthorizationManager.ClosingSessionCallback {
                override fun closed() {
                    endLoading()
                    refresh()
                }

                override fun error() {
                    endLoading()
                    showError(R.string.failed_to_close_session)
                }
            })
    }

    fun closeAllSessions() {
        val sessionList = sessions.value?.data ?: return

        val tokensIdToClose = arrayListOf<Long>()
        for (session: Session in sessionList) {
            if (session.isCurrent) continue
            tokensIdToClose.add(session.tokenId)
        }

        if (tokensIdToClose.isEmpty()) {
            showToast(R.string.nothing_to_close)
            return
        } else {
            startLoading()
            authorizationManager.closeOtherSession(
                tokensIdToClose,
                object : AuthorizationManager.ClosingSessionCallback {
                    override fun closed() {
                        Log.d(TAG, "All sessions are closed")
                        endLoading()
                        refresh()
                    }

                    override fun error() {
                        Log.e(TAG, "Failed to close sessions")
                        endLoading()
                        showError(R.string.failed_to_perform_operation)
                    }
                })
        }
    }

    class Factory(
        private val sessionRepository: SessionRepository,
        private val authorizationManager: AuthorizationManager
    ) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return SessionsViewModel(sessionRepository, authorizationManager) as T
        }
    }

    companion object {
        private const val TAG = "SessionsViewModel"
    }
}