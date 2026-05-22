package com.example.montaje_tamer.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.montaje_tamer.model.*
import com.example.montaje_tamer.viewmodel.MainViewModel
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HorarioScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()
    val montajes by viewModel.montajes.collectAsState()
    val empleados by viewModel.empleados.collectAsState()
    
    val montajesAccesibles = montajes.filter { 
        currentUser?.rol in listOf(Rol.DIRECTIVO, Rol.ADMINISTRATIVO) || it.encargadoId == currentUser?.id 
    }

    val montajesActivos = montajesAccesibles.filter { it.estado != "FINALIZADO" }

    var selectedMontaje by remember { mutableStateOf<Montaje?>(null) }
    var showMassiveNormalDialog by remember { mutableStateOf(false) }
    var showMassiveTravelDialog by remember { mutableStateOf(false) }
    var showRequestDialog by remember { mutableStateOf(false) }
    var selectedEmpleadoForNewRecord by remember { mutableStateOf<Empleado?>(null) }
    var selectedHorarioForEdit by remember { mutableStateOf<Horario?>(null) }
    var showIndividualDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf<Horario?>(null) }

    val uiMessage by viewModel.uiMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiMessage) {
        uiMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearUiMessage()
        }
    }

    LaunchedEffect(selectedMontaje) {
        selectedMontaje?.let { viewModel.loadHorariosPorMontaje(it.id) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Control Horario") },
                actions = {
                    if (selectedMontaje != null) {
                        IconButton(onClick = { selectedMontaje = null }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                if (selectedMontaje != null && (currentUser?.rol in listOf(Rol.DIRECTIVO, Rol.ADMINISTRATIVO))) {
                    SmallFloatingActionButton(
                        onClick = { viewModel.exportHorariosMontajePdf(context, selectedMontaje!!) },
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, "Exportar PDF Montaje")
                    }
                }
                if (selectedMontaje != null && selectedMontaje?.estado != "FINALIZADO") {
                    SmallFloatingActionButton(
                        onClick = { showRequestDialog = true },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Icon(Icons.Default.EventBusy, "Solicitar Permiso")
                    }
                }
                if (selectedMontaje != null && selectedMontaje?.estado != "FINALIZADO" && 
                    (currentUser?.id == selectedMontaje?.encargadoId || currentUser?.rol in listOf(Rol.DIRECTIVO, Rol.ADMINISTRATIVO))) {
                    ExtendedFloatingActionButton(
                        onClick = { showMassiveNormalDialog = true },
                        icon = { Icon(Icons.Default.Schedule, null) },
                        text = { Text("Carga Normal") },
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    ExtendedFloatingActionButton(
                        onClick = { showMassiveTravelDialog = true },
                        icon = { Icon(Icons.Default.FlightTakeoff, null) },
                        text = { Text("Carga Viaje") }
                    )
                }
            }
        }
    ) { padding ->
        if (selectedMontaje == null) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text("Seleccionar Montaje Activo", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                }
                items(montajesActivos) { montaje ->
                    val encargado = empleados.find { it.id == montaje.encargadoId }
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { selectedMontaje = montaje },
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(montaje.nombre, style = MaterialTheme.typography.titleMedium)
                            Text("Responsable: ${encargado?.nombre ?: "Sin asignar"} ${encargado?.apellido ?: ""}", style = MaterialTheme.typography.bodySmall)
                            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                            val inicioStr = montaje.fechaInicio?.let { sdf.format(it.toDate()) } ?: "N/A"
                            val finStr = montaje.fechaFinalEstimada?.let { sdf.format(it.toDate()) } ?: "En curso"
                            Text("Fechas: $inicioStr - $finStr", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        } else {
            val personalAsignado = empleados.filter { it.id in selectedMontaje!!.personalAsignadoIds }
            
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                val encargado = empleados.find { it.id == selectedMontaje!!.encargadoId }
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(selectedMontaje!!.nombre, style = MaterialTheme.typography.titleLarge)
                        Text("Responsable: ${encargado?.nombre} ${encargado?.apellido}")
                        Text("Personal asignado: ${personalAsignado.size}")
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(personalAsignado) { empleado ->
                        EmpleadoHorarioItem(
                            empleado = empleado, 
                            viewModel = viewModel, 
                            montaje = selectedMontaje!!,
                            canEdit = currentUser?.rol !in listOf(Rol.ADMINISTRATIVO) && selectedMontaje?.estado != "FINALIZADO",
                            onNewRecord = {
                                selectedEmpleadoForNewRecord = empleado
                                selectedHorarioForEdit = null
                                showIndividualDialog = true
                            },
                            onEditRecord = { horario ->
                                selectedEmpleadoForNewRecord = empleado
                                selectedHorarioForEdit = horario
                                showIndividualDialog = true
                            },
                            onDeleteRecord = { horario ->
                                showDeleteConfirm = horario
                            }
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }

        if (showIndividualDialog && selectedEmpleadoForNewRecord != null && selectedMontaje != null) {
            IndividualHorarioDialog(
                empleado = selectedEmpleadoForNewRecord!!,
                montaje = selectedMontaje!!,
                existingHorario = selectedHorarioForEdit,
                onDismiss = { 
                    showIndividualDialog = false
                    selectedEmpleadoForNewRecord = null
                    selectedHorarioForEdit = null
                },
                onConfirm = { horario ->
                    viewModel.saveHorario(context, horario)
                    showIndividualDialog = false
                    selectedEmpleadoForNewRecord = null
                    selectedHorarioForEdit = null
                }
            )
        }

        if (showDeleteConfirm != null) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = null },
                title = { Text("Eliminar Registro") },
                text = { Text("¿Está seguro que desea eliminar este registro de horario?") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteHorario(context, showDeleteConfirm!!)
                            showDeleteConfirm = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { Text("Eliminar") }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = null }) { Text("Cancelar") }
                }
            )
        }

        if (showRequestDialog && selectedMontaje != null) {
            SolicitudEspecialDialog(
                montaje = selectedMontaje!!,
                onDismiss = { showRequestDialog = false },
                onConfirm = { motivo ->
                    viewModel.addSolicitudEspecial(
                        SolicitudTrabajoEspecial(
                            empleadoId = currentUser?.id ?: "",
                            montajeId = selectedMontaje!!.id,
                            fechaTrabajo = Timestamp.now(), // Debería ser un DatePicker
                            motivo = motivo
                        )
                    )
                    showRequestDialog = false
                }
            )
        }

        if (showMassiveNormalDialog && selectedMontaje != null) {
            MassiveNormalDialog(
                montaje = selectedMontaje!!,
                onDismiss = { showMassiveNormalDialog = false },
                onConfirm = { fecha, ent, sal ->
                    viewModel.saveHorariosMasivos(
                        context = context,
                        lugarId = selectedMontaje!!.id,
                        fecha = fecha,
                        entrada = ent,
                        salida = sal,
                        vSale = "",
                        vLlega = "",
                        empleadosIds = emptyList()
                    )
                    showMassiveNormalDialog = false
                }
            )
        }

        if (showMassiveTravelDialog && selectedMontaje != null) {
            MassiveTravelDialog(
                montaje = selectedMontaje!!,
                onDismiss = { showMassiveTravelDialog = false },
                onConfirm = { fecha, vSaleFull, vLlegaFull ->
                    viewModel.saveHorariosMasivos(
                        context = context,
                        lugarId = selectedMontaje!!.id,
                        fecha = fecha,
                        entrada = "",
                        salida = "",
                        vSale = "",
                        vLlega = "",
                        empleadosIds = emptyList(),
                        viajeSaleFull = vSaleFull,
                        viajeLlegaFull = vLlegaFull
                    )
                    showMassiveTravelDialog = false
                }
            )
        }
    }
}

