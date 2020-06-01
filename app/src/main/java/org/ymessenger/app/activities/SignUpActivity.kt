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

import android.content.Intent
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import kotlinx.android.synthetic.main.activity_sign_up.*
import org.ymessenger.app.R
import org.ymessenger.app.data.remote.entities.Node
import org.ymessenger.app.databinding.ActivitySignUpBinding
import org.ymessenger.app.di.Injection
import org.ymessenger.app.utils.FirstPlusTextWatcher
import org.ymessenger.app.viewmodels.SignUpViewModel

class SignUpActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding: ActivitySignUpBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_sign_up)

        val factory = Injection.provideSignUpViewModelFactory(appBase)
        val viewModel = ViewModelProviders.of(this, factory).get(SignUpViewModel::class.java)

        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        initToolbar()

        etPhoneCode.addTextChangedListener(FirstPlusTextWatcher())

        btnChangeServer.setOnClickListener { ChooseServerActivity.startActivity(this) }

        subscribeUi(viewModel)
    }

    private fun subscribeUi(viewModel: SignUpViewModel) {
        viewModel.subscribeOnEvents(this)

        viewModel.registerDoneEvent.observe(this, Observer {
            goToMainActivity()
        })

        appBase.nodeManager.getCurrentNode().observe(this, Observer {
            viewModel.setCurrentNode(it)
            updateCurrentServer(it)
        })

        appBase.getWebSocketService().getConnectionStatus().observe(this, Observer {
            tvConnectionStatus.text = getText(if (it) R.string.connected else R.string.connection)
            viewModel.setConnected(it)
        })

        viewModel.inputErrorList.observe(this, Observer {
            handleErrors(it)
        })
    }

    private fun handleErrors(inputErrorList: List<SignUpViewModel.InputError>) {
        phone_number_input_layout.error = null
        email_input_layout.error = null
        vcode_input_layout.error = null

        for (inputError in inputErrorList) {
            when (inputError.input) {
                SignUpViewModel.Input.Phone -> {
                    phone_number_input_layout.error = inputError.errorDescription
                }

                SignUpViewModel.Input.Email -> {
                    email_input_layout.error = inputError.errorDescription
                }

                SignUpViewModel.Input.VCode -> {
                    vcode_input_layout.error = inputError.errorDescription
                }
            }
        }
    }

    private fun updateCurrentServer(node: Node) {
        tvServerName.text = node.getDisplayName()
    }

    private fun goToMainActivity() {
        val intent = MainActivity.getIntentAfterRegister(this)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }
}