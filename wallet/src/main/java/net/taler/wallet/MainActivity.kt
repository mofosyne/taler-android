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
import com.google.android.material.snackbar.BaseTransientBottomBar.LENGTH_SHORT
import com.google.android.material.snackbar.Snackbar
import com.google.zxing.integration.android.IntentIntegrator
import com.google.zxing.integration.android.IntentIntegrator.parseActivityResult
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import net.taler.common.isOnline
import net.taler.wallet.BuildConfig.VERSION_CODE
import net.taler.wallet.BuildConfig.VERSION_NAME
import net.taler.wallet.HostCardEmulatorService.Companion.HTTP_TUNNEL_RESPONSE
import net.taler.wallet.HostCardEmulatorService.Companion.MERCHANT_NFC_CONNECTED
import net.taler.wallet.HostCardEmulatorService.Companion.MERCHANT_NFC_DISCONNECTED
import net.taler.wallet.HostCardEmulatorService.Companion.TRIGGER_PAYMENT_ACTION
import net.taler.wallet.refund.RefundStatus
import java.util.Locale.ROOT

class MainActivity : AppCompatActivity(), OnNavigationItemSelectedListener,
    OnPreferenceStartFragmentCallback {

    private val model: MainViewModel by viewModels()

    private lateinit var nav: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        nav = navHostFragment.navController
        nav_view.setupWithNavController(nav)
        nav_view.setNavigationItemSelectedListener(this)
        if (savedInstanceState == null) {
            nav_view.menu.getItem(0).isChecked = true
        }

        setSupportActionBar(toolbar)
        val appBarConfiguration = AppBarConfiguration(
            setOf(R.id.nav_main, R.id.nav_settings, R.id.nav_pending_operations, R.id.nav_history),
            drawer_layout
        )
        toolbar.setupWithNavController(nav, appBarConfiguration)

        model.showProgressBar.observe(this, Observer { show ->
            progress_bar.visibility = if (show) VISIBLE else INVISIBLE
        })

        val versionView: TextView = nav_view.getHeaderView(0).findViewById(R.id.versionView)
        model.devMode.observe(this, Observer { enabled ->
            nav_view.menu.findItem(R.id.nav_dev).isVisible = enabled
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
        if (drawer_layout.isDrawerOpen(START)) drawer_layout.closeDrawer(START)
        else super.onBackPressed()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_home -> nav.navigate(R.id.nav_main)
            R.id.nav_settings -> nav.navigate(R.id.nav_settings)
            R.id.nav_pending_operations -> nav.navigate(R.id.nav_pending_operations)
            R.id.nav_history -> nav.navigate(R.id.nav_history)
        }
        drawer_layout.closeDrawer(START)
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
        when {
            url.toLowerCase(ROOT).startsWith("taler://pay/") -> {
                Log.v(TAG, "navigating!")
                nav.navigate(R.id.action_nav_main_to_promptPayment)
                model.paymentManager.preparePay(url)
            }
            url.toLowerCase(ROOT).startsWith("taler://withdraw/") -> {
                Log.v(TAG, "navigating!")
                nav.navigate(R.id.action_nav_main_to_promptWithdraw)
                model.withdrawManager.getWithdrawalInfo(url)
            }
            url.toLowerCase(ROOT).startsWith("taler://refund/") -> {
                model.showProgressBar.value = true
                model.refundManager.refund(url).observe(this, Observer { status ->
                    model.showProgressBar.value = false
                    val res = when (status) {
                        is RefundStatus.Error -> R.string.refund_error
                        // TODO once wallet-core exposes currency, navigate to its transaction list
                        is RefundStatus.Success -> R.string.refund_success
                    }
                    Snackbar.make(nav_view, res, LENGTH_LONG).show()
                })
            }
            else -> {
                Snackbar.make(
                    nav_view,
                    "URL from $from doesn't contain a supported Taler Uri.",
                    LENGTH_SHORT
                ).show()
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
