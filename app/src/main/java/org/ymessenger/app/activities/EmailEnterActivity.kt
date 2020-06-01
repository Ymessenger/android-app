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

import android.animation.LayoutTransition
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintSet
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import kotlinx.android.synthetic.main.activity_email_enter.*
import org.ymessenger.app.R
import org.ymessenger.app.data.remote.entities.Node
import org.ymessenger.app.di.Injection
import org.ymessenger.app.utils.AnimationUtils
import org.ymessenger.app.utils.LengthEndActionTextWatcher
import org.ymessenger.app.utils.WidgetUtils
import org.ymessenger.app.viewmodels.EmailLoginViewModel


class EmailEnterActivity : BaseActivity() {

    private lateinit var viewModel: EmailLoginViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_email_enter)

        val factory = Injection.provideEmailLoginViewModelFactory(appBase)
        viewModel = ViewModelProviders.of(this, factory).get(EmailLoginViewModel::class.java)

        initToolbar()

        // this is need for views animation
        clRoot.layoutTransition.enableTransitionType(LayoutTransition.CHANGING)

        btnChangeServer.setOnClickListener { openChooseServer() }
        btnReceiveCode.setOnClickListener { sendCode() }
        btnDone.setOnClickListener { login() }

        etEmail.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO) {
                sendCode()
                return@setOnEditorActionListener true
            }

            false
        }

        etCode.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                login()
                return@setOnEditorActionListener true
            }

            false
        }
        val codeLength = resources.getInteger(R.integer.verification_code_length)
        etCode.addTextChangedListener(LengthEndActionTextWatcher(codeLength) {
            login()
        })

        // This is centered the error message
        WidgetUtils.textInputLayoutCenterErrorMessage(emailLayout)
        WidgetUtils.textInputLayoutCenterErrorMessage(codeLayout)

        subscribeUi(viewModel)
    }

    private fun subscribeUi(viewModel: EmailLoginViewModel) {
        viewModel.subscribeOnEvents(this)

        viewModel.getStatus().observe(this, Observer { handleStatus(it) })
        viewModel.getState().observe(this, Observer { handleState(it) })
        viewModel.loading.observe(this, Observer {
            progressBar.visibility = if (it) {
                View.VISIBLE
            } else {
                View.GONE
            }

            btnReceiveCode.isEnabled = !it
            btnDone.isEnabled = !it
        })

        appBase.nodeManager.getCurrentNode().observe(this, Observer {
            updateCurrentServer(it)
        })

        appBase.getWebSocketService().getConnectionStatus().observe(this, Observer {
            tvConnectionStatus.text = getText(if (it) R.string.connected else R.string.connection)
            viewModel.setConnectionStatus(it)
        })

        viewModel.emailEnabled.observe(this, Observer {
            etEmail.isEnabled = it
        })
    }

    private fun updateCurrentServer(node: Node) {
        tvServerName.text = node.getDisplayName()
    }

    private fun handleStatus(status: EmailLoginViewModel.Status) {
        when (status) {
            EmailLoginViewModel.Status.WRONG_EMAIL_FORMAT -> wrongEmailFormatError()
            EmailLoginViewModel.Status.EMPTY_CODE -> emptyCodeError()
            EmailLoginViewModel.Status.EMAIL_NOT_FOUND -> emailNotFoundError()
            EmailLoginViewModel.Status.ERROR -> unknownError()
            EmailLoginViewModel.Status.CODE_SENT -> codeSent()
            EmailLoginViewModel.Status.AUTHORIZED -> goToMainActivity()
            EmailLoginViewModel.Status.AUTHORIZATION_ERROR -> authorizationError()
            else -> Toast.makeText(this, "Some error. Try again!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun wrongEmailFormatError() {
        emailLayout.startAnimation(AnimationUtils.shakeError())
        emailLayout.error = getString(R.string.wrong_email_format)
    }

    private fun emptyCodeError() {
        codeLayout.startAnimation(AnimationUtils.shakeError())
        codeLayout.error = getString(R.string.code_is_too_short)
    }

    private fun emailNotFoundError() {
        emailLayout.error = getString(R.string.email_not_found)
        AlertDialog.Builder(this)
            .setTitle(R.string.user_not_found)
            .setMessage(R.string.change_node_or_register)
            .setNeutralButton(R.string.cancel, null)
            .setPositiveButton(R.string.sign_up) { p0, p1 ->
                openSignUp()
            }.setNegativeButton(R.string.change_server) { p0, p1 ->
                openChooseServer()
            }.show()
    }

    private fun unknownError() {
        showError(R.string.unknown_error)
    }

    private fun codeSent() {
        viewModel.setCodeSent(true)
        etCode.text.clear()
        codeLayout.error = null
        nextAnimation()
    }

    private fun authorizationError() {
        showError(R.string.authorization_error)
    }

    private fun handleState(state: EmailLoginViewModel.State) {
        if (state.codeSent) {
            codeSent()
        }
        Log.d(TAG, "handleState")
    }

    private fun sendCode() {
        emailLayout.error = null
        viewModel.sendVerificationCode(etEmail.text.toString())
    }

    private fun nextAnimation() {
        val set = ConstraintSet()
        set.clone(clRoot)

        set.clear(choose_server_container.id, ConstraintSet.START)
        set.connect(
            choose_server_container.id,
            ConstraintSet.END,
            ConstraintSet.PARENT_ID,
            ConstraintSet.START
        )

        set.connect(
            btnReceiveCode.id,
            ConstraintSet.END,
            ConstraintSet.PARENT_ID,
            ConstraintSet.START
        )
        set.clear(btnReceiveCode.id, ConstraintSet.START)

        set.connect(
            enter_code_container.id,
            ConstraintSet.START,
            ConstraintSet.PARENT_ID,
            ConstraintSet.START
        )
        set.connect(
            enter_code_container.id,
            ConstraintSet.END,
            ConstraintSet.PARENT_ID,
            ConstraintSet.END
        )

        set.connect(
            we_sent_email_with_code_label.id,
            ConstraintSet.START,
            ConstraintSet.PARENT_ID,
            ConstraintSet.START
        )
        set.connect(
            we_sent_email_with_code_label.id,
            ConstraintSet.END,
            ConstraintSet.PARENT_ID,
            ConstraintSet.END,
            (we_sent_email_with_code_label.layoutParams as ViewGroup.MarginLayoutParams).marginStart
        )
        // We need to specify margin end because set.clear(END) in backAnimation removes constraints and margin end too (WTF)

        set.connect(
            emailLayout.id,
            ConstraintSet.TOP,
            we_sent_email_with_code_label.id,
            ConstraintSet.BOTTOM
        )

        set.applyTo(clRoot)
    }

    private fun backToEnterPhone() {
        viewModel.setCodeSent(false)
        etEmail.isEnabled = true
        hideKeyboard(this)
        backAnimation()
    }

    private fun backAnimation() {
        val set = ConstraintSet()
        set.clone(clRoot)

        set.connect(
            choose_server_container.id,
            ConstraintSet.START,
            ConstraintSet.PARENT_ID,
            ConstraintSet.START
        )
        set.connect(
            choose_server_container.id,
            ConstraintSet.END,
            ConstraintSet.PARENT_ID,
            ConstraintSet.END
        )

        set.connect(
            btnReceiveCode.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START,
            (btnReceiveCode.layoutParams as ViewGroup.MarginLayoutParams).marginEnd
        )
        set.connect(
            btnReceiveCode.id,
            ConstraintSet.END,
            ConstraintSet.PARENT_ID,
            ConstraintSet.END
        )

        set.clear(enter_code_container.id, ConstraintSet.END)
        set.connect(
            enter_code_container.id,
            ConstraintSet.START,
            ConstraintSet.PARENT_ID,
            ConstraintSet.END
        )

        set.clear(we_sent_email_with_code_label.id, ConstraintSet.END)
        set.connect(
            we_sent_email_with_code_label.id,
            ConstraintSet.START,
            ConstraintSet.PARENT_ID,
            ConstraintSet.END
        )

        set.connect(
            emailLayout.id,
            ConstraintSet.TOP,
            choose_server_container.id,
            ConstraintSet.BOTTOM
        )

        set.applyTo(clRoot)
    }

    override fun onBackPressed() {
        if (viewModel.getState().value?.codeSent!!) {
            backToEnterPhone()
            return
        }
        super.onBackPressed()
    }

    private fun login() {
        codeLayout.error = null
        hideKeyboard(this)
        viewModel.authorize(etCode.text.toString())
    }

    private fun hideKeyboard(activity: Activity) {
        val imm = activity.getSystemService(Activity.INPUT_METHOD_SERVICE) as (InputMethodManager)
        var view = activity.currentFocus
        if (view == null) {
            view = View(activity)
        }
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun openChooseServer() {
        ChooseServerActivity.startActivity(this)
    }

    private fun openSignUp() {
        startActivity(Intent(this, SignUpActivity::class.java))
    }

    private fun goToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    companion object {
        private const val TAG = "EmailEnterActivity"
    }
}