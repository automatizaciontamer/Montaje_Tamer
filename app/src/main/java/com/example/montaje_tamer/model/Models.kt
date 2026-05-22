package com.example.montaje_tamer.model

import com.google.firebase.Timestamp

enum class Rol {
    DIRECTIVO, ADMINISTRATIVO, SUPERVISOR, EMPLEADO
}

data class Empleado(
    val id: String = "",
    val usuario: String = "",
    val clave: String = "14569", // Clave por defecto
    val nombre: String = "",
    val apellido: String = "",
    val email: String = "",
    val legajo: String = "",
    val categoria: String = "",
    val tipoLiquidacion: String = "", // Quincenal o Mensual
    val rol: Rol = Rol.EMPLEADO
)

data class RegistroEfectivo(
    val monto: Double = 0.0,
    val fecha: Timestamp = Timestamp.now()
)

data class Montaje(
    val id: String = "",
    val nombre: String = "",
    val ubicacion: String = "",
    val encargadoId: String = "",
    val personalAsignadoIds: List<String> = emptyList(),
    val fechaInicio: Timestamp? = Timestamp.now(),
    val fechaFinalEstimada: Timestamp? = null,
    val fechaFinalReal: Timestamp? = null,
    val estado: String = "PENDIENTE_CONFIRMACION", // PENDIENTE_CONFIRMACION, EN_PROCESO, FINALIZACION_SOLICITADA, FINALIZADO
    val motivoRechazoFinalizacion: String = "",
    val diasAdicionales: Int = 0,
    val cajaEfectivoInicial: Double = 0.0,
    val registrosEfectivo: List<RegistroEfectivo> = emptyList(),
    val numeroTarjeta: String = "", // 16 dígitos
    val usaHorarioEmpresa: Boolean = true,
    val horarioEspecifico: Map<String, String> = emptyMap(), // "Lunes" -> "08:00 - 17:00", etc.
    val confirmaciones: Map<String, String> = emptyMap(), // ID Empleado -> "PENDIENTE", "ACEPTADO", "RECHAZADO"
    val motivosRechazoAsignacion: Map<String, String> = emptyMap() // ID Empleado -> Motivo
)

data class Gasto(
    val id: String = "",
    val personalEncargadoId: String = "",
    val lugarMontajeId: String = "",
    val detalle: String = "",
    val efectivoRecibido: Double = 0.0,
    val numeroComprobante: String = "",
    val tipoPago: String = "", // EFECTIVO o TARJETA
    val totalGasto: Double = 0.0,
    val fecha: Timestamp = Timestamp.now()
)

data class Horario(
    val id: String = "",
    val empleadoId: String = "",
    val lugarMontajeId: String = "",
    val fecha: Timestamp = Timestamp.now(),
    val horaEntrada: Timestamp? = null,
    val horaSalida: Timestamp? = null,
    val viajeSale: Timestamp? = null,
    val viajeLlega: Timestamp? = null,
    val estado: String = "REGISTRADO", // REGISTRADO, PENDIENTE_CAMBIO, APROBADO
    val esDiaEspecial: Boolean = false, // Sábado, Domingo o Feriado
    val autorizadoPorId: String? = null,
    val fechaAutorizacion: Timestamp? = null
)

data class SolicitudTrabajoEspecial(
    val id: String = "",
    val empleadoId: String = "",
    val montajeId: String = "",
    val fechaTrabajo: Timestamp = Timestamp.now(),
    val motivo: String = "",
    val estado: String = "PENDIENTE", // PENDIENTE, APROBADO, RECHAZADO
    val solicitadoPorId: String = "",
    val respondidoPorId: String? = null,
    val nombreRespuesta: String? = null, // Nombre de quien aprobó/denegó
    val motivoRechazo: String? = null,
    val fechaRespuesta: Timestamp? = null
)

data class SolicitudCambioHorario(
    val id: String = "",
    val horarioId: String = "",
    val encargadoId: String = "",
    val nuevoHorario: Horario? = null,
    val motivo: String = "",
    val estado: String = "PENDIENTE", // PENDIENTE, APROBADO, RECHAZADO
    val fechaSolicitud: Timestamp = Timestamp.now()
)

data class SolicitudExtensionMontaje(
    val id: String = "",
    val montajeId: String = "",
    val solicitadoPorId: String = "",
    val diasSolicitados: Int = 0,
    val motivo: String = "",
    val estado: String = "PENDIENTE", // PENDIENTE, APROBADO, RECHAZADO
    val fechaSolicitud: Timestamp = Timestamp.now(),
    val respondidoPorId: String? = null,
    val motivoRechazo: String? = null,
    val fechaRespuesta: Timestamp? = null
)

data class SolicitudEfectivo(
    val id: String = "",
    val montajeId: String = "",
    val solicitadoPorId: String = "",
    val monto: Double = 0.0,
    val motivo: String = "",
    val estado: String = "PENDIENTE", // PENDIENTE, APROBADO, RECHAZADO
    val fechaSolicitud: Timestamp = Timestamp.now(),
    val respondidoPorId: String? = null,
    val motivoRechazo: String? = null,
    val fechaRespuesta: Timestamp? = null
)

data class ConfiguracionEmpresa(
    val id: String = "config",
    val horarios: Map<String, String> = mapOf(
        "Lunes" to "08:00 - 17:00",
        "Martes" to "08:00 - 17:00",
        "Miércoles" to "08:00 - 17:00",
        "Jueves" to "08:00 - 17:00",
        "Viernes" to "08:00 - 16:00",
        "Sábado" to "No Laboral",
        "Domingo" to "No Laboral"
    ),
    val feriados: List<Timestamp> = emptyList()
)

data class NotificationData(
    val message: String = "",
    val route: String = "home"
)
