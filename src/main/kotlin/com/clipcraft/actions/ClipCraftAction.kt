import com.clipcraft.services.ClipCraftSettings.Companion.getInstance
import java.awt.HeadlessException
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future
import javax.swing.JOptionPane
import kotlin.math.min

/**
 * Main class for ClipCraft functionality.
 * Handles collecting file contents and copying them to the clipboard.
 */
class ClipCraftAction {
    // Reference to settings (singleton for configuration values)
    private val settings = getInstance()

    /**
     * Executes the ClipCraft action: gather content and copy to clipboard.
     */
    fun performAction() {
        // Get target files (could be set via user selection or a saved profile)
        val files: MutableList<File>? = settings.getTargetFiles()
        if (files == null || files.isEmpty()) {
            showError("No files or directories selected for copying.")
            return
        }

        // Collect content from files (with concurrency and limits)
        val content = collectContent(files)
        if (content.isEmpty()) {
            showError("No content to copy (files may be empty or unreadable).")
            return
        }

        // If a max token limit is set, enforce it
        val maxTokens: Int = settings.getMaxTokens()
        if (maxTokens > 0) {
            val tokenCount = countTokens(content)
            if (tokenCount > maxTokens) {
                // Ask user to confirm copying a very large content block
                val choice = JOptionPane.showConfirmDialog(
                    null,
                    "The content has " + tokenCount + " tokens, exceeding the limit of " + maxTokens + ". Continue with copy?",
                    "Large Copy Confirmation",
                    JOptionPane.YES_NO_OPTION
                )
                if (choice != JOptionPane.YES_OPTION) {
                    // User chose not to proceed with the large copy
                    return
                }
            }
        }

        // Copy the (possibly truncated) content to the system clipboard
        try {
            copyToClipboard(content)
        } catch (e: IOException) {
            showError("Failed to copy to clipboard: " + e.message)
        }
    }

    /**
     * Reads and aggregates content from the given list of files (and directories).
     * Uses concurrent processing for efficiency and respects the max token limit.
     * @param files List of files and directories to process.
     * @return Combined text content from all files.
     */
    private fun collectContent(files: MutableList<File>): String {
        // Expand any directories in the list into individual files
        val allFiles = expandFiles(files)

        // Prepare a thread pool for concurrent reading
        val threadCount = min(allFiles.size.toDouble(), Runtime.getRuntime().availableProcessors().toDouble()).toInt()
        val executor = Executors.newFixedThreadPool(threadCount)
        val futures: MutableList<Future<String?>> = ArrayList<Future<String?>>()

        // Submit reading tasks for each file
        for (file in allFiles) {
            futures.add(executor.submit<String?>(Callable { readFileContent(file) }))
        }

        // Use StringBuilder for efficient concatenation
        val combined = StringBuilder()
        val maxTokens: Int = settings.getMaxTokens()
        var truncated = false // flag to indicate if we truncated output

        // Retrieve file contents in the original order
        fileLoop@ for (i in futures.indices) {
            val future = futures.get(i)
            var text: String?
            try {
                text = future.get() // get content from thread
            } catch (e: Exception) {
                System.err.println("Error reading file: " + e.message)
                continue  // Skip this file on error
            }
            if (text == null) text = ""
            // If limit is set and adding this text would exceed it, truncate and break
            if (maxTokens > 0) {
                val currentTokens = countTokens(combined.toString())
                val newTokens = countTokens(text)
                if (currentTokens + newTokens > maxTokens) {
                    // Calculate how many tokens we can still add
                    val allowedTokens = maxTokens - currentTokens
                    if (allowedTokens > 0) {
                        // Append only the allowed portion of text (token-wise)
                        val words: Array<String?> =
                            text.split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                        val partial = StringBuilder()
                        var w = 0
                        while (w < allowedTokens && w < words.size) {
                            partial.append(words[w])
                            if (w < allowedTokens - 1) {
                                partial.append(" ")
                            }
                            w++
                        }
                        combined.append(partial)
                    }
                    truncated = true
                    break@fileLoop
                }
            }
            // Append the full text from this file
            combined.append(text)
            // Optionally, add a separator newline between file contents
            if (i < futures.size - 1) {
                combined.append(System.lineSeparator())
            }
        }

        // Shutdown the executor service
        executor.shutdown()

        // If content was truncated due to limit, add a notation at the end
        if (truncated) {
            combined.append(System.lineSeparator()).append("[...] (truncated due to size limit)")
        }

        return combined.toString()
    }

