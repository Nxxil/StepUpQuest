package com.example.stepupquest_tv

import com.github.mikephil.charting.data.BarEntry
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

// Clase de datos para el punto del gráfico, usada internamente por la TV
data class TvChartDataPoint(val label: String, val value: Float)

object TvStatsProcessor {

    /**
     * Procesa el historial completo para obtener datos de los últimos 'daysToShow' días.
     * @param fullHistory Mapa de "yyyy-MM-dd" a pasos.
     * @param daysToShow Cuántos días hacia atrás mostrar.
     * @return Lista de TvChartDataPoint listos para el gráfico.
     */
    fun getDailyChartDataFromHistory(
        fullHistory: Map<String, Int>,
        daysToShow: Int = 7
    ): List<TvChartDataPoint> {
        val dailyData = mutableListOf<Pair<String, Int>>()
        val calendar = Calendar.getInstance()

        // Iterar hacia atrás para los últimos 'daysToShow'
        for (i in 0 until daysToShow) {
            val dateString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
            val steps = fullHistory[dateString] ?: 0
            dailyData.add(Pair(dateString, steps))
            calendar.add(Calendar.DAY_OF_YEAR, -1)
        }

        // Revertir para que el más antiguo esté primero (mejor para el orden del gráfico)
        // y transformar a TvChartDataPoint
        return dailyData.reversed().map {
            TvChartDataPoint(formatDateForLabel(it.first), it.second.toFloat())
        }
    }

    /**
     * Procesa el historial completo para obtener datos semanales agrupados.
     * @param fullHistory Mapa de "yyyy-MM-dd" a pasos.
     * @return Lista de TvChartDataPoint listos para el gráfico.
     */
    fun getWeeklyChartDataFromHistory(
        fullHistory: Map<String, Int>
    ): List<TvChartDataPoint> {
        if (fullHistory.isEmpty()) return emptyList()

        val weeklyTotals = mutableMapOf<String, Int>() // Key: "yyyy-MM-dd" del lunes de esa semana
        val sortedDates = fullHistory.keys.sorted() // Asegurar orden cronológico

        for (dateString in sortedDates) {
            val steps = fullHistory[dateString] ?: 0
            val calendar = Calendar.getInstance()
            calendar.time = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateString)!!

            // Ajustar al lunes de esa semana
            calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek) // firstDayOfWeek suele ser Domingo o Lunes según Locale
            // Si el primer día es Domingo y quieres Lunes como inicio, necesitas un ajuste extra:
            if (calendar.firstDayOfWeek == Calendar.SUNDAY && calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                // Si hoy es domingo, y la semana empieza en domingo, para agrupar por "semana que empieza en Lunes",
                // podríamos considerar que el domingo pertenece a la semana que "termina" ese domingo.
                // O, si queremos que la etiqueta sea "Lunes de X", retrocedemos si es Domingo.
                // Para simplificar, usemos la fecha del Lunes como identificador de la semana.
            }
            // Si el día actual es domingo, y la semana laboral es Lun-Dom, podríamos moverlo al lunes anterior
            // para que la etiqueta de la semana sea consistente.
            // Esto se vuelve complejo con localización. Una aproximación:
            while (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
                calendar.add(Calendar.DAY_OF_YEAR, -1) // Retrocede hasta encontrar un Lunes
                if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY) break // Encontrado
            }
            // Habiendo encontrado el Lunes (o el inicio de semana según el locale)
            val weekStartDateString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

            weeklyTotals[weekStartDateString] = (weeklyTotals[weekStartDateString] ?: 0) + steps
        }

        // Convertir a TvChartDataPoint y formatear etiqueta
        // Ordenar por la fecha de inicio de semana
        return weeklyTotals.entries.sortedBy { it.key }.map { entry ->
            val weekLabel = "Sem. ${formatDateForLabel(entry.key)}" // "Sem. dd/MM"
            TvChartDataPoint(weekLabel, entry.value.toFloat())
        }
    }


    /**
     * Formatea "yyyy-MM-dd" a "dd/MM" para etiquetas más cortas.
     */
    private fun formatDateForLabel(dateString: String): String {
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val formatter = SimpleDateFormat("dd/MM", Locale.getDefault())
            formatter.format(parser.parse(dateString)!!)
        } catch (e: Exception) {
            dateString // Fallback
        }
    }
}

