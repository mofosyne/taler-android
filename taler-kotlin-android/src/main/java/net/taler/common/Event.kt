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

package net.taler.common

import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Used as a wrapper for data that is exposed via a [LiveData] that represents an one-time event.
 */
open class Event<out T>(private val content: T) {

    private val isConsumed = AtomicBoolean(false)

    /**
     * Returns the content and prevents its use again.
     */
    fun getIfNotConsumed(): T? {
        return if (isConsumed.compareAndSet(false, true)) content else null
    }

    fun getEvenIfConsumedAlready(): T {
        return content
    }

}

fun <T> T.toEvent() = Event(this)

/**
 * An [Observer] for [Event]s, simplifying the pattern of checking if the [Event]'s content has
 * already been consumed.
 *
 * [onEvent] is *only* called if the [Event]'s contents has not been consumed.
 */
class EventObserver<T>(private val onEvent: (T) -> Unit) : Observer<Event<T>> {
    override fun onChanged(value: Event<T>) {
        value.getIfNotConsumed()?.let { onEvent(it) }
    }
}
