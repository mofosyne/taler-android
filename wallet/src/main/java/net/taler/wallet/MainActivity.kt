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

package net.taler.wallet

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.content.IntentFilter
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View.GONE
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.core.view.GravityCompat.START
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceFragmentCompat.OnPreferenceStartFragmentCallback
import com.google.android.material.elevation.SurfaceColors
import com.google.android.material.navigation.NavigationView.OnNavigationItemSelectedListener
import com.google.android.material.snackbar.BaseTransientBottomBar.LENGTH_LONG
import com.google.android.material.snackbar.Snackbar
import com.google.zxing.client.android.Intents.Scan.MIXED_SCAN
import com.google.zxing.client.android.Intents.Scan.SCAN_TYPE
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.journeyapps.barcodescanner.ScanOptions.QR_CODE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.taler.common.EventObserver
import net.taler.common.isOnline
import net.taler.common.showError
import net.taler.wallet.BuildConfig.VERSION_CODE
import net.taler.wallet.BuildConfig.VERSION_NAME
import net.taler.wallet.HostCardEmulatorService.Companion.HTTP_TUNNEL_RESPONSE
import net.taler.wallet.HostCardEmulatorService.Companion.MERCHANT_NFC_CONNECTED
import net.taler.wallet.HostCardEmulatorService.Companion.MERCHANT_NFC_DISCONNECTED
import net.taler.wallet.HostCardEmulatorService.Companion.TRIGGER_PAYMENT_ACTION
import net.taler.wallet.databinding.ActivityMainBinding
import net.taler.wallet.refund.RefundStatus
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale.ROOT
import javax.net.ssl.HttpsURLConnection


