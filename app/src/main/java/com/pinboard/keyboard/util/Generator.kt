package com.pinboard.keyboard.util

import java.security.SecureRandom

/**
 * Random alphanumeric generator.
 * Pool = A-Z, a-z, 0-9 only (no symbols). Mixed case, random every time.
 * Example outputs: hgGt76g, Ab8xLp2, mQ72Za
 */
object Generator {
    private val POOL = (('a'..'z') + ('A'..'Z') + ('0'..'9')).toCharArray()
    private val random = SecureRandom()

    fun generate(length: Int): String {
        val len = length.coerceIn(6, 20)
        val sb = StringBuilder(len)
        repeat(len) { sb.append(POOL[random.nextInt(POOL.size)]) }
        return sb.toString()
    }
}