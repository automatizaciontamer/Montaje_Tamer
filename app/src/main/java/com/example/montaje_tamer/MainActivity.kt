package com.example.montaje_tamer

import android.os.Bundle
import com.google.firebase.FirebaseApp
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.montaje_tamer.model.Rol
import com.example.montaje_tamer.ui.screens.*
import com.example.montaje_tamer.ui.theme.Montaje_TamerTheme
import com.example.montaje_tamer.utils.SoundNotifications
import com.example.montaje_tamer.viewmodel.MainViewModel
import com.example.montaje_tamer.utils.UpdateManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    companion object {
        const val APP_VERSION = "V4.0.1"
        const val VERSION_CODE = 2
    }

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.example.montaje_tamer.utils.FileLogger.setupCrashLogger()
        com.example.montaje_tamer.utils.FileLogger.logInfo("Aplicación iniciada")
        FirebaseApp.initializeApp(this)
        enableEdgeToEdge()
        
        // Limpiar actualizaciones anteriores al iniciar
        UpdateManager.cleanOldUpdates(this)
        
        lifecycleScope.launch {
            viewModel.notificationEvent.collect { event ->
                if (event != null) {
                    SoundNotifications.playNotificationSound(this@MainActivity)
                }
            }
        }

        setContent {
            Montaje_TamerTheme {
                var updateUrl by remember { mutableStateOf<String?>(null) }
                var newVersionName by remember { mutableStateOf("") }
                var isDownloading by remember { mutableStateOf(false) }

                // Verificación de actualizaciones
                LaunchedEffect(Unit) {
                    UpdateManager.checkForUpdates(this@MainActivity, APP_VERSION) { version, url ->
                        newVersionName = version
                        updateUrl = url
                    }
                }

                if (updateUrl != null) {
                    AlertDialog(
                        onDismissRequest = { },
                        title = { Text("Actualización Disponible") },
                        text = { Text("Hay una nueva versión ($newVersionName) disponible. ¿Deseas descargarla e instalarla ahora?") },
                        confirmButton = {
                            Button(
                                onClick = {
                                    isDownloading = true
                                    lifecycleScope.launch {
                                        UpdateManager.downloadAndInstallApk(this@MainActivity, updateUrl!!)
                                    }
                                },
                                enabled = !isDownloading
                            ) {
                                if (isDownloading) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                                } else {
                                    Text("Actualizar")
                                }
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { updateUrl = null }, enabled = !isDownloading) {
                                Text("Más tarde")
                            }
                        }
                    )
                }

                MainApp(viewModel)
            }
        }
    }

    override fun onDestroy() {
        com.example.montaje_tamer.utils.FileLogger.logInfo("Aplicación cerrada (onDestroy)")
        super.onDestroy()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val isAuthenticated by viewModel.isAuthenticated.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val snackbarHostState = remember { SnackbarHostState() }
    val notificationEvent by viewModel.notificationEvent.collectAsState()

    LaunchedEffect(notificationEvent) {
        notificationEvent?.let { data ->
            val result = snackbarHostState.showSnackbar(
                message = data.message,
                actionLabel = "VER",
                duration = SnackbarDuration.Long
            )
            if (result == SnackbarResult.ActionPerformed) {
                navController.navigate(data.route)
            }
            viewModel.clearNotificationEvent()
        }
    }

    if (!isAuthenticated) {
        LoginScreen(viewModel, onLoginSuccess = {
            // El cambio en 'isAuthenticated' dentro del ViewModel disparará la recomposición
            // y mostrará automáticamente el NavHost con startDestination = "home"
        })
    } else {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet {
                    val isAdmin = currentUser?.rol in listOf(Rol.DIRECTIVO, Rol.ADMINISTRATIVO)
                    
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

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Construction, null) },
                        label = { Text("Montajes") },
                        selected = currentRoute == "lugares",
                        onClick = {
                            navController.navigate("lugares")
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.AutoMirrored.Filled.ReceiptLong, null) },
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

                    if (isAdmin) {
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
                        icon = { Icon(Icons.AutoMirrored.Filled.Logout, null) },
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
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    CenterAlignedTopAppBar(
                        title = { 
                            val title = when(currentRoute) {
                                "home" -> "Inicio"
                                "empleados" -> "Personal"
                                "lugares" -> "Montajes"
                                "gastos" -> "Gastos"
                                "horarios" -> "Control Horario"
                                "admin" -> "Administración"
                                "config" -> "Configuración"
                                else -> "Montaje Tamer"
                            }
                            Text(title)
                        },
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
                        composable("home") { HomeScreen(navController, viewModel) }
                        composable("empleados") { EmployeeScreen(viewModel) }
                        composable("lugares") { MontajeScreen(viewModel) }
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
