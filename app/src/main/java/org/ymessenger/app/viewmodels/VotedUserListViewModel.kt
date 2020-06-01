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
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.gson.Gson
import org.ymessenger.app.data.remote.Listing
import org.ymessenger.app.data.remote.requests.Polling
import org.ymessenger.app.data.repositories.KeysRepository
import org.ymessenger.app.data.repositories.PollRepository
import org.ymessenger.app.helpers.EncryptHelper
import org.ymessenger.app.helpers.EncryptionWrapper
import org.ymessenger.app.models.PollVotedUser

class VotedUserListViewModel(
    private val pollId: String,
    private val optionId: Int,
    private val conversationId: Long,
    private val conversationType: Int,
    private val signRequired: Boolean,
    private val pollRepository: PollRepository,
    private val encryptionWrapper: EncryptionWrapper,
    private val keysRepository: KeysRepository
) : BaseViewModel() {

    private val repoResult = MutableLiveData<Listing<PollVotedUser>>(
        pollRepository.getVotedUserList(
            pollId,
            optionId,
            conversationId,
            conversationType,
            signRequired
        )
    )

    val pollVotedUsers = Transformations.switchMap(repoResult) { it.pagedList }
    val refreshState = Transformations.switchMap(repoResult) { it.refreshState }

    fun refresh() {
        repoResult.value?.refresh?.invoke()
    }

    fun verifyVoteSign(pollVotedUser: PollVotedUser, verifyVoteSign: IVerifyVoteSign) {
        try {
            val yEncrypt = encryptionWrapper.getYEncrypt()

            val sign = EncryptHelper.base64ToBytes(pollVotedUser.sign!!)
            val metaData = yEncrypt.getMetaData(sign)
            val idSignKey = metaData.idSignKey

            keysRepository.getKeysByUser(pollVotedUser.user.id, idSignKey) { signKey ->
                try {
                    if (signKey == null) {
                        throw Exception("There is no sign key to verify message")
                    }

                    yEncrypt.publicSignKeyToReceive = signKey.publicKey
                    val decryptedSign = yEncrypt.veryfiMsg(sign)
                    val decryptedSignStr = String(decryptedSign.msg)
                    val gson = Gson()
                    val decryptedSignObj =
                        gson.fromJson(decryptedSignStr, Polling.Option.SignData::class.java)
                    if (decryptedSignObj.optionId == optionId) {
                        verifyVoteSign.verified()
                    } else {
                        throw Exception("Option id is not the same as in sign")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, e.message)
                    verifyVoteSign.error()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, e.message)
            verifyVoteSign.error()
        }
    }

    interface IVerifyVoteSign {
        fun verified()
        fun error()
    }

    class Factory(
        private val pollId: String,
        private val optionId: Int,
        private val conversationId: Long,
        private val conversationType: Int,
        private val signRequired: Boolean,
        private val pollRepository: PollRepository,
        private val encryptionWrapper: EncryptionWrapper,
        private val keysRepository: KeysRepository
    ) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return VotedUserListViewModel(
                pollId,
                optionId,
                conversationId,
                conversationType,
                signRequired,
                pollRepository,
                encryptionWrapper,
                keysRepository
            ) as T
        }
    }

    companion object {
        private const val TAG = "VotedUserListVM"
    }

}