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

import androidx.annotation.GuardedBy
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.Continuation

class RequestManager {

    @GuardedBy("this")
    private val contMap = ConcurrentHashMap<Int, Continuation<ApiResponse>>()

    @Volatile
    @GuardedBy("this")
    private var currentId = 0

    @Synchronized
    fun addRequest(cont: Continuation<ApiResponse>, block: (Int) -> Unit) {
        val id = currentId++
        contMap[id] = cont
        block(id)
    }

    @Synchronized
    fun getAndRemoveContinuation(id: Int): Continuation<ApiResponse>? {
        return contMap.remove(id)
    }

}
