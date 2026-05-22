package com.example.montaje_tamer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

import com.example.montaje_tamer.model.Empleado
import com.example.montaje_tamer.model.Rol
import com.example.montaje_tamer.model.Montaje

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController, viewModel: com.example.montaje_tamer.viewmodel.MainViewModel) {
    val montajes by viewModel.montajes.collectAsState()
    val montajesPendientes by viewModel.montajesPendientesConfirmacion.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    
    val montajesActivos = montajes.filter { it.estado != "FINALIZADO" && it.estado != "PENDIENTE_CONFIRMACION" }
    
    var showRejectionDialog by remember { mutableStateOf<Montaje?>(null) }
    var rejectionReason by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text("Panel de Control") })
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // SECCIÓN: SOLICITUDES DE ASIGNACIÓN PENDIENTES
            if (montajesPendientes.isNotEmpty()) {
                item {
                    Text(
                        "Solicitudes de Asignación",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                items(montajesPendientes) { montaje ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("¡Fuiste asignado a un nuevo montaje!", style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.height(4.dp))
                            Text("Proyecto: ${montaje.nombre}", style = MaterialTheme.typography.titleMedium)
                            Text("Ubicación: ${montaje.ubicacion}")
                            
                            val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                            Text("Inicio: ${montaje.fechaInicio?.let { sdf.format(it.toDate()) } ?: "No definida"}")
                            Text("Fin Est.: ${montaje.fechaFinalEstimada?.let { sdf.format(it.toDate()) } ?: "No definida"}")
                            
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                TextButton(onClick = { showRejectionDialog = montaje }) {
                                    Text("Rechazar", color = MaterialTheme.colorScheme.error)
                                }
                                Button(onClick = { viewModel.responderConfirmacionMontaje(montaje.id, true) }) {
                                    Text("Aceptar")
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    "Progreso de Montajes en Curso",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (montajesActivos.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "No hay montajes activos actualmente",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            items(montajesActivos) { montaje ->
                MontajeProgressCard(montaje)
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
            
            item {
                Text(
                    "Accesos Directos",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (currentUser?.rol == Rol.DIRECTIVO) {
                        SmallMenuButton("Personal", Icons.Default.People, Modifier.weight(1f)) { navController.navigate("empleados") }
                    }
                    SmallMenuButton("Montajes", Icons.Default.Build, Modifier.weight(1f)) { navController.navigate("lugares") }
                }
            }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmallMenuButton("Gastos", Icons.Default.ReceiptLong, Modifier.weight(1f)) { navController.navigate("gastos") }
                    SmallMenuButton("Horarios", Icons.Default.Schedule, Modifier.weight(1f)) { navController.navigate("horarios") }
                }
            }
            
            if (currentUser?.rol == Rol.DIRECTIVO || currentUser?.rol == Rol.ADMINISTRATIVO) {
                item {
                    MenuButton("Administración / Informes") { navController.navigate("admin") }
                }
            }
        }
    }

    if (showRejectionDialog != null) {
        AlertDialog(
            onDismissRequest = { showRejectionDialog = null },
            title = { Text("Rechazar Asignación") },
            text = {
                Column {
                    Text("Por favor, indica el motivo del rechazo para el montaje: ${showRejectionDialog?.nombre}")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = rejectionReason,
                        onValueChange = { rejectionReason = it },
                        label = { Text("Motivo") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (rejectionReason.isNotBlank()) {
                            viewModel.responderConfirmacionMontaje(showRejectionDialog!!.id, false, rejectionReason)
                            showRejectionDialog = null
                            rejectionReason = ""
                        }
                    },
                    enabled = rejectionReason.isNotBlank()
                ) {
                    Text("Enviar Rechazo")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRejectionDialog = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun MontajeProgressCard(montaje: Montaje) {
    val inicio = montaje.fechaInicio?.toDate()?.time ?: System.currentTimeMillis()
    val finEstimado = montaje.fechaFinalEstimada?.toDate()?.time ?: (inicio + 86400000 * 7) // 1 semana por defecto
    val hoy = System.currentTimeMillis()
    
    val totalTime = (finEstimado - inicio).coerceAtLeast(1)
    val elapsedTime = (hoy - inicio).coerceIn(0, totalTime)
    val progress = elapsedTime.toFloat() / totalTime.toFloat()
    
    val diasRestantes = ((finEstimado - hoy) / 86400000).toInt()

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = montaje.nombre, style = MaterialTheme.typography.titleLarge)
            Text(text = "Ubicación: ${montaje.ubicacion}", style = MaterialTheme.typography.bodySmall)
            
            Spacer(modifier = Modifier.height(12.dp))
            
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = if (progress > 0.9f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )
            
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("${(progress * 100).toInt()}% transcurrido", style = MaterialTheme.typography.labelSmall)
                Text(
                    if (diasRestantes >= 0) "$diasRestantes días restantes" else "Retrasado por ${-diasRestantes} días",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (diasRestantes < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SmallMenuButton(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        contentPadding = PaddingValues(12.dp)
    ) {
        Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
            Icon(icon, null)
            Text(text, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
fun MenuButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium
    ) {
        Text(text)
    }
}
