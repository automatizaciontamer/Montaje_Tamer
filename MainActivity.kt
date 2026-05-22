package com.example.montaje_tamer

import android.os.Bundle
import com.google.firebase.FirebaseApp
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.montaje_tamer.model.Rol
import com.example.montaje_tamer.ui.screens.*
import com.example.montaje_tamer.ui.theme.Montaje_TamerTheme
import com.example.montaje_tamer.viewmodel.MainViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    companion object {
        const val APP_VERSION = "V1.2.0"
    }

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        enableEdgeToEdge()
        setContent {
            Montaje_TamerTheme {
                MainApp(viewModel)
            }
        }
    }
}

@Composable
fun MainApp(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val isAuthenticated by viewModel.isAuthenticated.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    if (!isAuthenticated) {
        LoginScreen(viewModel, onLoginSuccess = {
            navController.navigate("home") {
                popUpTo("login") { inclusive = true }
            }
        })
    } else {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Montaje Tamer",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.titleLarge
                    )
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Home, null) },
                        label = { Text("Inicio") },
                        selected = currentRoute == "home",
                        onClick = {
                            navController.navigate("home")
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                    if (currentUser?.rol in listOf(Rol.DIRECTIVO, Rol.ADMINISTRATIVO, Rol.SUPERVISOR)) {
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.People, null) },
                            label = { Text("Personal") },
                            selected = currentRoute == "empleados",
                            onClick = {
                                navController.navigate("empleados")
                                scope.launch { drawerState.close() }
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                    }
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Build, null) },
                        label = { Text("Montajes") },
                        selected = currentRoute == "montajes",
                        onClick = {
                            navController.navigate("montajes")
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.ReceiptLong, null) },
                        label = { Text("Gastos") },
                        selected = currentRoute == "gastos",
                        onClick = {
                            navController.navigate("gastos")
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Schedule, null) },
                        label = { Text("Control Horario") },
                        selected = currentRoute == "horarios",
                        onClick = {
                            navController.navigate("horarios")
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                    if (currentUser?.rol in listOf(Rol.DIRECTIVO, Rol.ADMINISTRATIVO)) {
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.AdminPanelSettings, null) },
                            label = { Text("Administración") },
                            selected = currentRoute == "admin",
                            onClick = {
                                navController.navigate("admin")
                                scope.launch { drawerState.close() }
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.Settings, null) },
                            label = { Text("Configuración") },
                            selected = currentRoute == "config",
                            onClick = {
                                navController.navigate("config")
                                scope.launch { drawerState.close() }
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Logout, null) },
                        label = { Text("Cerrar Sesión") },
                        selected = false,
                        onClick = {
                            viewModel.logout()
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }
        ) {
            Scaffold(
                topBar = {
                    CenterAlignedTopAppBar(
                        title = { Text(currentRoute?.replaceFirstChar { it.uppercase() } ?: "Montaje Tamer") },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menú")
                            }
                        }
                    )
                }
            ) { padding ->
                Surface(modifier = Modifier.padding(padding)) {
                    NavHost(navController = navController, startDestination = "home") {
                        composable("home") { HomeScreen(navController, currentUser) }
                        composable("empleados") { EmployeeScreen(viewModel) }
                        composable("montajes") { MontajeScreen(viewModel) }
                        composable("gastos") { GastoScreen(viewModel) }
                        composable("horarios") { HorarioScreen(viewModel) }
                        composable("admin") { AdminScreen(viewModel) }
                        composable("config") { ConfigScreen(viewModel) }
                    }
                }
            }
        }
    }
}
