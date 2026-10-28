package com.sarpai.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "conversations")
data class ConversationEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val title: String, val updatedAt: Long)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val conversationId: Long, val role: String, val content: String, val createdAt: Long
)

@Entity(tableName = "memory")
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String, val text: String, val enabled: Boolean = true, val createdAt: Long
)

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String, val body: String, val isTask: Boolean = false, val done: Boolean = false, val updatedAt: Long
)

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations ORDER BY updatedAt DESC") fun observeAll(): Flow<List<ConversationEntity>>
    @Insert suspend fun insert(c: ConversationEntity): Long
    @Query("UPDATE conversations SET updatedAt = :t WHERE id = :id") suspend fun touch(id: Long, t: Long)
    @Query("UPDATE conversations SET title = :title WHERE id = :id") suspend fun rename(id: Long, title: String)
    @Query("DELETE FROM conversations WHERE id = :id") suspend fun delete(id: Long)
    @Query("DELETE FROM conversations") suspend fun deleteAll()
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE conversationId = :cid ORDER BY id") fun observe(cid: Long): Flow<List<MessageEntity>>
    @Query("SELECT * FROM messages WHERE conversationId = :cid ORDER BY id") suspend fun list(cid: Long): List<MessageEntity>
    @Insert suspend fun insert(m: MessageEntity): Long
    @Query("DELETE FROM messages WHERE id = :id") suspend fun delete(id: Long)
    @Query("DELETE FROM messages WHERE conversationId = :cid") suspend fun deleteForConversation(cid: Long)
    @Query("DELETE FROM messages") suspend fun deleteAll()
}

@Dao
interface MemoryDao {
    @Query("SELECT * FROM memory ORDER BY createdAt DESC") fun observeAll(): Flow<List<MemoryEntity>>
    @Query("SELECT * FROM memory WHERE enabled = 1") suspend fun enabled(): List<MemoryEntity>
    @Insert suspend fun insert(m: MemoryEntity): Long
    @Query("UPDATE memory SET enabled = :on WHERE id = :id") suspend fun setEnabled(id: Long, on: Boolean)
    @Query("DELETE FROM memory WHERE id = :id") suspend fun delete(id: Long)
    @Query("DELETE FROM memory") suspend fun deleteAll()
}

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY updatedAt DESC") fun observeAll(): Flow<List<NoteEntity>>
    @Insert suspend fun insert(n: NoteEntity): Long
    @Query("UPDATE notes SET done = :done, updatedAt = :t WHERE id = :id") suspend fun setDone(id: Long, done: Boolean, t: Long)
    @Query("DELETE FROM notes WHERE id = :id") suspend fun delete(id: Long)
    @Query("DELETE FROM notes") suspend fun deleteAll()
}

@Database(
    entities = [ConversationEntity::class, MessageEntity::class, MemoryEntity::class, NoteEntity::class],
    version = 1, exportSchema = false
)
abstract class AppDb : RoomDatabase() {
    abstract fun conversations(): ConversationDao
    abstract fun messages(): MessageDao
    abstract fun memory(): MemoryDao
    abstract fun notes(): NoteDao
}
