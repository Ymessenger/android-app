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

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.ymessenger.app.data.local.db.models.ChatPreviewModel
import org.ymessenger.app.data.repositories.ChatPreviewRepository
import org.ymessenger.app.data.repositories.FavoriteConversationRepository

class SelectChatViewModel(
    private val chatPreviewRepository: ChatPreviewRepository,
    private val favoriteConversationRepository: FavoriteConversationRepository
) : BaseViewModel() {

    val favoriteConversations = favoriteConversationRepository.getFavoriteConversations()

    private val chatPreviewsResult = chatPreviewRepository.getChatPreviewModels()

    val chatPreviews: LiveData<List<ChatPreviewModel>> =
        Transformations.map(chatPreviewsResult.itemList) {
            it
        }

    class Factory(
        private val chatPreviewRepository: ChatPreviewRepository,
        private val favoriteConversationRepository: FavoriteConversationRepository
    ) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return SelectChatViewModel(
                chatPreviewRepository, favoriteConversationRepository
            ) as T
        }
    }

    companion object {
        private const val TAG = "SelectChatViewModel"
    }
}