package com.example.montaje_tamer.ui.screens

import androidx.compose.foundation.clickable
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
fun GastoScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()
    val montajes by viewModel.montajes.collectAsState()
    val gastos by viewModel.gastos.collectAsState()
    val empleados by viewModel.empleados.collectAsState()
    
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    // Filtrar montajes accesibles
    val montajesVisibles = montajes.filter { 
        currentUser?.rol in listOf(Rol.DIRECTIVO, Rol.ADMINISTRATIVO) || 
        it.encargadoId == currentUser?.id || 
        it.personalAsignadoIds.contains(currentUser?.id)
    }

    val montajesActivos = montajesVisibles.filter { it.estado != "FINALIZADO" }
    val montajesFinalizados = montajesVisibles.filter { it.estado == "FINALIZADO" }

    var selectedMontaje by remember { mutableStateOf<Montaje?>(null) }
    var showAddGastoDialog by remember { mutableStateOf(false) }
    var showSolicitarEfectivoDialog by remember { mutableStateOf(false) }
    var showFinishedPicker by remember { mutableStateOf(false) }

    val uiMessage by viewModel.uiMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiMessage) {
        uiMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearUiMessage()
        }
    }

    LaunchedEffect(selectedMontaje) {
        selectedMontaje?.let { viewModel.loadGastosPorMontaje(it.id) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Gestión de Gastos") },
                actions = {
                    if (selectedMontaje != null) {
                        IconButton(onClick = { selectedMontaje = null }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                        }
                    }
                    if (montajesFinalizados.isNotEmpty()) {
                        TextButton(onClick = { showFinishedPicker = true }) {
                            Text("SELECCIONAR REGISTRO")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (selectedMontaje != null && selectedMontaje?.estado != "FINALIZADO") {
                Column(horizontalAlignment = Alignment.End) {
                    if (currentUser?.id == selectedMontaje?.encargadoId || currentUser?.rol in listOf(Rol.DIRECTIVO, Rol.ADMINISTRATIVO)) {
                        SmallFloatingActionButton(
                            onClick = { showSolicitarEfectivoDialog = true },
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Icon(Icons.Default.MonetizationOn, "Solicitar Efectivo")
                        }
                    }
                    FloatingActionButton(onClick = { showAddGastoDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Nuevo Gasto")
                    }
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
                    Text("Montajes Activos", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                }
                if (montajesActivos.isEmpty()) {
                    item { Text("No hay montajes activos", style = MaterialTheme.typography.bodyMedium) }
                } else {
                    items(montajesActivos) { montaje ->
                        val encargado = empleados.find { it.id == montaje.encargadoId }
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { selectedMontaje = montaje },
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(montaje.nombre, style = MaterialTheme.typography.titleMedium)
                                Text("Responsable: ${encargado?.nombre ?: "Sin asignar"} ${encargado?.apellido ?: ""}", style = MaterialTheme.typography.bodySmall)
                                val inicioStr = montaje.fechaInicio?.let { sdf.format(it.toDate()) } ?: "N/A"
                                val finStr = if (montaje.estado == "FINALIZADO") {
                                    montaje.fechaFinalReal?.let { sdf.format(it.toDate()) } ?: "N/A"
                                } else {
                                    montaje.fechaFinalEstimada?.let { sdf.format(it.toDate()) } ?: "En curso"
                                }
                                Text("Fechas: $inicioStr - $finStr", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                val encargado = empleados.find { it.id == selectedMontaje!!.encargadoId }
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(selectedMontaje!!.nombre, style = MaterialTheme.typography.headlineSmall)
                        Text("Responsable: ${encargado?.nombre} ${encargado?.apellido}")
                        
                        val totalEfectivo = selectedMontaje!!.cajaEfectivoInicial + selectedMontaje!!.registrosEfectivo.sumOf { it.monto }
                        val gastosEfectivo = gastos.filter { it.tipoPago == "EFECTIVO" }.sumOf { it.totalGasto }
                        val saldoActual = totalEfectivo - gastosEfectivo
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Caja Efectivo:", style = MaterialTheme.typography.labelMedium)
                                Text("$${String.format("%.2f", saldoActual)}", style = MaterialTheme.typography.titleLarge, color = if (saldoActual < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Gastos Tarjeta:", style = MaterialTheme.typography.labelMedium)
                                val gastosTarjeta = gastos.filter { it.tipoPago == "TARJETA" }.sumOf { it.totalGasto }
                                Text("$${String.format("%.2f", gastosTarjeta)}", style = MaterialTheme.typography.titleMedium)
                            }
                        }

                        val inicioStr = selectedMontaje!!.fechaInicio?.let { sdf.format(it.toDate()) } ?: "N/A"
                        val finStr = if (selectedMontaje!!.estado == "FINALIZADO") {
                            selectedMontaje!!.fechaFinalReal?.let { sdf.format(it.toDate()) } ?: "N/A"
                        } else {
                            selectedMontaje!!.fechaFinalEstimada?.let { sdf.format(it.toDate()) } ?: "En curso"
                        }
                        Text("Período: $inicioStr al $finStr", style = MaterialTheme.typography.bodySmall)
                        Text("Estado: ${selectedMontaje!!.estado}", style = MaterialTheme.typography.bodySmall)
                    }
                }
                
                LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { Text("Gastos Registrados", style = MaterialTheme.typography.titleMedium) }
                    if (gastos.isEmpty()) {
                        item { Text("No hay gastos registrados", style = MaterialTheme.typography.bodySmall) }
                    } else {
                        items(gastos) { gasto ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(gasto.detalle, style = MaterialTheme.typography.titleMedium)
                                    Text("Monto: $${String.format("%.2f", gasto.totalGasto)} (${gasto.tipoPago})")
                                    Text("Fecha: ${sdf.format(gasto.fecha.toDate())}", style = MaterialTheme.typography.bodySmall)
                                    Text("Comprobante: ${gasto.numeroComprobante}", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }

        if (showFinishedPicker) {
            AlertDialog(
                onDismissRequest = { showFinishedPicker = false },
                title = { Text("Seleccionar Registro Finalizado") },
                text = {
                    Box(modifier = Modifier.heightIn(max = 400.dp)) {
                        LazyColumn {
                            items(montajesFinalizados) { montaje ->
                                val encargado = empleados.find { it.id == montaje.encargadoId }
                                ListItem(
                                    headlineContent = { Text(montaje.nombre) },
                                    supportingContent = { 
                                        Text("Responsable: ${encargado?.nombre} ${encargado?.apellido}\n" +
                                             "Finalizado: ${montaje.fechaFinalReal?.let { sdf.format(it.toDate()) } ?: "N/A"}")
                                    },
                                    modifier = Modifier.clickable {
                                        selectedMontaje = montaje
                                        showFinishedPicker = false
                                    }
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showFinishedPicker = false }) { Text("Cerrar") }
                }
            )
        }

        if (showAddGastoDialog && selectedMontaje != null) {
            AddGastoDialog(
                montaje = selectedMontaje!!,
                onDismiss = { showAddGastoDialog = false },
                onConfirm = { nuevoGasto ->
                    viewModel.addGasto(context, nuevoGasto.copy(personalEncargadoId = currentUser?.id ?: ""))
                    showAddGastoDialog = false
                }
            )
        }

        if (showSolicitarEfectivoDialog && selectedMontaje != null) {
            SolicitarEfectivoDialog(
                montaje = selectedMontaje!!,
                onDismiss = { showSolicitarEfectivoDialog = false },
                onConfirm = { monto, motivo ->
                    viewModel.solicitarEfectivo(
                        context,
                        SolicitudEfectivo(
                            montajeId = selectedMontaje!!.id,
                            solicitadoPorId = currentUser?.id ?: "",
                            monto = monto,
                            motivo = motivo
                        )
                    )
                    showSolicitarEfectivoDialog = false
                }
            )
        }
    }
}

@Composable
fun SolicitarEfectivoDialog(montaje: Montaje, onDismiss: () -> Unit, onConfirm: (Double, String) -> Unit) {
    var monto by remember { mutableStateOf("") }
    var motivo by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Solicitar Efectivo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Para el montaje: ${montaje.nombre}")
                TextField(
                    value = monto,
                    onValueChange = { monto = it },
                    label = { Text("Monto solicitado") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                TextField(
                    value = motivo,
                    onValueChange = { motivo = it },
                    label = { Text("Motivo / Detalle") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(monto.toDoubleOrNull() ?: 0.0, motivo) }, enabled = monto.isNotBlank()) {
                Text("Enviar Solicitud")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
fun AddGastoDialog(montaje: Montaje, onDismiss: () -> Unit, onConfirm: (Gasto) -> Unit) {
    var detalle by remember { mutableStateOf("") }
    var monto by remember { mutableStateOf("") }
    var comprobante by remember { mutableStateOf("") }
    var tipoPago by remember { mutableStateOf("EFECTIVO") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuevo Gasto en ${montaje.nombre}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextField(value = detalle, onValueChange = { detalle = it }, label = { Text("Detalle del gasto") }, modifier = Modifier.fillMaxWidth())
                TextField(
                    value = monto, 
                    onValueChange = { monto = it }, 
                    label = { Text("Monto Total") }, 
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                TextField(value = comprobante, onValueChange = { comprobante = it }, label = { Text("Número de Comprobante") }, modifier = Modifier.fillMaxWidth())
                
                Text("Tipo de Pago:")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = tipoPago == "EFECTIVO", onClick = { tipoPago = "EFECTIVO" })
                    Text("Efectivo")
                    Spacer(modifier = Modifier.width(16.dp))
                    RadioButton(selected = tipoPago == "TARJETA", onClick = { tipoPago = "TARJETA" })
                    Text("Tarjeta")
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onConfirm(Gasto(
                    lugarMontajeId = montaje.id,
                    detalle = detalle,
                    totalGasto = monto.toDoubleOrNull() ?: 0.0,
                    numeroComprobante = comprobante,
                    tipoPago = tipoPago,
                    fecha = Timestamp.now()
                ))
            }, enabled = detalle.isNotBlank() && monto.isNotBlank()) { Text("Registrar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
