/*
 * This file is part of GNU Taler
 * (C) 2023 Taler Systems S.A.
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

package net.taler.wallet.transactions

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import net.taler.wallet.R
import net.taler.wallet.transactions.TransactionMajorState.Pending
import net.taler.wallet.transactions.TransactionMinorState.BankConfirmTransfer
import net.taler.wallet.transactions.TransactionMinorState.ExchangeWaitReserve
import net.taler.wallet.transactions.TransactionMinorState.KycRequired

interface ActionListener {
    enum class Type {
        COMPLETE_KYC,
        CONFIRM_WITH_BANK,
        CONFIRM_MANUAL
    }

    fun onActionButtonClicked(tx: Transaction, type: Type)
}

@Composable
fun ActionButton(
    modifier: Modifier = Modifier,
    tx: TransactionWithdrawal,
    listener: ActionListener,
) {
    if (tx.txState.major == Pending) {
        when (tx.txState.minor) {
            KycRequired -> KycButton(modifier, tx, listener)
            BankConfirmTransfer -> ConfirmBankButton(modifier, tx, listener)
            ExchangeWaitReserve -> ConfirmManualButton(modifier, tx, listener)
            else -> {}
        }
    }
}

@Composable
private fun KycButton(
    modifier: Modifier = Modifier,
    tx: TransactionWithdrawal,
    listener: ActionListener,
) {
    Button(
        onClick = { listener.onActionButtonClicked(tx, ActionListener.Type.COMPLETE_KYC) },
        modifier = modifier,
    ) {
        Text(stringResource(R.string.transaction_action_kyc))
    }
}

@Composable
private fun ConfirmBankButton(
    modifier: Modifier = Modifier,
    tx: TransactionWithdrawal,
    listener: ActionListener,
) {
    Button(
        onClick = { listener.onActionButtonClicked(tx, ActionListener.Type.CONFIRM_WITH_BANK) },
        modifier = modifier,
    ) {
        val label = stringResource(R.string.withdraw_button_confirm_bank)
        Icon(
            Icons.Default.AccountBalance,
            label,
            modifier = Modifier.size(ButtonDefaults.IconSize)
        )
        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
        Text(label)
    }
}

@Composable
private fun ConfirmManualButton(
    modifier: Modifier = Modifier,
    tx: TransactionWithdrawal,
    listener: ActionListener,
) {
    Button(
        onClick = { listener.onActionButtonClicked(tx, ActionListener.Type.CONFIRM_MANUAL) },
        modifier = modifier,
    ) {
        val label = stringResource(R.string.withdraw_manual_ready_details_intro)
        Icon(
            Icons.Default.AccountBalance,
            label,
            modifier = Modifier.size(ButtonDefaults.IconSize)
        )
        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
        Text(label)
    }
}
