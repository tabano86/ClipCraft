# ClipCraft

<script src="https://plugins.jetbrains.com/assets/scripts/mp-widget.js"></script>
<script>
  // Replace "#yourelement" with the actual element ID where you want to embed the widget.
  MarketplaceWidget.setupMarketplaceWidget('install', 26483, "#yourelement");
</script>

**ClipCraft** is an IntelliJ IDEA plugin designed to streamline the process of extracting, formatting, and sharing code
snippets. Perfect for developers working with multiple files (including nested directories), it delivers a clean,
Markdown‑formatted output that’s customizable for your exact needs.

---

## Overview

ClipCraft provides:

- **Bulk Processing & Markdown Formatting:**  
  Recursively scans directories and aggregates code from several files. Each output is wrapped in language-specific code
  fences to highlight syntax accurately.

- **Optimized, Responsive Performance:**  
  Processes large files in the background, ensuring that IntelliJ IDEA remains responsive even with heavy tasks.

- **Configuration Excellence:**  
  Fine-tune output details like line numbering, file metadata inclusion, and even split long outputs into chunks for
  easier handling.

---

## Features

### Bulk Processing & Formatting

- **Recursive Scanning:**  
  Process multiple files and nested directories at once.

- **Markdown Output:**  
  Automatically wraps content in Markdown code fences with hints for languages such as Java, Kotlin, Python, and more.

### Optimized Handling of Large Files

- Files above a configurable threshold are processed in the background, so your IDE remains snappy.

### Customizable Output Options

- **Output Format:** Markdown, plain text, or HTML.
- **Blank Line Compression:** Collapse multiple blank lines.
- **File Metadata:** Optionally include file size, timestamps, and Git metadata.
- **Chunking:** Automatically split long outputs into multiple chunks based on a character count.

### Seamless IntelliJ IDEA Integration

- **UI Components:**  
  Integrated into context menus, toolbar buttons, and configurable keyboard shortcuts (default: Ctrl+Alt+X).

- **Setup Wizard:**  
  Guides you through the initial configuration.

### Persistent Preferences

- Save your formatting settings in named profiles for consistent reuse across sessions.

### Advanced Filtering & Processing Options

- **Regex Filtering & Ignore Lists:**  
  Customize which files or directories are processed.
- **Code Cleanup:**  
  Remove import statements, comments, and trailing whitespace for a streamlined snippet.
- **Next‑Gen Options:**  
  Enable GPT chunking, include a directory summary, or even compress whitespace selectively.

---

## Installation

### Building the Plugin

1. **Open Terminal:** Navigate to your project root.
2. **Build ClipCraft:**  
   Execute the following command:
   ```bash
   ./gradlew clean buildPlugin
   ```
   This produces a ZIP file (typically in `build/distributions/`) containing the plugin.

### Installing in IntelliJ IDEA

1. **Launch IntelliJ IDEA.**
2. **Open Settings:**  
   Go to **File → Settings** (or **Preferences** on macOS) and select **Plugins**.
3. **Install Plugin:**  
   Click the gear icon and choose **Install Plugin from Disk…**, then select the generated `ClipCraft.zip` file.
4. **Restart:**  
   Restart the IDE when prompted.

### Configuration

After installation, configure ClipCraft via **File → Settings → ClipCraft**. Key options include:

- **Basic Options:**
    - **Include Line Numbers:**  
      Prefix each code line with its line number for easier debugging.
    - **Show Preview:**  
      Live preview your formatted snippet before exporting.
    - **Export Options:**  
      Choose to copy output to the clipboard or write to a file (set the export file path accordingly).
    - **Include Metadata:**  
      Add extra file information (e.g., size, timestamp, Git details) to each snippet header.
    - **Output Format:**  
      Select Markdown, Plain, or HTML.
    - **Remove Import Statements:**  
      Strip out import/include declarations for a clean snippet.

- **Advanced & Next‑Gen Options:**
    - **Filter Regex & Ignore Patterns:**  
      Process only relevant files or directories.
    - **Remove Comments & Trim Whitespace:**  
      Create minimal, clean outputs.
    - **GPT Chunking & Directory Summary:**  
      Split long outputs and add a summary of the directory structure.

- **Integration Options:**
    - **Use .gitignore:**  
      Automatically exclude files listed in your project’s .gitignore.
    - **Compression Modes:**  
      Choose between None, Minimal, or Ultra compression. Ultra mode can selectively preserve important keywords (like
      “TODO”).

- **Persistent & Auto‑Apply:**
    - **Auto Process:**  
      Apply changes immediately upon toggling settings.
    - **Per-Project Config:**  
      Use project-specific settings where needed.

---

## Usage

1. **Selection:**  
   Open your project, and select one or more files/directories via the Project View or Editor.
2. **Trigger ClipCraft:**
    - Right-click and choose **ClipCraft: Copy Formatted Code**, or
    - Use the keyboard shortcut (default: **Ctrl+Alt+X**).
3. **Processing:**  
   ClipCraft processes each file based on your configuration. For files exceeding the threshold, processing occurs in
   the background.
4. **Output Options:**  
   Depending on your settings, the output is either:
    - Copied to the clipboard,
    - Written to a designated file, or
    - Displayed for preview and confirmation.

> **Tip:** Holding down the **Alt** key while triggering ClipCraft opens a quick configuration panel to override
> settings for that session.

---

## CI/CD and Publishing

ClipCraft integrates with GitHub Actions for continuous integration and automated publishing:

- **Checkout Repository:**  
  Uses the official checkout action.

- **Set Up JDK 17:**  
  Configured with the Temurin distribution.

- **Cache Gradle Files:**  
  Caches Gradle caches/wrapper files to speed up builds.

- **Build Plugin:**  
  Runs `./gradlew clean build --no-daemon` to compile the plugin.

- **Publish Plugin:**  
  On pushes to the main branch, the pipeline (using a secure token) publishes the plugin to the JetBrains Plugin
  Repository.

Relevant configuration details can be found in the `.github/workflows/publish.yml` file.

Additionally, project-specific build settings include:

- **Kotlin Compiler Arguments:**  
  Disable the coroutines Java agent:
  ```kotlin
  tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
      kotlinOptions.jvmTarget = "17"
      kotlinOptions.freeCompilerArgs += listOf("-Xdisable-coroutines-java-agent")
  }
  ```
- **Gradle JVM Arguments:**  
  Increase the heap size in `gradle.properties`:
  ```
  org.gradle.jvmargs=-Xmx4096m -Dfile.encoding=UTF-8
  ```
- **Instrumentation Disabled:**  
  Prevent code instrumentation from modifying the plugin descriptor:
  ```kotlin
  intellij {
      version.set("2022.3")
      type.set("IC")
      plugins.set(listOf("Git4Idea"))
      instrumentCode.set(false)
  }
  ```

---

## License

ClipCraft is licensed under the [Apache License 2.0](./LICENSE-2.0.txt). Please refer to the LICENSE file for complete
details.

---

## Contributing

Contributions are always welcome! To get started:

1. **Fork the Repository:**  
   Create your own copy.
2. **Create a Feature Branch:**  
   Work on your changes in an isolated branch.
3. **Submit a Pull Request:**  
   Once your changes are complete and tested, submit a pull request for review.

---

## Support

For questions, issues, or further assistance, please reach out at <tabano86@gmail.com>.

---

Happy coding with ClipCraft! Enjoy a neat and efficient workflow for managing your code snippets.