package it.unibo.yahm.server.maps

import com.fasterxml.jackson.annotation.JsonAlias

object MatchService {

    data class Result(
            /**
             * An list of Route objects that assemble the trace.
             */
            val matchings: List<Matching>,
            /**
             * List of Waypoint objects representing all points of the trace in order.
             * If the trace point was ommited by map matching because it is an outlier, the entry will be null
             */
            val tracepoints: List<Tracepoint>?,
            /**
             * If the request was successful Ok otherwise see the service dependent and general status codes.
             */
            val code: String
    )

    data class Matching(
            /**
             * Confidence of the matching. float value between 0 and 1. 1 is very confident that the matching is correct.
             */
            val confidence: Float,
            val geometry: String,
            val legs: List<Leg>,
            @JsonAlias("weight_name") val weightName: String,
            val weight: Int,
            val duration: Float,
            val distance: Float
    )

    data class Leg(
            val annotation: Annotation?,
            val summary: String,
            val weight: Int,
            val duration: Float,
            val steps: List<String>,
            val distance: Double
    )

    data class Annotation(
            val metadata: Metadata?,
            val nodes: List<Long>?,
            val datasources: List<Int>?,
            val weight: List<Float>?,
            val distance: List<Double>?,
            val duration: List<Float>?,
            val speed: List<Float>?
    )

    data class Metadata(
            @JsonAlias("datasource_names") val datasourceNames: List<String>
    )

    data class Tracepoint(
            /**
             * Number of probable alternative matchings for this trace point.
             * A value of zero indicate that this point was matched unambiguously.
             * Split the trace at these points for incremental map matching.
             */
            @JsonAlias("alternatives_count") val alternativesCount: Int,
            /**
             * Index of the waypoint inside the matched route.
             */
            @JsonAlias("waypoint_index") val waypointIndex: Int,
            /**
             * Index to the Route object in matchings the sub-trace was matched to.
             */
            @JsonAlias("matchings_index") val matchingsIndex: Int,
            val hint: String?,
            val distance: Double,
            val name: String,
            val location: List<Double>
    )

    enum class Annotations(val value: String) {
        TRUE("true"),
        NODES("nodes"),
        DISTANCE("distance"),
        DURATION("duration"),
        DATASOURCES("datasources"),
        WEIGHT("weight"),
        SPEED("speed")
    }

    enum class Overview(val value: String) {
        SIMPLIFIED("simplified"),
        FULL("full"),
        FALSE("false")
    }

    enum class Gaps(val value: String) {
        SPLIT("split"),
        IGNORE("ignore")
    }

    data class Options(
            /**
             * Returns additional metadata for each coordinate along the route geometry.
             */
            val annotations: Annotations = Annotations.NODES,
            /**
             * Add overview geometry either full, simplified according to highest zoom level it could be display on, or not at all.
             */
            val overview: Overview = Overview.SIMPLIFIED,
            /**
             * Allows the input track splitting based on huge timestamp gaps between points.
             */
            val gaps: Gaps = Gaps.IGNORE,
            /**
             * Allows the input track modification to obtain better matching quality for noisy tracks.
             */
            val tidy: Boolean = false
    ) {
        fun toUrlOptions(): String = "&geometries=polyline6&steps=false&annotations=${annotations.value}" +
                "&overview=${overview.value}&gaps=${gaps.value}&tidy=${tidy}"
    }

}
