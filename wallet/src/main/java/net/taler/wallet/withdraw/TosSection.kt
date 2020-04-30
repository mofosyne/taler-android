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

import io.noties.markwon.Markwon
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
            // check that this is a plain heading
            if (node.firstChild !is Text || node.firstChild.next != null) {
                throw ParseException(
                    "Primary heading includes more than just text", sections.size
                )
            }
            // start new section
            lastHeading = (node.firstChild as Text).literal
        } else if (lastHeading == null) {
            throw ParseException("Found text before first primary heading", 0)
        } else {
            section.appendChild(node)
        }
        node = next
    }
    check(lastHeading != null)
    sections.add(TosSection(lastHeading, section))
    return sections
}
