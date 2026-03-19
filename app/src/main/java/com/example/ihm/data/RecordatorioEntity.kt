package com.example.ihm.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class SubTask(
    val title: String,
    val completed: Boolean = false
)

@Entity(tableName = "recordatorios")
data class RecordatorioEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val titulo: String,
    val descripcion: String = "",
    val fecha: Long, // Fecha principal o de inicio
    val hora: String? = null, // Hora de inicio
    val fechaLimite: Long? = null,
    val horaLimite: String? = null,
    val intervaloHoras: Int? = null,
    val intervaloMinutos: Int? = null,
    val completado: Boolean = false,
    val categoria: String? = null, // "Evento", "Tarea", "Alarma", "Rutina"
    val icono: String? = null,
    val subtasks: List<SubTask> = emptyList(),
    val diasSemana: List<Int> = emptyList(), // 1=Lunes, 7=Domingo
    val horasEspecificas: List<String> = emptyList(),
    val diasExcluidos: List<Long> = emptyList(),
    val esAnual: Boolean = false // Para cumpleaños y aniversarios
)

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromStringList(value: String): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, listType) ?: emptyList()
    }

    @TypeConverter
    fun toStringList(list: List<String>): String = gson.toJson(list)

    @TypeConverter
    fun fromSubTaskList(value: String): List<SubTask> {
        val listType = object : TypeToken<List<SubTask>>() {}.type
        return gson.fromJson(value, listType) ?: emptyList()
    }

    @TypeConverter
    fun toSubTaskList(list: List<SubTask>): String = gson.toJson(list)

    @TypeConverter
    fun fromIntList(value: String): List<Int> {
        val listType = object : TypeToken<List<Int>>() {}.type
        return gson.fromJson(value, listType) ?: emptyList()
    }

    @TypeConverter
    fun toIntList(list: List<Int>): String = gson.toJson(list)

    @TypeConverter
    fun fromLongList(value: String): List<Long> {
        val listType = object : TypeToken<List<Long>>() {}.type
        return gson.fromJson(value, listType) ?: emptyList()
    }

    @TypeConverter
    fun toLongList(list: List<Long>): String = gson.toJson(list)
}
