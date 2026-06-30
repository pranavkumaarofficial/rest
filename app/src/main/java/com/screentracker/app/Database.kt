package com.screentracker.app

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

enum class EventType {
    SCREEN_ON,
    USER_PRESENT,
    FLASHLIGHT_ON,
    FLASHLIGHT_OFF,
    SLEEP_START,
    SLEEP_END
}

@Entity(tableName = "events")
data class Event(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: EventType,
    val timestamp: Long
)

@Entity(tableName = "sleep_sessions")
data class SleepSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startTime: Long,
    val endTime: Long?,
    val totalDurationMinutes: Int?,
    val interruptionCount: Int?,
    val completedCycles: Int?,
    val qualityScore: Int?,
    val qualityLabel: String?
)

@Dao
interface EventDao {
    @Insert
    suspend fun insertEvent(event: Event)

    @Query("SELECT * FROM events ORDER BY timestamp DESC")
    fun getAllEvents(): Flow<List<Event>>

    @Query("SELECT * FROM events WHERE type = :type ORDER BY timestamp DESC")
    fun getEventsByType(type: EventType): Flow<List<Event>>

    @Query("DELETE FROM events")
    suspend fun deleteAllEvents()

    @Query("SELECT * FROM events ORDER BY timestamp ASC")
    suspend fun getAllEventsOnce(): List<Event>

    @Query("SELECT COUNT(*) FROM events WHERE type = :type")
    suspend fun getEventCount(type: EventType): Int

    @Query("SELECT * FROM events WHERE timestamp >= :start AND timestamp <= :end AND type = :type ORDER BY timestamp ASC")
    suspend fun getEventsBetween(start: Long, end: Long, type: EventType): List<Event>

    @Query("DELETE FROM events WHERE timestamp >= :start AND timestamp <= :end")
    suspend fun deleteEventsBetween(start: Long, end: Long)

    @Query("DELETE FROM events")
    suspend fun deleteAll()
}

@Dao
interface SleepSessionDao {
    @Insert
    suspend fun insertSession(session: SleepSession): Long

    @Update
    suspend fun updateSession(session: SleepSession)

    @Query("SELECT * FROM sleep_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<SleepSession>>

    @Query("SELECT * FROM sleep_sessions WHERE endTime IS NULL LIMIT 1")
    suspend fun getActiveSession(): SleepSession?

    @Query("SELECT * FROM sleep_sessions ORDER BY startTime DESC LIMIT 1")
    suspend fun getLastSession(): SleepSession?

    @Query("SELECT * FROM sleep_sessions ORDER BY startTime ASC")
    suspend fun getAllSessionsOnce(): List<SleepSession>

    @Query("DELETE FROM sleep_sessions")
    suspend fun deleteAllSessions()

    @Query("DELETE FROM sleep_sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: Long)
}

@Database(entities = [Event::class, SleepSession::class], version = 2, exportSchema = false)
@TypeConverters(Converters::class)
abstract class EventDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao
    abstract fun sleepSessionDao(): SleepSessionDao

    companion object {
        @Volatile
        private var INSTANCE: EventDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS sleep_sessions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        startTime INTEGER NOT NULL,
                        endTime INTEGER,
                        totalDurationMinutes INTEGER,
                        interruptionCount INTEGER,
                        completedCycles INTEGER,
                        qualityScore INTEGER,
                        qualityLabel TEXT
                    )
                """)
            }
        }

        fun getInstance(context: Context): EventDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    EventDatabase::class.java,
                    "screen_tracker_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class Converters {
    @TypeConverter
    fun fromEventType(value: EventType): String {
        return value.name
    }

    @TypeConverter
    fun toEventType(value: String): EventType {
        return EventType.valueOf(value)
    }
}
