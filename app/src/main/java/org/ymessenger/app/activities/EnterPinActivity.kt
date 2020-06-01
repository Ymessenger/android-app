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

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import kotlinx.android.synthetic.main.activity_enter_pin.*
import org.ymessenger.app.AppBase
import org.ymessenger.app.R
import org.ymessenger.app.di.Injection
import org.ymessenger.app.viewmodels.EnterPinViewModel


class EnterPinActivity : AppCompatActivity() {

    private lateinit var viewModel: EnterPinViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_enter_pin)

        // Fix wrong behaviour when keyboard was opened
        Handler().postDelayed({
            glPinButtons.visibility = View.VISIBLE
        }, 300)

        val argMode = intent.getIntExtra(ARG_MODE, 0)
        if (argMode == 0) {
            Log.e(TAG, "No ARG_MODE passed. Aborting")
            finish()
            return
        }

        val mode = when (argMode) {
            MODE_SET_PIN -> EnterPinViewModel.Mode.NEW_PIN
            MODE_SET_SAFE_PIN -> EnterPinViewModel.Mode.NEW_SAFE_PIN
            else -> EnterPinViewModel.Mode.ENTER
        }

        val factory = Injection.provideEnterPinViewModelFactory(application as AppBase, mode)
        viewModel = ViewModelProviders.of(this, factory).get(EnterPinViewModel::class.java)

        initPinButtons()

        btnForgotPin.setOnClickListener { forgotPinClicked() }

        subscribeUi(viewModel)
    }

    private fun initPinButtons() {
        val pinButtonClickListener = View.OnClickListener { v ->
            val num = when (v?.id) {
                R.id.btn0 -> 0
                R.id.btn1 -> 1
                R.id.btn2 -> 2
                R.id.btn3 -> 3
                R.id.btn4 -> 4
                R.id.btn5 -> 5
                R.id.btn6 -> 6
                R.id.btn7 -> 7
                R.id.btn8 -> 8
                R.id.btn9 -> 9
                else -> -1
            }

            numberClicked(num)
        }

        btn0.setOnClickListener(pinButtonClickListener)
        btn1.setOnClickListener(pinButtonClickListener)
        btn2.setOnClickListener(pinButtonClickListener)
        btn3.setOnClickListener(pinButtonClickListener)
        btn4.setOnClickListener(pinButtonClickListener)
        btn5.setOnClickListener(pinButtonClickListener)
        btn6.setOnClickListener(pinButtonClickListener)
        btn7.setOnClickListener(pinButtonClickListener)
        btn8.setOnClickListener(pinButtonClickListener)
        btn9.setOnClickListener(pinButtonClickListener)

        btnC.setOnClickListener { viewModel.clear() }
        btnBack.setOnClickListener { onBackPressed() }
    }

    private fun numberClicked(num: Int) {
        viewModel.numberClicked(num)
    }

    private fun subscribeUi(viewModel: EnterPinViewModel) {
        viewModel.pinLength.observe(this, Observer {
            updatePinDots(it)
        })

        viewModel.enteredPinError.observe(this, Observer {
            tvError.visibility = it?.let { errorRes ->
                tvError.setText(errorRes)
                View.VISIBLE
            } ?: View.INVISIBLE
        })

        viewModel.mode.observe(this, Observer {
            if (it == EnterPinViewModel.Mode.NEW_PIN) {
                btnForgotPin.visibility = View.GONE
            } else {
                btnForgotPin.visibility = View.VISIBLE
            }
        })

        viewModel.success.observe(this, Observer {
            setResult(Activity.RESULT_OK)
            finish()
        })

        viewModel.safePinEnteredEvent.observe(this, Observer {
            goToMainActivitySafe()
        })

        viewModel.enterPinAgainEvent.observe(this, Observer {
            tvHint.setText(R.string.enter_pin_again)
        })
    }

    private fun goToMainActivitySafe() {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra(MainActivity.ARG_SAFE_MODE, true)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    private fun updatePinDots(length: Int) {
        val drawableUnselected = ContextCompat.getDrawable(this, R.drawable.dot_indicator)
        val drawableSelected = ContextCompat.getDrawable(this, R.drawable.dot_selected_indicator)

        for (dotPos in 0 until llPinDots.childCount) {
            (llPinDots.getChildAt(dotPos) as ImageView).setImageDrawable(if (dotPos < length) drawableSelected else drawableUnselected)
        }
    }

    private fun forgotPinClicked() {
        AlertDialog.Builder(this)
            .setTitle(R.string.we_are_sorry)
            .setMessage(R.string.but_due_our_privacy_policy)
            .setNeutralButton(R.string.ok, null)
            .show()
    }

    companion object {
        private const val TAG = "EnterPinActivity"

        private const val ARG_MODE = "ARG_MODE"
        private const val MODE_SET_PIN = 1
        private const val MODE_SET_SAFE_PIN = 2
        private const val MODE_ENTER = 3

        fun setPinModeIntent(context: Context): Intent {
            val intent = Intent(context, EnterPinActivity::class.java)
            intent.putExtra(ARG_MODE, MODE_SET_PIN)

            return intent
        }

        fun setSafePinModeIntent(context: Context): Intent {
            val intent = Intent(context, EnterPinActivity::class.java)
            intent.putExtra(ARG_MODE, MODE_SET_SAFE_PIN)

            return intent
        }

        fun enterModeIntent(context: Context): Intent {
            val intent = Intent(context, EnterPinActivity::class.java)
            intent.putExtra(ARG_MODE, MODE_ENTER)

            return intent
        }
    }
}