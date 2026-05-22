package com.example.montaje_tamer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.montaje_tamer.model.Empleado
import com.example.montaje_tamer.model.Rol
import com.example.montaje_tamer.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()
    val empleados by viewModel.empleados.collectAsState()
    
    var searchQuery by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }
    var selectedEmpleado by remember { mutableStateOf<Empleado?>(null) }

    val filteredEmpleados = empleados.filter {
        it.nombre.contains(searchQuery, ignoreCase = true) ||
        it.apellido.contains(searchQuery, ignoreCase = true) ||
        it.legajo.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(title = { Text("Gestión de Personal") })
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Buscar por nombre, apellido o legajo...") },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    singleLine = true
                )
            }
        },
        floatingActionButton = {
            if (currentUser?.rol in listOf(Rol.DIRECTIVO, Rol.ADMINISTRATIVO, Rol.SUPERVISOR)) {
                FloatingActionButton(onClick = { 
                    selectedEmpleado = null
                    showDialog = true 
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Agregar")
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredEmpleados) { empleado ->
                EmpleadoCard(
                    empleado = empleado,
                    onEdit = {
                        selectedEmpleado = it
                        showDialog = true
                    },
                    onDelete = {
                        viewModel.deleteEmpleado(context, it.id)
                    },
                    onExportPdf = {
                        viewModel.exportHorarioPersonalPdf(context, it)
                    },
                    canEdit = currentUser?.rol in listOf(Rol.DIRECTIVO, Rol.ADMINISTRATIVO),
                    canExport = currentUser?.rol in listOf(Rol.DIRECTIVO, Rol.ADMINISTRATIVO)
                )
            }
        }

        if (showDialog) {
            EmployeeDialog(
                empleado = selectedEmpleado,
                onDismiss = { showDialog = false },
                onConfirm = { nuevoEmpleado ->
                    viewModel.saveEmpleado(context, nuevoEmpleado)
                    showDialog = false
                }
            )
        }
    }
}

@Composable
fun EmpleadoCard(
    empleado: Empleado,
    onEdit: (Empleado) -> Unit,
    onDelete: (Empleado) -> Unit,
    onExportPdf: (Empleado) -> Unit,
    canEdit: Boolean,
    canExport: Boolean
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "${empleado.nombre} ${empleado.apellido}", style = MaterialTheme.typography.titleMedium)
                Text(text = "Legajo: ${empleado.legajo}", style = MaterialTheme.typography.bodySmall)
                Text(text = "Rol: ${empleado.rol}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
            }
            if (canExport) {
                IconButton(onClick = { onExportPdf(empleado) }) { Icon(Icons.Default.PictureAsPdf, "Exportar PDF") }
            }
            if (canEdit) {
                IconButton(onClick = { onEdit(empleado) }) { Icon(Icons.Default.Edit, "Editar") }
                IconButton(onClick = { onDelete(empleado) }) { Icon(Icons.Default.Delete, "Eliminar", tint = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeDialog(empleado: Empleado?, onDismiss: () -> Unit, onConfirm: (Empleado) -> Unit) {
    var usuario by remember { mutableStateOf(empleado?.usuario ?: "") }
    var clave by remember { mutableStateOf(empleado?.clave ?: "14569") }
    var nombre by remember { mutableStateOf(empleado?.nombre ?: "") }
    var apellido by remember { mutableStateOf(empleado?.apellido ?: "") }
    var legajo by remember { mutableStateOf(empleado?.legajo ?: "") }
    var categoria by remember { mutableStateOf(empleado?.categoria ?: "") }
    var tipo by remember { mutableStateOf(empleado?.tipoLiquidacion ?: "Mensual") }
    var rol by remember { mutableStateOf(empleado?.rol ?: Rol.EMPLEADO) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (empleado == null) "Nuevo Empleado" else "Editar Empleado") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { TextField(value = usuario, onValueChange = { usuario = it }, label = { Text("Usuario") }, modifier = Modifier.fillMaxWidth()) }
                item { TextField(value = clave, onValueChange = { clave = it }, label = { Text("Clave") }, modifier = Modifier.fillMaxWidth()) }
                item { TextField(value = nombre, onValueChange = { nombre = it }, label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth()) }
                item { TextField(value = apellido, onValueChange = { apellido = it }, label = { Text("Apellido") }, modifier = Modifier.fillMaxWidth()) }
                item { TextField(value = legajo, onValueChange = { legajo = it }, label = { Text("Legajo") }, modifier = Modifier.fillMaxWidth()) }
                item { TextField(value = categoria, onValueChange = { categoria = it }, label = { Text("Categoría") }, modifier = Modifier.fillMaxWidth()) }
                
                item {
                    Text("Rol:", style = MaterialTheme.typography.labelLarge)
                    Box {
                        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(rol.name)
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            Rol.values().forEach { r ->
                                DropdownMenuItem(
                                    text = { Text(r.name) },
                                    onClick = {
                                        rol = r
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    Text("Liquidación:", style = MaterialTheme.typography.labelLarge)
                    Row {
                        RadioButton(selected = tipo == "Mensual", onClick = { tipo = "Mensual" })
                        Text("Mensual", modifier = Modifier.padding(top = 12.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        RadioButton(selected = tipo == "Quincenal", onClick = { tipo = "Quincenal" })
                        Text("Quincenal", modifier = Modifier.padding(top = 12.dp))
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onConfirm((empleado ?: Empleado()).copy(
                    usuario = usuario,
                    clave = clave,
                    nombre = nombre, 
                    apellido = apellido, 
                    legajo = legajo, 
                    categoria = categoria, 
                    tipoLiquidacion = tipo,
                    rol = rol
                ))
            }) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
