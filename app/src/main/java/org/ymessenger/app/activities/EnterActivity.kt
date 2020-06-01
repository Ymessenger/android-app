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
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import kotlinx.android.synthetic.main.activity_enter.*
import org.ymessenger.app.AppBase
import org.ymessenger.app.R
import org.ymessenger.app.di.Injection
import org.ymessenger.app.viewmodels.EnterViewModel

/**
 * This is splash screen activity
 */
class EnterActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme) // set main theme after launcher theme
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_enter)

        val factory = Injection.provideEnterViewModelFactory(application as AppBase)
        val viewModel = ViewModelProviders.of(this, factory).get(EnterViewModel::class.java)

        btnPhoneNumber.setOnClickListener { goToPhoneSignIn() }
        btnEmail.setOnClickListener { goToEmailSignIn() }
        btnQRcode.setOnClickListener { goToQRCodeSignIn() }
        btnRegister.setOnClickListener { goToSingUp() }

        subscribeUi(viewModel)
    }

    private fun subscribeUi(viewModel: EnterViewModel) {
        viewModel.hasTokenEvent.observe(this, Observer {
            goToMainActivity()
        })

        viewModel.openSetPassphraseEvent.observe(this, Observer {
            SetPassphraseActivity.start(this, false)
        })

        viewModel.showIntroEvent.observe(this, Observer {
            showIntro()
        })

        viewModel.authorizationErrorEvent.observe(this, Observer { message ->
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        })
    }

    private fun showIntro() {
        startActivity(Intent(this, IntroActivity::class.java))
    }

    private fun goToEmailSignIn() {
        startActivity(Intent(this, EmailEnterActivity::class.java))
    }

    private fun goToPhoneSignIn() {
        startActivity(Intent(this, PhoneEnterActivity::class.java))
    }

    private fun goToQRCodeSignIn() {
        startActivity(Intent(this, QRCodeEnterActivity::class.java))
    }

    private fun goToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun goToSingUp() {
        startActivity(Intent(this, SignUpActivity::class.java))
    }
}