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

package net.taler.wallet.exchanges

import android.app.Dialog
import android.os.Bundle
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import com.google.accompanist.themeadapter.material3.Mdc3Theme
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import net.taler.common.Event
import net.taler.common.toEvent
import net.taler.wallet.R
import net.taler.wallet.cleanExchange
import net.taler.wallet.compose.collectAsStateLifecycleAware

class SelectExchangeDialogFragment: DialogFragment() {
    private var exchangeList = MutableLiveData<List<ExchangeItem>>()

    private var mExchangeSelection = MutableLiveData<Event<ExchangeItem>>()
    val exchangeSelection: LiveData<Event<ExchangeItem>> = mExchangeSelection

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = ComposeView(requireContext()).apply {
            setContent {
                val exchanges = exchangeList.asFlow().collectAsStateLifecycleAware(initial = emptyList())
                SelectExchangeComposable(exchanges.value) {
                    onExchangeSelected(it)
                }
            }
        }

        return MaterialAlertDialogBuilder(requireContext(), R.style.MaterialAlertDialog_Material3)
            .setIcon(R.drawable.ic_account_balance)
            .setTitle(R.string.exchange_list_select)
            .setView(view)
            .setNegativeButton(R.string.cancel) { _, _ ->
                dismiss()
            }
            .create()
    }

    fun setExchanges(exchanges: List<ExchangeItem>) {
        exchangeList.value = exchanges
    }

    private fun onExchangeSelected(exchange: ExchangeItem) {
        mExchangeSelection.value = exchange.toEvent()
        dismiss()
    }
}

@Composable
fun SelectExchangeComposable(
    exchanges: List<ExchangeItem>,
    onExchangeSelected: (exchange: ExchangeItem) -> Unit,
) {
    Mdc3Theme {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
        ) {
            items(exchanges) {
                ExchangeItemComposable(it) {
                    onExchangeSelected(it)
                }
            }
        }
    }
}

@Composable
fun ExchangeItemComposable(exchange: ExchangeItem, onSelected: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable { onSelected() },
        headlineContent = { Text(cleanExchange(exchange.exchangeBaseUrl)) },
        supportingContent = exchange.currency?.let {
            { Text(stringResource(R.string.exchange_list_currency, it)) }
        },
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent,
        )
    )
}