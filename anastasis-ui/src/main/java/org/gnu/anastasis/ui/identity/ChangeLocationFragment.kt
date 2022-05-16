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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import org.gnu.anastasis.ui.MainViewModel
import org.gnu.anastasis.ui.R
import org.gnu.anastasis.ui.databinding.FragmentChangeLocationBinding

class ChangeLocationFragment : Fragment() {

    private val viewModel: MainViewModel by activityViewModels()

    private var _binding: FragmentChangeLocationBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_change_location, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.switzerlandView.setOnClickListener {
            changeCountry(LOCATIONS[0])
        }
        binding.germanyView.setOnClickListener {
            changeCountry(LOCATIONS[1])
        }
        binding.usaView.setOnClickListener {
            changeCountry(LOCATIONS[2])
        }
        binding.indiaView.setOnClickListener {
            changeCountry(LOCATIONS[3])
        }
    }

    private fun changeCountry(location: Location) {
        viewModel.currentCountry.value = location
        findNavController().popBackStack()
    }

}
