package com.example.ihm.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

@Database(entities = [RecordatorioEntity::class], version = 8, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun recordatorioDao(): RecordatorioDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "agenda_database"
                )
                .fallbackToDestructiveMigration()
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Solo poblamos la base de datos la primera vez que se crea
                        INSTANCE?.let { database ->
                            CoroutineScope(Dispatchers.IO).launch {
                                populateDatabase(database.recordatorioDao())
                            }
                        }
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }

        suspend fun populateDatabase(dao: RecordatorioDao) {
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            // 1. Tarea con 3 subtareas
            dao.insert(
                RecordatorioEntity(
                    titulo = "Hacer las compras",
                    descripcion = "Comprar lo necesario para la cena",
                    fecha = today,
                    categoria = "Tarea",
                    icono = "Compras",
                    subtasks = listOf(
                        SubTask("Leche"),
                        SubTask("Pan"),
                        SubTask("Huevos")
                    )
                )
            )

            // 2. Tarea sin subtareas
            dao.insert(
                RecordatorioEntity(
                    titulo = "Llamar al banco",
                    descripcion = "Preguntar por la tarjeta de crédito",
                    fecha = today,
                    categoria = "Tarea",
                    icono = "Llamada"
                )
            )

            // 3. Una Alarma
            val soon = Calendar.getInstance().apply {
                add(Calendar.MINUTE, 5)
            }
            val alarmTime = String.format("%02d:%02d", soon.get(Calendar.HOUR_OF_DAY), soon.get(Calendar.MINUTE))
            dao.insert(
                RecordatorioEntity(
                    titulo = "Prueba de Alarma",
                    fecha = today,
                    hora = alarmTime,
                    categoria = "Alarma",
                    icono = "Despertador"
                )
            )

            // 4. Un evento para hoy
            dao.insert(
                RecordatorioEntity(
                    titulo = "Cita con el Dentista",
                    descripcion = "Revisión 15:00",
                    fecha = today,
                    hora = "15:00",
                    categoria = "Evento",
                    icono = "CitaMedica"
                )
            )

            // 5. Un evento para mañana
            val tomorrow = today + 24 * 60 * 60 * 1000
            dao.insert(
                RecordatorioEntity(
                    titulo = "Reunión de Equipo",
                    descripcion = "Planificación semanal",
                    fecha = tomorrow,
                    categoria = "Evento",
                    icono = "Profesional"
                )
            )
            
            val inThreeDays = today + 3L * 24 * 60 * 60 * 1000
            dao.insert(
                RecordatorioEntity(
                    titulo = "Cena con amigos",
                    fecha = inThreeDays,
                    categoria = "Evento",
                    icono = "Social"
                )
            )
        }
    }
}
