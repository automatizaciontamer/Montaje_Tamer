package com.example.montaje_tamer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.example.montaje_tamer.model.ConfiguracionEmpresa
import com.example.montaje_tamer.viewmodel.MainViewModel
import com.google.firebase.Timestamp
import java.util.*
import java.text.SimpleDateFormat
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val config by viewModel.configuracion.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    
    // Default hours if config is null
    val diasSemana = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")
    val initialHorarios = config?.horarios ?: mapOf(
        "Lunes" to "08:00 - 17:00",
        "Martes" to "08:00 - 17:00",
        "Miércoles" to "08:00 - 17:00",
        "Jueves" to "08:00 - 17:00",
        "Viernes" to "08:00 - 16:00",
        "Sábado" to "No Laboral",
        "Domingo" to "No Laboral"
    )

    var horariosState by remember { mutableStateOf(initialHorarios) }
    
    LaunchedEffect(config) {
        config?.let { horariosState = it.horarios }
    }

    Scaffold(
        topBar = { 
            TopAppBar(
                title = { Text("Configuración de Empresa") },
                navigationIcon = {
                    IconButton(onClick = { /* Debería volver o abrir menú */ }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Horarios Laborales por Defecto", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(diasSemana) { dia ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextField(
                        value = horariosState[dia] ?: "No Laboral",
                        onValueChange = { newValue ->
                            val updated = horariosState.toMutableMap()
                            updated[dia] = newValue
                            horariosState = updated
                        },
                        label = { Text(dia) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Feriados y Días No Laborales", style = MaterialTheme.typography.titleLarge)
            }

            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            items(config?.feriados ?: emptyList()) { feriado ->
                ListItem(
                    headlineContent = { Text(sdf.format(feriado.toDate())) },
                    trailingContent = {
                        IconButton(onClick = {
                            val newList = config?.feriados?.filter { it != feriado } ?: emptyList()
                            viewModel.saveConfiguracion(context, config!!.copy(feriados = newList))
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                )
            }

            item {
                Button(
                    onClick = { 
                        // Implementar un DatePicker real sería ideal
                        val today = Timestamp.now()
                        val newList = (config?.feriados ?: emptyList()) + today
                        viewModel.saveConfiguracion(context, (config ?: ConfiguracionEmpresa()).copy(feriados = newList))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Agregar Feriado (Hoy)")
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        viewModel.saveConfiguracion(context, (config ?: ConfiguracionEmpresa()).copy(
                            horarios = horariosState
                        ))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                ) {
                    Text("Guardar Cambios de Configuración")
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
