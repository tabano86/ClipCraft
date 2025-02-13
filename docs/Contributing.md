Contributing to ClipCraft
=========================

Thank you for your interest in contributing to ClipCraft! We appreciate your help in making this project better. Please review the guidelines below before submitting your contributions.

* * *

Table of Contents
-----------------
* [Code of Conduct](#code-of-conduct)
* [Business Source License 1.1](#business-source-license-11)
* [How to Contribute](#how-to-contribute)
    * [Reporting Issues](#reporting-issues)
    * [Suggesting Enhancements](#suggesting-enhancements)
    * [Submitting Pull Requests](#submitting-pull-requests)
* [Development Guidelines](#development-guidelines)
    * [Branching Model](#branching-model)
    * [Code Style](#code-style)
    * [Testing](#testing)
    * [Commit Messages](#commit-messages)
    * [Review Process](#review-process)
* [Questions and Feedback](#questions-and-feedback)

* * *

Code of Conduct
---------------
Please take a moment to read our [Code of Conduct](CODE_OF_CONDUCT.md). We strive to create a welcoming and supportive community for everyone. By contributing, you agree to abide by these guidelines.

* * *

Business Source License 1.1
---------------------------
ClipCraft is released under the Business Source License 1.1. This license governs how the source code can be used and modified. Before contributing, please review the license details in the [LICENSE](../LICENSE) file. By submitting a contribution, you agree that your changes will be licensed under the Business Source License 1.1.

* * *

How to Contribute
-----------------

### Reporting Issues
If you encounter a bug or have a feature request, please check our [issue tracker](https://github.com/clipcraft/clipcraft/issues).

### Suggesting Enhancements
For ideas or suggestions on how to improve ClipCraft, feel free to open an issue or join the discussion in our community channels. Your input is valuable and helps shape the future of the project.

### Submitting Pull Requests

1. **Fork the Repository**  
   Create your own fork of ClipCraft on GitHub.

2. **Clone Your Fork**  
   Open your terminal and run:
   ```bash
   git clone https://github.com/your-username/clipcraft.git
   ```

3. **Create a Feature Branch**  
   Navigate to your repository folder and create a new branch:
   ```bash
   git checkout -b feature/your-feature-name
   ```

4. **Make Your Changes**  
   Ensure that your code follows the project’s style guidelines and includes tests for any new functionality.

5. **Run the Tests Locally**  
   Verify your changes by running:
   ```bash
   ./gradlew test
   ```

6. **Run the Code Formatter**  
   Format your code with Spotless:
   ```bash
   ./gradlew spotlessApply
   ```

7. **Commit Your Changes**  
   Use Conventional Commits for your commit messages, for example:

    - `feat: add new snippet formatting`
    - `fix: resolve issue with file parsing`
    - `docs: update usage instructions`

8. **Push and Create a Pull Request**  
   Push your feature branch to your fork:
   ```bash
   git push origin feature/your-feature-name
   ```
   Then, open a pull request against the main branch in the upstream repository.

* * *

Development Guidelines
----------------------

### Branching Model

- **Main:**  
  The main branch contains production-ready code.

- **Feature Branches:**  
  Create separate branches for each feature or bug fix.

- **Pull Requests:**  
  All contributions should be merged via pull requests targeting the main branch.

### Code Style
- Follow Kotlin coding conventions.
- Format your code with Spotless:
  ```bash
  ./gradlew spotlessApply
  ```
- Ensure that your code adheres to the style rules defined in the project.

### Testing
- Add tests for every new feature or bug fix.
- Run tests locally with:
  ```bash
  ./gradlew test
  ```
- Follow the existing testing conventions.

### Commit Messages
We follow Conventional Commits for clear and consistent commit messages.

### Review Process
Your pull request will be reviewed by one or more maintainers. Feedback may be provided, and you might be asked to make changes before merging. We appreciate your responsiveness during this process.

* * *

Questions and Feedback
-----------------------
If you have any questions, need clarification, or would like to provide feedback, please open an issue or reach out through our community channels. Your contributions help improve ClipCraft for everyone.

Thank you for contributing to ClipCraft!