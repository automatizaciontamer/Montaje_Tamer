package com.example.montaje_tamer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.montaje_tamer.model.*
import com.example.montaje_tamer.viewmodel.MainViewModel
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MontajeScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val montajes by viewModel.montajes.collectAsState()
    val empleados by viewModel.empleados.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    
    var searchQuery by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }
    var showFinishDialog by remember { mutableStateOf(false) }
    var showExtensionDialog by remember { mutableStateOf(false) }
    var showEvaluateDialog by remember { mutableStateOf(false) }
    var selectedMontaje by remember { mutableStateOf<Montaje?>(null) }

    val filteredMontajes = montajes.filter {
        it.nombre.contains(searchQuery, ignoreCase = true) || 
        it.ubicacion.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(title = { Text("Gestión de Montajes") })
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Buscar montaje...") },
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    singleLine = true
                )
            }
        },
        floatingActionButton = {
            if (currentUser?.rol in listOf(Rol.DIRECTIVO, Rol.ADMINISTRATIVO)) {
                FloatingActionButton(onClick = { 
                    selectedMontaje = null
                    showDialog = true 
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Nuevo Montaje")
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredMontajes) { montaje ->
                MontajeCard(
                    montaje = montaje,
                    empleados = empleados,
                    currentUser = currentUser,
                    onEdit = {
                        selectedMontaje = it
                        showDialog = true
                    },
                    onDelete = {
                        viewModel.deleteMontaje(context, it.id)
                    },
                    onFinishRequest = {
                        selectedMontaje = it
                        showFinishDialog = true
                    },
                    onExtensionRequest = {
                        selectedMontaje = it
                        showExtensionDialog = true
                    },
                    onEvaluate = {
                        selectedMontaje = it
                        showEvaluateDialog = true
                    }
                )
            }
        }

        if (showDialog) {
            MontajeDialog(
                montaje = selectedMontaje,
                empleados = empleados,
                onDismiss = { showDialog = false },
                onConfirm = { nuevoMontaje ->
                    viewModel.saveMontaje(context, nuevoMontaje)
                    showDialog = false
                }
            )
        }

        if (showFinishDialog && selectedMontaje != null) {
            AlertDialog(
                onDismissRequest = { showFinishDialog = false },
                title = { Text("Solicitar Finalización") },
                text = { Text("¿Estás seguro de que deseas solicitar la finalización del montaje \"${selectedMontaje?.nombre}\"?") },
                confirmButton = {
                    Button(onClick = {
                        viewModel.solicitarFinalizacionMontaje(context, selectedMontaje!!)
                        showFinishDialog = false
                    }) { Text("Confirmar") }
                },
                dismissButton = {
                    TextButton(onClick = { showFinishDialog = false }) { Text("Cancelar") }
                }
            )
        }

        if (showExtensionDialog && selectedMontaje != null) {
            ExtensionRequestDialog(
                montaje = selectedMontaje!!,
                onDismiss = { showExtensionDialog = false },
                onConfirm = { dias, motivo ->
                    viewModel.solicitarExtensionMontaje(
                        context,
                        SolicitudExtensionMontaje(
                            montajeId = selectedMontaje!!.id,
                            solicitadoPorId = currentUser?.id ?: "",
                            diasSolicitados = dias,
                            motivo = motivo
                        )
                    )
                    showExtensionDialog = false
                }
            )
        }

        if (showEvaluateDialog && selectedMontaje != null) {
            EvaluateFinishDialog(
                montaje = selectedMontaje!!,
                onDismiss = { showEvaluateDialog = false },
                onConfirm = { aprobado, motivo, dias ->
                    viewModel.responderFinalizacionMontaje(context, selectedMontaje!!, aprobado, motivo, dias)
                    showEvaluateDialog = false
                }
            )
        }
    }
}

