# Usage & Actions

## Basic Usage

1. **Select File(s) or Directory**  
   Right-click on one or more files/folders in the Project or Editor context menus.

2. **Invoke ClipCraft Copy**
    - Use "ClipCraft Copy" to gather and format the selected items.
    - Alternatively, open the "ClipCraft Submenu" for more specialized actions.

3. **Snippet Queue**
    - The snippet queue tool window displays saved snippets (via `ClipCraftAddSnippetFromCursorOrSelectionAction` or others).
    - Combine, reorder, or clear snippets on the fly.

## Actions

- **ClipCraftAction**: The main action that copies selected code into a single output, chunked or concurrency-based.
- **ClipCraftGPTAction**: Combines snippet content with a prompt for GPT usage.
- **ClipCraftAddSnippetFromCursorOrSelectionAction**: Adds code at the caret or selection to the snippet queue.
- **ClipCraftResetDefaultsAction**: Resets plugin settings to defaults.
- **ClipCraftSwitchProfileAction**: Switch among named configuration profiles.
- **ClipCraftWizardAction**: Opens a wizard that guides you through initial configuration steps.

## Tool Windows

- **ClipCraft.SnippetQueue**: Manage snippets in a dedicated window.
- **GPT Chat**: A placeholder GPT interface for demonstration.

## Configuration

Check [Configuration](./Configuration.md) for advanced details.
