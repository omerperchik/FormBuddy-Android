package com.formbuddy.android.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "form_references")
data class FormReferenceEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "form_template_id")
    val formTemplateId: String,

    @ColumnInfo(name = "document_name")
    val documentName: String,

    @ColumnInfo(name = "pages_count")
    val pagesCount: Int,

    @ColumnInfo(name = "document_file_size_bytes")
    val documentFileSizeInBytes: Long,

    @ColumnInfo(name = "document_file_mime_type")
    val documentFileMimeType: String = "application/pdf",

    @ColumnInfo(name = "form_template_json")
    val formTemplateJson: String,

    @ColumnInfo(name = "document_file_path")
    val documentFilePath: String,

    @ColumnInfo(name = "thumbnail_file_path")
    val thumbnailFilePath: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "modified_at")
    val modifiedAt: Long = System.currentTimeMillis()
)
