package it.unibo.yahm.server.maps

object OverpassService {

    data class Result(
            val elements: List<Element>
    )

    data class Element(
            val type: String,
            val id: Long,
            val lat: Double,
            val lon: Double
    )

}