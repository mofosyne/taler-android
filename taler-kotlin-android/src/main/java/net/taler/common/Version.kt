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

import kotlin.math.sign

/**
 * Semantic versioning, but libtool-style.
 * See https://www.gnu.org/software/libtool/manual/html_node/Libtool-versioning.html
 */
public data class Version(
    val current: Int,
    val revision: Int,
    val age: Int
) {
    public companion object {
        public fun parse(v: String): Version? {
            val elements = v.split(":")
            if (elements.size != 3) return null
            val (currentStr, revisionStr, ageStr) = elements
            val current = currentStr.toIntOrNull()
            val revision = revisionStr.toIntOrNull()
            val age = ageStr.toIntOrNull()
            if (current == null || revision == null || age == null) return null
            return Version(current, revision, age)
        }
    }

    /**
     * Compare two libtool-style versions.
     *
     * Returns a [VersionMatchResult] or null if the given version was null.
     */
    public fun compare(other: Version?): VersionMatchResult? {
        if (other == null) return null
        val compatible = current - age <= other.current &&
                current >= other.current - other.age
        val currentCmp = sign((current - other.current).toDouble()).toInt()
        return VersionMatchResult(compatible, currentCmp)
    }

    /**
     * Result of comparing two libtool versions.
     */
    public data class VersionMatchResult(
        /**
         * Is the first version compatible with the second?
         */
        val compatible: Boolean,
        /**
         * Is the first version older (-1), newer (+1) or identical (0)?
         */
        val currentCmp: Int
    )

}
