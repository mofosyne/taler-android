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

package org.gnu.anastasis.ui.authentication

import android.os.Bundle
import android.view.Gravity.CENTER
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import com.google.android.material.card.MaterialCardView
import kotlinx.android.synthetic.main.fragment_authentication.*
import net.taler.common.Amount
import org.gnu.anastasis.ui.MainViewModel
import org.gnu.anastasis.ui.R

class AuthenticationFragment : Fragment() {

    private val viewModel: MainViewModel by activityViewModels()

    private var price: Amount = Amount.zero("KUDOS")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_authentication, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        passwordCard.setOnClickListener {
            showDialog(
                R.id.action_nav_anastasis_authentication_to_securityQuestionFragment,
                passwordCard,
                "question_card"
            )
        }
        postidentCard.setOnClickListener {
            toggleCard(
                postidentCard,
                Amount.fromJSONString("KUDOS:3.5")
            )
        }
        smsCard.setOnClickListener {
            showDialog(
                R.id.action_nav_anastasis_authentication_to_smsFragment,
                smsCard,
                "sms_card"
            )
        }
        videoCard.setOnClickListener {
            showDialog(
                R.id.action_nav_anastasis_authentication_to_videoFragment,
                videoCard,
                "video_card"
            )
        }

        viewModel.securityQuestionChecked.observe(viewLifecycleOwner, Observer { checked ->
            passwordCard.isChecked = checked
            updatePrice(checked, Amount.fromJSONString("KUDOS:0.5"))
            updateNextButtonState()
        })
        viewModel.smsChecked.observe(viewLifecycleOwner, Observer { checked ->
            smsCard.isChecked = checked
            updatePrice(checked, Amount.fromJSONString("KUDOS:1.0"))
            updateNextButtonState()
        })
        viewModel.videoChecked.observe(viewLifecycleOwner, Observer { checked ->
            videoCard.isChecked = checked
            updatePrice(checked, Amount.fromJSONString("KUDOS:2.25"))
            updateNextButtonState()
        })
    }

    private fun showDialog(@IdRes resId: Int, view: View, transitionName: String) {
        val extras = FragmentNavigatorExtras(view to transitionName)
        findNavController().navigate(resId, null, null, extras)
    }

    private fun toggleCard(card: MaterialCardView, price: Amount) {
        card.isChecked = !card.isChecked
        val text = "Imagine you entered information here"
        if (card.isChecked) Toast.makeText(requireContext(), text, LENGTH_SHORT).apply {
            setGravity(CENTER, 0, 0)
        }.show()
        updatePrice(card.isChecked, price)
        updateNextButtonState()
    }

    private fun updatePrice(add: Boolean, amount: Amount) {
        if (add) price += amount
        else price -= amount
        recoveryCostView.text = "Recovery cost: $price"
    }

    private fun updateNextButtonState() {
        var numChecked = 0
        numChecked += if (passwordCard.isChecked) 1 else 0
        numChecked += if (postidentCard.isChecked) 1 else 0
        numChecked += if (smsCard.isChecked) 1 else 0
        numChecked += if (videoCard.isChecked) 1 else 0
        nextAuthButton.isEnabled = numChecked >= 2
    }

}
