package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val durationMs: Long,
    val clipCount: Int,
    val outputPath: String? = null,
    val isExported: Boolean = false,
    val thumbnailUri: String? = null,
    val clipsJson: String = ""
)
