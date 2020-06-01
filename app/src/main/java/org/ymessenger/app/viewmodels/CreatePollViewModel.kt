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

import androidx.lifecycle.MutableLiveData
import org.ymessenger.app.R
import org.ymessenger.app.data.remote.entities.Poll
import org.ymessenger.app.data.remote.entities.PollOption
import org.ymessenger.app.utils.SingleLiveEvent

class CreatePollViewModel : BaseViewModel() {

    private var _question: String = ""
    private var _options = listOf<String>()
    private var multipleSelection: Boolean = false
    private var showResultsToEveryone: Boolean = true
    private var signRequired: Boolean = false
    val optionsLeft = MutableLiveData<Int>()
    val isValid = MutableLiveData<Boolean>().apply { postValue(false) }

    val pollCreatedEvent = SingleLiveEvent<Poll>()

    fun setOptionsCount(count: Int) {
        optionsLeft.postValue(MAX_OPTIONS - count)
    }

    fun setQuestion(question: String) {
        _question = question
        validation()
    }

    fun optionsChanged(options: List<String>) {
        _options = options
        validation()
    }

    fun setMultipleSelection(value: Boolean) {
        multipleSelection = value
    }

    fun setShowResultsToEveryone(value: Boolean) {
        showResultsToEveryone = value
    }

    fun setSignRequired(value: Boolean) {
        signRequired = value
    }

    private fun validation() {
        var valid = true

        if (_question.isBlank() || _options.size < 2) {
            valid = false
        }

        isValid.postValue(valid)
    }

    fun done() {
        if (_question.isBlank()) {
            showError(R.string.question_is_empty)
            return
        }

        if (_options.size < 2) {
            showError(R.string.at_least_two_options_are_needed)
            return
        }

        val pollOptions = arrayListOf<PollOption>()
        for ((index, option) in _options.withIndex()) {
            pollOptions.add(PollOption(index, option))
        }

        val poll =
            Poll(_question, multipleSelection, showResultsToEveryone, pollOptions, signRequired)

        pollCreatedEvent.postValue(poll)
    }

    companion object {
        private const val TAG = "CreatePollViewModel"
        private const val MAX_OPTIONS = 10
    }

}