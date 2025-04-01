package com.clipcraft.ui

import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.JTextField
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import kotlin.math.max
import kotlin.math.min

class SpinnerTextField(initialValue: Int, private val minValue: Int, private val maxValue: Int) : JTextField() {
    init {
        text = initialValue.toString()

        addKeyListener(object : KeyAdapter() {
            override fun keyTyped(e: KeyEvent?) {
                val c = e?.keyChar ?: return
                // Only allow digits, backspace, delete
                if (!c.isDigit() && c != '\b' && c != 127.toChar()) {
                    e.consume()
                }
            }
        })
    }

    fun getIntValue(): Int {
        val num = text.toIntOrNull() ?: minValue
        return max(minValue, min(maxValue, num))
    }

    fun setIntValue(value: Int) {
        text = max(minValue, min(maxValue, value)).toString()
    }

    /**
     * Helper to track changes and notify when value changes.
     */
    fun SpinnerTextField.whenValueChanged(listener: (Int) -> Unit) {
        this.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) {
                listener(getIntValue())
            }

            override fun removeUpdate(e: DocumentEvent?) {
                listener(getIntValue())
            }

            override fun changedUpdate(e: DocumentEvent?) {
                listener(getIntValue())
            }
        })
    }
}
