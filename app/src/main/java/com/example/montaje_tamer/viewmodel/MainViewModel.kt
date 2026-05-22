package com.example.montaje_tamer.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.montaje_tamer.model.*
import com.example.montaje_tamer.repository.FirestoreRepository
import com.example.montaje_tamer.utils.FileLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

class MainViewModel : ViewModel() {
    private val repository = FirestoreRepository()
    private val auth = FirebaseAuth.getInstance()

    private val _currentUser = MutableStateFlow<Empleado?>(null)
    val currentUser: StateFlow<Empleado?> = _currentUser.asStateFlow()

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _empleados = MutableStateFlow<List<Empleado>>(emptyList())
    val empleados: StateFlow<List<Empleado>> = _empleados

    private val _montajes = MutableStateFlow<List<Montaje>>(emptyList())
    val montajes: StateFlow<List<Montaje>> = _montajes

    private val _gastos = MutableStateFlow<List<Gasto>>(emptyList())
    val gastos: StateFlow<List<Gasto>> = _gastos

    private val _configuracion = MutableStateFlow<ConfiguracionEmpresa?>(null)
    val configuracion: StateFlow<ConfiguracionEmpresa?> = _configuracion

    private val _horarios = MutableStateFlow<List<Horario>>(emptyList())
    val horarios: StateFlow<List<Horario>> = _horarios.asStateFlow()

    private val _solicitudesEspeciales = MutableStateFlow<List<SolicitudTrabajoEspecial>>(emptyList())
    val solicitudesEspeciales: StateFlow<List<SolicitudTrabajoEspecial>> = _solicitudesEspeciales

    private val _solicitudesExtension = MutableStateFlow<List<SolicitudExtensionMontaje>>(emptyList())
    val solicitudesExtension: StateFlow<List<SolicitudExtensionMontaje>> = _solicitudesExtension

    private val _solicitudesEfectivo = MutableStateFlow<List<SolicitudEfectivo>>(emptyList())
    val solicitudesEfectivo: StateFlow<List<SolicitudEfectivo>> = _solicitudesEfectivo.asStateFlow()

    private val _montajesPendientesConfirmacion = MutableStateFlow<List<Montaje>>(emptyList())
    val montajesPendientesConfirmacion: StateFlow<List<Montaje>> = _montajesPendientesConfirmacion.asStateFlow()

    private val _notificacionesRechazo = MutableStateFlow<List<Pair<Montaje, String>>>(emptyList()) // Montaje y ID del empleado que rechazó
    val notificacionesRechazo: StateFlow<List<Pair<Montaje, String>>> = _notificacionesRechazo.asStateFlow()

    private val _notificationEvent = MutableStateFlow<NotificationData?>(null)
    val notificationEvent = _notificationEvent.asStateFlow()

    private val _uiMessage = MutableStateFlow<String?>(null)
    val uiMessage: StateFlow<String?> = _uiMessage.asStateFlow()

    fun clearUiMessage() { _uiMessage.value = null }

    init {
        checkAuth()
    }

    private fun checkAuth() {
        // No longer using Firebase Auth check here, keeping structure for compatibility
    }

    fun clearNotificationEvent() {
        _notificationEvent.value = null
    }

