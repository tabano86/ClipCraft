# Configuration

## Settings Page

`File > Settings > Tools > ClipCraft` (or the “Open ClipCraft Settings” action) opens a page with:

- **Concurrency**: Disabled, Thread Pool, or Coroutines.
- **Chunk Strategy**: None, By Size, By Methods.
- **Ignore Patterns**: `.gitignore`, custom patterns, folder matching, etc.
- **Metadata**: Include file size, last modified date, or Git info.
- **Theme**: Light or Dark.
- **Line Numbering**: Optionally prepend line numbers.

## Profiles

ClipCraft supports named “profiles,” each containing a unique set of options. Switch profiles quickly from the context menu or the settings page. This is useful if you have different formatting/ignore needs for various projects or tasks.

## Integration with GPT

- A simple GPT chat tool window is provided.
- `ClipCraftGPTAction` stubs out sending your code to GPT.
- If you want real GPT integration, add the relevant library or API credentials in `ClipCraftSharingService`.

## Code Quality

The project includes:

- **Spotless** for code formatting (Kotlin, Java, YAML, plus “misc” text).
- **Detekt** for static analysis.
- **Axion Release Plugin** for version bumps based on commit messages.
- **Gradle Pre-Commit Git Hooks** to enforce Conventional Commits.

Adjust or remove any of these if they’re unnecessary.
