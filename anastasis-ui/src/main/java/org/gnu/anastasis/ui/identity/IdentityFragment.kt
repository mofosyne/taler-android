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
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.telephony.TelephonyManager
import android.text.format.DateFormat.getDateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import androidx.core.content.getSystemService
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import org.gnu.anastasis.ui.MainViewModel
import org.gnu.anastasis.ui.R
import org.gnu.anastasis.ui.databinding.FragmentIdentityBinding
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit.DAYS

private const val MIN_AGE = 18

class AnastasisIdentityFragment : Fragment() {

    private val model: MainViewModel by activityViewModels()

    private var _binding: FragmentIdentityBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        _binding = FragmentIdentityBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        model.currentCountry.observe(viewLifecycleOwner, { country ->
            binding.countryView.text = country.name
            if (binding.stub != null) {
                binding.stub.layoutResource = country.layoutRes
                binding.stub.inflate()
            }
        })
        binding.changeCountryView.setOnClickListener {
            findNavController().navigate(R.id.action_nav_anastasis_identity_to_nav_change_location)
        }
        binding.birthDateInput.editText?.setOnClickListener {
            if (SDK_INT >= 24) {
                val picker = DatePickerDialog(requireContext())
                picker.datePicker.maxDate =
                    System.currentTimeMillis() - DAYS.toMillis(365) * MIN_AGE
                picker.setOnDateSetListener { _, year, month, dayOfMonth ->
                    val calender = Calendar.getInstance().apply {
                        set(year, month, dayOfMonth)
                    }
                    val date = Date(calender.timeInMillis)
                    val dateStr = getDateFormat(requireContext()).format(date)
                    binding.birthDateInput.editText?.setText(dateStr)
                }
                picker.show()
            } else {
                Toast.makeText(requireContext(), "Needs newer phone", LENGTH_LONG).show()
            }
        }
        binding.createIdentifierButton.setOnClickListener {
            findNavController().navigate(R.id.action_nav_anastasis_intro_to_nav_anastasis_authentication)
        }
    }

    @Suppress("unused")
    private fun getCountryName(): String {
        val tm = requireContext().getSystemService<TelephonyManager>()!!
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
