package com.formbuddy.android.data.local.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.formbuddy.android.data.local.db.entity.FormReferenceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FormDao {
    @Query("SELECT * FROM form_references ORDER BY modified_at DESC")
    fun getAllForms(): Flow<List<FormReferenceEntity>>

    @Query("SELECT * FROM form_references WHERE id = :id")
    suspend fun getFormById(id: String): FormReferenceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertForm(form: FormReferenceEntity)

    @Update
    suspend fun updateForm(form: FormReferenceEntity)

    @Delete
    suspend fun deleteForm(form: FormReferenceEntity)

    @Query("DELETE FROM form_references WHERE id = :id")
    suspend fun deleteFormById(id: String)

    @Query("SELECT COUNT(*) FROM form_references")
    suspend fun getFormCount(): Int
}
