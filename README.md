# ClipCraft

<script src="https://plugins.jetbrains.com/assets/scripts/mp-widget.js"></script>
<script>
  // Replace "#yourelement" with the actual element ID where you want to embed the widget.
  MarketplaceWidget.setupMarketplaceWidget('install', 26483, "#yourelement");
</script>

**ClipCraft** is an IntelliJ IDEA plugin designed to streamline the process of extracting, formatting, and sharing code snippets. Perfect for developers working with multiple files (including nested directories), it delivers a clean, Markdown‑formatted output that’s customizable for your exact needs.

---

## Overview

ClipCraft provides:

- **Bulk Processing & Markdown Formatting:**  
  Recursively scans directories and aggregates code from several files. Each output is wrapped in language-specific code fences to highlight syntax accurately.

- **Optimized, Responsive Performance:**  
  Processes large files in the background, ensuring that IntelliJ IDEA remains responsive even with heavy tasks.

- **Configuration Excellence:**  
  Fine-tune output details like line numbering, file metadata inclusion, and even split long outputs into chunks for easier handling.

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

- **Output Format:**  
  Choose among Markdown, plain text, or HTML.
- **Blank Line Compression:**  
  Collapse multiple blank lines.
- **File Metadata:**  
  Optionally include file size, timestamps, and Git metadata.
- **Chunking:**  
  Automatically split long outputs into multiple chunks based on a character count.

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
  Enable GPT chunking, include a directory summary, or compress whitespace selectively.

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
      Optionally cleanse your snippets for a leaner output.

---

## License

ClipCraft is distributed under the **Business Source License 1.1**. Under the terms of this license, use of the software is subject to specific conditions detailed within the license text. You may obtain a copy of the Business Source License, Version 1.1 at:

    https://mariadb.com/bsl11

**Important:**  
On or after the designated Change Date, specified in the license (currently set as [YYYY-MM-DD]), the software will transition and be made available under the **Apache License, Version 2.0**. Please review the license text for further details regarding permitted usage and commercialization.

---

## Contributing

Contributions to ClipCraft are welcome! Please follow the guidelines provided in [CONTRIBUTING.md](docs/CONTRIBUTING.md) when submitting issues or pull requests.

---

## Support

For support, report issues or request features through the GitHub issues page.

---

*ClipCraft — streamlining your code snippet workflows in IntelliJ IDEA.*