package net.taler.anastasis


sealed class Routes(
    val route: String
) {
    object Home: Routes("home")

    // Backup
    object BackupContinent: Routes("backup_continent")
    object BackupCountry: Routes("backup_country")
    object BackupUserAttributes: Routes("backup_user_attributes")

    // Recovery
    object RecoveryCountry: Routes("recovery_country")

    // Restore
    object RestoreInit: Routes("restore")

//    fun withArgs(args: Map<String, Any>) = with(Uri.parse(route).buildUpon()) {
//        args.forEach { entry ->
//            appendQueryParameter(entry.key, entry.value.toString())
//        }
//    }.toString()
}