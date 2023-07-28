package net.taler.anastasis


sealed class Routes(
    val route: String
) {
    object Home: Routes("home")

    // Common
    object SelectContinent: Routes("select_continent")
    object SelectCountry: Routes("select_country")
    object SelectUserAttributes: Routes("select_user_attributes")

    // Restore
    object RestoreInit: Routes("restore")

//    fun withArgs(args: Map<String, Any>) = with(Uri.parse(route).buildUpon()) {
//        args.forEach { entry ->
//            appendQueryParameter(entry.key, entry.value.toString())
//        }
//    }.toString()
}