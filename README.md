# ClipCraft

**ClipCraft** is a powerful and streamlined plugin designed for developers who need to quickly gather, format, and share
code. With ClipCraft, you can bulk-copy code from multiple files and directories—including all nested subdirectories—and
automatically convert the output into clean, ready-to-use Markdown format. This makes it easy to paste code snippets
into chat apps, documentation, or any Markdown-supported platform.

## Features

- **Bulk Processing & Markdown Formatting:**\
  Select an entire folder or multiple files, and ClipCraft will traverse through all subdirectories to extract code. It
  then generates a Markdown-formatted output where each file’s contents are wrapped in proper code fences, with optional
  language hints for syntax highlighting and optional line numbering.

- **Optimized for Large Files:**\
  For files exceeding a user-defined threshold, ClipCraft handles file reading in the background—keeping your IDE
  responsive even when processing very large files.

- **Customizable Output Options:**\
  Configure whether to merge all output into a single Markdown code block or keep separate blocks for each file. You can
  also choose to minimize extra blank lines and include file metadata (e.g., file size, last modified date) in the
  header of each snippet.

- **Persistent User Preferences:**\
  Your settings are saved between sessions, so whether you prefer always having line numbers, automatically exporting
  the output to a file, or displaying a preview before copying, your preferences are remembered.

- **Minimal Clicks, Maximum Efficiency:**\
  Designed for simplicity, ClipCraft integrates seamlessly into IntelliJ IDEA. Access it via context menus in the Editor
  and Project View, through a main toolbar button, or simply by using the keyboard shortcut (Ctrl+Alt+X) to trigger the
  action.

- **Language-Aware Snippets:**\
  ClipCraft automatically detects the programming language based on file extension (e.g., Java, Kotlin, Python,
  JavaScript, etc.) and applies the appropriate language hint in the Markdown code fences. This ensures that your shared
  code is formatted with proper syntax highlighting wherever Markdown is supported.

## Installation

1. **Build the Plugin**  
   Open your project root and run the following command:
   ```bash
   ./gradlew clean buildPlugin
   ```
   This command generates a ZIP file (typically found in the `build/distributions` directory) that contains your plugin.

2. **Install in IntelliJ IDEA**
    - Open IntelliJ IDEA.
    - Navigate to **File → Settings** (or **Preferences** on macOS) → **Plugins**.
    - Click the gear icon and select **Install Plugin from Disk…**.
    - Locate and select the generated `ClipCraft.zip` file.
    - Restart the IDE when prompted.

## Configuration

Once the plugin is installed, you can customize its behavior by navigating to:

**File → Settings → ClipCraft**

Here you can adjust the following options:

- **Include Line Numbers:** Toggle to add line numbers in the formatted output.
- **Show Preview:** Enable to display a preview before copying or exporting.
- **Export to File:** Set a file path if you prefer saving the output to a file instead of copying it to the clipboard.
- **Auto Process:** Enable to run ClipCraft automatically without prompting.
- **Large File Threshold:** Define the file size (in bytes) at which ClipCraft uses a background loader.

## Usage

1. **Selecting Files/Directories:**  
   Open a project and select one or more files or folders.

2. **Triggering ClipCraft:**
    - Right-click in the editor or project view and choose **ClipCraft: Copy Formatted Code**; or
    - Use the keyboard shortcut **Ctrl+Alt+X** (the default shortcut).

3. **Output Options:**  
   Depending on your configuration, ClipCraft will:
    - Copy the formatted code to the clipboard,
    - Export the formatted output to a specified file, or
    - Display a preview dialog for you to review the code.

## CI/CD and Publishing

This project is set up for continuous integration with GitHub Actions. The workflow builds the plugin and can be
extended to publish it to the JetBrains Plugin Repository. When publishing:

- **Channel:** Publish your first stable release to the stable channel.
- **Distribution:** Upload the generated ZIP file from the build process along with required metadata such as license,
  tags (e.g., IntelliJ Plugin, Code Formatter), and a channel designation.

## License

ClipCraft is licensed under the Apache License 2.0. Please refer to the `LICENSE` file for full details.

## Contributing

Contributions are welcome! To contribute:

1. Fork the repository.
2. Create a new feature branch.
3. Submit a pull request with your changes.

## Support

If you have any questions or encounter any issues, please reach out via our support channel
at [tabano86@gmail.com](mailto:tabano86@gmail.com).

Enjoy using ClipCraft for efficient and effortless code copying and formatting in IntelliJ IDEA!