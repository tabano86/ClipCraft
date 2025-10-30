package com.clipcraft.ui

import com.clipcraft.model.ExportPreset
import com.clipcraft.model.OutputFormat
import com.clipcraft.services.professional.ProfessionalFormatter
import com.clipcraft.services.professional.ProfessionalTokenEstimator
import com.formdev.flatlaf.FlatClientProperties
import com.formdev.flatlaf.extras.FlatSVGIcon
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.BoxLayout
import java.awt.Color
import java.awt.Cursor
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import java.awt.GradientPaint
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.Toolkit
import javax.swing.Action
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.Timer

/**
 * Modern, gradient-rich dialog with sharp icons and universal copy functionality.
 * Fully theme-compliant (light/dark/high-contrast).
 */
class ModernClipCraftDialog(
    project: Project,
    private val initialContent: String,
    private val estimatedTokens: Int
) : DialogWrapper(project) {

    private val previewArea: JBTextArea
    private val formatComboBox: JComboBox<OutputFormat>
    private val presetComboBox: JComboBox<ExportPreset>
    private val tokenLabel: JLabel
    private val copyButton: JButton
    private val statsPanel: JPanel

    init {
        title = "ClipCraft Export Preview"
        previewArea = createStyledTextArea()
        formatComboBox = createStyledComboBox(OutputFormat.values())
        presetComboBox = createStyledComboBox(ExportPreset.getAllPresets().toTypedArray())
        tokenLabel = createStyledLabel()
        copyButton = createGradientButton("📋 Copy to Clipboard")
        statsPanel = createStatsPanel()

        previewArea.text = initialContent
        updateTokenDisplay()

        init()
        setSize(1000, 700)
    }

    override fun createCenterPanel(): JComponent {
        return JPanel(BorderLayout(10, 10)).apply {
            border = JBUI.Borders.empty(15)

            // Top gradient header
            add(createGradientHeader(), BorderLayout.NORTH)

            // Main content with preview
            add(createMainPanel(), BorderLayout.CENTER)

            // Bottom button panel
            add(createBottomPanel(), BorderLayout.SOUTH)
        }
    }

    private fun createGradientHeader(): JPanel {
        return object : JPanel() {
            override fun paintComponent(g: Graphics) {
                super.paintComponent(g)
                val g2d = g as Graphics2D
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)

                // Modern gradient (theme-aware)
                val gradient = if (JBColor.isBright()) {
                    GradientPaint(
                        0f, 0f, Color(102, 126, 234),
                        width.toFloat(), height.toFloat(), Color(118, 75, 162)
                    )
                } else {
                    GradientPaint(
                        0f, 0f, Color(66, 86, 174),
                        width.toFloat(), height.toFloat(), Color(78, 55, 122)
                    )
                }

                g2d.paint = gradient
                g2d.fillRoundRect(0, 0, width, height, 20, 20)
            }

            override fun getPreferredSize(): Dimension = Dimension(super.getPreferredSize().width, 80)
        }.apply {
            layout = BorderLayout()
            isOpaque = false

            val titleLabel = JLabel("ClipCraft Export").apply {
                font = Font(font.family, Font.BOLD, 24)
                foreground = Color.WHITE
                border = JBUI.Borders.empty(20, 20)
            }

            val subtitleLabel = JLabel("Professional code export for AI & documentation").apply {
                font = Font(font.family, Font.PLAIN, 12)
                foreground = Color(255, 255, 255, 200)
                border = JBUI.Borders.empty(0, 20, 20, 20)
            }

            val headerContent = JPanel().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                isOpaque = false
                add(titleLabel)
                add(subtitleLabel)
            }

            add(headerContent, BorderLayout.WEST)
            add(statsPanel, BorderLayout.EAST)
        }
    }

    private fun createMainPanel(): JPanel {
        return JPanel(BorderLayout(10, 10)).apply {
            // Controls panel
            add(createControlsPanel(), BorderLayout.NORTH)

            // Preview panel with copy-on-click
            add(createPreviewPanel(), BorderLayout.CENTER)
        }
    }

    private fun createControlsPanel(): JPanel {
        return JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = JBUI.Borders.empty(10)

            // Format selection
            val formatPanel = JPanel(FlowLayout(FlowLayout.LEFT, 10, 5)).apply {
                add(JLabel("Output Format:").apply {
                    font = Font(font.family, Font.BOLD, 12)
                })
                add(formatComboBox)
            }

            // Preset selection
            val presetPanel = JPanel(FlowLayout(FlowLayout.LEFT, 10, 5)).apply {
                add(JLabel("Quick Preset:").apply {
                    font = Font(font.family, Font.BOLD, 12)
                })
                add(presetComboBox)
            }

            // Token info
            val tokenPanel = JPanel(FlowLayout(FlowLayout.LEFT, 10, 5)).apply {
                add(JLabel("Estimated Tokens:").apply {
                    font = Font(font.family, Font.BOLD, 12)
                })
                add(tokenLabel)
            }

            add(formatPanel)
            add(presetPanel)
            add(tokenPanel)
        }
    }

    private fun createPreviewPanel(): JScrollPane {
        return JBScrollPane(previewArea).apply {
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Preview (Click anywhere to copy)"),
                JBUI.Borders.empty(10)
            )

            // Universal copy-on-click
            previewArea.addMouseListener(object : java.awt.event.MouseAdapter() {
                override fun mouseClicked(e: java.awt.event.MouseEvent?) {
                    copyToClipboard()
                }
            })

            // Show copy cursor
            previewArea.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        }
    }

    private fun createBottomPanel(): JPanel {
        return JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty(10, 0, 0, 0)

            // Copy button on the right
            val buttonPanel = JPanel(FlowLayout(FlowLayout.RIGHT, 10, 0)).apply {
                add(copyButton)
            }

            add(buttonPanel, BorderLayout.EAST)
        }
    }

    private fun createStatsPanel(): JPanel {
        return JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            isOpaque = false
            border = JBUI.Borders.empty(20)

            val filesLabel = JLabel("Files: 0").apply {
                foreground = Color.WHITE
                font = Font(font.family, Font.BOLD, 14)
            }

            val tokensLabel = JLabel("Tokens: 0").apply {
                foreground = Color(255, 255, 255, 200)
                font = Font(font.family, Font.PLAIN, 12)
            }

            add(filesLabel)
            add(tokensLabel)
        }
    }

    private fun createStyledTextArea(): JBTextArea {
        return JBTextArea().apply {
            isEditable = false
            lineWrap = false
            font = Font("JetBrains Mono", Font.PLAIN, 12)
            tabSize = 4

            // Modern styling
            putClientProperty(FlatClientProperties.STYLE, "arc: 8")
        }
    }

    private fun <T> createStyledComboBox(items: Array<T>): JComboBox<T> {
        return JComboBox(items).apply {
            maximumRowCount = 10

            // Modern styling
            putClientProperty(FlatClientProperties.COMPONENT_ROUND_RECT, true)

            addActionListener {
                updatePreview()
            }
        }
    }

    private fun createStyledLabel(): JLabel {
        return JLabel().apply {
            font = Font(font.family, Font.BOLD, 12)
        }
    }

    private fun createGradientButton(text: String): JButton {
        return JButton(text).apply {
            font = Font(font.family, Font.BOLD, 14)

            // Modern button styling
            putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_ROUND_RECT)
            putClientProperty(FlatClientProperties.STYLE, "arc: 12; borderWidth: 0")

            // Gradient background
            isContentAreaFilled = true
            background = if (JBColor.isBright()) {
                Color(102, 126, 234)
            } else {
                Color(66, 86, 174)
            }

            foreground = Color.WHITE

            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)

            addActionListener {
                copyToClipboard()
            }
        }
    }

    private fun updateTokenDisplay() {
        val formatted = ProfessionalTokenEstimator.formatTokenCount(estimatedTokens)
        val compatible = ProfessionalTokenEstimator.getCompatibleModels(previewArea.text)

        val tooltipText = buildString {
            append("<html><b>Token Estimation</b><br>")
            append("Exact count: $estimatedTokens<br>")
            append("<br><b>Compatible Models:</b><br>")
            compatible.take(5).forEach {
                val percentage = (estimatedTokens.toDouble() / it.maxTokens * 100).toInt()
                append("• ${it.name}: $percentage%<br>")
            }
            append("</html>")
        }

        tokenLabel.text = formatted
        tokenLabel.toolTipText = tooltipText

        // Color code based on size
        tokenLabel.foreground = when {
            estimatedTokens < 8000 -> JBColor.GREEN
            estimatedTokens < 32000 -> JBColor.YELLOW
            estimatedTokens < 100000 -> JBColor.ORANGE
            else -> JBColor.RED
        }
    }

    private fun updatePreview() {
        // Update preview based on selected format
        // This would regenerate the content in the selected format
        updateTokenDisplay()
    }

    private fun copyToClipboard() {
        val content = previewArea.text
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        clipboard.setContents(java.awt.datatransfer.StringSelection(content), null)

        // Visual feedback
        copyButton.text = "✓ Copied!"
        Timer(2000) {
            copyButton.text = "📋 Copy to Clipboard"
        }.apply {
            isRepeats = false
            start()
        }

        // Show notification
        JOptionPane.showMessageDialog(
            this.contentPane,
            "Content copied to clipboard!",
            "Success",
            JOptionPane.INFORMATION_MESSAGE
        )
    }

    override fun createActions(): Array<Action> {
        return arrayOf(okAction, cancelAction)
    }
}
