package com.lyx.phone.note.domain

data class NormalizedPhoneNumber(
    val rawNumber: String,
    val normalizedNumber: String,
    val last11Digits: String?,
    val isLikelyChineseMobile: Boolean
)

object PhoneNumberNormalizer {
    private val mobileRegex = Regex("^1[3-9]\\d{9}$")

    fun normalize(raw: String): NormalizedPhoneNumber {
        val trimmed = raw.trim()
        val cleaned = buildString {
            trimmed.forEachIndexed { index, char ->
                when {
                    char.isDigit() -> append(char)
                    char == '+' && index == trimmed.indexOf('+') && isEmpty() -> append(char)
                }
            }
        }

        val normalized = when {
            cleaned.startsWith("+86") && cleaned.drop(3).matches(mobileRegex) -> cleaned
            cleaned.startsWith("0086") && cleaned.drop(4).matches(mobileRegex) -> "+86${cleaned.drop(4)}"
            cleaned.startsWith("086") && cleaned.drop(3).matches(mobileRegex) -> "+86${cleaned.drop(3)}"
            cleaned.matches(mobileRegex) -> "+86$cleaned"
            else -> cleaned
        }
        val digits = normalized.filter(Char::isDigit)
        val last11 = digits.takeLast(11).takeIf { it.matches(mobileRegex) }
        return NormalizedPhoneNumber(
            rawNumber = raw,
            normalizedNumber = normalized,
            last11Digits = last11,
            isLikelyChineseMobile = last11 != null
        )
    }

    fun mask(number: String): String {
        val normalized = normalize(number).normalizedNumber
        val digits = normalized.filter(Char::isDigit)
        return when {
            normalized.startsWith("+86") && digits.length >= 13 -> "+86 ${digits.drop(2).take(3)}****${digits.takeLast(4)}"
            digits.length >= 11 -> "${digits.take(3)}****${digits.takeLast(4)}"
            digits.length >= 7 -> "${digits.take(3)}****${digits.takeLast(3)}"
            else -> normalized
        }
    }
}