@Composable
fun SolicitudEspecialDialog(
    montaje: Montaje,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var motivo by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Solicitar Trabajo Especial") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Solicitar permiso para trabajar en feriado o fuera de horario en: ${montaje.nombre}")
                TextField(
                    value = motivo,
                    onValueChange = { motivo = it },
                    label = { Text("Motivo / Justificación") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(motivo) }, enabled = motivo.isNotBlank()) {
                Text("Enviar Solicitud")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
fun EmpleadoHorarioItem(
    empleado: Empleado, 
    viewModel: MainViewModel, 
    montaje: Montaje,
    canEdit: Boolean,
    onNewRecord: () -> Unit,
    onEditRecord: (Horario) -> Unit,
    onDeleteRecord: (Horario) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val horarios by viewModel.horarios.collectAsState()
    val personalHorarios = horarios.filter { it.empleadoId == empleado.id && it.lugarMontajeId == montaje.id }
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    
    Card(
        modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Person, null, modifier = Modifier.size(40.dp))
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("${empleado.nombre} ${empleado.apellido}", style = MaterialTheme.typography.titleMedium)
                    Text("Legajo: ${empleado.legajo}", style = MaterialTheme.typography.bodySmall)
                }
                val currentUser by viewModel.currentUser.collectAsState()
                if (currentUser?.rol in listOf(Rol.DIRECTIVO, Rol.ADMINISTRATIVO)) {
                    IconButton(onClick = { viewModel.exportHorarioPersonalPdf(context, empleado) }) {
                        Icon(Icons.Default.PictureAsPdf, "Exportar PDF Individual")
                    }
                }
                Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
            }
            
            if (expanded) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                Text("Registros de Horario:", style = MaterialTheme.typography.labelLarge)
                
                if (personalHorarios.isEmpty()) {
                    Text("No hay registros para este montaje", style = MaterialTheme.typography.bodySmall)
                } else {
                    personalHorarios.sortedByDescending { it.fecha }.forEach { h ->
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            val fechaStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(h.fecha.toDate())
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Fecha: $fechaStr", style = MaterialTheme.typography.labelSmall)
                                    if (h.horaEntrada != null || h.horaSalida != null) {
                                        Text("Jornada: ${h.horaEntrada?.let { SimpleDateFormat("HH:mm", Locale.getDefault()).format(it.toDate()) } ?: "--:--"} - ${h.horaSalida?.let { SimpleDateFormat("HH:mm", Locale.getDefault()).format(it.toDate()) } ?: "--:--"}", 
                                            style = MaterialTheme.typography.bodySmall)
                                    }
                                    if (h.viajeSale != null || h.viajeLlega != null) {
                                        Text("Viaje: ${h.viajeSale?.let { sdf.format(it.toDate()) } ?: "--:--"} - ${h.viajeLlega?.let { sdf.format(it.toDate()) } ?: "--:--"}", 
                                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                                if (canEdit) {
                                    Row {
                                        IconButton(onClick = { onEditRecord(h) }) {
                                            Icon(Icons.Default.Edit, "Editar", modifier = Modifier.size(18.dp))
                                        }
                                        IconButton(onClick = { onDeleteRecord(h) }) {
                                            Icon(Icons.Default.Delete, "Eliminar", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                            HorizontalDivider(modifier = Modifier.padding(top = 4.dp), thickness = 0.5.dp)
                        }
                    }
                }
                
                if (canEdit) {
                    Button(
                        onClick = onNewRecord,
                        modifier = Modifier.align(Alignment.End).padding(top = 8.dp)
                    ) {
                        Text("Nuevo Registro")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MassiveNormalDialog(
    montaje: Montaje,
    onDismiss: () -> Unit,
    onConfirm: (Timestamp, String, String) -> Unit
) {
    var hEntrada by remember { mutableStateOf("08:00") }
    var hSalida by remember { mutableStateOf("17:00") }
    
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())
    var showDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Carga Masiva Normal - ${montaje.nombre}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Carga de horario laboral para todo el personal asignado.")
                
                OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                    val date = datePickerState.selectedDateMillis?.let { Date(it) } ?: Date()
                    Text("Día: ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date)}")
                }

                TextField(value = hEntrada, onValueChange = { hEntrada = it }, label = { Text("Entrada (HH:mm)") }, modifier = Modifier.fillMaxWidth())
                TextField(value = hSalida, onValueChange = { hSalida = it }, label = { Text("Salida (HH:mm)") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = { 
                val timestamp = Timestamp(Date(datePickerState.selectedDateMillis ?: System.currentTimeMillis()))
                onConfirm(timestamp, hEntrada, hSalida)
            }) { Text("Aplicar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = { TextButton(onClick = { showDatePicker = false }) { Text("Aceptar") } }
        ) { DatePicker(state = datePickerState) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IndividualHorarioDialog(
    empleado: Empleado,
    montaje: Montaje,
    existingHorario: Horario? = null,
    onDismiss: () -> Unit,
    onConfirm: (Horario) -> Unit
) {
    var type by remember { mutableStateOf(if (existingHorario?.viajeSale != null) "VIAJE" else "NORMAL") }
    var hEntrada by remember { mutableStateOf(existingHorario?.horaEntrada?.let { SimpleDateFormat("HH:mm", Locale.getDefault()).format(it.toDate()) } ?: "08:00") }
    var hSalida by remember { mutableStateOf(existingHorario?.horaSalida?.let { SimpleDateFormat("HH:mm", Locale.getDefault()).format(it.toDate()) } ?: "17:00") }
    var hSale by remember { mutableStateOf(existingHorario?.viajeSale?.let { SimpleDateFormat("HH:mm", Locale.getDefault()).format(it.toDate()) } ?: "07:00") }
    var hLlega by remember { mutableStateOf(existingHorario?.viajeLlega?.let { SimpleDateFormat("HH:mm", Locale.getDefault()).format(it.toDate()) } ?: "18:00") }
    
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = existingHorario?.fecha?.toDate()?.time ?: System.currentTimeMillis())
    val travelLlegaDatePickerState = rememberDatePickerState(initialSelectedDateMillis = existingHorario?.viajeLlega?.toDate()?.time ?: existingHorario?.fecha?.toDate()?.time ?: System.currentTimeMillis())
    
    var showDatePicker by remember { mutableStateOf(false) }
    var showTravelLlegaDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existingHorario == null) "Nuevo Registro: ${empleado.nombre}" else "Editar Registro: ${empleado.nombre}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = type == "NORMAL",
                        onClick = { type = "NORMAL" },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) { Text("Normal") }
                    SegmentedButton(
                        selected = type == "VIAJE",
                        onClick = { type = "VIAJE" },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) { Text("Viaje") }
                }

                OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                    val date = datePickerState.selectedDateMillis?.let { Date(it) } ?: Date()
                    Text("Fecha: ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date)}")
                }

                if (type == "NORMAL") {
                    TextField(value = hEntrada, onValueChange = { hEntrada = it }, label = { Text("Entrada (HH:mm)") }, modifier = Modifier.fillMaxWidth())
                    TextField(value = hSalida, onValueChange = { hSalida = it }, label = { Text("Salida (HH:mm)") }, modifier = Modifier.fillMaxWidth())
                } else {
                    TextField(value = hSale, onValueChange = { hSale = it }, label = { Text("Hora Salida (HH:mm)") }, modifier = Modifier.fillMaxWidth())
                    
                    Text("Llegada (si es otro día):", style = MaterialTheme.typography.labelSmall)
                    OutlinedButton(onClick = { showTravelLlegaDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                        val date = travelLlegaDatePickerState.selectedDateMillis?.let { Date(it) } ?: Date()
                        Text("Fecha Llegada: ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date)}")
                    }
                    TextField(value = hLlega, onValueChange = { hLlega = it }, label = { Text("Hora Llegada (HH:mm)") }, modifier = Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {
            Button(onClick = { 
                val baseDateMillis = datePickerState.selectedDateMillis ?: System.currentTimeMillis()
                val baseDate = Timestamp(Date(baseDateMillis))
                
                fun combine(millis: Long, timeStr: String): Timestamp? {
                    val cal = Calendar.getInstance()
                    cal.timeInMillis = millis
                    val cleanTime = timeStr.replace(":", "").replace(".", "")
                    if (cleanTime.length < 4) return null
                    cal.set(Calendar.HOUR_OF_DAY, cleanTime.substring(0, 2).toIntOrNull() ?: 0)
                    cal.set(Calendar.MINUTE, cleanTime.substring(2, 4).toIntOrNull() ?: 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    return Timestamp(cal.time)
                }

                val horario = if (type == "NORMAL") {
                    (existingHorario?.copy(
                        fecha = baseDate,
                        horaEntrada = combine(baseDateMillis, hEntrada),
                        horaSalida = combine(baseDateMillis, hSalida),
                        viajeSale = null,
                        viajeLlega = null
                    ) ?: Horario(
                        empleadoId = empleado.id,
                        lugarMontajeId = montaje.id,
                        fecha = baseDate,
                        horaEntrada = combine(baseDateMillis, hEntrada),
                        horaSalida = combine(baseDateMillis, hSalida)
                    ))
                } else {
                    (existingHorario?.copy(
                        fecha = baseDate,
                        viajeSale = combine(baseDateMillis, hSale),
                        viajeLlega = combine(travelLlegaDatePickerState.selectedDateMillis ?: baseDateMillis, hLlega),
                        horaEntrada = null,
                        horaSalida = null
                    ) ?: Horario(
                        empleadoId = empleado.id,
                        lugarMontajeId = montaje.id,
                        fecha = baseDate,
                        viajeSale = combine(baseDateMillis, hSale),
                        viajeLlega = combine(travelLlegaDatePickerState.selectedDateMillis ?: baseDateMillis, hLlega)
                    ))
                }
                onConfirm(horario)
            }) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = { TextButton(onClick = { showDatePicker = false }) { Text("Aceptar") } }
        ) { DatePicker(state = datePickerState) }
    }
    if (showTravelLlegaDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showTravelLlegaDatePicker = false },
            confirmButton = { TextButton(onClick = { showTravelLlegaDatePicker = false }) { Text("Aceptar") } }
        ) { DatePicker(state = travelLlegaDatePickerState) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MassiveTravelDialog(
    montaje: Montaje,
    onDismiss: () -> Unit,
    onConfirm: (Timestamp, Timestamp, Timestamp) -> Unit
) {
    var hSale by remember { mutableStateOf("07:00") }
    var hLlega by remember { mutableStateOf("18:00") }
    
    val saleDatePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())
    val llegaDatePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())
    
    var showSaleDatePicker by remember { mutableStateOf(false) }
    var showLlegaDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Carga Masiva Viaje - ${montaje.nombre}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Carga de horas de viaje para todo el personal asignado.")
                
                Text("Salida:", style = MaterialTheme.typography.labelLarge)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { showSaleDatePicker = true }, modifier = Modifier.weight(1f)) {
                        val date = saleDatePickerState.selectedDateMillis?.let { Date(it) } ?: Date()
                        Text(SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date))
                    }
                    Spacer(Modifier.width(8.dp))
                    TextField(value = hSale, onValueChange = { hSale = it }, label = { Text("HH:mm") }, modifier = Modifier.width(100.dp))
                }

                Text("Llegada:", style = MaterialTheme.typography.labelLarge)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { showLlegaDatePicker = true }, modifier = Modifier.weight(1f)) {
                        val date = llegaDatePickerState.selectedDateMillis?.let { Date(it) } ?: Date()
                        Text(SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date))
                    }
                    Spacer(Modifier.width(8.dp))
                    TextField(value = hLlega, onValueChange = { hLlega = it }, label = { Text("HH:mm") }, modifier = Modifier.width(100.dp))
                }
            }
        },
        confirmButton = {
            Button(onClick = { 
                val baseDate = Timestamp(Date(saleDatePickerState.selectedDateMillis ?: System.currentTimeMillis()))
                
                fun combine(millis: Long?, timeStr: String): Timestamp {
                    val cal = Calendar.getInstance()
                    cal.timeInMillis = millis ?: System.currentTimeMillis()
                    val cleanTime = timeStr.replace(":", "").replace(".", "")
                    if (cleanTime.length >= 4) {
                        cal.set(Calendar.HOUR_OF_DAY, cleanTime.substring(0, 2).toIntOrNull() ?: 0)
                        cal.set(Calendar.MINUTE, cleanTime.substring(2, 4).toIntOrNull() ?: 0)
                    }
                    return Timestamp(cal.time)
                }

                onConfirm(baseDate, combine(saleDatePickerState.selectedDateMillis, hSale), combine(llegaDatePickerState.selectedDateMillis, hLlega))
            }) { Text("Aplicar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )

    if (showSaleDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showSaleDatePicker = false },
            confirmButton = { TextButton(onClick = { showSaleDatePicker = false }) { Text("Aceptar") } }
        ) { DatePicker(state = saleDatePickerState) }
    }
    if (showLlegaDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showLlegaDatePicker = false },
            confirmButton = { TextButton(onClick = { showLlegaDatePicker = false }) { Text("Aceptar") } }
        ) { DatePicker(state = llegaDatePickerState) }
    }
}
