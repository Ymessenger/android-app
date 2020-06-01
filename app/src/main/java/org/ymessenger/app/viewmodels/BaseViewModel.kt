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

import androidx.annotation.StringRes
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import org.ymessenger.app.R
import org.ymessenger.app.activities.BaseActivity
import org.ymessenger.app.data.ErrorCodeHelper
import org.ymessenger.app.fragments.BaseFragment
import org.ymessenger.app.interfaces.IBaseScreenEvents
import org.ymessenger.app.utils.SingleLiveEvent

open class BaseViewModel : ViewModel() {

    private val showToastEvent = SingleLiveEvent<Int>()
    private val showToastTextEvent = SingleLiveEvent<String>()
    private val showErrorEvent = SingleLiveEvent<Int>()
    private val showLoading = MutableLiveData<Pair<Boolean, Int?>>()

    protected fun showToast(@StringRes message: Int) {
        showToastEvent.postValue(message)
    }

    protected fun showToast(message: String) {
        showToastTextEvent.postValue(message)
    }

    protected fun showError(@StringRes message: Int) {
        showErrorEvent.postValue(message)
    }

    protected fun showErrorFromCode(errorCode: Int) {
        showErrorEvent.postValue(ErrorCodeHelper.getErrorMessage(errorCode))
    }

    protected fun startLoading(@StringRes message: Int? = null) {
        showLoading.postValue(true to message)
    }

    protected fun endLoading() {
        showLoading.postValue(false to null)
    }

    protected fun unsupportedOperation() {
        showToast(R.string.operation_is_not_supported)
    }

    fun onErrorEvent() = showErrorEvent

    fun onToastEvent() = showToastEvent

    fun subscribeOnEvents(baseActivity: BaseActivity) {
        subscribeOnEvents(baseActivity, baseActivity)
    }

    fun subscribeOnEvents(baseFragment: BaseFragment) {
        subscribeOnEvents(baseFragment, baseFragment)
    }

    private fun subscribeOnEvents(
        lifecycleOwner: LifecycleOwner,
        baseScreenEvents: IBaseScreenEvents
    ) {
        showErrorEvent.observe(lifecycleOwner, Observer {
            baseScreenEvents.showError(it)
        })

        showToastEvent.observe(lifecycleOwner, Observer {
            baseScreenEvents.showToast(it)
        })

        showToastTextEvent.observe(lifecycleOwner, Observer {
            baseScreenEvents.showToast(it, true)
        })

        showLoading.observe(lifecycleOwner, Observer {
            if (it.first) {
                baseScreenEvents.showLoadingDialog(it.second)
            } else {
                baseScreenEvents.hideLoadingDialog()
            }
        })
    }

}