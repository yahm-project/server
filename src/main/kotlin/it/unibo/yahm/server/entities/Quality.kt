package it.unibo.yahm.server.entities

import java.lang.reflect.Type

enum class Quality(val value: Int) {
    QUALITY0(0),
    QUALITY1(1),
    QUALITY2(2),
    QUALITY3(3),
    QUALITY4(4);

    companion object {
        private val map = values().associateBy(Quality::value)
        fun fromValue(value: Int) = map[value]
    }
}
