package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.repository.SolarRepository
import com.example.ui.screens.CustomerDetailScreen
import com.example.ui.screens.CustomerDetailViewModel
import com.example.ui.screens.CustomersScreen
import com.example.ui.screens.CustomersViewModel
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DashboardViewModel
import com.example.ui.screens.InvoicesPaymentsPlaceholderScreen
import com.example.ui.screens.PendingActionsPlaceholderScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SettingsViewModel

object NavRoutes {
    const val DASHBOARD = "dashboard"
    const val CUSTOMERS = "customers"
    const val CUSTOMER_DETAIL = "customer_detail/{customerId}"
    const val INVOICES = "invoices"
    const val PENDING_ACTIONS = "pending_actions"
    const val SETTINGS = "settings"

    fun customerDetail(customerId: String): String = "customer_detail/$customerId"
}

enum class TopLevelDestination(
    val route: String,
    val compactLabel: String,
    val wideLabel: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    DASHBOARD(
        route = NavRoutes.DASHBOARD,
        compactLabel = "Home",
        wideLabel = "Dashboard",
        selectedIcon = Icons.Filled.Dashboard,
        unselectedIcon = Icons.Outlined.Dashboard
    ),
    CUSTOMERS(
        route = NavRoutes.CUSTOMERS,
        compactLabel = "Customers",
        wideLabel = "Customers",
        selectedIcon = Icons.Filled.People,
        unselectedIcon = Icons.Outlined.People
    ),
    INVOICES(
        route = NavRoutes.INVOICES,
        compactLabel = "Invoices",
        wideLabel = "Invoices & Payments",
        selectedIcon = Icons.Filled.AccountBalanceWallet,
        unselectedIcon = Icons.Outlined.AccountBalanceWallet
    ),
    PENDING_ACTIONS(
        route = NavRoutes.PENDING_ACTIONS,
        compactLabel = "Actions",
        wideLabel = "Pending Actions",
        selectedIcon = Icons.AutoMirrored.Filled.Assignment,
        unselectedIcon = Icons.AutoMirrored.Outlined.Assignment
    ),
    SETTINGS(
        route = NavRoutes.SETTINGS,
        compactLabel = "Settings",
        wideLabel = "Settings",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings
    )
}

@Composable
fun SolarDeskApp(
    repository: SolarRepository,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val settings by repository.settings.collectAsStateWithLifecycle(initialValue = null)
    val businessTitle = settings?.businessName?.takeIf { it.isNotBlank() } ?: "SolarDesk"

    val isDetailRoute = currentDestination?.route == NavRoutes.CUSTOMER_DETAIL

    fun navigateTopLevel(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val useNavRail = maxWidth >= 600.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets.safeDrawing,
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                if (!useNavRail && !isDetailRoute) {
                    Column {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 0.dp
                        ) {
                            TopLevelDestination.entries.forEach { dest ->
                                val selected = currentDestination?.hierarchy?.any { it.route == dest.route } == true
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateTopLevel(dest.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = dest.wideLabel
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = dest.compactLabel,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                    ),
                                    modifier = Modifier.testTag("nav_${dest.route}")
                                )
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavRail && !isDetailRoute) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        header = {
                            Column(
                                modifier = Modifier
                                    .width(88.dp)
                                    .padding(horizontal = 8.dp, vertical = 12.dp)
                            ) {
                                Text(
                                    text = "SolarDesk",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = businessTitle,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    ) {
                        Spacer(modifier = Modifier.height(8.dp))
                        TopLevelDestination.entries.forEach { dest ->
                            val selected = currentDestination?.hierarchy?.any { it.route == dest.route } == true
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTopLevel(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.wideLabel
                                    )
                                },
                                label = {
                                    Text(
                                        text = dest.compactLabel,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                ),
                                modifier = Modifier.testTag("rail_${dest.route}")
                            )
                        }
                    }
                    VerticalDivider(
                        modifier = Modifier.fillMaxHeight(),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }

                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    NavHost(
                        navController = navController,
                        startDestination = NavRoutes.DASHBOARD
                    ) {
                        composable(NavRoutes.DASHBOARD) {
                            val vm: DashboardViewModel = viewModel(
                                factory = AppViewModelFactory(repository)
                            )
                            DashboardScreen(
                                viewModel = vm,
                                onOpenCustomer = { customerId ->
                                    navController.navigate(NavRoutes.customerDetail(customerId))
                                },
                                onNavigateToCustomers = {
                                    navigateTopLevel(NavRoutes.CUSTOMERS)
                                }
                            )
                        }

                        composable(NavRoutes.CUSTOMERS) {
                            val vm: CustomersViewModel = viewModel(
                                factory = AppViewModelFactory(repository)
                            )
                            CustomersScreen(
                                viewModel = vm,
                                onOpenCustomer = { customerId ->
                                    navController.navigate(NavRoutes.customerDetail(customerId))
                                }
                            )
                        }

                        composable(
                            route = NavRoutes.CUSTOMER_DETAIL,
                            arguments = listOf(navArgument("customerId") { type = NavType.StringType })
                        ) { entry ->
                            val customerId = entry.arguments?.getString("customerId").orEmpty()
                            val vm: CustomerDetailViewModel = viewModel(
                                key = "customer_detail_$customerId",
                                factory = AppViewModelFactory(repository, customerId = customerId)
                            )
                            CustomerDetailScreen(
                                viewModel = vm,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(NavRoutes.INVOICES) {
                            InvoicesPaymentsPlaceholderScreen(
                                onOpenCustomers = { navigateTopLevel(NavRoutes.CUSTOMERS) },
                                onOpenDashboard = { navigateTopLevel(NavRoutes.DASHBOARD) }
                            )
                        }

                        composable(NavRoutes.PENDING_ACTIONS) {
                            PendingActionsPlaceholderScreen(
                                onOpenCustomers = { navigateTopLevel(NavRoutes.CUSTOMERS) },
                                onOpenDashboard = { navigateTopLevel(NavRoutes.DASHBOARD) }
                            )
                        }

                        composable(NavRoutes.SETTINGS) {
                            val vm: SettingsViewModel = viewModel(
                                factory = AppViewModelFactory(repository)
                            )
                            SettingsScreen(viewModel = vm)
                        }
                    }
                }
            }
        }
    }
}
