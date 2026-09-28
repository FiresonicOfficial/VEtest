package com.example.data.repository

import com.example.data.dao.ProjectDao
import com.example.data.entity.ProjectEntity
import kotlinx.coroutines.flow.Flow

class ProjectRepository(private val projectDao: ProjectDao) {
    val allProjects: Flow<List<ProjectEntity>> = projectDao.getAllProjects()
    val exportedVideos: Flow<List<ProjectEntity>> = projectDao.getExportedVideos()

    suspend fun getProjectById(id: Long): ProjectEntity? = projectDao.getProjectById(id)

    suspend fun insertProject(project: ProjectEntity): Long = projectDao.insertProject(project)

    suspend fun updateProject(project: ProjectEntity) = projectDao.updateProject(project)

    suspend fun deleteProjectById(id: Long) = projectDao.deleteProjectById(id)
}
