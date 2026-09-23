package com.example.service

object EmojiDigits {
    private val map = mapOf(
        '0' to "\u0030\uFE0F\u20E3", '1' to "\u0031\uFE0F\u20E3",
        '2' to "\u0032\uFE0F\u20E3", '3' to "\u0033\uFE0F\u20E3",
        '4' to "\u0034\uFE0F\u20E3", '5' to "\u0035\uFE0F\u20E3",
        '6' to "\u0036\uFE0F\u20E3", '7' to "\u0037\uFE0F\u20E3",
        '8' to "\u0038\uFE0F\u20E3", '9' to "\u0039\uFE0F\u20E3"
    )

    fun convert(code: String): String =
        code.map { map[it] ?: it.toString() }.joinToString(" ")
}