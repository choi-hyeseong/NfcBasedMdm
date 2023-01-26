package com.comet.nfcbasedmdm.util

import java.util.*
import java.util.concurrent.ThreadLocalRandom

class StringUtil {
    companion object {
        fun isUUID(input: String?): Boolean {
            return try {
                UUID.fromString(input)
                true
            } catch (e: IllegalArgumentException) {
                false
            }
        }

        fun generateRandomString(size: Int): String {
            val left = 48
            val right = 122
            val random = ThreadLocalRandom.current() //효과 좋은 랜덤
            return random.ints(left, right + 1)
                .filter { i: Int -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97) }
                .limit(size.toLong())
                .collect(
                    { StringBuilder() },
                    { obj: java.lang.StringBuilder, codePoint: Int ->
                        obj.appendCodePoint(
                            codePoint
                        )
                    }
                ) { obj: java.lang.StringBuilder, s: java.lang.StringBuilder? ->
                    obj.append(
                        s
                    )
                } //첫번째 인자 -> 생성할 객체, 두번째 인자 -> 들어온 값 어떻게 처리할지, 세번째 인자 -> 병렬처리에서 나온 결과 어떻게 할지
                .toString()
        }
    }
}