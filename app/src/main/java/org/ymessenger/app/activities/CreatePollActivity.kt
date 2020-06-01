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

package org.ymessenger.app.activities

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import kotlinx.android.synthetic.main.activity_create_poll.*
import kotlinx.android.synthetic.main.item_option.view.*
import org.ymessenger.app.R
import org.ymessenger.app.data.remote.entities.Poll
import org.ymessenger.app.utils.SimpleTextWatcher
import org.ymessenger.app.viewmodels.CreatePollViewModel

class CreatePollActivity : BaseActivity() {

    private lateinit var viewModel: CreatePollViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_poll)

        viewModel = ViewModelProviders.of(this).get(CreatePollViewModel::class.java)

        initToolbar()

        btnAddOption.setOnClickListener { addNewOption() }
        fabDone.setOnClickListener { viewModel.done() }

        addNewOption()

        etQuestion.requestFocus()

        etQuestion.addTextChangedListener(object : SimpleTextWatcher() {
            override fun onChanged(text: String) {
                viewModel.setQuestion(text)
            }
        })

        switchMultipleSelection.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setMultipleSelection(isChecked)
        }

        switchShowResults.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setShowResultsToEveryone(isChecked)
        }

        subscribeUi(viewModel)
    }

    private fun subscribeUi(viewModel: CreatePollViewModel) {
        viewModel.optionsLeft.observe(this, Observer { optionsLeft ->
            if (optionsLeft <= 0) {
                btnAddOption.visibility = View.GONE
                tvOptionsLeftLabel.text = getString(R.string.no_options_left)
            } else {
                btnAddOption.visibility = View.VISIBLE
                tvOptionsLeftLabel.text =
                    resources.getQuantityString(R.plurals.options_left, optionsLeft, optionsLeft)
            }
        })

        viewModel.isValid.observe(this, Observer {
            if (it) {
                fabDone.show()
            } else {
                fabDone.hide()
            }
        })

        viewModel.pollCreatedEvent.observe(this, Observer {
            createPoll(it)
        })

        viewModel.subscribeOnEvents(this)
    }

    private fun addNewOption() {
        val newOption = layoutInflater.inflate(R.layout.item_option, llOptions, false)
        newOption.btnDeleteOption.setOnClickListener { removeOption(it) }

        newOption.editText.addTextChangedListener(object : SimpleTextWatcher() {
            override fun onChanged(text: String) {
                optionsChanged()
            }
        })

        llOptions.addView(newOption)
        newOption.editText.requestFocus()
        countOptions()
    }

    private fun removeOption(view: View) {
        llOptions.removeView(view.parent as View)
        countOptions()
        optionsChanged()
    }

    private fun optionsChanged() {
        val options = arrayListOf<String>()

        for (i in 0 until llOptions.childCount) {
            val option = llOptions.getChildAt(i).editText.text.toString()
            if (option.isNotBlank()) {
                options.add(option)
            }
        }

        viewModel.optionsChanged(options)
    }

    private fun countOptions() {
        viewModel.setOptionsCount(llOptions.childCount)
    }

    private fun createPoll(poll: Poll) {
        showToast("TODO: Attach the poll \"${poll.title}\" with ${poll.pollOptions.size} options and send message")
    }
}