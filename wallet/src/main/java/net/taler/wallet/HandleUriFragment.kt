/*
 * This file is part of GNU Taler
 * (C) 2024 Taler Systems S.A.
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

package net.taler.wallet

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast.LENGTH_LONG
import androidx.compose.ui.platform.ComposeView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.taler.common.isOnline
import net.taler.common.showError
import net.taler.wallet.compose.LoadingScreen
import net.taler.wallet.compose.TalerSurface
import net.taler.wallet.refund.RefundStatus
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

class HandleUriFragment: Fragment() {
    private val model: MainViewModel by activityViewModels()

    lateinit var uri: String
    lateinit var from: String

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        uri = arguments?.getString("uri") ?: error("no uri passed")
        from = arguments?.getString("from") ?: error("no from passed")

        return ComposeView(requireContext()).apply {
            setContent {
                TalerSurface {
                    LoadingScreen()
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val uri = Uri.parse(uri)
        if (uri.fragment != null && !requireContext().isOnline()) {
            connectToWifi(requireContext(), uri.fragment!!)
        }

        getTalerAction(uri, 3, MutableLiveData<String>()).observe(viewLifecycleOwner) { u ->
            Log.v(TAG, "found action $u")

            if (u.startsWith("payto://", ignoreCase = true)) {
                Log.v(TAG, "navigating with paytoUri!")
                val bundle = bundleOf("uri" to u)
                findNavController().navigate(R.id.action_handleUri_to_nav_payto_uri, bundle)
                return@observe
            }

            val normalizedURL = u.lowercase(Locale.ROOT)
            var ext = false
            val action = normalizedURL.substring(
                if (normalizedURL.startsWith("taler://", ignoreCase = true)) {
                    "taler://".length
                } else if (normalizedURL.startsWith("ext+taler://", ignoreCase = true)) {
                    ext = true
                    "ext+taler://".length
                } else if (normalizedURL.startsWith("taler+http://", ignoreCase = true) &&
                    model.devMode.value == true
                ) {
                    "taler+http://".length
                } else {
                    normalizedURL.length
                }
            )

            // Remove ext+ scheme prefix if present
            val u2 = if (ext) {
                "taler://" + u.substring("ext+taler://".length)
            } else u

            when {
                action.startsWith("pay/", ignoreCase = true) -> {
                    Log.v(TAG, "navigating!")
                    findNavController().navigate(R.id.action_handleUri_to_promptPayment)
                    model.paymentManager.preparePay(u2)
                }
                action.startsWith("withdraw/", ignoreCase = true) -> {
                    Log.v(TAG, "navigating!")
                    // there's more than one entry point, so use global action
                    findNavController().navigate(R.id.action_handleUri_to_promptWithdraw)
                    model.withdrawManager.getWithdrawalDetails(u2)
                }

                action.startsWith("withdraw-exchange/", ignoreCase = true) -> {
                    prepareManualWithdrawal(u2)
                }

                action.startsWith("refund/", ignoreCase = true) -> {
                    model.showProgressBar.value = true
                    model.refundManager.refund(u2).observe(viewLifecycleOwner, Observer(::onRefundResponse))
                }
                action.startsWith("pay-pull/", ignoreCase = true) -> {
                    findNavController().navigate(R.id.action_handleUri_to_promptPullPayment)
                    model.peerManager.preparePeerPullDebit(u2)
                }
                action.startsWith("pay-push/", ignoreCase = true) -> {
                    findNavController().navigate(R.id.action_handleUri_to_promptPushPayment)
                    model.peerManager.preparePeerPushCredit(u2)
                }
                action.startsWith("pay-template/", ignoreCase = true) -> {
                    val bundle = bundleOf("uri" to u2)
                    findNavController().navigate(R.id.action_handleUri_to_promptPayTemplate, bundle)
                }
                else -> {
                    showError(R.string.error_unsupported_uri, "From: $from\nURI: $u2")
                    findNavController().popBackStack()
                }
            }
        }
    }

    private fun getTalerAction(
        uri: Uri,
        maxRedirects: Int,
        actionFound: MutableLiveData<String>,
    ): MutableLiveData<String> {
        val scheme = uri.scheme ?: return actionFound

        if (scheme == "http" || scheme == "https") {
            model.viewModelScope.launch(Dispatchers.IO) {
                val conn = URL(uri.toString()).openConnection() as HttpURLConnection
                Log.v(TAG, "prepare query: $uri")
                conn.setRequestProperty("Accept", "text/html")
                conn.connectTimeout = 5000
                conn.requestMethod = "HEAD"
                try {
                    conn.connect()
                } catch (e: IOException) {
                    Log.e(TAG, "Error connecting to $uri ", e)
                    showError(R.string.error_broken_uri, "$uri")
                    return@launch
                }
                val status = conn.responseCode

                if (status == HttpURLConnection.HTTP_OK || status == HttpURLConnection.HTTP_PAYMENT_REQUIRED) {
                    val talerHeader = conn.headerFields["Taler"]
                    if (talerHeader != null && talerHeader[0] != null) {
                        Log.v(TAG, "taler header: ${talerHeader[0]}")
                        val talerHeaderUri = Uri.parse(talerHeader[0])
                        getTalerAction(talerHeaderUri, 0, actionFound)
                    }
                } else if (status == HttpURLConnection.HTTP_MOVED_TEMP
                    || status == HttpURLConnection.HTTP_MOVED_PERM
                    || status == HttpURLConnection.HTTP_SEE_OTHER
                ) {
                    val location = conn.headerFields["Location"]
                    if (location != null && location[0] != null) {
                        Log.v(TAG, "location redirect: ${location[0]}")
                        val locUri = Uri.parse(location[0])
                        getTalerAction(locUri, maxRedirects - 1, actionFound)
                    }
                } else {
                    showError(R.string.error_broken_uri, "$uri")
                    findNavController().popBackStack()
                }
            }
        } else {
            actionFound.postValue(uri.toString())
        }

        return actionFound
    }

    private fun prepareManualWithdrawal(uri: String) {
        model.showProgressBar.value = true
        lifecycleScope.launch(Dispatchers.IO) {
            val response = model.withdrawManager.prepareManualWithdrawal(uri)
            if (response == null) withContext(Dispatchers.Main) {
                model.showProgressBar.value = false
                findNavController().navigate(R.id.errorFragment)
            } else {
                val exchange =
                    model.exchangeManager.findExchangeByUrl(response.exchangeBaseUrl)
                if (exchange == null) withContext(Dispatchers.Main) {
                    model.showProgressBar.value = false
                    showError(R.string.exchange_add_error)
                    findNavController().navigateUp()
                } else {
                    model.exchangeManager.withdrawalExchange = exchange
                    withContext(Dispatchers.Main) {
                        model.showProgressBar.value = false
                        val args = Bundle().apply {
                            if (response.amount != null) {
                                putString("amount", response.amount.toJSONString())
                            }
                        }

                        findNavController().navigate(R.id.action_handleUri_to_manualWithdrawal, args)
                    }
                }
            }
        }
    }

    private fun onRefundResponse(status: RefundStatus) {
        model.showProgressBar.value = false
        when (status) {
            is RefundStatus.Error -> {
                if (model.devMode.value == true) {
                    showError(status.error)
                } else {
                    showError(R.string.refund_error, status.error.userFacingMsg)
                }

                findNavController().navigateUp()
            }
            is RefundStatus.Success -> {
                lifecycleScope.launch {
                    val transactionId = status.response.transactionId
                    val transaction = model.transactionManager.getTransactionById(transactionId)
                    if (transaction != null) {
                        // TODO: currency what? scopes are the cool thing now
                        // val currency = transaction.amountRaw.currency
                        // model.showTransactions(currency)
                        Snackbar.make(requireView(), getString(R.string.refund_success), LENGTH_LONG).show()
                    }

                    findNavController().navigateUp()
                }
            }

        }
    }
}