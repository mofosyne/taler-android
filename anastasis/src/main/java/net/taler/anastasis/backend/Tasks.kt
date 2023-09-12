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

package net.taler.anastasis.backend
data class Tasks(
    val background: Int = 0,
    val foreground: Int = 0,
) {
    enum class Type {
        None,
        Background,
        Foreground,
    }

    val isBackgroundLoading: Boolean get() = background > 0
    val isForegroundLoading: Boolean get() = foreground > 0

    fun addTask(type: Type): Tasks = when (type) {
        Type.None -> copy()
        Type.Background -> copy(background = background + 1)
        Type.Foreground -> copy(foreground = foreground + 1)
    }

    fun removeTask(type: Type): Tasks = when (type) {
        Type.None -> copy()
        Type.Background -> copy(background = if (background == 0) 0 else background - 1)
        Type.Foreground -> copy(foreground = if(foreground == 0) 0 else foreground - 1)
    }

}