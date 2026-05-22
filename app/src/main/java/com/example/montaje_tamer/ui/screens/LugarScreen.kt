package com.example.montaje_tamer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.montaje_tamer.model.Montaje
import com.example.montaje_tamer.viewmodel.MainViewModel
import com.google.firebase.Timestamp
import java.util.*

import com.example.montaje_tamer.model.Rol

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LugarScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()
    val lugares by viewModel.montajes.collectAsState()
    var showDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Lugares de Montaje") }) },
        floatingActionButton = {
            if (currentUser?.rol == Rol.DIRECTIVO || currentUser?.rol == Rol.SUPERVISOR) {
                FloatingActionButton(onClick = { showDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Agregar")
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(lugares) { lugar ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = lugar.nombre, style = MaterialTheme.typography.titleMedium)
                        Text(text = "Ubicación: ${lugar.ubicacion}")
                        Text(text = "Inicio: ${lugar.fechaInicio?.toDate() ?: "N/A"}")
                        Text(text = "Fin: ${lugar.fechaFinalEstimada?.toDate() ?: "N/A"}")
                    }
                }
            }
        }

        if (showDialog) {
            AddLugarDialog(
                onDismiss = { showDialog = false },
                onConfirm = { nuevoLugar ->
                    viewModel.saveMontaje(context, nuevoLugar)
                    showDialog = false
                }
            )
        }
    }
}

@Composable
fun AddLugarDialog(onDismiss: () -> Unit, onConfirm: (Montaje) -> Unit) {
    var nombre by remember { mutableStateOf("") }
    var ubicacion by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuevo Lugar de Montaje") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextField(value = nombre, onValueChange = { nombre = it }, label = { Text("Nombre Planta") })
                TextField(value = ubicacion, onValueChange = { ubicacion = it }, label = { Text("Ubicación") })
            }
        },
        confirmButton = {
            Button(onClick = {
                onConfirm(Montaje(
                    nombre = nombre,
                    ubicacion = ubicacion,
                    fechaInicio = Timestamp.now()
                ))
            }) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
