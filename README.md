# ClipCraft - Professional Code Export for AI & Documentation

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![IntelliJ Plugin](https://img.shields.io/badge/IntelliJ-Plugin-orange.svg)](https://plugins.jetbrains.com/plugin/clipcraft)

> The ultimate IntelliJ IDEA plugin for exporting code to AI assistants, documentation, and team collaboration.

## 🚀 Overview

ClipCraft is a powerful IntelliJ IDEA plugin that transforms how you share code with AI assistants, create documentation, and collaborate with your team. Export your codebase in multiple formats with intelligent filtering, token estimation, and AI-optimized formatting.

## ✨ Key Features

### 🎯 Multiple Export Formats
- **Markdown** - Standard GitHub-flavored markdown with code fencing
- **Markdown with TOC** - Includes auto-generated table of contents
- **XML** - Structured XML output with full metadata
- **JSON** - Machine-readable JSON format
- **HTML** - Formatted HTML with syntax styling
- **Plain Text** - Simple text format for universal compatibility

### 🤖 AI-Optimized Formats
- **Claude-Optimized** - Tailored for Anthropic's Claude with XML-style file tags
- **ChatGPT-Optimized** - Formatted for OpenAI's GPT models
- **Gemini-Optimized** - Optimized for Google's Gemini AI

### 📊 Token Estimation
- Real-time token count estimation for AI context windows
- Context window compatibility checker (GPT-3.5, GPT-4, Claude 2/3, etc.)
- Byte size and line count statistics
- Helps you stay within AI model limits

### 🎛️ Smart Presets
Pre-configured export templates for common use cases:
- **AI/LLM Context** - Optimized for AI assistants with token limits
- **Documentation** - Export documentation and markdown files
- **Code Review** - Code files only, excluding tests
- **Full Project** - Complete project structure
- **Test Files** - Test files and test directories only

### 🔧 Advanced Filtering
- **Glob Patterns** - Include/exclude files using powerful glob syntax
- **Regex Filtering** - Advanced pattern matching
- **.gitignore Support** - Respect your repository's ignore rules
- **File Size Limits** - Min/max file size filtering
- **Date Filtering** - Include only recently modified files
- **Line Count Filtering** - Filter by code length

### 🔒 Security & Privacy
- **Secret Detection** - Automatically detect API keys, tokens, passwords
- **Secret Masking** - Auto-mask detected secrets with asterisks
- **PII Detection** - Warn about personally identifiable information
- **Sensitive Data Warnings** - Get notified before exporting sensitive content

### 📦 Smart Chunking
- **Automatic Chunking** - Split large exports for AI context windows
- **Multiple Strategies**:
  - By Size - Split when token limit reached
  - By File Count - Fixed number of files per chunk
  - By Directory - Group by folder structure
  - By File Type - Group by language/extension
  - Smart - Intelligently group related files

### 🎨 Rich Metadata
- **Git Information** - Branch, commit hash, author, commit message
- **Timestamps** - Export date and time
- **File Statistics** - Total files, bytes, tokens, lines
- **Project Info** - Project name and structure
- **Table of Contents** - Auto-generated navigation

### ⚙️ Customization Options
- **Line Numbers** - Include line numbers in code blocks
- **Comment Stripping** - Remove comments from code
- **Whitespace Normalization** - Clean up formatting
- **Path Formatting** - Relative, absolute, or custom paths
- **File Grouping** - Group by directory structure
- **Sort Options** - Multiple sorting strategies

### ⚡ Quick Actions
- **Quick Export Current File** - One-click export of active file
- **Quick Export Project** - Export entire project with AI preset
- **Export with Preset** - Choose from predefined templates
- **Export to File** - Save to disk instead of clipboard
- **Copy to Clipboard** - Quick clipboard copy

## 📋 Usage

### Basic Usage

1. **Right-click on files/folders** in Project view
2. Select **ClipCraft** from context menu
3. Choose your export option:
   - **Copy as Markdown** - Standard clipboard export
   - **Quick Export Current File** - Fast single-file export
   - **Quick Export Project** - Full project export
   - **Export with Preset...** - Choose a preset template
   - **Export to File...** - Save to file

### Quick Export Project

From **Tools → ClipCraft → Quick Export Project** for instant full project export.

### Settings Configuration

Go to **Settings → Tools → ClipCraft Settings** to configure:

#### File Filtering
```
Include patterns:
**/*.kt
**/*.java
**/*.py
**/*.js
**/*.ts

Exclude patterns:
**/build/**
**/node_modules/**
**/.git/**
```

#### Output Options
- Choose default format (Markdown, XML, JSON, etc.)
- Enable line numbers
- Strip comments
- Include metadata
- Include Git info
- Generate table of contents
- Group files by directory

#### Security & Privacy
- Enable secret detection
- Auto-mask secrets
- Respect .gitignore files

#### Chunking Settings
- Enable automatic chunking
- Set max tokens per chunk (default: 100,000)
- Choose chunking strategy

## 🎯 Use Cases

### 1. AI Assistant Context
Export your codebase for Claude, ChatGPT, or Gemini:
```
Right-click project → ClipCraft → Export with Preset → AI/LLM Context
```
- Optimized formatting for AI consumption
- Token estimation ensures you stay within limits
- Includes relevant context and metadata

### 2. Code Reviews
Share code with reviewers:
```
Select files → ClipCraft → Export with Preset → Code Review
```
- Excludes test files
- Includes line numbers
- Clean, readable format

### 3. Documentation Generation
Export documentation files:
```
Select docs folder → ClipCraft → Export with Preset → Documentation
```
- Markdown files with TOC
- Organized structure
- Cross-references preserved

### 4. Team Collaboration
Share specific code sections:
```
Select files → ClipCraft → Export to File → Choose format
```
- Multiple format options
- Share via file or clipboard
- Preserves formatting and structure

### 5. Project Analysis
Get complete project overview:
```
Tools → ClipCraft → Quick Export Project
```
- Full project structure
- Statistics and metrics
- Git information included

## 🔧 Advanced Configuration

### Custom Export Options

Create your own export configuration in Settings:

```
Include Globs:
src/**/*.{kt,java}
config/**/*.{yml,yaml}
docs/**/*.md

Exclude Globs:
**/*Test.{kt,java}
**/test/**
**/build/**

Max File Size: 2048 KB
Enable Chunking: Yes
Max Tokens: 100000
Chunk Strategy: Smart
```

### Integration with CI/CD

Export for automated documentation:

```bash
# Use IntelliJ headless mode
idea --export-clipcraft --preset=documentation --output=docs/export.md
```

## 🛠️ Development

### Building from Source

```bash
git clone https://github.com/yourorg/clipcraft.git
cd clipcraft
./gradlew build
```

### Running Tests

```bash
./gradlew test
```

### Plugin Development

The plugin is built with:
- Kotlin 1.9.23
- IntelliJ Platform SDK 2023.3+
- Gradle 8.14

## 📊 Statistics

ClipCraft provides detailed statistics for each export:

- **Files Processed** - Number of files successfully exported
- **Files Skipped** - Files excluded by filters or errors
- **Total Size** - Combined size in bytes/KB/MB
- **Estimated Tokens** - Approximate AI token count
- **Context Window Fit** - Which AI models can handle this size
- **Export Time** - Timestamp of export
- **Git Info** - Branch and commit details

## 🌟 Pro Tips

1. **Use Presets** - Save time with pre-configured templates
2. **Token Estimation** - Check token count before sending to AI
3. **Smart Chunking** - Enable for large projects to avoid context limits
4. **Secret Masking** - Always enable when sharing publicly
5. **Export to File** - Save exports for future reference
6. **Git Integration** - Enable Git info for version tracking
7. **Custom Filters** - Create project-specific include/exclude patterns
8. **Quick Actions** - Use keyboard shortcuts for frequent exports

## 🤝 Contributing

Contributions are welcome! Please read our [Contributing Guide](docs/Contributing.md) for details.

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🐛 Bug Reports & Feature Requests

Please use the [GitHub Issues](https://github.com/yourorg/clipcraft/issues) page to report bugs or request features.

## 📚 Documentation

- [Configuration Guide](docs/Configuration.md)
- [Usage Examples](docs/Usage.md)
- [FAQ](docs/FAQ.md)
- [API Documentation](docs/Overview.md)

## 🙏 Acknowledgments

- IntelliJ Platform SDK team
- Open source community
- All contributors and users

## 📈 Roadmap

### Planned Features
- [ ] Export history and search
- [ ] Incremental export (only changes)
- [ ] Custom formatting templates
- [ ] Multi-language documentation support
- [ ] IDE theme integration for HTML exports
- [ ] Export profiles with team sharing
- [ ] Code metrics and complexity analysis
- [ ] Diff export between versions
- [ ] Real-time preview dialog
- [ ] Batch export automation

## 💬 Support

- **Documentation**: [docs.clipcraft.io](https://docs.clipcraft.io)
- **Issues**: [GitHub Issues](https://github.com/yourorg/clipcraft/issues)
- **Discussions**: [GitHub Discussions](https://github.com/yourorg/clipcraft/discussions)

---

**Made with ❤️ for developers who work with AI**

*ClipCraft - Export smarter, collaborate better, build faster.*
