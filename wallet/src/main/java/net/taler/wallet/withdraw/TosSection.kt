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

import android.util.Log
import io.noties.markwon.Markwon
import kotlinx.serialization.Serializable
import org.commonmark.node.Code
import org.commonmark.node.Document
import org.commonmark.node.Heading
import org.commonmark.node.Node
import org.commonmark.node.Text
import java.text.ParseException

data class TosSection(
    val title: String,
    val node: Node,
    var expanded: Boolean = false
)

@Throws(ParseException::class)
internal fun parseTos(markwon: Markwon, text: String): List<TosSection> {
    var node: Node? =
        markwon.parse(text).firstChild ?: throw ParseException("Invalid markdown", 0)
    var lastHeading: String? = null
    var section = Document()
    val sections = ArrayList<TosSection>()
    while (node != null) {
        val next: Node? = node.next
        if (node is Heading && node.level == 1) {
            // if lastHeading exists, close previous section
            if (lastHeading != null) {
                sections.add(TosSection(lastHeading, section))
                section = Document()
            }
            // start new section with new heading (stripped of markup)
            lastHeading = getNodeText(node)
            if (lastHeading.isBlank()) throw ParseException("Empty heading", 0)
        } else if (lastHeading == null) {
            throw ParseException("The exchange ToS does not follow the correct format", 0)
        } else {
            section.appendChild(node)
        }
        node = next
    }
    check(lastHeading != null)
    sections.add(TosSection(lastHeading, section))
    return sections
}

private fun getNodeText(rootNode: Node): String {
    var node: Node? = rootNode.firstChild
    var text = ""
    while (node != null) {
        text += when (node) {
            is Text -> node.literal
            is Code -> node.literal
            else -> getNodeText(node)
        }
        node = node.next
    }
    return text
}

@Serializable
data class TosResponse(
    val content: String,
    val currentEtag: String
)
