package com.example.petling.ui.navigation

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.petling.ui.price.PriceProductScreen
import com.example.petling.ui.price.PriceScreen
import com.example.petling.ui.settings.SettingsScreen
import com.example.petling.ui.theme.Brand500
import com.example.petling.ui.theme.Dimens

@Composable
fun PetlingNavHost(navController: NavHostController = rememberNavController()) {
    val backStack by navController.currentBackStackEntryAsState()
    val currentDest = backStack?.destination

    val showBottomBar = currentDest?.let {
        it.hasRoute(PriceRoute::class) || it.hasRoute(SettingsRoute::class)
    } ?: false

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(modifier = Modifier.height(Dimens.NavBarHeight + Dimens.Space10)) {
                    tabItem(currentDest, PriceRoute, Icons.Filled.Sell, "가격") { navigateTab(navController, PriceRoute) }
                    tabItem(currentDest, SettingsRoute, Icons.Filled.Settings, "설정") { navigateTab(navController, SettingsRoute) }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = PriceRoute,
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            composable<PriceRoute> {
                PriceScreen(
                    onOpenProduct = { navController.navigate(PriceProductRoute(it)) },
                )
            }
            composable<PriceProductRoute> { entry ->
                val route = entry.toRoute<PriceProductRoute>()
                PriceProductScreen(
                    productId = route.productId,
                    onBack = { navController.popBackStack() },
                )
            }
            composable<SettingsRoute> { SettingsScreen() }
        }
    }
}

@Composable
private fun <T : Any> RowScope.tabItem(
    currentDest: androidx.navigation.NavDestination?,
    route: T,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    val selected = currentDest?.hasRoute(route::class) ?: false
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = { Icon(icon, contentDescription = label) },
        label = { Text(label) },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = Brand500,
            selectedTextColor = Brand500,
            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    )
}

private fun <T : Any> navigateTab(navController: NavHostController, route: T) {
    navController.navigate(route) {
        popUpTo(PriceRoute) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
