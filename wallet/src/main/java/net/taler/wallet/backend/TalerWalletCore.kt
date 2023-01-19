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

package net.taler.wallet.backend

import com.sun.jna.Callback
import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer

interface TalerWalletCore: Library {
    companion object {
        val INSTANCE: TalerWalletCore by lazy {
            Native.load("talerwalletcore", TalerWalletCore::class.java)
        }
    }

    interface TALER_WALLET_MessageHandlerFn: Callback {
        fun invoke(handler_p: Pointer, message: String)
    }

    interface TALER_LogFn: Callback {
        fun invoke(cls: Pointer, stream: Int, msg: String)
    }

    fun TALER_WALLET_create(): Pointer
    fun TALER_WALLET_set_message_handler(twi: Pointer, handler_f: TALER_WALLET_MessageHandlerFn, handler_p: Pointer)
    fun TALER_WALLET_send_request(twi: Pointer, request: String): Int
    fun TALER_WALLET_run(twi: Pointer): Int
    fun TALER_WALLET_join(twi: Pointer)
    fun TALER_start_redirect_std(logfn: TALER_LogFn, cls: Pointer)
}