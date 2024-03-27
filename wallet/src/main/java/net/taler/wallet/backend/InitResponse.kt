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

import kotlinx.serialization.Serializable
import net.taler.wallet.exchanges.BuiltinExchange

@Serializable
data class InitResponse(
    val versionInfo: WalletCoreVersion,
)

@Serializable
data class WalletRunConfig(
    val builtin: Builtin? = Builtin(),
    val testing: Testing? = Testing(),
    val features: Features? = Features(),
) {
    /**
     * Initialization values useful for a complete startup.
     *
     * These are values may be overridden by different wallets
     */
    @Serializable
    data class Builtin(
        val exchanges: List<BuiltinExchange> = emptyList(),
    )

    /**
     * Unsafe options which it should only be used to create
     * testing environment.
     */
    @Serializable
    data class Testing(
        /**
         * Allow withdrawal of denominations even though they are about to expire.
         */
        val denomselAllowLate: Boolean = false,
        val devModeActive: Boolean = false,
        val insecureTrustExchange: Boolean = false,
        val preventThrottling: Boolean = false,
        val skipDefaults: Boolean = false,
        val emitObservabilityEvents: Boolean? = false,
    )

    /**
     * Configurations values that may be safe to show to the user
     */
    @Serializable
    data class Features(
        val allowHttp: Boolean = false,
    )
}

fun interface VersionReceiver {
    fun onVersionReceived(versionInfo: WalletCoreVersion)
}

@Serializable
data class WalletCoreVersion(
    val implementationSemver: String,
    val implementationGitHash: String,
    val version: String,
    val exchange: String,
    val merchant: String,
    val bankIntegrationApiRange: String,
    val bankConversionApiRange: String,
    val corebankApiRange: String,
    val devMode: Boolean,
)
