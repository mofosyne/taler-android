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

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_CREATE_DOCUMENT
import android.content.Intent.ACTION_OPEN_DOCUMENT
import android.content.Intent.CATEGORY_OPENABLE
import android.content.Intent.EXTRA_TITLE
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.fragment_settings.*


interface ResetDialogEventListener {
    fun onResetConfirmed()
    fun onResetCancelled()
}


class ResetDialogFragment : DialogFragment() {
    private lateinit var listener: ResetDialogEventListener

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            // Use the Builder class for convenient dialog construction
            val builder = AlertDialog.Builder(it)
            builder.setMessage("Do you really want to reset the wallet and lose all coins and purchases?  Consider making a backup first.")
                .setPositiveButton("Reset") { _, _ ->
                    listener.onResetConfirmed()
                }
                .setNegativeButton("Cancel") { _, _ ->
                    listener.onResetCancelled()
                }
            // Create the AlertDialog object and return it
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            listener = context as ResetDialogEventListener
        } catch (e: ClassCastException) {
            // The activity doesn't implement the interface, throw exception
            throw ClassCastException(
                (context.toString() +
                        " must implement ResetDialogEventListener")
            )
        }
    }
}

class Settings : Fragment() {

    companion object {
        private const val TAG = "taler-wallet"
        private const val CREATE_FILE = 1
        private const val PICK_FILE = 2
    }

    private val model: WalletViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        model.devMode.observe(viewLifecycleOwner, Observer { enabled ->
            val visibility = if (enabled) VISIBLE else GONE
            devSettingsTitle.visibility = visibility
            button_reset_wallet_dangerously.visibility = visibility
        })

        textView4.text = BuildConfig.VERSION_NAME
        button_reset_wallet_dangerously.setOnClickListener {
            val d = ResetDialogFragment()
            d.show(parentFragmentManager, "walletResetDialog")
        }
        button_backup_export.setOnClickListener {
            val intent = Intent(ACTION_CREATE_DOCUMENT).apply {
                addCategory(CATEGORY_OPENABLE)
                type = "application/json"
                putExtra(EXTRA_TITLE, "taler-wallet-backup.json")

                // Optionally, specify a URI for the directory that should be opened in
                // the system file picker before your app creates the document.
                //putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri)
            }
            startActivityForResult(intent, CREATE_FILE)
        }
        button_backup_import.setOnClickListener {
            val intent = Intent(ACTION_OPEN_DOCUMENT).apply {
                addCategory(CATEGORY_OPENABLE)
                type = "application/json"

                //putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri)
            }
            startActivityForResult(intent, PICK_FILE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (data == null) return
        when (requestCode) {
            CREATE_FILE -> Log.i(TAG, "got createFile result with URL ${data.data}")
            PICK_FILE -> Log.i(TAG, "got pickFile result with URL ${data.data}")
        }
    }

}
