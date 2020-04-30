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

package net.taler.wallet.withdraw

import android.transition.TransitionManager.beginDelayedTransition
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import io.noties.markwon.Markwon
import net.taler.wallet.R

class TosAdapter(
    private val markwon: Markwon
) : RecyclerView.Adapter<TosAdapter.TosSectionViewHolder>() {

    private val items = ArrayList<TosSection>()

    init {
        setHasStableIds(true)
    }

    override fun getItemCount() = items.size

    override fun getItemId(position: Int): Long {
        return items[position].node.hashCode().toLong()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TosSectionViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.list_item_tos, parent, false)
        return TosSectionViewHolder(v)
    }

    override fun onBindViewHolder(holder: TosSectionViewHolder, position: Int) {
        holder.bind(items[position])
    }

    fun setSections(sections: List<TosSection>) {
        items.clear()
        items.addAll(sections)
        notifyDataSetChanged()
    }

    inner class TosSectionViewHolder(private val v: View) : RecyclerView.ViewHolder(v) {
        private val sectionTitle: TextView = v.findViewById(R.id.sectionTitle)
        private val expandButton: ImageView = v.findViewById(R.id.expandButton)
        private val sectionText: TextView = v.findViewById(R.id.sectionText)

        fun bind(item: TosSection) {
            sectionTitle.text = item.title
            showSection(item, item.expanded)
            val onClickListener = View.OnClickListener {
                if (!item.expanded) beginDelayedTransition(v as ViewGroup)
                item.expanded = !item.expanded
                showSection(item, item.expanded)
            }
            sectionTitle.setOnClickListener(onClickListener)
        }

        private fun showSection(item: TosSection, show: Boolean) {
            if (show) {
                expandButton.setImageResource(R.drawable.ic_keyboard_arrow_up)
                markwon.setParsedMarkdown(sectionText, markwon.render(item.node))
                sectionText.visibility = VISIBLE
            } else {
                expandButton.setImageResource(R.drawable.ic_keyboard_arrow_down)
                sectionText.visibility = GONE
            }
        }
    }

}
