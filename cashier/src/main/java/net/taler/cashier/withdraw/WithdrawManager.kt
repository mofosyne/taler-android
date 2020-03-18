/*
 * This file is part of GNU Taler
 * (C) 2020 Taler Systems S.A.
 *
 * GNU Taler is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 3, or (at your option) any later version.
 *
 * GNU Taler is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * GNU Taler; see the file COPYING.  If not, see <http://www.gnu.org/licenses/>
 */

package net.taler.cashier.withdraw

import android.app.Application
import android.graphics.Bitmap
import android.os.CountDownTimer
import android.util.Log
import androidx.annotation.UiThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import net.taler.cashier.BalanceResult
import net.taler.cashier.HttpHelper.makeJsonGetRequest
import net.taler.cashier.HttpHelper.makeJsonPostRequest
import net.taler.cashier.HttpJsonResult.Error
import net.taler.cashier.HttpJsonResult.Success
import net.taler.cashier.MainViewModel
import net.taler.cashier.R
import net.taler.common.QrCodeManager.makeQrCode
import org.json.JSONObject
import java.util.concurrent.TimeUnit.MINUTES
import java.util.concurrent.TimeUnit.SECONDS

private val TAG = WithdrawManager::class.java.simpleName

private val INTERVAL = SECONDS.toMillis(1)
private val TIMEOUT = MINUTES.toMillis(2)

