// SPDX-License-Identifier: GPL-3.0-only
// Sulanga Keyboard - Singlish combiner (based on BnKhiproCombiner from HeliBoard)

package helium314.keyboard.event

import helium314.keyboard.keyboard.internal.keyboard_parser.floris.KeyCode
import helium314.keyboard.latin.common.Constants
import java.util.ArrayList

/**
 * Collects the latin letters of the current word and shows them converted to Sinhala
 * (SinglishEngine). The converted text is committed on space / enter / functional keys.
 */
class SiSinglishCombiner : Combiner {

    private val composingText = StringBuilder()

    override fun processEvent(previousEvents: ArrayList<Event>?, event: Event): Event {
        val codePoint = event.codePoint

        if (event.keyCode == KeyCode.SHIFT) return event

        if (event.keyCode == KeyCode.DELETE) {
            if (composingText.isNotEmpty()) {
                val cp = composingText.codePointBefore(composingText.length)
                composingText.delete(composingText.length - Character.charCount(cp), composingText.length)
                if (composingText.isEmpty()) {
                    reset()
                    return Event.createHardwareKeypressEvent(0x20, Constants.CODE_SPACE, 0, event, event.isKeyRepeat)
                }
                return Event.createConsumedEvent(event)
            }
            return event
        }

        val isValidCodePoint = codePoint != Integer.MAX_VALUE && Character.isValidCodePoint(codePoint)
        val isWhitespace = isValidCodePoint && Character.isWhitespace(codePoint)

        if (event.isFunctionalKeyEvent || isWhitespace) {
            return commitAndReset(event)
        }

        if (!isValidCodePoint) return Event.createConsumedEvent(event)

        // Only latin letters and the escape char are composed; anything else (digits, punctuation,
        // Sinhala/emoji from other keys) commits the current word and is typed normally.
        val isLatinLetter = (codePoint in 'a'.code..'z'.code) || (codePoint in 'A'.code..'Z'.code) || codePoint == '\\'.code
        if (!isLatinLetter) {
            if (composingText.isEmpty()) return event
            val converted = combiningStateFeedback.toString()
            reset()
            return Event.createSoftwareTextEvent(converted + String(Character.toChars(codePoint)), KeyCode.MULTIPLE_CODE_POINTS, event)
        }

        composingText.append(Character.toChars(codePoint))
        return Event.createConsumedEvent(event)
    }

    override val combiningStateFeedback: CharSequence
        get() = SinglishEngine.convert(composingText.toString())

    override fun reset() {
        composingText.setLength(0)
    }

    private fun commitAndReset(event: Event): Event {
        val converted = combiningStateFeedback
        reset()
        return Event.createSoftwareTextEvent(converted, KeyCode.MULTIPLE_CODE_POINTS, event)
    }
}