@Composable
fun ExtensionRequestDialog(
    montaje: Montaje,
    onDismiss: () -> Unit,
    onConfirm: (Int, String) -> Unit
) {
    var dias by remember { mutableStateOf("1") }
    var motivo by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Solicitar Extensión") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("¿Cuántos días adicionales necesitas para \"${montaje.nombre}\"?")
                TextField(
                    value = dias,
                    onValueChange = { if (it.isEmpty() || it.toIntOrNull() != null) dias = it },
                    label = { Text("Días") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                TextField(
                    value = motivo,
                    onValueChange = { motivo = it },
                    label = { Text("Motivo / Justificación") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(dias.toIntOrNull() ?: 1, motivo) },
                enabled = motivo.isNotBlank() && (dias.toIntOrNull() ?: 0) > 0
            ) { Text("Solicitar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
fun MontajeCard(
    montaje: Montaje,
    empleados: List<Empleado>,
    currentUser: Empleado?,
    onEdit: (Montaje) -> Unit,
    onDelete: (Montaje) -> Unit,
    onFinishRequest: (Montaje) -> Unit,
    onExtensionRequest: (Montaje) -> Unit,
    onEvaluate: (Montaje) -> Unit
) {
    val encargado = empleados.find { it.id == montaje.encargadoId }
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val isAdmin = currentUser?.rol in listOf(Rol.DIRECTIVO, Rol.ADMINISTRATIVO)
    val isEncargado = currentUser?.id == montaje.encargadoId

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = montaje.nombre, style = MaterialTheme.typography.titleLarge)
                    Text(text = montaje.ubicacion, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (isAdmin) {
                    IconButton(onClick = { onEdit(montaje) }) { Icon(Icons.Default.Edit, null) }
                    IconButton(onClick = { onDelete(montaje) }) { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
                }
            }
            Spacer(Modifier.height(8.dp))
            
            Text("Encargado: ${encargado?.let { "${it.nombre} ${it.apellido}" } ?: "No asignado"}", style = MaterialTheme.typography.bodyMedium)
            
            if (montaje.personalAsignadoIds.isNotEmpty()) {
                val teamNames = montaje.personalAsignadoIds.mapNotNull { id ->
                    empleados.find { it.id == id }?.nombre
                }.joinToString(", ")
                Text("Equipo: $teamNames", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(Modifier.height(4.dp))
            Text(
                "Vigencia: ${montaje.fechaInicio?.let { sdf.format(it.toDate()) } ?: "?"} - ${montaje.fechaFinalEstimada?.let { sdf.format(it.toDate()) } ?: "?"}",
                style = MaterialTheme.typography.bodySmall
            )
            
            val estadoColor = when(montaje.estado) {
                "FINALIZACION_SOLICITADA" -> MaterialTheme.colorScheme.tertiary
                "FINALIZADO" -> MaterialTheme.colorScheme.secondary
                else -> MaterialTheme.colorScheme.primary
            }
            Text("Estado: ${montaje.estado.replace("_", " ")}", color = estadoColor, style = MaterialTheme.typography.labelLarge)

            if (montaje.diasAdicionales > 0) {
                Text("Días adicionales: ${montaje.diasAdicionales}", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            
            if (montaje.estado == "EN_PROCESO" && montaje.motivoRechazoFinalizacion.isNotEmpty()) {
                Text("Nota rechazo: ${montaje.motivoRechazoFinalizacion}", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(8.dp))
            val saldo = (montaje.cajaEfectivoInicial + montaje.registrosEfectivo.sumOf { it.monto })
            Text("Caja actual: $${String.format(Locale.getDefault(), "%.2f", saldo)}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
            
            if (montaje.numeroTarjeta.isNotEmpty()) {
                Text("Tarjeta: ${formatCardNumber(montaje.numeroTarjeta)}", style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(8.dp))
            
            // Botones de acción según el Rol
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                if (montaje.estado == "EN_PROCESO" && (isEncargado || isAdmin)) {
                    OutlinedButton(onClick = { onExtensionRequest(montaje) }) {
                        Text("Solicitar Extensión")
                    }
                    Button(onClick = { onFinishRequest(montaje) }) {
                        Text("Solicitar Finalización")
                    }
                }
                
                if (montaje.estado == "FINALIZACION_SOLICITADA" && isAdmin) {
                    Button(
                        onClick = { onEvaluate(montaje) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                    ) {
                        Text("Evaluar Finalización")
                    }
                }
            }
        }
    }
}


@Composable
fun EvaluateFinishDialog(
    montaje: Montaje,
    onDismiss: () -> Unit,
    onConfirm: (Boolean, String, Int) -> Unit
) {
    var motivoRechazo by remember { mutableStateOf("") }
    var diasAdicionales by remember { mutableStateOf("1") }
    var isRejecting by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Evaluar Finalización") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("¿Deseas aprobar la finalización del montaje \"${montaje.nombre}\"?")
                
                if (isRejecting) {
                    Spacer(Modifier.height(8.dp))
                    Text("Detalles del Rechazo:", style = MaterialTheme.typography.labelLarge)
                    TextField(
                        value = motivoRechazo,
                        onValueChange = { motivoRechazo = it },
                        label = { Text("Motivo del rechazo") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    TextField(
                        value = diasAdicionales,
                        onValueChange = { if (it.isEmpty() || it.toIntOrNull() != null) diasAdicionales = it },
                        label = { Text("Días adicionales de extensión") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Row {
                if (!isRejecting) {
                    TextButton(onClick = { isRejecting = true }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                        Text("Rechazar")
                    }
                    Button(onClick = { onConfirm(true, "", 0) }) {
                        Text("Aprobar")
                    }
                } else {
                    Button(onClick = { 
                        onConfirm(false, motivoRechazo, diasAdicionales.toIntOrNull() ?: 0) 
                    }, enabled = motivoRechazo.isNotBlank()) {
                        Text("Confirmar Rechazo")
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = {
                if (isRejecting) isRejecting = false else onDismiss()
            }) { Text("Cancelar") }
        }
    )
}

fun formatCardNumber(number: String): String {
    return number.chunked(4).joinToString(" ")
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MontajeDialog(
    montaje: Montaje?,
    empleados: List<Empleado>,
    onDismiss: () -> Unit,
    onConfirm: (Montaje) -> Unit
) {
    var nombre by remember { mutableStateOf(montaje?.nombre ?: "") }
    var ubicacion by remember { mutableStateOf(montaje?.ubicacion ?: "") }
    var encargadoId by remember { mutableStateOf(montaje?.encargadoId ?: "") }
    var selectedPersonalIds by remember { mutableStateOf(montaje?.personalAsignadoIds?.toSet() ?: emptySet()) }
    var cajaInicial by remember { mutableStateOf(montaje?.cajaEfectivoInicial?.toString() ?: "0.0") }
    var tarjeta by remember { mutableStateOf(montaje?.numeroTarjeta ?: "") }
    var usaHorarioEmpresa by remember { mutableStateOf(montaje?.usaHorarioEmpresa ?: true) }
    
    var expandedEncargado by remember { mutableStateOf(false) }
    var showInicioPicker by remember { mutableStateOf(false) }
    var showFinalPicker by remember { mutableStateOf(false) }

    val inicioPickerState = rememberDatePickerState(
        initialSelectedDateMillis = (montaje?.fechaInicio ?: Timestamp.now()).toDate().time
    )
    val finalPickerState = rememberDatePickerState(
        initialSelectedDateMillis = (montaje?.fechaFinalEstimada ?: Timestamp(Date(System.currentTimeMillis() + 86400000L * 7))).toDate().time
    )

    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (montaje == null) "Nuevo Montaje" else "Editar Montaje") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { TextField(value = nombre, onValueChange = { nombre = it }, label = { Text("Nombre del Montaje") }, modifier = Modifier.fillMaxWidth()) }
                item { TextField(value = ubicacion, onValueChange = { ubicacion = it }, label = { Text("Ubicación / Planta") }, modifier = Modifier.fillMaxWidth()) }
                
                item {
                    Text("Fecha de Inicio:", style = MaterialTheme.typography.labelLarge)
                    OutlinedButton(
                        onClick = { showInicioPicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val date = inicioPickerState.selectedDateMillis?.let { Date(it) } ?: Date()
                        Text(dateFormatter.format(date))
                    }
                }

                item {
                    Text("Fecha de Finalización Estimada:", style = MaterialTheme.typography.labelLarge)
                    OutlinedButton(
                        onClick = { showFinalPicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val date = finalPickerState.selectedDateMillis?.let { Date(it) } ?: Date()
                        Text(dateFormatter.format(date))
                    }
                }

                item {
                    Text("Encargado:", style = MaterialTheme.typography.labelLarge)
                    Box {
                        OutlinedButton(onClick = { expandedEncargado = true }, modifier = Modifier.fillMaxWidth()) {
                            val enc = empleados.find { it.id == encargadoId }
                            Text(enc?.let { "${it.nombre} ${it.apellido}" } ?: "Seleccionar Encargado")
                        }
                        DropdownMenu(expanded = expandedEncargado, onDismissRequest = { expandedEncargado = false }) {
                            empleados.forEach { emp ->
                                DropdownMenuItem(
                                    text = { Text("${emp.nombre} ${emp.apellido}") },
                                    onClick = {
                                        encargadoId = emp.id
                                        expandedEncargado = false
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    Text("Asignar Personal:", style = MaterialTheme.typography.labelLarge)
                    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        empleados.forEach { emp ->
                            FilterChip(
                                selected = selectedPersonalIds.contains(emp.id),
                                onClick = {
                                    if (selectedPersonalIds.contains(emp.id)) {
                                        selectedPersonalIds -= emp.id
                                    } else {
                                        selectedPersonalIds += emp.id
                                    }
                                },
                                label = { Text("${emp.nombre} ${emp.apellido.firstOrNull() ?: ""}.") }
                            )
                        }
                    }
                }

                item {
                    TextField(
                        value = cajaInicial, 
                        onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) cajaInicial = it }, 
                        label = { Text("Efectivo Inicial (Caja)") }, 
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    TextField(
                        value = tarjeta, 
                        onValueChange = { if (it.length <= 16) tarjeta = it }, 
                        label = { Text("Número Tarjeta (16 dígitos)") }, 
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = usaHorarioEmpresa, onCheckedChange = { usaHorarioEmpresa = it })
                        Text("Usar Horario de Empresa")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        (montaje ?: Montaje()).copy(
                            nombre = nombre,
                            ubicacion = ubicacion,
                            encargadoId = encargadoId,
                            personalAsignadoIds = selectedPersonalIds.toList(),
                            cajaEfectivoInicial = cajaInicial.toDoubleOrNull() ?: 0.0,
                            numeroTarjeta = tarjeta,
                            usaHorarioEmpresa = usaHorarioEmpresa,
                            fechaInicio = inicioPickerState.selectedDateMillis?.let { Timestamp(Date(it)) } ?: Timestamp.now(),
                            fechaFinalEstimada = finalPickerState.selectedDateMillis?.let { Timestamp(Date(it)) } ?: Timestamp.now()
                        )
                    )
                },
                enabled = nombre.isNotBlank() && encargadoId.isNotBlank()
            ) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )

    if (showInicioPicker) {
        DatePickerDialog(
            onDismissRequest = { showInicioPicker = false },
            confirmButton = {
                TextButton(onClick = { showInicioPicker = false }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { showInicioPicker = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = inicioPickerState)
        }
    }

    if (showFinalPicker) {
        DatePickerDialog(
            onDismissRequest = { showFinalPicker = false },
            confirmButton = {
                TextButton(onClick = { showFinalPicker = false }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { showFinalPicker = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = finalPickerState)
        }
    }
}
