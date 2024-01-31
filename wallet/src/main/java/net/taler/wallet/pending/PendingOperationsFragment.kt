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

package net.taler.wallet.pending

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import net.taler.common.showError
import net.taler.wallet.MainViewModel
import net.taler.wallet.R
import net.taler.wallet.databinding.FragmentPendingOperationsBinding
import org.json.JSONObject

interface PendingOperationClickListener {
    fun onPendingOperationClick(type: String, detail: JSONObject)
}

class PendingOperationsFragment : Fragment(), PendingOperationClickListener {

    private val model: MainViewModel by activityViewModels()
    private val pendingOperationsManager by lazy { model.pendingOperationsManager }

    private lateinit var ui: FragmentPendingOperationsBinding
    private val pendingAdapter = PendingOperationsAdapter(emptyList(), this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        ui = FragmentPendingOperationsBinding.inflate(inflater, container, false)
        return ui.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ui.listPending.apply {
            val myLayoutManager = LinearLayoutManager(requireContext())
            val myItemDecoration =
                DividerItemDecoration(requireContext(), myLayoutManager.orientation)
            layoutManager = myLayoutManager
            adapter = pendingAdapter
            addItemDecoration(myItemDecoration)
        }

        pendingOperationsManager.pendingOperations.observe(viewLifecycleOwner) {
            updatePending(it)
        }
    }

    override fun onStart() {
        super.onStart()
        pendingOperationsManager.getPending()
    }

    @Deprecated("Deprecated in Java")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.retry_pending -> {
                pendingOperationsManager.retryPendingNow()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.pending_operations, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    private fun updatePending(pendingOperations: List<PendingOperationInfo>) {
        pendingAdapter.update(pendingOperations)
    }

    override fun onPendingOperationClick(type: String, detail: JSONObject) {
        requireActivity().showError("No detail view for $type implemented yet.")
    }
}

class PendingOperationsAdapter(
    private var items: List<PendingOperationInfo>,
    private val listener: PendingOperationClickListener
) :
    RecyclerView.Adapter<PendingOperationsAdapter.MyViewHolder>() {

    init {
        setHasStableIds(false)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val rowView =
            LayoutInflater.from(parent.context).inflate(R.layout.list_item_pending_operation, parent, false)
        return MyViewHolder(rowView)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val p = items[position]
        val pendingContainer = holder.rowView.findViewById<LinearLayout>(R.id.pending_container)
        pendingContainer.setOnClickListener {
            listener.onPendingOperationClick(p.type, p.detail)
        }
        val textView = holder.rowView.findViewById<TextView>(R.id.pending_text)
        val subTextView = holder.rowView.findViewById<TextView>(R.id.pending_subtext)
        subTextView.text = p.detail.toString(1)
        textView.text = p.type
    }

    fun update(items: List<PendingOperationInfo>) {
        this.items = items
        this.notifyDataSetChanged()
    }

    class MyViewHolder(val rowView: View) : RecyclerView.ViewHolder(rowView)

}
