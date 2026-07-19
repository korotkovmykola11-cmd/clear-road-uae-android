package com.clearroad.app.stagea

import android.os.SystemClock

fun interface MonotonicClock {
    fun nowMs(): Long
}

object StageASessionClock {
    val DEFAULT: MonotonicClock = MonotonicClock { SystemClock.elapsedRealtime() }
}

class FakeMonotonicClock(initialMs: Long = 0L) : MonotonicClock {
    private var nowMsValue: Long = initialMs

    override fun nowMs(): Long = nowMsValue

    fun advance(byMs: Long) {
        nowMsValue += byMs
    }
}
