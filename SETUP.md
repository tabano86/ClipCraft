# ClipCraft - Complete Setup, Test, and Deployment Guide

> **Exhaustive guide for building, testing, and deploying ClipCraft plugin.**
> If you can't repeat it from this doc, it's not documented well enough.

## Table of Contents
- [Prerequisites](#prerequisites)
- [Initial Setup](#initial-setup)
- [Build Instructions](#build-instructions)
- [Testing Strategy](#testing-strategy)
- [Manual Testing Checklist](#manual-testing-checklist)
- [Deployment](#deployment)
- [Troubleshooting](#troubleshooting)

---

## Prerequisites

### Required Software
1. **JDK 17** (Exactly version 17 - not 16, not 18)
   ```bash
   # Verify installation
   java -version
   # Should output: openjdk version "17.x.x"
   ```

   **Download:** https://adoptium.net/temurin/releases/?version=17

2. **IntelliJ IDEA 2023.3+** (Community or Ultimate)
   - Download: https://www.jetbrains.com/idea/download/

3. **Git** (for version control)
   ```bash
   git --version
   # Should output: git version 2.x.x
   ```

### Environment Setup

1. **Set JAVA_HOME**

   **Windows:**
   ```cmd
   setx JAVA_HOME "C:\Program Files\Eclipse Adoptium\jdk-17.x.x"
   setx PATH "%JAVA_HOME%\bin;%PATH%"
   ```

   **macOS/Linux:**
   ```bash
   export JAVA_HOME=/path/to/jdk-17
   export PATH=$JAVA_HOME/bin:$PATH
   ```

2. **Configure Gradle (if not using wrapper)**
   - The project uses Gradle Wrapper (`gradlew`), so no manual Gradle installation needed
   - Gradle will auto-download on first build

---

## Initial Setup

### Step 1: Clone Repository
```bash
git clone https://github.com/tabano86/ClipCraft.git
cd clipcraft
```

### Step 2: Verify Gradle Wrapper
```bash
# Windows
gradlew.bat --version

# macOS/Linux
./gradlew --version
```

Should output:
```
Gradle 8.14
Kotlin: 1.9.23
Groovy: 3.0.x
```

### Step 3: Open in IntelliJ IDEA

1. **Open IntelliJ IDEA**
2. **File → Open**
3. Select the `clipcraft` directory
4. Wait for Gradle to sync (watch bottom right status bar)
5. **Trust the project** when prompted

### Step 4: Configure IntelliJ Plugin DevKit

1. **File → Project Structure → Project**
   - SDK: JDK 17
   - Language Level: 17

2. **File → Project Structure → Modules**
   - Verify `clipcraft.main` module exists
   - Verify dependencies are loaded

3. **File → Settings → Build, Execution, Deployment → Build Tools → Gradle**
   - Build and run using: **Gradle**
   - Run tests using: **Gradle**
   - Gradle JVM: **Project SDK (17)**

---

## Build Instructions

### Standard Build
```bash
# Windows
gradlew.bat build

# macOS/Linux
./gradlew build
```

**What happens:**
1. Downloads dependencies (first time only)
2. Compiles Kotlin code
3. Runs code quality checks (detekt, spotless)
4. Runs tests
5. Packages plugin JAR
6. Runs plugin verifier

**Expected Output:**
```
BUILD SUCCESSFUL in 2m 15s
45 actionable tasks: 45 executed
```

**Artifacts Created:**
- `build/distributions/clipcraft-x.x.x.zip` - Plugin distribution
- `build/libs/clipcraft-x.x.x.jar` - Plugin JAR

### Quick Build (Skip Tests)
```bash
./gradlew build -x test
```

### Clean Build
```bash
./gradlew clean build
```

### Build for Specific IDE Version
```bash
./gradlew build -PideaVersion=2024.1
```

---

## Testing Strategy

### 1. Automated Unit Tests

**Run all tests:**
```bash
./gradlew test
```

**Run specific test class:**
```bash
./gradlew test --tests "com.clipcraft.services.professional.ProfessionalTokenEstimatorTest"
```

**Run with coverage:**
```bash
./gradlew test jacocoTestReport
```

**View coverage report:**
- Open `build/reports/jacoco/test/html/index.html` in browser

**Test Structure:**
```
src/test/kotlin/
├── com/clipcraft/
│   ├── services/
│   │   ├── professional/
│   │   │   ├── ProfessionalTokenEstimatorTest.kt
│   │   │   ├── ProfessionalGitServiceTest.kt
│   │   │   └── ProfessionalFormatterTest.kt
│   ├── action/
│   │   └── ClipCraftActionTest.kt
│   └── integration/
│       └── FullWorkflowTest.kt
```

### 2. Plugin Verification

**Verify plugin compatibility:**
```bash
./gradlew runPluginVerifier
```

**What it checks:**
- API compatibility with target IDE versions
- Deprecated API usage
- Internal API usage
- Binary compatibility

**Expected Output:**
```
Verification reports: build/reports/pluginVerifier/
```

### 3. Code Quality Checks

**Run detekt (static analysis):**
```bash
./gradlew detekt
```

**Run spotless (code formatting):**
```bash
./gradlew spotlessCheck
```

**Auto-fix formatting:**
```bash
./gradlew spotlessApply
```

---

## Manual Testing Checklist

### Test Environment Setup

1. **Install plugin in test IDE:**
   ```bash
   ./gradlew runIde
   ```
   This launches a separate IDE instance with the plugin installed.

2. **Create test projects:**
   - Small project (< 10 files)
   - Medium project (10-100 files)
   - Large project (100+ files)
   - Multi-language project (Kotlin, Java, Python, JS)

### Core Functionality Tests

#### ✅ Test 1: Basic Export
- [ ] Right-click single file → ClipCraft → Copy as Markdown
- [ ] Verify content in clipboard (Ctrl+V in text editor)
- [ ] Check notification appears with file count
- [ ] Verify token count shown in notification

#### ✅ Test 2: Multi-File Export
- [ ] Select 5 files in Project view
- [ ] Right-click → ClipCraft → Copy as Markdown
- [ ] Verify all 5 files in clipboard
- [ ] Check files are properly separated
- [ ] Verify metadata is included

#### ✅ Test 3: Directory Export
- [ ] Right-click folder with 10+ files
- [ ] ClipCraft → Copy as Markdown
- [ ] Verify recursive file collection
- [ ] Check filters are applied
- [ ] Verify excluded files are skipped

#### ✅ Test 4: Quick Export Actions
- [ ] Open any file in editor
- [ ] Right-click → ClipCraft → Quick Export Current File
- [ ] Verify immediate clipboard copy
- [ ] Test Tools → ClipCraft → Quick Export Project
- [ ] Verify entire project exported

#### ✅ Test 5: Export with Presets
- [ ] Select files → ClipCraft → Export with Preset
- [ ] Test each preset:
  - [ ] AI/LLM Context
  - [ ] Documentation
  - [ ] Code Review
  - [ ] Full Project
  - [ ] Test Files
- [ ] Verify preset filters work correctly

#### ✅ Test 6: Export to File
- [ ] Select files → ClipCraft → Export to File
- [ ] Choose Markdown format
- [ ] Save to disk
- [ ] Open saved file, verify content
- [ ] Repeat for XML, JSON, HTML formats

### UI/UX Tests

#### ✅ Test 7: Settings Panel
- [ ] File → Settings → Tools → ClipCraft Settings
- [ ] Verify all options visible
- [ ] Change include globs, click Apply
- [ ] Export file, verify new filters applied
- [ ] Test each output format option
- [ ] Test chunking options
- [ ] Test security options

#### ✅ Test 8: Theme Compliance
- [ ] **Light Theme:**
  - [ ] Settings → Appearance → Theme → IntelliJ Light
  - [ ] Open ClipCraft settings
  - [ ] Verify readable text, proper contrast
  - [ ] Export file, check notification colors
- [ ] **Dark Theme:**
  - [ ] Settings → Appearance → Theme → Darcula
  - [ ] Repeat all visual checks
  - [ ] Verify gradient colors look good
- [ ] **High Contrast:**
  - [ ] Settings → Appearance → Theme → High Contrast
  - [ ] Verify all UI elements clearly visible
  - [ ] Check icon visibility

#### ✅ Test 9: Copy Functionality
- [ ] Run export that shows preview dialog
- [ ] Click anywhere in preview area
- [ ] Verify immediate clipboard copy
- [ ] Check "Copied!" feedback appears
- [ ] Click copy button explicitly
- [ ] Verify both methods work

### Performance Tests

#### ✅ Test 10: Large Project Performance
- [ ] Open project with 500+ files
- [ ] Right-click root → ClipCraft → Copy as Markdown
- [ ] Verify progress indicator appears
- [ ] Time the operation (should be < 10 seconds)
- [ ] Check IDE remains responsive
- [ ] Verify no lag after completion

#### ✅ Test 11: Chunking Performance
- [ ] Enable chunking in settings (max 50K tokens)
- [ ] Export large project (> 50K tokens)
- [ ] Verify automatic chunking triggers
- [ ] Check chunks are properly separated
- [ ] Verify token counts per chunk

#### ✅ Test 12: Responsive UI
- [ ] Test on 1080p display
- [ ] Test on 4K display
- [ ] Test on ultrawide monitor
- [ ] Resize settings panel
- [ ] Verify no layout breaks
- [ ] Check all buttons remain clickable

### Security Tests

#### ✅ Test 13: Secret Detection
- [ ] Create file with API key: `API_KEY=sk_test_1234567890abcdef`
- [ ] Export file with secret detection enabled
- [ ] Verify warning appears
- [ ] Check secret is masked in output
- [ ] Test with real-looking AWS key, GitHub token

#### ✅ Test 14: PII Detection
- [ ] Create file with email, phone number
- [ ] Export with PII detection
- [ ] Verify warnings shown
- [ ] Check tooltip/notification details

### Integration Tests

#### ✅ Test 15: Git Integration
- [ ] Open project with Git repository
- [ ] Enable "Include Git Info" in settings
- [ ] Export files
- [ ] Verify Git metadata in output:
  - [ ] Current branch
  - [ ] Commit hash
  - [ ] Author name
  - [ ] Commit message

#### ✅ Test 16: .gitignore Respect
- [ ] Create .gitignore with `*.log`
- [ ] Create test.log file
- [ ] Enable "Respect .gitignore" in settings
- [ ] Export directory
- [ ] Verify test.log is excluded

#### ✅ Test 17: Token Estimation Accuracy
- [ ] Export known-size file
- [ ] Use external tokenizer (https://platform.openai.com/tokenizer)
- [ ] Compare ClipCraft estimate with actual
- [ ] Verify within 5% accuracy

### Cross-Platform Tests

#### ✅ Test 18: Windows
- [ ] Install on Windows 10/11
- [ ] Run all core functionality tests
- [ ] Check path separators (\ vs /)
- [ ] Verify file chooser works

#### ✅ Test 19: macOS
- [ ] Install on macOS (Intel)
- [ ] Install on macOS (Apple Silicon)
- [ ] Run all core functionality tests
- [ ] Check keyboard shortcuts (Cmd vs Ctrl)

#### ✅ Test 20: Linux
- [ ] Install on Ubuntu/Fedora
- [ ] Run all core functionality tests
- [ ] Check file permissions
- [ ] Verify GTK theme compatibility

---

## Deployment

### Prerequisites
1. **JetBrains Marketplace Account**
   - Sign up: https://plugins.jetbrains.com/
   - Generate API token: https://plugins.jetbrains.com/author/me/tokens

2. **Configure Token**
   ```bash
   # Add to ~/.gradle/gradle.properties
   intellijPlatformPublishingToken=perm:YOUR_TOKEN_HERE
   ```

### Step 1: Prepare Release

1. **Update version in build.gradle.kts:**
   ```kotlin
   version = "1.0.0" // Update this
   ```

2. **Update CHANGELOG.md:**
   ```markdown
   ## [1.0.0] - 2025-01-15
   ### Added
   - Feature X
   - Feature Y
   ### Fixed
   - Bug Z
   ```

3. **Update plugin.xml description if needed**

### Step 2: Build Distribution

```bash
# Clean build
./gradlew clean

# Build plugin
./gradlew buildPlugin

# Verify plugin
./gradlew runPluginVerifier
```

**Artifacts:**
- `build/distributions/clipcraft-1.0.0.zip`

### Step 3: Test Distribution Locally

```bash
# Install in IDE
./gradlew runIde
```

Run through critical tests from manual checklist.

### Step 4: Publish to Marketplace

**Option A: Automated (Recommended)**
```bash
./gradlew publishPlugin
```

**Option B: Manual Upload**
1. Go to https://plugins.jetbrains.com/plugin/add
2. Upload `build/distributions/clipcraft-1.0.0.zip`
3. Fill in release notes
4. Submit for review

### Step 5: Create GitHub Release

```bash
# Tag release
git tag -a v1.0.0 -m "Release version 1.0.0"
git push origin v1.0.0

# Create release on GitHub
# Attach clipcraft-1.0.0.zip to release
```

### Step 6: Monitor

- Check JetBrains approval (usually 1-3 days)
- Monitor for crash reports
- Watch download statistics
- Respond to user feedback

---

## Troubleshooting

### Build Fails: "Cannot find Java 17"

**Solution:**
```bash
# Set JAVA_HOME explicitly for Gradle
# Add to gradle.properties:
org.gradle.java.home=/path/to/jdk-17
```

### Build Fails: "Toolchain not found"

**Solution:**
```bash
# Let Gradle auto-provision JDK
# Add to gradle.properties:
org.gradle.java.installations.auto-detect=true
org.gradle.java.installations.auto-download=true
```

### Test Fails: "ClassNotFoundException"

**Solution:**
```bash
# Clean and rebuild
./gradlew clean test --refresh-dependencies
```

### Plugin Doesn't Appear in IDE

**Solution:**
1. Check IDE version compatibility (2023.3+)
2. Verify plugin is enabled: Settings → Plugins
3. Restart IDE
4. Check IDE logs: Help → Show Log in Explorer

### Export Hangs on Large Project

**Solution:**
1. Enable chunking in settings
2. Reduce max file size limit
3. Add more exclusion patterns
4. Check IDE logs for errors

### Icons Not Showing

**Solution:**
1. Verify SVG icons in resources
2. Check FlatLaf library is loaded
3. Restart IDE after plugin update

### Theme Colors Look Wrong

**Solution:**
1. Verify FlatLaf initialization
2. Check JBColor usage (not hardcoded colors)
3. Test with multiple themes
4. Check CSS in HTML output

### Git Info Not Showing

**Solution:**
1. Verify project has .git directory
2. Enable "Include Git Info" in settings
3. Check JGit library is loaded
4. Verify repository is not corrupted

### Token Count Inaccurate

**Solution:**
1. Verify tiktoken library (jtokkit) is loaded
2. Check encoding type (CL100K_BASE for GPT-4)
3. Compare with OpenAI's tokenizer
4. Report discrepancies with example

---

## Performance Benchmarks

### Expected Performance
- **Small project** (< 10 files): < 1 second
- **Medium project** (10-100 files): < 5 seconds
- **Large project** (100-1000 files): < 10 seconds
- **Huge project** (1000+ files): < 30 seconds

### Memory Usage
- **Base memory**: ~50 MB
- **With large export**: ~200 MB
- **Peak memory**: < 500 MB

### UI Responsiveness
- **Settings open**: < 100ms
- **Action menu**: < 50ms
- **Preview dialog**: < 200ms
- **Copy to clipboard**: < 10ms

---

## Continuous Integration

### GitHub Actions Setup

Create `.github/workflows/build.yml`:
```yaml
name: Build and Test

on: [push, pull_request]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
      - name: Build with Gradle
        run: ./gradlew build
      - name: Run Tests
        run: ./gradlew test
      - name: Upload Artifact
        uses: actions/upload-artifact@v3
        with:
          name: plugin-distribution
          path: build/distributions/*.zip
```

---

## Support

- **Issues**: https://github.com/tabano86/ClipCraft/issues
- **Discussions**: https://github.com/tabano86/ClipCraft/discussions
- **Email**: anthony.tabano.dev@gmail.com

---

**Last Updated:** 2025-01-15
**Document Version:** 1.0.0
