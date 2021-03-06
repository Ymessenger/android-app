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
import android.util.Log
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import org.ymessenger.app.R
import org.ymessenger.app.databinding.ActivityChangeNameBinding
import org.ymessenger.app.di.Injection
import org.ymessenger.app.viewmodels.ChangeNameViewModel

class ChangeNameActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding: ActivityChangeNameBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_change_name)
        binding.lifecycleOwner = this

        initToolbar()

        val userId = appBase.authorizationManager.getAuthorizedUserId()

        if (userId == null) {
            Log.e(TAG, "UserId is null")
            showToast(R.string.unknown_error)
            finish()
            return
        }

        val factory = Injection.provideChangeNameViewModelFactory(appBase, userId)
        val viewModel = ViewModelProviders.of(this, factory).get(ChangeNameViewModel::class.java)

        binding.viewModel = viewModel

        subscribeUi(viewModel)
    }

    private fun subscribeUi(viewModel: ChangeNameViewModel) {
        viewModel.subscribeOnEvents(this)

        viewModel.doneEvent.observe(this, Observer {
            finish()
        })
    }

    companion object {
        private const val TAG = "ChangeNameActivity"
    }

}