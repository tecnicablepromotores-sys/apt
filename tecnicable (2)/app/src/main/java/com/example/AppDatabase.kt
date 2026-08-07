package com.example

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ContratoDao {
    @Query("SELECT * FROM contratos ORDER BY timestamp DESC")
    fun getAllContratos(): Flow<List<ContratoDiario>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContrato(contrato: ContratoDiario)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContratos(contratos: List<ContratoDiario>)

    @Delete
    suspend fun deleteContrato(contrato: ContratoDiario)

    @Query("DELETE FROM contratos")
    suspend fun deleteAllContratos()
}

@Dao
interface PerfilDao {
    @Query("SELECT * FROM perfiles ORDER BY nombre ASC")
    fun getAllPerfiles(): Flow<List<PerfilUsuario>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPerfil(perfil: PerfilUsuario)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPerfiles(perfiles: List<PerfilUsuario>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(perfil: PerfilUsuario)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(perfiles: List<PerfilUsuario>)

    @Delete
    suspend fun deletePerfil(perfil: PerfilUsuario)

    @Query("DELETE FROM perfiles")
    suspend fun deleteAllPerfiles()

    @Query("DELETE FROM perfiles WHERE usuario != :adminUsername")
    suspend fun deleteAllExceptAdmin(adminUsername: String)
}

@Dao
interface ProspectoDao {
    @Query("SELECT * FROM prospectos_censo ORDER BY timestamp DESC")
    fun getAllProspectos(): Flow<List<ProspectoCenso>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProspecto(prospecto: ProspectoCenso)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProspectos(prospectos: List<ProspectoCenso>)

    @Delete
    suspend fun deleteProspecto(prospecto: ProspectoCenso)

    @Query("DELETE FROM prospectos_censo")
    suspend fun deleteAllProspectos()
}

@Dao
interface ContratoWebDao {
    @Query("SELECT * FROM contratos_web ORDER BY timestamp DESC")
    fun getAllContratosWeb(): Flow<List<ContratoWeb>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContratoWeb(contrato: ContratoWeb)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContratosWeb(contratos: List<ContratoWeb>)

    @Delete
    suspend fun deleteContratoWeb(contrato: ContratoWeb)

    @Query("DELETE FROM contratos_web")
    suspend fun deleteAllContratosWeb()

    @Query("DELETE FROM contratos_web WHERE uuid = :uuid")
    suspend fun deleteContratoWebByUuid(uuid: String)
}

@Database(entities = [ContratoDiario::class, PerfilUsuario::class, ProspectoCenso::class, ContratoWeb::class], version = 9, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun contratoDao(): ContratoDao
    abstract fun perfilDao(): PerfilDao
    abstract fun prospectoDao(): ProspectoDao
    abstract fun contratoWebDao(): ContratoWebDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tecnicable_local_db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
