package mw.noor.app.ui

/** A tiny route stack for this single-activity Compose app; screen data remains in saved UI state. */
internal data class RouteHistory private constructor(
    val currentRoute: String,
    private val previousRoutes: List<String>,
) {
    fun navigateTo(route: String): RouteHistory {
        if (route == currentRoute) return this
        return copy(
            currentRoute = route,
            previousRoutes = (previousRoutes + currentRoute).takeLast(MAX_HISTORY),
        )
    }

    fun navigateBack(): RouteHistory {
        if (previousRoutes.isEmpty()) {
            return if (currentRoute == ROOT_ROUTE) this else copy(currentRoute = ROOT_ROUTE)
        }
        return copy(
            currentRoute = previousRoutes.last(),
            previousRoutes = previousRoutes.dropLast(1),
        )
    }

    fun encode(): String = "$currentRoute|${previousRoutes.joinToString(",")}"

    companion object {
        private const val ROOT_ROUTE = "tabs"
        private const val MAX_HISTORY = 24

        fun initial(): RouteHistory = RouteHistory(ROOT_ROUTE, emptyList())

        fun decode(encoded: String): RouteHistory {
            val route = encoded.substringBefore('|').ifBlank { ROOT_ROUTE }
            val history = encoded.substringAfter('|', "")
                .split(',')
                .filter(String::isNotBlank)
                .takeLast(MAX_HISTORY)
            return RouteHistory(route, history)
        }
    }
}
