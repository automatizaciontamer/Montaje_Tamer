package com.example.montaje_tamer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.montaje_tamer.model.*
import com.example.montaje_tamer.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(viewModel: MainViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val montajes by viewModel.montajes.collectAsState()
    val empleados by viewModel.empleados.collectAsState()
    val gastos by viewModel.gastos.collectAsState()
    val solicitudes by viewModel.solicitudesEspeciales.collectAsState()
    val solicitudesExtension by viewModel.solicitudesExtension.collectAsState()
    val solicitudesEfectivo by viewModel.solicitudesEfectivo.collectAsState()
    
    var selectedMontaje by remember { mutableStateOf<Montaje?>(null) }
    var showReassignDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val currentUserId = currentUser?.id ?: ""
    val notificacionesRechazo by viewModel.notificacionesRechazo.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Administración y Auditoría") }) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Sección de Notificaciones de Rechazo de Asignación
            if (notificacionesRechazo.isNotEmpty()) {
                item {
                    Text("Rechazos de Asignación", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                }
                items(notificacionesRechazo) { (montaje, empleadoId) ->
                    val emp = empleados.find { it.id == empleadoId }
                    val motivo = montaje.motivosRechazoAsignacion[empleadoId] ?: "Sin motivo especificado"
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Montaje: ${montaje.nombre}", style = MaterialTheme.typography.titleSmall)
                            Text("Empleado: ${emp?.nombre} ${emp?.apellido} rechazó la asignación.")
                            Text("Motivo: $motivo", style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = { 
                                    selectedMontaje = montaje
                                    showReassignDialog = true
                                },
                                modifier = Modifier.align(androidx.compose.ui.Alignment.End)
                            ) {
                                Text("Reasignar Personal")
                            }
                        }
                    }
                }
            }

            // Sección de Solicitudes de Extensión
            if (solicitudesExtension.isNotEmpty()) {
                item {
                    Text("Solicitudes de Extensión", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                }
                items(solicitudesExtension) { sol ->
                    val mon = montajes.find { it.id == sol.montajeId }
                    val solicitante = empleados.find { it.id == sol.solicitadoPorId }
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Montaje: ${mon?.nombre ?: "Desconocido"}", style = MaterialTheme.typography.titleSmall)
                            Text("Solicitado por: ${solicitante?.nombre} ${solicitante?.apellido}")
                            Text("Días solicitados: ${sol.diasSolicitados}")
                            Text("Motivo: ${sol.motivo}")
                            
                            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                TextButton(onClick = { 
                                    viewModel.responderSolicitudExtension(context, sol, false, currentUserId, "Rechazado por administración") 
                                }) { Text("Rechazar", color = MaterialTheme.colorScheme.error) }
                                
                                Button(onClick = { 
                                    viewModel.responderSolicitudExtension(context, sol, true, currentUserId) 
                                }) { Text("Aprobar") }
                            }
                        }
                    }
                }
            }

            // Sección de Solicitudes de Efectivo
            if (solicitudesEfectivo.isNotEmpty()) {
                item {
                    Text("Solicitudes de Efectivo", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.tertiary)
                }
                items(solicitudesEfectivo) { sol ->
                    val mon = montajes.find { it.id == sol.montajeId }
                    val solicitante = empleados.find { it.id == sol.solicitadoPorId }
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Montaje: ${mon?.nombre ?: "Desconocido"}", style = MaterialTheme.typography.titleSmall)
                            Text("Solicitado por: ${solicitante?.nombre} ${solicitante?.apellido}")
                            Text("Monto: $${String.format("%.2f", sol.monto)}")
                            Text("Motivo: ${sol.motivo}")
                            
                            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                TextButton(onClick = { 
                                    viewModel.responderSolicitudEfectivo(context, sol, false, currentUserId, "Rechazado") 
                                }) { Text("Rechazar", color = MaterialTheme.colorScheme.error) }
                                
                                Button(onClick = { 
                                    viewModel.responderSolicitudEfectivo(context, sol, true, currentUserId)
                                }) { Text("Aprobar") }
                            }
                        }
                    }
                }
            }

            // Sección de Solicitudes Pendientes
            if (solicitudes.isNotEmpty()) {
                item {
                    Text("Solicitudes de Permiso Pendientes", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                }
                items(solicitudes) { sol ->
                    val emp = empleados.find { it.id == sol.empleadoId }
                    val mon = montajes.find { it.id == sol.montajeId }
                    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Personal: ${emp?.nombre} ${emp?.apellido}", style = MaterialTheme.typography.titleSmall)
                            Text("Montaje: ${mon?.nombre}")
                            Text("Fecha: ${sdf.format(sol.fechaTrabajo.toDate())}")
                            Text("Justificación: ${sol.motivo}")
                            
                            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                TextButton(onClick = { 
                                    viewModel.responderSolicitudEspecial(sol, false, currentUserId) 
                                }) { Text("Denegar", color = MaterialTheme.colorScheme.error) }
                                
                                Button(onClick = { 
                                    viewModel.responderSolicitudEspecial(sol, true, currentUserId) 
                                }) { Text("Aprobar") }
                            }
                        }
                    }
                }
            }

            item {
                Text("Seleccionar Montaje para Informes:", style = MaterialTheme.typography.titleMedium)
                var expanded by remember { mutableStateOf(false) }
                OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(selectedMontaje?.nombre ?: "Seleccionar Montaje")
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    montajes.forEach { m ->
                        DropdownMenuItem(
                            text = { Text(m.nombre) },
                            onClick = {
                                selectedMontaje = m
                                viewModel.loadGastosPorMontaje(m.id)
                                expanded = false
                            }
                        )
                    }
                }
            }

            if (selectedMontaje != null) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Acciones de Reporte", style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = { viewModel.exportGastosPdf(context, selectedMontaje!!) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.PictureAsPdf, null)
                                Spacer(Modifier.width(8.dp))
                                Text("PDF Detalle de Gastos")
                            }
                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = { viewModel.exportHorariosMontajePdf(context, selectedMontaje!!) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Icon(Icons.Default.PictureAsPdf, null)
                                Spacer(Modifier.width(8.dp))
                                Text("PDF Horarios del Personal")
                            }
                        }
                    }
                }

                item {
                    Text("Resumen Financiero", style = MaterialTheme.typography.titleMedium)
                    val totalGastos = gastos.sumOf { it.totalGasto }
                    val totalEfectivo = selectedMontaje!!.cajaEfectivoInicial + selectedMontaje!!.registrosEfectivo.sumOf { it.monto }
                    Text("Efectivo Total Recibido: $${String.format("%.2f", totalEfectivo)}")
                    Text("Total Gastado: $${String.format("%.2f", totalGastos)}")
                    Text("Saldo en Caja: $${String.format("%.2f", totalEfectivo - totalGastos)}", 
                        color = if (totalEfectivo - totalGastos >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                }

                item {
                    Text("Últimos Gastos Registrados", style = MaterialTheme.typography.labelLarge)
                }
                
                items(gastos.take(5)) { gasto ->
                    ListItem(
                        headlineContent = { Text(gasto.detalle) },
                        supportingContent = { Text("Monto: $${gasto.totalGasto} - ${gasto.tipoPago}") }
                    )
                }
            }
        }
    }

    if (showReassignDialog && selectedMontaje != null) {
        MontajeDialog(
            montaje = selectedMontaje,
            empleados = empleados,
            onDismiss = { showReassignDialog = false },
            onConfirm = { actualizado ->
                viewModel.saveMontaje(context, actualizado)
                showReassignDialog = false
            }
        )
    }
}
