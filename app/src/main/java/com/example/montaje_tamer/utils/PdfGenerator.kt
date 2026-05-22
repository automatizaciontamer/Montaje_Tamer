package com.example.montaje_tamer.utils

import android.content.Context
import android.os.Environment
import com.example.montaje_tamer.model.Empleado
import com.example.montaje_tamer.model.Gasto
import com.example.montaje_tamer.model.Horario
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.TextAlignment
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object PdfGenerator {

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    fun generateGastosPdf(context: Context, lugarNombre: String, gastos: List<Gasto>): File {
        val fileName = "Gastos_${lugarNombre.replace(" ", "_")}_${System.currentTimeMillis()}.pdf"
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)
        
        val writer = PdfWriter(FileOutputStream(file))
        val pdf = PdfDocument(writer)
        val document = Document(pdf)

        document.add(Paragraph("MONTAJES TAMER - Informe de Gastos").setBold().setFontSize(20f).setTextAlignment(TextAlignment.CENTER))
        document.add(Paragraph("Obra/Montaje: $lugarNombre").setBold())
        document.add(Paragraph("Fecha de Generación: ${dateFormat.format(Date())} ${timeFormat.format(Date())}"))
        document.add(Paragraph("\n"))

        val table = Table(floatArrayOf(2f, 4f, 2f, 2f, 2f))
        table.addCell("Fecha").setBold()
        table.addCell("Detalle").setBold()
        table.addCell("Comp. Nº").setBold()
        table.addCell("Tipo Pago").setBold()
        table.addCell("Monto").setBold()

        var total = 0.0
        for (gasto in gastos.sortedBy { it.fecha }) {
            table.addCell(dateFormat.format(gasto.fecha.toDate()))
            table.addCell(gasto.detalle)
            table.addCell(gasto.numeroComprobante)
            table.addCell(gasto.tipoPago)
            table.addCell("$${String.format(Locale.US, "%.2f", gasto.totalGasto)}")
            total += gasto.totalGasto
        }

        document.add(table)
        document.add(Paragraph("\nTotal Gastos Acumulados: $${String.format(Locale.US, "%.2f", total)}").setBold())
        
        document.close()
        return file
    }

    fun generateHorarioMontajePdf(
        context: Context, 
        lugarNombre: String, 
        horarios: List<Horario>, 
        empleadosMap: Map<String, Empleado>
    ): File {
        val fileName = "Horarios_${lugarNombre.replace(" ", "_")}_${System.currentTimeMillis()}.pdf"
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)

        val writer = PdfWriter(FileOutputStream(file))
        val pdf = PdfDocument(writer)
        val document = Document(pdf)

        document.add(Paragraph("MONTAJES TAMER - Reporte de Asistencia").setBold().setFontSize(20f).setTextAlignment(TextAlignment.CENTER))
        document.add(Paragraph("Montaje: $lugarNombre").setBold())
        document.add(Paragraph("Fecha: ${dateFormat.format(Date())}"))
        document.add(Paragraph("\n"))

        // Agrupar por empleado para un informe más ordenado
        val horariosPorEmpleado = horarios.groupBy { it.empleadoId }

        horariosPorEmpleado.forEach { (empId, listaHorarios) ->
            val emp = empleadosMap[empId]
            document.add(Paragraph("Personal: ${emp?.nombre} ${emp?.apellido} (Legajo: ${emp?.legajo})").setBold().setUnderline())
            
            val table = Table(floatArrayOf(2f, 2f, 2f, 2f, 2f))
            table.addCell("Fecha").setBold()
            table.addCell("V. Inicio").setBold()
            table.addCell("Entrada").setBold()
            table.addCell("Salida").setBold()
            table.addCell("V. Fin").setBold()

            listaHorarios.sortedBy { it.fecha }.forEach { h ->
                table.addCell(dateFormat.format(h.fecha.toDate()))
                table.addCell(h.viajeSale?.let { timeFormat.format(it.toDate()) } ?: "-")
                table.addCell(h.horaEntrada?.let { timeFormat.format(it.toDate()) } ?: "-")
                table.addCell(h.horaSalida?.let { timeFormat.format(it.toDate()) } ?: "-")
                table.addCell(h.viajeLlega?.let { timeFormat.format(it.toDate()) } ?: "-")
            }
            document.add(table)
            document.add(Paragraph("\n"))
        }

        document.close()
        return file
    }

    fun generateHorarioPersonalPdf(
        context: Context, 
        empleado: Empleado, 
        horarios: List<Horario>,
        montajesMap: Map<String, String>
    ): File {
        val fileName = "Horario_${empleado.apellido}_${System.currentTimeMillis()}.pdf"
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)

        val writer = PdfWriter(FileOutputStream(file))
        val pdf = PdfDocument(writer)
        val document = Document(pdf)

        document.add(Paragraph("MONTAJES TAMER").setBold().setFontSize(14f))
        document.add(Paragraph("Informe de Asistencia Individual").setBold().setFontSize(18f).setTextAlignment(TextAlignment.CENTER))
        document.add(Paragraph("Personal: ${empleado.nombre} ${empleado.apellido}"))
        document.add(Paragraph("Legajo: ${empleado.legajo}"))
        document.add(Paragraph("Tipo de Liquidación: ${empleado.tipoLiquidacion}"))
        document.add(Paragraph("Fecha de Emisión: ${dateFormat.format(Date())}"))
        document.add(Paragraph("\n"))

        val table = Table(floatArrayOf(3f, 2f, 2f, 2f, 2f, 2f))
        table.addCell("Montaje/Obra").setBold()
        table.addCell("Fecha").setBold()
        table.addCell("V. Inicio").setBold()
        table.addCell("Entrada").setBold()
        table.addCell("Salida").setBold()
        table.addCell("V. Fin").setBold()

        for (h in horarios.sortedBy { it.fecha }) {
            table.addCell(montajesMap[h.lugarMontajeId] ?: "Desconocido")
            table.addCell(dateFormat.format(h.fecha.toDate()))
            table.addCell(h.viajeSale?.let { timeFormat.format(it.toDate()) } ?: "-")
            table.addCell(h.horaEntrada?.let { timeFormat.format(it.toDate()) } ?: "-")
            table.addCell(h.horaSalida?.let { timeFormat.format(it.toDate()) } ?: "-")
            table.addCell(h.viajeLlega?.let { timeFormat.format(it.toDate()) } ?: "-")
        }

        document.add(table)
        
        document.add(Paragraph("\n\n\n\n"))
        document.add(Paragraph("___________________________\nFirma del Empleado").setTextAlignment(TextAlignment.CENTER))

        document.close()
        return file
    }
}
