package com.example.edu_kt_hw_06

import androidx.navigation.NavController
import androidx.navigation.navOptions

internal fun NavController.navigateToTab(destinationId: Int) {
    navigate(
        destinationId,
        null,
        navOptions {
            popUpTo(R.id.nav_graph_main)
            launchSingleTop = true
        }
    )
}
