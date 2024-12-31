package com.comet.nfcbasedmdm.common.util

import java.util.*
import java.util.concurrent.ThreadLocalRandom

// String - UUID 변환 확장함수, 실패시 null
fun String.toUUID() : UUID? = kotlin.runCatching { UUID.fromString(this) }.getOrNull()