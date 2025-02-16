# ClipCraft

ClipCraft is an IntelliJ plugin that lets you copy, format, and organize code snippets quickly—across multiple files or
directories. It supports concurrency (thread pools or coroutines), GPT integration stubs, a snippet queue, chunking,
ignoring files/folders (including `.gitignore`), and a live preview in settings.

<p style="display: flex; justify-content: center;">
  <img src="src/main/resources/icons/clipcraft_32.svg" alt="ClipCraft Logo" width="150"/>
</p>

## Features

- **Concurrency Modes**
  Choose between disabled, thread pool, or Kotlin coroutines to process files in parallel.

- **Chunking & Overlap**
  Split code by size or methods. Ideal for GPT-based flows, large code generation, or partial refactors.

- **Directory & File Ignores**
  Automatic `.gitignore` usage, plus custom ignore patterns or folder-level ignoring.

- **GPT Integration (Stubs)**
  Send snippet text plus a prompt to a GPT-based service (mocked for demonstration). Great for code explanation or quick
  generation.

- **User-Friendly UI**
  A dedicated settings page with chunk/overlap preview, plus a snippet queue window, wizard for initial setup, and
  multiple actions in the context menus.

- **Additional Goodies**
  Pre-commit Git hooks (for Conventional Commits) set up by default, code style checks, Detekt for static analysis, and
  Spotless for code formatting.

## Quick Start

1. **Install the Plugin**
    - Build the plugin using Gradle, or download the latest release from JetBrains Marketplace (if available).

2. **Open Settings**
    - Go to `Settings > Tools > ClipCraft`.
    - Configure concurrency, chunking, ignore patterns, etc.

3. **Use ClipCraft Actions**
    - Right-click in the Project panel or Editor, then select **ClipCraft Copy** or **ClipCraft Submenu** actions.
    - Or use the dedicated snippet queue or GPT chat tool window.

## Repository Layout

- **`settings.gradle.kts`**
  Applies the Gradle pre-commit Git hooks plugin and sets up Conventional Commits.
- **`build.gradle.kts`**
  Configures dependencies, Axion release versioning, IntelliJ plugin details, Detekt, Spotless, and more.
- **`src/`**
  Core plugin code, including:
    - **`actions`**: IntelliJ actions (e.g., `ClipCraftAction` for copying snippets, `ClipCraftGPTAction`, etc.).
    - **`model`**: Data classes like `Snippet`, `ClipCraftOptions`, concurrency modes, etc.
    - **`services`**: Project/application-level services (profile manager, snippet queue, GPT stubs).
    - **`ui`**: UI components for setup wizard, snippet queue panel, settings, etc.
    - **`integration`**: Git or search integration stubs.
    - **`util`**: Utility classes (e.g. `CodeFormatter`, ignore logic).

## Documentation

- [Overview](./docs/Overview.md)
- [Usage & Actions](./docs/Usage.md)
- [Configuration](./docs/Configuration.md)
- [FAQ](./docs/FAQ.md)

## Contributing

We welcome pull requests and suggestions. Please see [Contributing.md](./docs/Contributing.md) for details on how to
contribute, run tests, or share ideas.

## License

Apache 2.0. See [LICENSE](./LICENSE) for details.