    /**
     * Recursively expands directories into a list of files.
     * @param files List of files and directories.
     * @return List containing all files (no directories).
     */
    private fun expandFiles(files: MutableList<File>): MutableList<File> {
        val resultList: MutableList<File> = ArrayList<File>()
        for (file in files) {
            if (file.isDirectory()) {
                // Walk through directory to gather regular files
                try {
                    Files.walk(file.toPath()).use { stream ->
                        stream.filter { path: Path? -> Files.isRegularFile(path) }
                            .forEach { path: Path? -> resultList.add(path!!.toFile()) }
                    }
                } catch (e: IOException) {
                    System.err.println("Unable to read directory " + file.getName() + ": " + e.message)
                }
            } else {
                resultList.add(file)
            }
        }
        return resultList
    }

    /**
     * Reads the entire content of a single file as a UTF-8 string.
     * @param file File to read.
     * @return File content as a string (or empty string if an error occurs).
     */
    private fun readFileContent(file: File): String {
        try {
            // Use NIO Files to read all bytes and convert to string (UTF-8)
            val bytes = Files.readAllBytes(file.toPath())
            return String(bytes, StandardCharsets.UTF_8)
        } catch (e: IOException) {
            // Log error and return empty on failure
            System.err.println("Could not read file " + file.getName() + ": " + e.message)
            return ""
        }
    }

    /**
     * Copies the given text to the system clipboard, with special handling for WSL.
     * @param text The text to copy.
     * @throws IOException if the clipboard operation fails.
     */
    @Throws(IOException::class)
    private fun copyToClipboard(text: String) {
        // Attempt using the standard clipboard API (works on Windows and Linux with GUI)
        var text = text
        try {
            val clipboard = Toolkit.getDefaultToolkit().getSystemClipboard()
            clipboard.setContents(StringSelection(text), null)
            return  // success
        } catch (e: IllegalStateException) {
            // If in a headless environment (e.g., WSL without GUI), proceed to fallback
        } catch (e: HeadlessException) {
        }

        // Fallback method: use OS-specific command-line utilities
        val os = System.getProperty("os.name").lowercase(Locale.getDefault())
        val isWsl = os.contains("linux") && System.getenv("WSL_DISTRO_NAME") != null
        if (os.contains("windows") || isWsl) {
            // On Windows (or WSL), use clip.exe. Ensure newline at end to avoid WSL bug.
            if (!text.endsWith("\n")) {
                text = text + "\n"
            }
            val p = Runtime.getRuntime().exec("cmd /c clip")
            p.getOutputStream().use { outStream ->
                outStream.write(text.toByteArray(StandardCharsets.UTF_8))
            }
        } else if (os.contains("mac")) {
            // On macOS, use pbcopy
            val p = Runtime.getRuntime().exec("pbcopy")
            p.getOutputStream().use { outStream ->
                outStream.write(text.toByteArray(StandardCharsets.UTF_8))
            }
        } else {
            // On Linux (desktop), use xclip to copy to clipboard
            val p = Runtime.getRuntime().exec("xclip -selection clipboard")
            p.getOutputStream().use { outStream ->
                outStream.write(text.toByteArray(StandardCharsets.UTF_8))
            }
        }
        // (We assume success if no exception; in a real application, we'd check the process exit value or catch errors.)
    }

    /**
     * Counts tokens (words) in the given text.
     * @param text The text to evaluate.
     * @return Number of tokens (split by whitespace).
     */
    private fun countTokens(text: String?): Int {
        if (text == null || text.isEmpty()) return 0
        val tokens: Array<String?> = text.split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        return tokens.size
    }

    /**
     * Displays an error message to the user or console.
     * @param message The message to display.
     */
    private fun showError(message: String?) {
        // This could be a GUI dialog in a full application; here we just log to console.
        System.err.println("ERROR: " + message)
    }
}