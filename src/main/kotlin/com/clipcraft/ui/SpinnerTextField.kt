package com.clipcraft.ui

import java.awt.FlowLayout
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JTextField

class SpinnerTextField(
    initialValue: Int,
    private val min: Int,
    private val max: Int
) : JPanel(FlowLayout(FlowLayout.LEFT, 0, 0)) {

    private val textField: JTextField = JTextField(initialValue.toString(), 5)
    private val incrementButton: JButton = JButton("+")
    private val decrementButton: JButton = JButton("-")

    init {
        add(decrementButton)
        add(textField)
        add(incrementButton)

        incrementButton.addActionListener {
            val current = textField.text.toIntOrNull() ?: initialValue
            if (current < max) {
                textField.text = (current + 1).toString()
                firePropertyChange("value", current, current + 1)
            }
        }
        decrementButton.addActionListener {
            val current = textField.text.toIntOrNull() ?: initialValue
            if (current > min) {
                textField.text = (current - 1).toString()
                firePropertyChange("value", current, current - 1)
            }
        }
        textField.addFocusListener(object : FocusAdapter() {
            override fun focusLost(e: FocusEvent?) {
                val current = textField.text.toIntOrNull() ?: min
                val clamped = current.coerceIn(min, max)
                if (clamped != current) {
                    textField.text = clamped.toString()
                    firePropertyChange("value", current, clamped)
                }
            }
        })
    }

    fun whenValueChanged(listener: (Int) -> Unit) {
        addPropertyChangeListener("value") { evt ->
            (evt.newValue as? Int)?.let { listener(it) }
        }
    }

    fun getValue(): Int = textField.text.toIntOrNull() ?: min
}
