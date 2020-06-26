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

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Bundle
import android.telephony.TelephonyManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.transition.MaterialContainerTransform
import com.google.android.material.transition.MaterialContainerTransform.FADE_MODE_CROSS
import kotlinx.android.synthetic.main.fragment_sms.*
import org.gnu.anastasis.ui.MainViewModel
import org.gnu.anastasis.ui.PERMISSION_REQUEST_CODE
import org.gnu.anastasis.ui.R

private const val PERMISSION = Manifest.permission.READ_PHONE_STATE

class SmsFragment : Fragment() {

    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        sharedElementEnterTransition = MaterialContainerTransform().apply {
            fadeMode = FADE_MODE_CROSS
        }
        return inflater.inflate(R.layout.fragment_sms, container, false).apply {
            transitionName = "sms_card"
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        smsView.editText?.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) checkPerm()
        }
        saveSmsButton.setOnClickListener {
            viewModel.smsChecked.value = true
            findNavController().popBackStack()
        }
    }

    private fun checkPerm() = when {
        ContextCompat.checkSelfPermission(requireContext(), PERMISSION)
                == PERMISSION_GRANTED -> {
            // You can use the API that requires the permission.
            fillPhoneNumber()
        }
        shouldShowRequestPermissionRationale(PERMISSION) -> {
            // In an educational UI, explain to the user why your app requires this
            // permission for a specific feature to behave as expected. In this UI,
            // include a "cancel" or "no thanks" button that allows the user to
            // continue using your app without granting the permission.
        }
        else -> {
            // You can directly ask for the permission.
            requestPermissions(arrayOf(PERMISSION), PERMISSION_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() &&
            grantResults[0] == PERMISSION_GRANTED
        ) checkPerm()
    }

    @SuppressLint("HardwareIds")
    @RequiresPermission(PERMISSION)
    private fun fillPhoneNumber() {
        val telephonyService = requireContext().getSystemService(TelephonyManager::class.java)
        telephonyService?.line1Number?.let { phoneNumber ->
            smsView?.editText?.setText(phoneNumber)
            smsView?.editText?.setSelection(phoneNumber.length)
        }
    }

}
