package it.unibo.yahm.server.entities


enum class Quality(val value: Int) {
    VERY_BAD(0),
    BAD(1),
    OK(2),
    GOOD(3),
    PERFECT(4);

    companion object {
        private val map = values().associateBy(Quality::value)
        fun fromValue(value: Int) = map[value]
    }
}
