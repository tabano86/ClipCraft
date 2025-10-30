package com.clipcraft.services

object SecretDetector {

    private val SECRET_PATTERNS = listOf(
        // API Keys and Tokens
        Regex("(?i)(api[_-]?key|apikey)\\s*[:=]\\s*['\"]?([a-zA-Z0-9_\\-]{20,})['\"]?"),
        Regex("(?i)(access[_-]?token|accesstoken)\\s*[:=]\\s*['\"]?([a-zA-Z0-9_\\-]{20,})['\"]?"),
        Regex("(?i)(secret[_-]?key|secretkey)\\s*[:=]\\s*['\"]?([a-zA-Z0-9_\\-]{20,})['\"]?"),
        Regex("(?i)(auth[_-]?token|authtoken)\\s*[:=]\\s*['\"]?([a-zA-Z0-9_\\-]{20,})['\"]?"),

        // AWS
        Regex("AKIA[0-9A-Z]{16}"),
        Regex("(?i)aws[_-]?secret[_-]?access[_-]?key\\s*[:=]\\s*['\"]?([a-zA-Z0-9/+=]{40})['\"]?"),

        // GitHub Tokens
        Regex("ghp_[a-zA-Z0-9]{36}"),
        Regex("gho_[a-zA-Z0-9]{36}"),
        Regex("ghu_[a-zA-Z0-9]{36}"),
        Regex("ghs_[a-zA-Z0-9]{36}"),
        Regex("ghr_[a-zA-Z0-9]{36}"),

        // Private Keys
        Regex("-----BEGIN (RSA |DSA |EC )?PRIVATE KEY-----"),
        Regex("-----BEGIN OPENSSH PRIVATE KEY-----"),

        // Database Connection Strings
        Regex("(?i)(mongodb|mysql|postgresql|sqlserver)://[^\\s]+:[^\\s]+@"),
        Regex("(?i)jdbc:[^\\s]+password=[^\\s;]+"),

        // Generic Password Patterns
        Regex("(?i)password\\s*[:=]\\s*['\"]([^'\"\\s]{8,})['\"]"),
        Regex("(?i)passwd\\s*[:=]\\s*['\"]([^'\"\\s]{8,})['\"]"),
        Regex("(?i)pwd\\s*[:=]\\s*['\"]([^'\"\\s]{8,})['\"]"),

        // OAuth and JWT
        Regex("eyJ[a-zA-Z0-9_-]{10,}\\.[a-zA-Z0-9_-]{10,}\\.[a-zA-Z0-9_-]{10,}"),

        // Slack Tokens
        Regex("xox[baprs]-[0-9]{10,13}-[0-9]{10,13}-[a-zA-Z0-9]{24,}"),

        // Stripe Keys
        Regex("(sk|pk)_(test|live)_[0-9a-zA-Z]{24,}"),

        // Google API Keys
        Regex("AIza[0-9A-Za-z\\-_]{35}"),

        // Twilio
        Regex("SK[0-9a-fA-F]{32}"),

        // Azure
        Regex("(?i)azure[_-]?storage[_-]?connection[_-]?string"),
        Regex("DefaultEndpointsProtocol=https;AccountName=")
    )

    private val PII_PATTERNS = listOf(
        // Email addresses
        Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"),

        // Social Security Numbers (US)
        Regex("\\b\\d{3}-\\d{2}-\\d{4}\\b"),

        // Credit Card Numbers
        Regex("\\b(?:\\d{4}[-\\s]?){3}\\d{4}\\b"),

        // Phone Numbers
        Regex("\\b(?:\\+\\d{1,3}[-\\s]?)?(?:\\(\\d{3}\\)|\\d{3})[-\\s]?\\d{3}[-\\s]?\\d{4}\\b"),

        // IP Addresses
        Regex("\\b(?:[0-9]{1,3}\\.){3}[0-9]{1,3}\\b")
    )

    data class DetectedSecret(
        val type: String,
        val value: String,
        val line: Int,
        val severity: Severity
    )

    enum class Severity {
        HIGH, MEDIUM, LOW
    }

    fun detectSecrets(content: String): List<DetectedSecret> {
        val detected = mutableListOf<DetectedSecret>()
        val lines = content.lines()

        lines.forEachIndexed { index, line ->
            SECRET_PATTERNS.forEach { pattern ->
                pattern.findAll(line).forEach { match ->
                    detected.add(
                        DetectedSecret(
                            type = "Secret/API Key",
                            value = match.value,
                            line = index + 1,
                            severity = Severity.HIGH
                        )
                    )
                }
            }
        }

        return detected
    }

    fun detectPII(content: String): List<DetectedSecret> {
        val detected = mutableListOf<DetectedSecret>()
        val lines = content.lines()

        lines.forEachIndexed { index, line ->
            PII_PATTERNS.forEach { pattern ->
                pattern.findAll(line).forEach { match ->
                    detected.add(
                        DetectedSecret(
                            type = "PII",
                            value = match.value,
                            line = index + 1,
                            severity = Severity.MEDIUM
                        )
                    )
                }
            }
        }

        return detected
    }

    fun maskSecrets(content: String): String {
        var masked = content

        SECRET_PATTERNS.forEach { pattern ->
            masked = pattern.replace(masked) { matchResult ->
                val original = matchResult.value
                val visibleChars = minOf(4, original.length / 4)
                original.take(visibleChars) + "*".repeat(original.length - visibleChars)
            }
        }

        return masked
    }

    fun hasSensitiveContent(content: String): Boolean {
        return SECRET_PATTERNS.any { it.containsMatchIn(content) } ||
                PII_PATTERNS.any { it.containsMatchIn(content) }
    }

    fun getSensitivityReport(content: String): String {
        val secrets = detectSecrets(content)
        val pii = detectPII(content)

        if (secrets.isEmpty() && pii.isEmpty()) {
            return "✓ No sensitive data detected"
        }

        val sb = StringBuilder()
        sb.append("⚠ Sensitive Data Detected:\n")

        if (secrets.isNotEmpty()) {
            sb.append("  - ${secrets.size} potential secret(s)\n")
        }

        if (pii.isNotEmpty()) {
            sb.append("  - ${pii.size} PII occurrence(s)\n")
        }

        return sb.toString()
    }
}
