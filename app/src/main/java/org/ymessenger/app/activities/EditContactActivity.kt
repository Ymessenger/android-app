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

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import org.ymessenger.app.R
import org.ymessenger.app.databinding.ActivityEditContactBinding
import org.ymessenger.app.di.Injection
import org.ymessenger.app.viewmodels.EditContactViewModel

class EditContactActivity : BaseActivity() {

    companion object {
        private const val TAG = "ChangeNameActivity"

        private const val ARG_CONTACT_ID = "CONTACT_ID"

        fun start(context: Context, contactId: String) {
            val intent = Intent(context, EditContactActivity::class.java)
            intent.putExtra(ARG_CONTACT_ID, contactId)

            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding: ActivityEditContactBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_edit_contact)
        binding.lifecycleOwner = this

        initToolbar()

        val contactId = intent.getStringExtra(ARG_CONTACT_ID)

        if (contactId == null) {
            Log.e(TAG, "ContactId is null")
            showToast(R.string.unknown_error)
            finish()
            return
        }

        val factory = Injection.provideEditContactViewModelFactory(contactId, appBase)
        val viewModel = ViewModelProviders.of(this, factory).get(EditContactViewModel::class.java)

        binding.viewModel = viewModel

        subscribeUi(viewModel)
    }

    private fun subscribeUi(viewModel: EditContactViewModel) {
        viewModel.subscribeOnEvents(this)

        viewModel.doneEvent.observe(this, Observer {
            finish()
        })
    }

}