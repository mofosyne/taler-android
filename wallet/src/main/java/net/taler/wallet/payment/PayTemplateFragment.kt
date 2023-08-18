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

package net.taler.wallet.payment

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import net.taler.wallet.MainViewModel
import net.taler.wallet.R
import net.taler.wallet.compose.TalerSurface

class PayTemplateFragment : Fragment() {

    private val model: MainViewModel by activityViewModels()
    private lateinit var uriString: String
    private lateinit var uri: Uri

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        uriString = arguments?.getString("uri") ?: error("no amount passed")
        uri = Uri.parse(uriString)

        return ComposeView(requireContext()).apply {
            setContent {
                TalerSurface {
                    PayTemplateComposable(
                        uri = uri,
                        currencies = model.getCurrencies(),
                        fragment = this@PayTemplateFragment,
                        model = model,
                        onSubmit = { createOrder(it) },
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // TODO: this is not ideal, if the template is fixed, the
        //  user shouldn't even have to go through this fragment.
        if (uri.queryParameterNames?.isEmpty() == true) {
            createOrder(emptyMap())
        }
    }

    private fun createOrder(params: Map<String, String>) {
        model.paymentManager.preparePayForTemplate(uriString, params).invokeOnCompletion {
            // TODO maybe better to observe/collect payStatus instead of invokeOnCompletion
            //  and then only reacting to one of the possible payStatus values
            if (model.paymentManager.payStatus.value is PayStatus.Prepared) {
                val navOptions = NavOptions.Builder()
                    .setPopUpTo(R.id.nav_main, true)
                    .build()
                findNavController().navigate(R.id.action_global_promptPayment, null, navOptions)
            }
        }
    }
}
