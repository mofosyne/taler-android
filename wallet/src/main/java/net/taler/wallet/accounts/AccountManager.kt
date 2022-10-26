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

package net.taler.wallet.accounts

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.taler.wallet.backend.WalletBackendApi

class AccountManager(
    private val api: WalletBackendApi,
    private val scope: CoroutineScope,
) {

    fun listKnownBankAccounts() {
        scope.launch {
            val response = api.request("listKnownBankAccounts", KnownBankAccounts.serializer())
            response.onError {
                throw AssertionError("Wallet core failed to return known bank accounts!")
            }.onSuccess { knownBankAccounts ->

            }
        }
    }

    fun addKnownBankAccount(paytoUri: String, alias: String, currency: String) {
        scope.launch {
            val response = api.request<Unit>("addKnownBankAccounts") {
                put("payto", paytoUri)
                put("alias", alias)
                put("currency", currency)
            }
            response.onError {
                throw AssertionError("Wallet core failed to add known bank account!")
            }.onSuccess {

            }
        }
    }

}
