package com.example.storemobile.util

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

object Format {

    /** 1234567.0 -> "1 234 567" (space-grouped, no decimals). */
    fun money(value: Double): String {
        val rounded = Math.round(value)
        val sb = StringBuilder(rounded.toString())
        val negative = sb.startsWith("-")
        if (negative) sb.deleteCharAt(0)
        var i = sb.length - 3
        while (i > 0) {
            sb.insert(i, ' ')
            i -= 3
        }
        if (negative) sb.insert(0, '-')
        return sb.toString()
    }

    fun sum(value: Double): String = money(value) + " so'm"

    // ────────────────────────────────────────────────────────────────
    //  VAQT: server va ilova barcha vaqtlarni UTC da saqlaydi (ISO-8601).
    //  Shu sabab ISO ni UTC deb o'qib, qurilmaning MAHALLIY vaqtiga o'giramiz.
    //  Shunda kassir/boshliq ekranida vaqt to'g'ri (mahalliy) ko'rinadi
    //  (avval 'Z' olib tashlanib, qurilma zonasida o'qilardi -> 5 soat farq).
    // ────────────────────────────────────────────────────────────────
    private fun parseUtc(iso: String): java.util.Date? {
        val input = iso.substringBefore('.').replace("Z", "").trim()
        if (input.isBlank()) return null
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        parser.timeZone = TimeZone.getTimeZone("UTC")
        return parser.parse(input)
    }

    /** ISO (UTC) sanani MAHALLIY vaqtda "dd.MM.yyyy  HH:mm" ko'rinishida qaytaradi. */
    fun dateTime(iso: String): String {
        if (iso.isBlank()) return ""
        return try {
            val date = parseUtc(iso) ?: return iso
            val out = SimpleDateFormat("dd.MM.yyyy  HH:mm", Locale.US)
            out.timeZone = TimeZone.getDefault()
            out.format(date)
        } catch (e: Exception) {
            iso
        }
    }

    /** ISO (UTC) sanadan MAHALLIY vaqtda faqat "HH:mm" qaytaradi. */
    fun timeOnly(iso: String): String {
        if (iso.isBlank()) return ""
        return try {
            val date = parseUtc(iso) ?: return ""
            val out = SimpleDateFormat("HH:mm", Locale.US)
            out.timeZone = TimeZone.getDefault()
            out.format(date)
        } catch (e: Exception) {
            ""
        }
    }
}
