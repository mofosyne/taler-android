/*
 * This file is part of GNU Taler
 * (C) 2022 Taler Systems S.A.
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

package net.taler.wallet.deposit

import android.net.Uri
import android.util.Log
import androidx.annotation.UiThread
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import net.taler.common.Amount
import net.taler.wallet.TAG
import net.taler.wallet.accounts.PaytoUriIban
import net.taler.wallet.backend.WalletBackendApi

class DepositManager(
    private val api: WalletBackendApi,
    private val scope: CoroutineScope,
) {

    private val mDepositState = MutableStateFlow<DepositState>(DepositState.Start)
    internal val depositState = mDepositState.asStateFlow()

    fun isSupportedPayToUri(uriString: String): Boolean {
        if (!uriString.startsWith("payto://")) return false
        val u = Uri.parse(uriString)
        if (!u.authority.equals("iban", ignoreCase = true)) return false
        return u.pathSegments.size >= 1
    }

    @UiThread
    fun onDepositButtonClicked(amount: Amount, receiverName: String, iban: String, bic: String) {
        val paytoUri: String = PaytoUriIban(
            iban = iban,
            bic = bic,
            targetPath = "",
            params = mapOf("receiver-name" to receiverName),
        ).paytoUri

        if (depositState.value.showFees) makeDeposit(
            paytoUri = paytoUri,
            amount = amount,
            totalDepositCost = depositState.value.totalDepositCost
                ?: Amount.zero(amount.currency),
            effectiveDepositAmount = depositState.value.effectiveDepositAmount
                ?: Amount.zero(amount.currency),
        ) else {
            prepareDeposit(paytoUri, amount)
        }
    }

    private fun prepareDeposit(paytoUri: String, amount: Amount) {
        mDepositState.value = DepositState.CheckingFees
        scope.launch {
            api.request("prepareDeposit", PrepareDepositResponse.serializer()) {
                put("depositPaytoUri", paytoUri)
                put("amount", amount.toJSONString())
            }.onError {
                Log.e(TAG, "Error prepareDeposit $it")
                mDepositState.value = DepositState.Error(it.userFacingMsg)
            }.onSuccess {
                mDepositState.value = DepositState.FeesChecked(
                    totalDepositCost = it.totalDepositCost,
                    effectiveDepositAmount = it.effectiveDepositAmount,
                )
            }
        }
    }

    private fun makeDeposit(
        paytoUri: String,
        amount: Amount,
        totalDepositCost: Amount,
        effectiveDepositAmount: Amount,
    ) {
        mDepositState.value = DepositState.MakingDeposit(
            totalDepositCost = totalDepositCost,
            effectiveDepositAmount = effectiveDepositAmount,
        )
        scope.launch {
            api.request("createDepositGroup", CreateDepositGroupResponse.serializer()) {
                put("depositPaytoUri", paytoUri)
                put("amount", amount.toJSONString())
            }.onError {
                Log.e(TAG, "Error createDepositGroup $it")
                mDepositState.value = DepositState.Error(it.userFacingMsg)
            }.onSuccess {
                mDepositState.value = DepositState.Success
            }
        }
    }

    @UiThread
    fun resetDepositState() {
        mDepositState.value = DepositState.Start
    }
}

@Serializable
data class PrepareDepositResponse(
    val totalDepositCost: Amount,
    val effectiveDepositAmount: Amount,
)

@Serializable
data class CreateDepositGroupResponse(
    val depositGroupId: String,
    val transactionId: String,
)
