# ClipCraft

**ClipCraft** is an IntelliJ Platform plugin that makes it easy to bulk-copy and format code from multiple files and directories—including nested ones—with just a few clicks. The plugin is optimized for performance, can handle large files in the background, and saves your preferences for a hassle-free experience.

## Features

- **Bulk Copy & Formatting**  
  Process multiple files or entire directories at once. Automatically format code by adding headers, optional line numbers, and language hints.

- **File Metadata**  
  Optionally include details such as file size and last modified date with the copied code.

- **Large File Support**  
  Files exceeding a user-defined size threshold load in the background with a dedicated progress indicator to ensure the IDE remains responsive.

- **Persistent Settings**  
  Configure your preferences in the IDE settings page once and have them persist for future sessions. Options include:
    - Enabling/disabling line numbers.
    - Choosing between a preview or immediate copy.
    - Defining an export file path.
    - Enabling automatic processing without prompts.
    - Setting the large file size threshold.

- **Multiple Access Points**  
  Activate ClipCraft from the editor context menu, the project view, or the main toolbar. A default keyboard shortcut (Ctrl+Alt+X) is also provided for quick access.

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

This project is set up for continuous integration with GitHub Actions. The workflow builds the plugin and can be extended to publish it to the JetBrains Plugin Repository. When publishing:
- **Channel:** Publish your first stable release to the stable channel.
- **Distribution:** Upload the generated ZIP file from the build process along with required metadata such as license, tags (e.g., IntelliJ Plugin, Code Formatter), and a channel designation.

## License

ClipCraft is licensed under the Apache License 2.0. Please refer to the `LICENSE` file for full details.

## Contributing

Contributions are welcome! To contribute:
1. Fork the repository.
2. Create a new feature branch.
3. Submit a pull request with your changes.

## Support

If you have any questions or encounter any issues, please reach out via our support channel at [tabano86@gmail.com](mailto:tabano86@gmail.com).

Enjoy using ClipCraft for efficient and effortless code copying and formatting in IntelliJ IDEA!