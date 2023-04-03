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

/**
 * Based on “DD 37: Wallet Transaction Lifecycle”
 *
 * TODO: implement sub-states (pending in wallet-core)
 * TODO: implement sub-state specific transitions
 */

enum class Transition {
    // Common States
    Delete,
    Retry,
    Abort,
    Suspend,
    Resume,
    AbortForce,

    // Payment to Merchant
    PayAccept,
    Expired,
    CheckRefund,
    PayReplay,

    // Tip
    AcceptTip,

    // Peer Pull Debit
    ConfirmPay,
}

fun Transaction.canPerform(t: Transition): Boolean {
    return when (t) {
        Transition.Delete -> extendedStatus in arrayOf(
            ExtendedStatus.Done,
            ExtendedStatus.Aborted,
            ExtendedStatus.Failed,
        )
        Transition.Retry -> extendedStatus in arrayOf(
            ExtendedStatus.Pending,
            ExtendedStatus.Aborting,
        )
        Transition.Abort -> extendedStatus in arrayOf(
            ExtendedStatus.Pending,
        )
        Transition.Suspend -> extendedStatus in arrayOf(
            ExtendedStatus.Pending,
        )
        Transition.Resume -> extendedStatus in arrayOf(
            ExtendedStatus.Suspended,
        )
        Transition.AbortForce -> extendedStatus in arrayOf(
            ExtendedStatus.Aborting,
        )
        else -> false
    }
}