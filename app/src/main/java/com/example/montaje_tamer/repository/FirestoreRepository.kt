package com.example.montaje_tamer.repository

import com.example.montaje_tamer.model.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirestoreRepository {
    private val db = FirebaseFirestore.getInstance()

    // EMPLEADOS
    suspend fun addEmpleado(empleado: Empleado) {
        val id = if (empleado.id.isEmpty()) db.collection("empleados").document().id else empleado.id
        db.collection("empleados").document(id).set(empleado.copy(id = id)).await()
    }

    suspend fun getEmpleado(id: String): Empleado? {
        return try {
            db.collection("empleados").document(id).get().await().toObject(Empleado::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getEmpleadoByEmail(email: String): Empleado? {
        return try {
            val snapshot = db.collection("empleados").get().await()
            val document = snapshot.documents.firstOrNull { doc ->
                val dbEmail = doc.getString("email") ?: doc.getString("Email") ?: 
                             doc.getString("usuario") ?: doc.getString("Usuario") ?: ""
                dbEmail.trim().lowercase() == email.trim().lowercase()
            }
            
            document?.let { doc ->
                val empleado = doc.toObject(Empleado::class.java)
                val valorEmail = doc.getString("email") ?: doc.getString("Email") ?: 
                                doc.getString("usuario") ?: doc.getString("Usuario") ?: ""
                
                empleado?.copy(
                    id = doc.id,
                    email = if (empleado.email.isEmpty()) valorEmail else empleado.email,
                    usuario = if (empleado.usuario.isEmpty()) valorEmail else empleado.usuario
                )
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getEmpleadoByUsuario(usuario: String): Empleado? {
        return try {
            val snapshot = db.collection("empleados").get().await()
            val document = snapshot.documents.firstOrNull { doc ->
                val dbUser = doc.getString("usuario") ?: doc.getString("Usuario") ?: 
                             doc.getString("email") ?: doc.getString("Email") ?: ""
                dbUser.trim().lowercase() == usuario.trim().lowercase()
            }
            
            document?.let { doc ->
                val empleado = doc.toObject(Empleado::class.java)
                val valorUser = doc.getString("usuario") ?: doc.getString("Usuario") ?: 
                               doc.getString("email") ?: doc.getString("Email") ?: ""
                
                empleado?.copy(
                    id = doc.id,
                    usuario = if (empleado.usuario.isEmpty()) valorUser else empleado.usuario,
                    email = if (empleado.email.isEmpty()) valorUser else empleado.email
                )
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getEmpleados(): List<Empleado> {
        return db.collection("empleados").get().await().toObjects(Empleado::class.java)
    }

    suspend fun deleteEmpleado(id: String) {
        db.collection("empleados").document(id).delete().await()
    }

    // MONTAJES
    suspend fun addMontaje(montaje: Montaje) {
        val ref = if (montaje.id.isEmpty()) db.collection("montajes").document() else db.collection("montajes").document(montaje.id)
        val finalMontaje = montaje.copy(id = ref.id)
        ref.set(finalMontaje).await()
    }

    suspend fun getMontajes(): List<Montaje> {
        return db.collection("montajes").get().await().toObjects(Montaje::class.java)
    }

    fun listenToMontajes(onUpdate: (List<Montaje>) -> Unit) {
        db.collection("montajes").addSnapshotListener { snapshot, error ->
            if (error != null) {
                com.example.montaje_tamer.utils.FileLogger.logInfo("Error en listener de montajes: ${error.message}")
                return@addSnapshotListener
            }
            val montajes = snapshot?.toObjects(Montaje::class.java) ?: emptyList()
            onUpdate(montajes)
        }
    }

    suspend fun deleteMontaje(id: String) {
        db.collection("montajes").document(id).delete().await()
    }

    suspend fun getMontaje(id: String): Montaje? {
        return db.collection("montajes").document(id).get().await().toObject(Montaje::class.java)
    }

    // GASTOS
    suspend fun addGasto(gasto: Gasto) {
        val ref = db.collection("gastos").document()
        ref.set(gasto.copy(id = ref.id)).await()
    }

    suspend fun updateGasto(gasto: Gasto) {
        if (gasto.id.isNotEmpty()) {
            db.collection("gastos").document(gasto.id).set(gasto).await()
        }
    }

    suspend fun deleteGasto(id: String) {
        db.collection("gastos").document(id).delete().await()
    }

    suspend fun getGastosPorMontaje(montajeId: String): List<Gasto> {
        return db.collection("gastos")
            .whereEqualTo("lugarMontajeId", montajeId)
            .get().await().toObjects(Gasto::class.java)
    }

    // HORARIOS
    suspend fun addHorario(horario: Horario) {
        val ref = db.collection("horarios").document()
        db.collection("horarios").document(ref.id).set(horario.copy(id = ref.id)).await()
    }

    suspend fun updateHorario(horario: Horario) {
        if (horario.id.isNotEmpty()) {
            db.collection("horarios").document(horario.id).set(horario).await()
        }
    }

    suspend fun getHorarios(): List<Horario> {
        return db.collection("horarios").get().await().toObjects(Horario::class.java)
    }

    suspend fun getHorariosPorEmpleado(empleadoId: String): List<Horario> {
        return db.collection("horarios")
            .whereEqualTo("empleadoId", empleadoId)
            .get().await().toObjects(Horario::class.java)
    }

    suspend fun getHorariosPorMontaje(montajeId: String): List<Horario> {
        return db.collection("horarios")
            .whereEqualTo("lugarMontajeId", montajeId)
            .get().await().toObjects(Horario::class.java)
    }

    suspend fun deleteHorario(id: String) {
        db.collection("horarios").document(id).delete().await()
    }

    // SOLICITUDES DE CAMBIO
    suspend fun addSolicitudCambio(solicitud: SolicitudCambioHorario) {
        db.collection("solicitudes_cambio").add(solicitud).await()
    }

    suspend fun getSolicitudesPendientes(): List<SolicitudCambioHorario> {
        return db.collection("solicitudes_cambio")
            .whereEqualTo("estado", "PENDIENTE")
            .get().await().toObjects(SolicitudCambioHorario::class.java)
    }

    // SOLICITUDES DE TRABAJO ESPECIAL (Sábados, Domingos, Feriados)
    suspend fun addSolicitudTrabajoEspecial(solicitud: SolicitudTrabajoEspecial) {
        val ref = db.collection("solicitudes_especiales").document()
        ref.set(solicitud.copy(id = ref.id)).await()
    }

    suspend fun getSolicitudesEspecialesPendientes(): List<SolicitudTrabajoEspecial> {
        return db.collection("solicitudes_especiales")
            .whereEqualTo("estado", "PENDIENTE")
            .get().await().toObjects(SolicitudTrabajoEspecial::class.java)
    }

    suspend fun updateSolicitudEspecial(solicitud: SolicitudTrabajoEspecial) {
        if (solicitud.id.isNotEmpty()) {
            db.collection("solicitudes_especiales").document(solicitud.id).set(solicitud).await()
        }
    }

    // SOLICITUDES DE EXTENSIÓN
    suspend fun addSolicitudExtension(solicitud: SolicitudExtensionMontaje) {
        val ref = db.collection("solicitudes_extension").document()
        ref.set(solicitud.copy(id = ref.id)).await()
    }

    suspend fun getSolicitudesExtensionPendientes(): List<SolicitudExtensionMontaje> {
        return try {
            db.collection("solicitudes_extension")
                .whereEqualTo("estado", "PENDIENTE")
                .get().await().toObjects(SolicitudExtensionMontaje::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun updateSolicitudExtension(solicitud: SolicitudExtensionMontaje) {
        if (solicitud.id.isNotEmpty()) {
            db.collection("solicitudes_extension").document(solicitud.id).set(solicitud).await()
        }
    }

    fun listenToSolicitudesExtension(onUpdate: (List<SolicitudExtensionMontaje>) -> Unit) {
        db.collection("solicitudes_extension")
            .whereEqualTo("estado", "PENDIENTE")
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                val docs = snapshot?.toObjects(SolicitudExtensionMontaje::class.java) ?: emptyList()
                onUpdate(docs)
            }
    }

    fun listenToSolicitudesExtensionPorUsuario(usuarioId: String, onUpdate: (List<SolicitudExtensionMontaje>) -> Unit) {
        db.collection("solicitudes_extension")
            .whereEqualTo("solicitadoPorId", usuarioId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                val docs = snapshot?.toObjects(SolicitudExtensionMontaje::class.java) ?: emptyList()
                onUpdate(docs)
            }
    }

    // SOLICITUDES DE EFECTIVO
    fun addSolicitudEfectivo(solicitud: SolicitudEfectivo) {
        val ref = if (solicitud.id.isEmpty()) db.collection("solicitudesEfectivo").document() else db.collection("solicitudesEfectivo").document(solicitud.id)
        ref.set(solicitud.copy(id = ref.id))
    }

    fun updateSolicitudEfectivo(solicitud: SolicitudEfectivo) {
        db.collection("solicitudesEfectivo").document(solicitud.id).set(solicitud)
    }

    fun listenToSolicitudesEfectivo(onUpdate: (List<SolicitudEfectivo>) -> Unit) {
        db.collection("solicitudesEfectivo")
            .whereEqualTo("estado", "PENDIENTE")
            .addSnapshotListener { snapshot, _ ->
                val solicitudes = snapshot?.toObjects(SolicitudEfectivo::class.java) ?: emptyList()
                onUpdate(solicitudes)
            }
    }

    fun listenToSolicitudesEfectivoPorUsuario(usuarioId: String, onUpdate: (List<SolicitudEfectivo>) -> Unit) {
        db.collection("solicitudesEfectivo")
            .whereEqualTo("solicitadoPorId", usuarioId)
            .addSnapshotListener { snapshot, _ ->
                val solicitudes = snapshot?.toObjects(SolicitudEfectivo::class.java) ?: emptyList()
                onUpdate(solicitudes)
            }
    }

    fun updateMontajeSaldo(montajeId: String, monto: Double) {
        db.runTransaction { transaction ->
            val ref = db.collection("montajes").document(montajeId)
            val snapshot = transaction.get(ref)
            val montaje = snapshot.toObject(Montaje::class.java) ?: return@runTransaction
            
            val nuevosRegistros = montaje.registrosEfectivo.toMutableList()
            nuevosRegistros.add(RegistroEfectivo(monto = monto))
            
            transaction.update(ref, "registrosEfectivo", nuevosRegistros)
        }
    }

    fun updateConfirmacionMontaje(montajeId: String, empleadoId: String, estado: String, motivo: String? = null) {
        db.runTransaction { transaction ->
            val ref = db.collection("montajes").document(montajeId)
            val snapshot = transaction.get(ref)
            val montaje = snapshot.toObject(Montaje::class.java) ?: return@runTransaction
            
            val nuevasConfirmaciones = montaje.confirmaciones.toMutableMap()
            nuevasConfirmaciones[empleadoId] = estado
            
            val nuevosMotivos = montaje.motivosRechazoAsignacion.toMutableMap()
            if (estado == "RECHAZADO" && motivo != null) {
                nuevosMotivos[empleadoId] = motivo
            } else {
                nuevosMotivos.remove(empleadoId)
            }
            
            transaction.update(ref, "confirmaciones", nuevasConfirmaciones)
            transaction.update(ref, "motivosRechazoAsignacion", nuevosMotivos)
            
            // Si todos los que están en la lista han aceptado, cambiar estado a EN_PROCESO
            // Ignoramos a los que hayan rechazado si queremos que avance, 
            // pero aquí la lógica es que TODOS los actuales deben haber dado una respuesta positiva.
            val valores = nuevasConfirmaciones.values
            val todosAceptaron = valores.isNotEmpty() && valores.all { it == "ACEPTADO" }

            if (todosAceptaron && montaje.estado == "PENDIENTE_CONFIRMACION") {
                transaction.update(ref, "estado", "EN_PROCESO")
            }
        }.addOnFailureListener { e ->
            com.example.montaje_tamer.utils.FileLogger.logInfo("Error actualizando confirmación: ${e.message}")
        }
    }

    fun listenToMontajesPendientes(usuarioId: String, onUpdate: (List<Montaje>) -> Unit) {
        db.collection("montajes")
            .whereEqualTo("estado", "PENDIENTE_CONFIRMACION")
            .addSnapshotListener { snapshot, _ ->
                val montajes = snapshot?.toObjects(Montaje::class.java) ?: emptyList()
                val misPendientes = montajes.filter { m ->
                    m.confirmaciones[usuarioId] == "PENDIENTE"
                }
                onUpdate(misPendientes)
            }
    }

    suspend fun getConfiguracion(): ConfiguracionEmpresa? {
        return try {
            db.collection("configuracion").document("config").get().await().toObject(ConfiguracionEmpresa::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun saveConfiguracion(config: ConfiguracionEmpresa) {
        db.collection("configuracion").document("config").set(config).await()
    }
}
