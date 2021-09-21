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
import androidx.core.view.GravityCompat.START
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceFragmentCompat.OnPreferenceStartFragmentCallback
import com.google.android.material.navigation.NavigationView.OnNavigationItemSelectedListener
import com.google.android.material.snackbar.BaseTransientBottomBar.LENGTH_LONG
import com.google.android.material.snackbar.Snackbar
import com.google.zxing.integration.android.IntentIntegrator
import com.google.zxing.integration.android.IntentIntegrator.parseActivityResult
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
import java.util.Locale.ROOT

class MainActivity : AppCompatActivity(), OnNavigationItemSelectedListener,
    OnPreferenceStartFragmentCallback {

    private val model: MainViewModel by viewModels()

    private lateinit var ui: ActivityMainBinding
    private lateinit var nav: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

        model.showProgressBar.observe(this, { show ->
            ui.content.progressBar.visibility = if (show) VISIBLE else INVISIBLE
        })

        val versionView: TextView = ui.navView.getHeaderView(0).findViewById(R.id.versionView)
        model.devMode.observe(this, { enabled ->
            ui.navView.menu.findItem(R.id.nav_dev).isVisible = enabled
            if (enabled) {
                @SuppressLint("SetTextI18n")
                versionView.text = "$VERSION_NAME ($VERSION_CODE)"
                versionView.visibility = VISIBLE
            } else versionView.visibility = GONE
        })

        if (intent.action == ACTION_VIEW) intent.dataString?.let { uri ->
            handleTalerUri(uri, "intent")
        }

        //model.startTunnel()

        registerReceiver(triggerPaymentReceiver, IntentFilter(TRIGGER_PAYMENT_ACTION))
        registerReceiver(nfcConnectedReceiver, IntentFilter(MERCHANT_NFC_CONNECTED))
        registerReceiver(nfcDisconnectedReceiver, IntentFilter(MERCHANT_NFC_DISCONNECTED))
        registerReceiver(tunnelResponseReceiver, IntentFilter(HTTP_TUNNEL_RESPONSE))
    }

    override fun onBackPressed() {
        if (ui.drawerLayout.isDrawerOpen(START)) ui.drawerLayout.closeDrawer(START)
        else super.onBackPressed()
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == IntentIntegrator.REQUEST_CODE) {
            parseActivityResult(requestCode, resultCode, data)?.contents?.let { contents ->
                handleTalerUri(contents, "QR code")
            }
        }
    }

    override fun onDestroy() {
        unregisterReceiver(triggerPaymentReceiver)
        unregisterReceiver(nfcConnectedReceiver)
        unregisterReceiver(nfcDisconnectedReceiver)
        unregisterReceiver(tunnelResponseReceiver)
        super.onDestroy()
    }

    private fun handleTalerUri(url: String, from: String) {
        val uri = Uri.parse(url)
        if (uri.fragment != null && !isOnline()) {
            connectToWifi(this, uri.fragment!!)
        }
        val normalizedURL = url.lowercase(ROOT)
        val action = normalizedURL.substring(
            if (normalizedURL.startsWith("taler://")) {
                "taler://".length
            } else if (normalizedURL.startsWith("taler+http://") && model.devMode.value == true) {
                "taler+http://".length
            } else {
                normalizedURL.length
            }
        )
        when {
            action.startsWith("pay/") -> {
                Log.v(TAG, "navigating!")
                nav.navigate(R.id.action_nav_main_to_promptPayment)
                model.paymentManager.preparePay(url)
            }
            action.startsWith("withdraw/") -> {
                Log.v(TAG, "navigating!")
                // there's more than one entry point, so use global action
                nav.navigate(R.id.action_global_promptWithdraw)
                model.withdrawManager.getWithdrawalDetails(url)
            }
            action.startsWith("refund/") -> {
                model.showProgressBar.value = true
                model.refundManager.refund(url).observe(this, Observer(::onRefundResponse))
            }
            else -> {
                showError(R.string.error_unsupported_uri, "From: $from\nURI: $url")
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
        pref: Preference
    ): Boolean {
        when (pref.key) {
            "pref_backup" -> nav.navigate(R.id.action_nav_settings_to_nav_settings_backup)
            "pref_exchanges" -> nav.navigate(R.id.action_nav_settings_to_nav_settings_exchanges)
        }
        return true
    }

}
