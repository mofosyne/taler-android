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

package net.taler.merchantpos

import android.content.Intent
import android.content.Intent.ACTION_MAIN
import android.content.Intent.CATEGORY_HOME
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Bundle
import android.os.Handler
import android.view.MenuItem
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat.START
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.navigation.NavigationView.OnNavigationItemSelectedListener
import net.taler.common.NfcManager
import net.taler.merchantpos.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), OnNavigationItemSelectedListener {

    private val model: MainViewModel by viewModels()
    private val nfcManager = NfcManager()

    private lateinit var ui: ActivityMainBinding
    private lateinit var nav: NavController

    private var reallyExit = false

    companion object {
        const val TAG = "taler-pos"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = ActivityMainBinding.inflate(layoutInflater)
        setContentView(ui.root)

        model.paymentManager.payment.observe(this) { payment ->
            payment?.talerPayUri?.let {
                nfcManager.setTagString(it)
            }
        }

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
        nav = navHostFragment.navController

        ui.navView.setupWithNavController(nav)
        ui.navView.setNavigationItemSelectedListener(this)

        setSupportActionBar(ui.main.toolbar)
        val appBarConfiguration = AppBarConfiguration(nav.graph, ui.drawerLayout)
        ui.main.toolbar.setupWithNavController(nav, appBarConfiguration)
    }

    override fun onStart() {
        super.onStart()
        if (!model.configManager.config.isValid() && nav.currentDestination?.id != R.id.nav_settings) {
            nav.navigate(R.id.action_global_merchantSettings)
        } else if (model.configManager.merchantConfig == null && nav.currentDestination?.id != R.id.configFetcher) {
            nav.navigate(R.id.action_global_configFetcher)
        }
    }

    public override fun onResume() {
        super.onResume()
        // TODO should we only read tags when a payment is to be made?
        NfcManager.start(this, nfcManager)
    }

    public override fun onPause() {
        super.onPause()
        NfcManager.stop(this)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_order -> nav.navigate(R.id.action_global_order)
            R.id.nav_history -> nav.navigate(R.id.action_global_merchantHistory)
            R.id.nav_settings -> nav.navigate(R.id.action_global_merchantSettings)
        }
        ui.drawerLayout.closeDrawer(START)
        return true
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val currentDestination = nav.currentDestination?.id
        if (ui.drawerLayout.isDrawerOpen(START)) {
            ui.drawerLayout.closeDrawer(START)
        } else if (currentDestination == R.id.nav_settings && !model.configManager.config.isValid()) {
            // we are in the configuration screen and need a config to continue
            val intent = Intent(ACTION_MAIN).apply {
                addCategory(CATEGORY_HOME)
                flags = FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
        } else if (currentDestination == R.id.nav_order) {
            if (reallyExit) super.onBackPressed()
            else {
                // this closes the app and causes orders to be lost, so let's confirm first
                reallyExit = true
                Toast.makeText(this, R.string.toast_back_to_exit, LENGTH_SHORT).show()
                Handler().postDelayed({ reallyExit = false }, 3000)
            }
        } else super.onBackPressed()
    }

}