    fun login(usuario: String, clave: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                // Check for master admin
                if (usuario == "admin" && clave == "14569") {
                    val adminEmpleado = Empleado(
                        id = "master_admin",
                        usuario = "admin",
                        clave = "14569",
                        nombre = "Administrador",
                        apellido = "Maestro",
                        rol = Rol.DIRECTIVO
                    )
                    _currentUser.value = adminEmpleado
                    _isAuthenticated.value = true
                    loadInitialData()
                    onSuccess()
                    return@launch
                }

                // Login manual buscando en la lista de empleados de Firestore
                FileLogger.logInfo("Intentando login manual para usuario: $usuario")
                val empleado = repository.getEmpleadoByUsuario(usuario)
                if (empleado != null && empleado.clave == clave) {
                    FileLogger.logInfo("Login manual exitoso para: ${empleado.nombre}")
                    _currentUser.value = empleado
                    _isAuthenticated.value = true
                    loadInitialData()
                    onSuccess()
                } else {
                    FileLogger.logInfo("Login manual fallido: Usuario no encontrado o clave incorrecta")
                    onError("Usuario o clave incorrectos")
                }
            } catch (e: Exception) {
                onError(e.message ?: "Error al iniciar sesión")
            }
        }
    }

    fun loginWithGoogle(idToken: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val firebaseUser = task.result?.user
                    if (firebaseUser != null) {
                        val emailGoogle = firebaseUser.email ?: ""
                        FileLogger.logInfo("Firebase Auth exitoso con Google: $emailGoogle")
                        viewModelScope.launch {
                            val empleado = repository.getEmpleadoByEmail(emailGoogle)
                            if (empleado != null) {
                                FileLogger.logInfo("Empleado encontrado en Firestore para el email: $emailGoogle (ID: ${empleado.id})")
                                _currentUser.value = empleado
                                _isAuthenticated.value = true
                                loadInitialData()
                                onSuccess()
                            } else {
                                FileLogger.logInfo("ERROR: El email $emailGoogle NO existe en la colección 'empleados'")
                                auth.signOut()
                                onError("El email $emailGoogle no está registrado como personal.")
                            }
                        }
                    }
                } else {
                    onError(task.exception?.message ?: "Error al autenticar con Google")
                }
            }
    }

    fun logout() {
        auth.signOut()
        _currentUser.value = null
        _isAuthenticated.value = false
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _empleados.value = repository.getEmpleados()
            _montajes.value = repository.getMontajes()
            _configuracion.value = repository.getConfiguracion()
            _solicitudesEspeciales.value = repository.getSolicitudesEspecialesPendientes()
            loadAllHorarios()
            startRealtimeListeners()
        }
    }

    private fun startRealtimeListeners() {
        val user = _currentUser.value ?: return

        // Listener para montajes pendientes de mi confirmación
        repository.listenToMontajesPendientes(user.id) { lista ->
            val oldIds = _montajesPendientesConfirmacion.value.map { it.id }.toSet()
            val hasNew = lista.any { it.id !in oldIds }
            if (hasNew) {
                _notificationEvent.value = NotificationData(
                    message = "Has sido asignado a un nuevo montaje. Por favor, confirma tu asistencia.",
                    route = "home"
                )
            }
            _montajesPendientesConfirmacion.value = lista
        }

        // Listener para directivos: ver si alguien rechazó un montaje
        if (user.rol in listOf(Rol.DIRECTIVO, Rol.ADMINISTRATIVO)) {
            repository.listenToMontajes { lista ->
                val rechazos = mutableListOf<Pair<Montaje, String>>()
                lista.forEach { montaje ->
                    montaje.confirmaciones.forEach { (empId, estado) ->
                        if (estado == "RECHAZADO") {
                            rechazos.add(montaje to empId)
                        }
                    }
                }
                
                val currentRechazos = _notificacionesRechazo.value
                val hasNewRechazo = rechazos.any { new -> 
                    !currentRechazos.any { old -> old.first.id == new.first.id && old.second == new.second } 
                }
                
                if (hasNewRechazo) {
                    _notificationEvent.value = NotificationData(
                        message = "Un empleado ha rechazado la asignación a un montaje.",
                        route = "admin"
                    )
                }
                _notificacionesRechazo.value = rechazos
                _montajes.value = lista
            }
        } else {
            repository.listenToMontajes { lista ->
                _montajes.value = lista
            }
        }
        
        // Listener para nuevas solicitudes (para Directivos/Admin)
        if (user.rol in listOf(Rol.DIRECTIVO, Rol.ADMINISTRATIVO)) {
            repository.listenToSolicitudesExtension { nuevasSolicitudes ->
                val oldIds = _solicitudesExtension.value.map { it.id }.toSet()
                val hasNew = nuevasSolicitudes.any { it.id !in oldIds && it.estado == "PENDIENTE" }
                if (hasNew) {
                    _notificationEvent.value = NotificationData(
                        message = "Nueva solicitud de extensión recibida",
                        route = "admin"
                    )
                }
                _solicitudesExtension.value = nuevasSolicitudes.filter { it.estado == "PENDIENTE" }
            }
            repository.listenToSolicitudesEfectivo { nuevasSolicitudes ->
                val oldIds = _solicitudesEfectivo.value.map { it.id }.toSet()
                val hasNew = nuevasSolicitudes.any { it.id !in oldIds && it.estado == "PENDIENTE" }
                if (hasNew) {
                    _notificationEvent.value = NotificationData(
                        message = "Nueva solicitud de efectivo recibida",
                        route = "admin"
                    )
                }
                _solicitudesEfectivo.value = nuevasSolicitudes.filter { it.estado == "PENDIENTE" }
            }
        }

        // Listener para respuestas a mis solicitudes (para el Encargado)
        repository.listenToSolicitudesExtensionPorUsuario(user.id) { misSolicitudes ->
            val respondidaRecientemente = misSolicitudes.any { 
                it.estado != "PENDIENTE" && it.fechaRespuesta != null && 
                (System.currentTimeMillis() - it.fechaRespuesta!!.toDate().time) < 10000 
            }
            if (respondidaRecientemente) {
                _notificationEvent.value = NotificationData(
                    message = "Tu solicitud de extensión ha sido respondida",
                    route = "lugares"
                )
            }
            // Actualizar montajes si hubo aprobación
            viewModelScope.launch {
                _montajes.value = repository.getMontajes()
            }
        }

        repository.listenToSolicitudesEfectivoPorUsuario(user.id) { misSolicitudes ->
            val respondidaRecientemente = misSolicitudes.any {
                it.estado != "PENDIENTE" && it.fechaRespuesta != null &&
                        (System.currentTimeMillis() - it.fechaRespuesta!!.toDate().time) < 10000
            }
            if (respondidaRecientemente) {
                _notificationEvent.value = NotificationData(
                    message = "Tu solicitud de efectivo ha sido respondida",
                    route = "lugares"
                )
            }
            viewModelScope.launch {
                _montajes.value = repository.getMontajes()
            }
        }
    }

    fun loadAllHorarios() {
        viewModelScope.launch {
            _horarios.value = repository.getHorarios()
        }
    }

    fun loadHorariosPorMontaje(montajeId: String) {
        viewModelScope.launch {
            _horarios.value = repository.getHorariosPorMontaje(montajeId)
        }
    }

    fun solicitarExtensionMontaje(context: Context, solicitud: SolicitudExtensionMontaje) {
        viewModelScope.launch {
            try {
                repository.addSolicitudExtension(solicitud)
                _solicitudesExtension.value = repository.getSolicitudesExtensionPendientes()
                FileLogger.logInfo("Solicitud de extensión enviada por ${solicitud.solicitadoPorId}")
            } catch (e: Exception) {
                FileLogger.logError(context, "Error al solicitar extensión", e)
            }
        }
    }

    fun responderSolicitudExtension(
        context: Context,
        solicitud: SolicitudExtensionMontaje,
        aprobado: Boolean,
        autorizadorId: String,
        motivoRechazo: String = ""
    ) {
        viewModelScope.launch {
            try {
                val updatedSolicitud = solicitud.copy(
                    estado = if (aprobado) "APROBADO" else "RECHAZADO",
                    respondidoPorId = autorizadorId,
                    motivoRechazo = if (!aprobado) motivoRechazo else null,
                    fechaRespuesta = com.google.firebase.Timestamp.now()
                )
                repository.updateSolicitudExtension(updatedSolicitud)

                if (aprobado) {
                    val montaje = _montajes.value.find { it.id == solicitud.montajeId }
                    montaje?.let {
                        val calendar = java.util.Calendar.getInstance()
                        it.fechaFinalEstimada?.let { fecha -> calendar.time = fecha.toDate() }
                        calendar.add(java.util.Calendar.DAY_OF_YEAR, solicitud.diasSolicitados)
                        
                        val montajeActualizado = it.copy(
                            fechaFinalEstimada = com.google.firebase.Timestamp(calendar.time),
                            diasAdicionales = it.diasAdicionales + solicitud.diasSolicitados
                        )
                        repository.addMontaje(montajeActualizado)
                    }
                }
                
                _montajes.value = repository.getMontajes()
                _solicitudesExtension.value = repository.getSolicitudesExtensionPendientes()
                FileLogger.logInfo("Solicitud de extensión respondida: ${solicitud.id} - Aprobado: $aprobado")
            } catch (e: Exception) {
                FileLogger.logError(context, "Error al responder solicitud de extensión", e)
            }
        }
    }

    fun addSolicitudEspecial(solicitud: SolicitudTrabajoEspecial) {
        viewModelScope.launch {
            repository.addSolicitudTrabajoEspecial(solicitud)
            _solicitudesEspeciales.value = repository.getSolicitudesEspecialesPendientes()
        }
    }

    fun responderSolicitudEspecial(solicitud: SolicitudTrabajoEspecial, aprobado: Boolean, autorizadorId: String) {
        viewModelScope.launch {
            val updated = solicitud.copy(
                estado = if (aprobado) "APROBADO" else "RECHAZADO",
                respondidoPorId = autorizadorId,
                fechaRespuesta = com.google.firebase.Timestamp.now()
            )
            repository.updateSolicitudEspecial(updated)
            _solicitudesEspeciales.value = repository.getSolicitudesEspecialesPendientes()
        }
    }

    fun saveEmpleado(context: Context, empleado: Empleado) {
        viewModelScope.launch {
            try {
                repository.addEmpleado(empleado)
                _empleados.value = repository.getEmpleados()
            } catch (e: Exception) {
                FileLogger.logError(context, "Error al guardar empleado", e)
            }
        }
    }

    fun deleteEmpleado(context: Context, id: String) {
        viewModelScope.launch {
            try {
                repository.deleteEmpleado(id)
                _empleados.value = repository.getEmpleados()
            } catch (e: Exception) {
                FileLogger.logError(context, "Error al eliminar empleado", e)
            }
        }
    }

    fun responderConfirmacionMontaje(montajeId: String, aceptado: Boolean, motivo: String? = null) {
        val user = _currentUser.value ?: return
        repository.updateConfirmacionMontaje(
            montajeId = montajeId,
            empleadoId = user.id,
            estado = if (aceptado) "ACEPTADO" else "RECHAZADO",
            motivo = motivo
        )
    }

    fun saveMontaje(context: Context, montaje: Montaje) {
        viewModelScope.launch {
            try {
                // Al crear un montaje nuevo, inicializamos las confirmaciones como PENDIENTE
                val confirmacionesIniciales = mutableMapOf<String, String>()
                confirmacionesIniciales[montaje.encargadoId] = "PENDIENTE"
                montaje.personalAsignadoIds.forEach { id ->
                    confirmacionesIniciales[id] = "PENDIENTE"
                }
                
                val nuevoMontaje = montaje.copy(
                    estado = "PENDIENTE_CONFIRMACION",
                    confirmaciones = confirmacionesIniciales
                )
                
                repository.addMontaje(nuevoMontaje)
                _montajes.value = repository.getMontajes()
                FileLogger.logInfo("Montaje creado y pendiente de confirmación: ${montaje.nombre}")
            } catch (e: Exception) {
                FileLogger.logError(context, "Error al guardar montaje", e)
            }
        }
    }

    fun solicitarFinalizacionMontaje(context: Context, montaje: Montaje) {
        viewModelScope.launch {
            try {
                val actualizado = montaje.copy(estado = "FINALIZACION_SOLICITADA")
                repository.addMontaje(actualizado)
                _montajes.value = repository.getMontajes()
                FileLogger.logInfo("Finalización solicitada para: ${montaje.nombre}")
            } catch (e: Exception) {
                FileLogger.logError(context, "Error al solicitar finalización", e)
            }
        }
    }

    fun responderFinalizacionMontaje(
        context: Context, 
        montaje: Montaje, 
        aprobado: Boolean, 
        motivoRechazo: String = "",
        diasAdicionales: Int = 0
    ) {
        viewModelScope.launch {
            try {
                val actualizado = if (aprobado) {
                    montaje.copy(
                        estado = "FINALIZADO",
                        fechaFinalReal = com.google.firebase.Timestamp.now()
                    )
                } else {
                    val calendar = java.util.Calendar.getInstance()
                    montaje.fechaFinalEstimada?.let { calendar.time = it.toDate() }
                    calendar.add(java.util.Calendar.DAY_OF_YEAR, diasAdicionales)
                    
                    montaje.copy(
                        estado = "EN_PROCESO",
                        motivoRechazoFinalizacion = motivoRechazo,
                        diasAdicionales = montaje.diasAdicionales + diasAdicionales,
                        fechaFinalEstimada = com.google.firebase.Timestamp(calendar.time)
                    )
                }
                repository.addMontaje(actualizado)
                _montajes.value = repository.getMontajes()
                FileLogger.logInfo("Respuesta a finalización: ${montaje.nombre} - Aprobado: $aprobado")
            } catch (e: Exception) {
                FileLogger.logError(context, "Error al responder finalización", e)
            }
        }
    }

    fun deleteMontaje(context: Context, id: String) {
        viewModelScope.launch {
            try {
                repository.deleteMontaje(id)
                _montajes.value = repository.getMontajes()
            } catch (e: Exception) {
                FileLogger.logError(context, "Error al eliminar montaje", e)
            }
        }
    }

    fun addRegistroEfectivo(context: Context, montajeId: String, monto: Double) {
        viewModelScope.launch {
            try {
                val montaje = repository.getMontaje(montajeId)
                if (montaje != null) {
                    val nuevosRegistros = montaje.registrosEfectivo + RegistroEfectivo(monto = monto)
                    repository.addMontaje(montaje.copy(registrosEfectivo = nuevosRegistros))
                    _montajes.value = repository.getMontajes()
                }
            } catch (e: Exception) {
                FileLogger.logError(context, "Error al agregar efectivo", e)
            }
        }
    }

    fun addGasto(context: Context, gasto: Gasto) {
        viewModelScope.launch {
            try {
                repository.addGasto(gasto)
                loadGastosPorMontaje(gasto.lugarMontajeId)
            } catch (e: Exception) {
                FileLogger.logError(context, "Error al agregar gasto", e)
            }
        }
    }

    fun updateGasto(context: Context, gasto: Gasto) {
        viewModelScope.launch {
            try {
                repository.updateGasto(gasto)
                loadGastosPorMontaje(gasto.lugarMontajeId)
            } catch (e: Exception) {
                FileLogger.logError(context, "Error al actualizar gasto", e)
            }
        }
    }

    fun deleteGasto(context: Context, gasto: Gasto) {
        viewModelScope.launch {
            try {
                repository.deleteGasto(gasto.id)
                loadGastosPorMontaje(gasto.lugarMontajeId)
            } catch (e: Exception) {
                FileLogger.logError(context, "Error al eliminar gasto", e)
            }
        }
    }

    fun loadGastosPorMontaje(montajeId: String) {
        viewModelScope.launch {
            _gastos.value = repository.getGastosPorMontaje(montajeId)
        }
    }

    fun saveConfiguracion(context: Context, config: ConfiguracionEmpresa) {
        viewModelScope.launch {
            try {
                repository.saveConfiguracion(config)
                _configuracion.value = config
            } catch (e: Exception) {
                FileLogger.logError(context, "Error al guardar configuración", e)
            }
        }
    }

    fun solicitarEfectivo(context: Context, solicitud: SolicitudEfectivo) {
        viewModelScope.launch {
            try {
                repository.addSolicitudEfectivo(solicitud)
                FileLogger.logInfo("Solicitud de efectivo enviada por ${solicitud.solicitadoPorId}")
                _uiMessage.value = "Solicitud de efectivo enviada correctamente"
            } catch (e: Exception) {
                FileLogger.logError(context, "Error al solicitar efectivo", e)
            }
        }
    }

    fun responderSolicitudEfectivo(
        context: Context,
        solicitud: SolicitudEfectivo,
        aprobado: Boolean,
        autorizadorId: String,
        motivoRechazo: String = ""
    ) {
        viewModelScope.launch {
            try {
                val updatedSolicitud = solicitud.copy(
                    estado = if (aprobado) "APROBADO" else "RECHAZADO",
                    respondidoPorId = autorizadorId,
                    motivoRechazo = if (!aprobado) motivoRechazo else null,
                    fechaRespuesta = com.google.firebase.Timestamp.now()
                )
                repository.updateSolicitudEfectivo(updatedSolicitud)

                if (aprobado) {
                    repository.updateMontajeSaldo(solicitud.montajeId, solicitud.monto)
                }

                _montajes.value = repository.getMontajes()
                _solicitudesEfectivo.value = _solicitudesEfectivo.value.filter { it.id != solicitud.id }
                FileLogger.logInfo("Solicitud de efectivo respondida: ${solicitud.id} - Aprobado: $aprobado")
                _uiMessage.value = if (aprobado) "Solicitud aprobada" else "Solicitud rechazada"
            } catch (e: Exception) {
                FileLogger.logError(context, "Error al responder solicitud de efectivo", e)
            }
        }
    }
    
    fun saveHorariosMasivos(
        context: Context,
        lugarId: String,
        fecha: com.google.firebase.Timestamp,
        entrada: String,
        salida: String,
        vSale: String,
        vLlega: String,
        empleadosIds: List<String>,
        viajeSaleFull: com.google.firebase.Timestamp? = null,
        viajeLlegaFull: com.google.firebase.Timestamp? = null
    ) {
        viewModelScope.launch {
            try {
                val montaje = _montajes.value.find { it.id == lugarId }
                val personalAFectar = if (empleadosIds.isEmpty()) {
                    montaje?.personalAsignadoIds ?: emptyList()
                } else {
                    empleadosIds
                }

                personalAFectar.forEach { empId ->
                    val horario = Horario(
                        empleadoId = empId,
                        lugarMontajeId = lugarId,
                        fecha = fecha,
                        horaEntrada = if (entrada.isNotBlank()) parseTimeToTimestamp(fecha, entrada) else null,
                        horaSalida = if (salida.isNotBlank()) parseTimeToTimestamp(fecha, salida) else null,
                        viajeSale = viajeSaleFull ?: if (vSale.isNotBlank()) parseTimeToTimestamp(fecha, vSale) else null,
                        viajeLlega = viajeLlegaFull ?: if (vLlega.isNotBlank()) parseTimeToTimestamp(fecha, vLlega) else null,
                        estado = "REGISTRADO"
                    )
                    repository.addHorario(horario)
                }
                loadHorariosPorMontaje(lugarId)
                FileLogger.logInfo("Carga masiva completada para montaje: $lugarId")
            } catch (e: Exception) {
                FileLogger.logError(context, "Error al guardar horarios masivos", e)
            }
        }
    }

    fun saveHorario(context: Context, horario: Horario) {
        viewModelScope.launch {
            try {
                if (horario.id.isEmpty()) {
                    repository.addHorario(horario)
                } else {
                    repository.updateHorario(horario)
                }
                loadHorariosPorMontaje(horario.lugarMontajeId)
            } catch (e: Exception) {
                FileLogger.logError(context, "Error al guardar horario", e)
            }
        }
    }

    fun deleteHorario(context: Context, horario: Horario) {
        viewModelScope.launch {
            try {
                repository.deleteHorario(horario.id)
                loadHorariosPorMontaje(horario.lugarMontajeId)
            } catch (e: Exception) {
                FileLogger.logError(context, "Error al eliminar horario", e)
            }
        }
    }

    fun exportGastosPdf(context: Context, montaje: Montaje) {
        viewModelScope.launch {
            try {
                val gastos = repository.getGastosPorMontaje(montaje.id)
                val file = com.example.montaje_tamer.utils.PdfGenerator.generateGastosPdf(context, montaje.nombre, gastos)
                FileLogger.logInfo("PDF de gastos generado: ${file.absolutePath}")
            } catch (e: Exception) {
                FileLogger.logError(context, "Error al exportar PDF de gastos", e)
            }
        }
    }

    fun exportHorariosMontajePdf(context: Context, montaje: Montaje) {
        viewModelScope.launch {
            try {
                val horarios = repository.getHorariosPorMontaje(montaje.id)
                val empleadosMap = _empleados.value.associateBy { it.id }
                val file = com.example.montaje_tamer.utils.PdfGenerator.generateHorarioMontajePdf(
                    context, 
                    montaje.nombre, 
                    horarios, 
                    empleadosMap
                )
                FileLogger.logInfo("PDF de horarios generado: ${file.absolutePath}")
            } catch (e: Exception) {
                FileLogger.logError(context, "Error al exportar PDF de horarios", e)
            }
        }
    }

    fun exportHorarioPersonalPdf(context: Context, empleado: Empleado) {
        viewModelScope.launch {
            try {
                val horarios = repository.getHorariosPorEmpleado(empleado.id)
                val montajesMap = montajes.value.associate { it.id to it.nombre }
                val file = com.example.montaje_tamer.utils.PdfGenerator.generateHorarioPersonalPdf(
                    context,
                    empleado,
                    horarios,
                    montajesMap
                )
                FileLogger.openFile(context, file)
            } catch (e: Exception) {
                FileLogger.logError(context, "Error al exportar PDF individual", e)
            }
        }
    }

    private fun parseTimeToTimestamp(baseDate: com.google.firebase.Timestamp, timeStr: String): com.google.firebase.Timestamp? {
        val cleanTime = timeStr.replace(":", "").replace(".", "")
        if (cleanTime.length < 4) return null
        val hours = cleanTime.substring(0, 2).toIntOrNull() ?: return null
        val minutes = cleanTime.substring(2, 4).toIntOrNull() ?: return null
        val calendar = java.util.Calendar.getInstance().apply {
            time = baseDate.toDate()
            set(java.util.Calendar.HOUR_OF_DAY, hours)
            set(java.util.Calendar.MINUTE, minutes)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        return com.google.firebase.Timestamp(calendar.time)
    }
}