class MainActivity : AppCompatActivity(), OnNavigationItemSelectedListener,
    OnPreferenceStartFragmentCallback {

    private val model: MainViewModel by viewModels()

    private lateinit var ui: ActivityMainBinding
    private lateinit var nav: NavController

    private val barcodeLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result == null || result.contents == null) return@registerForActivityResult
        handleTalerUri(result.contents, "QR code")
    }

    private val insetsController: WindowInsetsControllerCompat? by lazy {
        window?.let { window -> WindowInsetsControllerCompat(window, window.decorView) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set status bar color
        val color = SurfaceColors.SURFACE_0.getColor(this)
        window.statusBarColor = color
        window.navigationBarColor = color
        insetsController?.isAppearanceLightStatusBars =
            resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK != Configuration.UI_MODE_NIGHT_YES

        ui = ActivityMainBinding.inflate(layoutInflater)
        setContentView(ui.root)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        nav = navHostFragment.navController
        ui.navView.setupWithNavController(nav)
        ui.navView.setNavigationItemSelectedListener(this)
        if (savedInstanceState == null) {
            ui.navView.menu.getItem(0).isChecked = true
        }

        setSupportActionBar(ui.content.toolbar)
        val appBarConfiguration = AppBarConfiguration(
            setOf(R.id.nav_main, R.id.nav_settings, R.id.nav_pending_operations),
            ui.drawerLayout
        )
        ui.content.toolbar.setupWithNavController(nav, appBarConfiguration)

        model.showProgressBar.observe(this) { show ->
            ui.content.progressBar.visibility = if (show) VISIBLE else INVISIBLE
        }

        val versionView: TextView = ui.navView.getHeaderView(0).findViewById(R.id.versionView)
        model.devMode.observe(this) { enabled ->
            ui.navView.menu.findItem(R.id.nav_dev).isVisible = enabled
            if (enabled) {
                @SuppressLint("SetTextI18n")
                versionView.text = "$VERSION_NAME ($VERSION_CODE)"
                versionView.visibility = VISIBLE
            } else versionView.visibility = GONE
        }

        if (intent.action == ACTION_VIEW) intent.dataString?.let { uri ->
            handleTalerUri(uri, "intent")
        }

        //model.startTunnel()

        registerReceiver(triggerPaymentReceiver, IntentFilter(TRIGGER_PAYMENT_ACTION))
        registerReceiver(nfcConnectedReceiver, IntentFilter(MERCHANT_NFC_CONNECTED))
        registerReceiver(nfcDisconnectedReceiver, IntentFilter(MERCHANT_NFC_DISCONNECTED))
        registerReceiver(tunnelResponseReceiver, IntentFilter(HTTP_TUNNEL_RESPONSE))

        model.scanCodeEvent.observe(this, EventObserver {
            val scanOptions = ScanOptions().apply {
                setPrompt("")
                setBeepEnabled(true)
                setOrientationLocked(false)
                setDesiredBarcodeFormats(QR_CODE)
                addExtra(SCAN_TYPE, MIXED_SCAN)
            }
            if (it) barcodeLauncher.launch(scanOptions)
        })
    }

    override fun onBackPressed() {
        if (ui.drawerLayout.isDrawerOpen(START)) ui.drawerLayout.closeDrawer(START)
        else super.onBackPressed()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent?.action == ACTION_VIEW) intent.dataString?.let { uri ->
            handleTalerUri(uri, "intent")
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_home -> nav.navigate(R.id.nav_main)
            R.id.nav_settings -> nav.navigate(R.id.nav_settings)
            R.id.nav_pending_operations -> nav.navigate(R.id.nav_pending_operations)
        }
        ui.drawerLayout.closeDrawer(START)
        return true
    }

    override fun onDestroy() {
        unregisterReceiver(triggerPaymentReceiver)
        unregisterReceiver(nfcConnectedReceiver)
        unregisterReceiver(nfcDisconnectedReceiver)
        unregisterReceiver(tunnelResponseReceiver)
        super.onDestroy()
    }

    private fun getTalerAction(
        uri: Uri,
        maxRedirects: Int,
        actionFound: MutableLiveData<String>,
    ): MutableLiveData<String> {
        val scheme = uri.scheme ?: return actionFound

        if (scheme == "http" || scheme == "https") {
            model.viewModelScope.launch(Dispatchers.IO) {
                val conn: HttpsURLConnection =
                    URL(uri.toString()).openConnection() as HttpsURLConnection
                Log.v(TAG, "prepare query: $uri")
                conn.setRequestProperty("Accept", "text/html")
                conn.connectTimeout = 5000
                conn.requestMethod = "HEAD"
                conn.connect()
                val status = conn.responseCode

                if (status == HttpURLConnection.HTTP_OK || status == HttpURLConnection.HTTP_PAYMENT_REQUIRED) {
                    val talerHeader = conn.headerFields["Taler"]
                    if (talerHeader != null && talerHeader[0] != null) {
                        Log.v(TAG, "taler header: ${talerHeader[0]}")
                        val talerHeaderUri = Uri.parse(talerHeader[0])
                        getTalerAction(talerHeaderUri, 0, actionFound)
                    }
                }
                if (status == HttpURLConnection.HTTP_MOVED_TEMP
                    || status == HttpURLConnection.HTTP_MOVED_PERM
                    || status == HttpURLConnection.HTTP_SEE_OTHER
                ) {
                    val location = conn.headerFields["Location"]
                    if (location != null && location[0] != null) {
                        Log.v(TAG, "location redirect: ${location[0]}")
                        val locUri = Uri.parse(location[0])
                        getTalerAction(locUri, maxRedirects - 1, actionFound)
                    }
                }
            }
        } else {
            actionFound.postValue(uri.toString())
        }

        return actionFound
    }

    private fun handleTalerUri(url: String, from: String) {
        val uri = Uri.parse(url)
        if (uri.fragment != null && !isOnline()) {
            connectToWifi(this, uri.fragment!!)
        }

        getTalerAction(uri, 3, MutableLiveData<String>()).observe(this) { u ->
            Log.v(TAG, "found action $u")

            if (u.startsWith("payto://", ignoreCase = true)) {
                Log.v(TAG, "navigating with paytoUri!")
                val bundle = bundleOf("uri" to u)
                nav.navigate(R.id.action_nav_payto_uri, bundle)
                return@observe
            }

            val normalizedURL = u.lowercase(ROOT)
            val action = normalizedURL.substring(
                if (normalizedURL.startsWith("taler://", ignoreCase = true)) {
                    "taler://".length
                } else if (normalizedURL.startsWith("taler+http://", ignoreCase = true) &&
                    model.devMode.value == true
                ) {
                    "taler+http://".length
                } else {
                    normalizedURL.length
                }
            )

            when {
                action.startsWith("pay/", ignoreCase = true) -> {
                    Log.v(TAG, "navigating!")
                    nav.navigate(R.id.action_global_promptPayment)
                    model.paymentManager.preparePay(u)
                }
                action.startsWith("tip/", ignoreCase = true) -> {
                    Log.v(TAG, "navigating!")
                    nav.navigate(R.id.action_global_promptTip)
                    model.tipManager.prepareTip(u)
                }
                action.startsWith("withdraw/", ignoreCase = true) -> {
                    Log.v(TAG, "navigating!")
                    // there's more than one entry point, so use global action
                    nav.navigate(R.id.action_global_promptWithdraw)
                    model.withdrawManager.getWithdrawalDetails(u)
                }
                action.startsWith("refund/", ignoreCase = true) -> {
                    model.showProgressBar.value = true
                    model.refundManager.refund(u).observe(this, Observer(::onRefundResponse))
                }
                action.startsWith("pay-pull/", ignoreCase = true) -> {
                    nav.navigate(R.id.action_global_prompt_pull_payment)
                    model.peerManager.checkPeerPullPayment(u)
                }
                action.startsWith("pay-push/", ignoreCase = true) -> {
                    nav.navigate(R.id.action_global_prompt_push_payment)
                    model.peerManager.checkPeerPushPayment(u)
                }
                else -> {
                    showError(R.string.error_unsupported_uri, "From: $from\nURI: $u")
                }
            }
        }
    }

    private fun onRefundResponse(status: RefundStatus) {
        model.showProgressBar.value = false
        when (status) {
            is RefundStatus.Error -> {
                showError(R.string.refund_error, status.msg)
            }
            is RefundStatus.Success -> {
                val amount = status.response.amountRefundGranted
                model.showTransactions(amount.currency)
                val str = getString(R.string.refund_success, amount.amountStr)
                Snackbar.make(ui.navView, str, LENGTH_LONG).show()
            }
        }
    }

    private val triggerPaymentReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (nav.currentDestination?.id == R.id.promptPayment) return
            intent.extras?.getString("contractUrl")?.let { url ->
                nav.navigate(R.id.action_global_promptPayment)
                model.paymentManager.preparePay(url)
            }
        }
    }

    private val nfcConnectedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.v(TAG, "got MERCHANT_NFC_CONNECTED")
            //model.startTunnel()
        }
    }

    private val nfcDisconnectedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.v(TAG, "got MERCHANT_NFC_DISCONNECTED")
            //model.stopTunnel()
        }
    }

    private val tunnelResponseReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.v("taler-tunnel", "got HTTP_TUNNEL_RESPONSE")
            intent.getStringExtra("response")?.let {
                model.tunnelResponse(it)
            }
        }
    }

    override fun onPreferenceStartFragment(
        caller: PreferenceFragmentCompat,
        pref: Preference,
    ): Boolean {
        when (pref.key) {
            "pref_backup" -> nav.navigate(R.id.action_nav_settings_to_nav_settings_backup)
            "pref_exchanges" -> nav.navigate(R.id.action_nav_settings_to_nav_settings_exchanges)
        }
        return true
    }

}