class WithdrawManager(
    private val app: Application,
    private val viewModel: MainViewModel
) {
    private val scope
        get() = viewModel.viewModelScope

    private val config
        get() = viewModel.config

    private val currency: String?
        get() = viewModel.currency.value

    private var withdrawStatusCheck: Job? = null

    private val mWithdrawAmount = MutableLiveData<String>()
    val withdrawAmount: LiveData<String> = mWithdrawAmount

    private val mWithdrawResult = MutableLiveData<WithdrawResult>()
    val withdrawResult: LiveData<WithdrawResult> = mWithdrawResult

    private val mWithdrawStatus = MutableLiveData<WithdrawStatus>()
    val withdrawStatus: LiveData<WithdrawStatus> = mWithdrawStatus

    private val mLastTransaction = MutableLiveData<LastTransaction>()
    val lastTransaction: LiveData<LastTransaction> = mLastTransaction

    @UiThread
    fun hasSufficientBalance(amount: Int): Boolean {
        val balanceResult = viewModel.balance.value
        if (balanceResult !is BalanceResult.Success) return false
        val balanceStr = balanceResult.amount.amount
        val balanceDouble = balanceStr.toDouble()
        return amount <= balanceDouble
    }

    @UiThread
    fun withdraw(amount: Int) {
        check(amount > 0) { "Withdraw amount was <= 0" }
        check(currency != null) { "Currency is null" }
        mWithdrawResult.value = null
        mWithdrawAmount.value = "$amount $currency"
        scope.launch(Dispatchers.IO) {
            val url = "${config.bankUrl}/accounts/${config.username}/withdrawals"
            Log.d(TAG, "Starting withdrawal at $url")
            val body = JSONObject(mapOf("amount" to "${currency}:${amount}")).toString()
            when (val result = makeJsonPostRequest(url, body, config)) {
                is Success -> {
                    val talerUri = result.json.getString("taler_withdraw_uri")
                    val withdrawResult = WithdrawResult.Success(
                        id = result.json.getString("withdrawal_id"),
                        talerUri = talerUri,
                        qrCode = makeQrCode(talerUri)
                    )
                    mWithdrawResult.postValue(withdrawResult)
                    timer.start()
                }
                is Error -> {
                    val errorStr = app.getString(R.string.withdraw_error_fetch)
                    mWithdrawResult.postValue(WithdrawResult.Error(errorStr))
                }
            }
        }
    }

    private val timer: CountDownTimer = object : CountDownTimer(TIMEOUT, INTERVAL) {
        override fun onTick(millisUntilFinished: Long) {
            val result = withdrawResult.value
            if (result is WithdrawResult.Success) {
                // check for active jobs and only do one at a time
                val hasActiveCheck = withdrawStatusCheck?.isActive ?: false
                if (!hasActiveCheck) {
                    withdrawStatusCheck = checkWithdrawStatus(result.id)
                }
            } else {
                cancel()
            }
        }

        override fun onFinish() {
            abort()
            mWithdrawStatus.postValue(WithdrawStatus.Error)
            cancel()
        }
    }

    private fun checkWithdrawStatus(withdrawalId: String) = scope.launch(Dispatchers.IO) {
        val url = "${config.bankUrl}/accounts/${config.username}/withdrawals/${withdrawalId}"
        Log.d(TAG, "Checking withdraw status at $url")
        val response = makeJsonGetRequest(url, config)
        if (response !is Success) return@launch  // ignore errors and continue trying
        val oldStatus = mWithdrawStatus.value
        when {
            response.json.getBoolean("aborted") -> {
                cancelWithdrawStatusCheck()
                mWithdrawStatus.postValue(WithdrawStatus.Aborted)
            }
            response.json.getBoolean("confirmation_done") -> {
                if (oldStatus !is WithdrawStatus.Success) {
                    cancelWithdrawStatusCheck()
                    mWithdrawStatus.postValue(WithdrawStatus.Success)
                    viewModel.getBalance()
                }
            }
            response.json.getBoolean("selection_done") -> {
                // only update status, if there's none, yet
                // so we don't re-notify or overwrite newer status info
                if (oldStatus == null) {
                    mWithdrawStatus.postValue(WithdrawStatus.SelectionDone(withdrawalId))
                }
            }
        }
    }

    private fun cancelWithdrawStatusCheck() {
        timer.cancel()
        withdrawStatusCheck?.cancel()
    }

    /**
     * Aborts the last [withdrawResult], if it exists und there is no [withdrawStatus].
     * Otherwise this is a no-op.
     */
    @UiThread
    fun abort() {
        val result = withdrawResult.value
        val status = withdrawStatus.value
        if (result is WithdrawResult.Success && status == null) {
            cancelWithdrawStatusCheck()
            abort(result.id)
        }
    }

    private fun abort(withdrawalId: String) = scope.launch(Dispatchers.IO) {
        val url = "${config.bankUrl}/accounts/${config.username}/withdrawals/${withdrawalId}/abort"
        Log.d(TAG, "Aborting withdrawal at $url")
        makeJsonPostRequest(url, "", config)
    }

    @UiThread
    fun confirm(withdrawalId: String) {
        mWithdrawStatus.value = WithdrawStatus.Confirming
        scope.launch(Dispatchers.IO) {
            val url =
                "${config.bankUrl}/accounts/${config.username}/withdrawals/${withdrawalId}/confirm"
            Log.d(TAG, "Confirming withdrawal at $url")
            when (val result = makeJsonPostRequest(url, "", config)) {
                is Success -> {
                    // no-op still waiting for [timer] to confirm our confirmation
                }
                is Error -> {
                    Log.e(TAG, "Error confirming withdrawal. Status code: ${result.statusCode}")
                    mWithdrawStatus.postValue(WithdrawStatus.Error)
                }
            }
        }
    }

    @UiThread
    fun completeTransaction() {
        mLastTransaction.value = LastTransaction(withdrawAmount.value!!, withdrawStatus.value!!)
        withdrawStatusCheck = null
        mWithdrawAmount.value = null
        mWithdrawResult.value = null
        mWithdrawStatus.value = null
    }

}

sealed class WithdrawResult {
    object InsufficientBalance : WithdrawResult()
    class Error(val msg: String) : WithdrawResult()
    class Success(val id: String, val talerUri: String, val qrCode: Bitmap) : WithdrawResult()
}

sealed class WithdrawStatus {
    object Error : WithdrawStatus()
    object Aborted : WithdrawStatus()
    class SelectionDone(val withdrawalId: String) : WithdrawStatus()
    object Confirming : WithdrawStatus()
    object Success : WithdrawStatus()
}

data class LastTransaction(
    val withdrawAmount: String,
    val withdrawStatus: WithdrawStatus
)
