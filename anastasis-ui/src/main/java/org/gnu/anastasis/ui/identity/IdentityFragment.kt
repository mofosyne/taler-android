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

package org.gnu.anastasis.ui.identity

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.os.Bundle
import android.telephony.TelephonyManager
import android.text.format.DateFormat.getDateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import kotlinx.android.synthetic.main.fragment_identity.*
import org.gnu.anastasis.ui.MainViewModel
import org.gnu.anastasis.ui.R
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit.DAYS

private const val MIN_AGE = 18

class AnastasisIdentityFragment : Fragment() {

    private val model: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_identity, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        model.currentCountry.observe(viewLifecycleOwner, Observer { country ->
            countryView.text = country.name
            if (stub != null) {
                stub.layoutResource = country.layoutRes
                stub.inflate()
            }
        })
        changeCountryView.setOnClickListener {
            findNavController().navigate(R.id.action_nav_anastasis_identity_to_nav_change_location)
        }
        birthDateInput.editText?.setOnClickListener {
            val picker = DatePickerDialog(requireContext())
            picker.datePicker.maxDate = System.currentTimeMillis() - DAYS.toMillis(365) * MIN_AGE
            picker.setOnDateSetListener { _, year, month, dayOfMonth ->
                val calender = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth)
                }
                val date = Date(calender.timeInMillis)
                val dateStr = getDateFormat(requireContext()).format(date)
                birthDateInput.editText?.setText(dateStr)
            }
            picker.show()
        }
        createIdentifierButton.setOnClickListener {
            findNavController().navigate(R.id.action_nav_anastasis_intro_to_nav_anastasis_authentication)
        }
    }

    @Suppress("unused")
    private fun getCountryName(): String {
        val tm = requireContext().getSystemService(TelephonyManager::class.java)!!
        val countryIso = if (tm.networkCountryIso.isNullOrEmpty()) {
            if (tm.simCountryIso.isNullOrEmpty()) {
                if (Locale.getDefault().country.isNullOrEmpty()) "unknown"
                else Locale.getDefault().country
            } else tm.simCountryIso
        } else tm.networkCountryIso
        var countryName = countryIso
        for (locale in Locale.getAvailableLocales()) {
            @SuppressLint("DefaultLocale")
            if (locale.country.toLowerCase() == countryIso) {
                countryName = locale.displayCountry
                break
            }
        }
        return countryName
    }

}
