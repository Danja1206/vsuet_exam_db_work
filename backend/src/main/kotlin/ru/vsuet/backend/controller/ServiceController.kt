package ru.vsuet.backend.controller

import org.springframework.web.bind.annotation.*
import ru.vsuet.backend.model.dto.ArchiveRequest
import ru.vsuet.backend.model.dto.BackupRequest
import ru.vsuet.backend.model.dto.BackupFile
import ru.vsuet.backend.service.BackupService

@RestController
@RequestMapping("/api/service")
@CrossOrigin(origins = ["*"])
class ServiceController(
    private val backupService: BackupService,
) {

    @PostMapping("/backup")
    fun createBackup(@RequestBody backupRequest: BackupRequest): Map<String, String> {
        val result = backupService.createBackup(backupRequest)
        return mapOf("message" to result)
    }

    @PostMapping("/restore")
    fun restoreBackup(@RequestBody request: Map<String, String>): Map<String, String> {
        val backupFile = request["backupFile"] ?: throw IllegalArgumentException("backupFile is required")
        val result = backupService.restoreBackup(backupFile)
        return mapOf("message" to result)
    }

    @PostMapping("/archive")
    fun archiveData(@RequestBody archiveRequest: ArchiveRequest): Map<String, String> {
        val result = backupService.archiveOldData(archiveRequest)
        return mapOf("message" to result)
    }

    @GetMapping("/backups")
    fun listBackups(): List<BackupFile> = backupService.listBackups()

    @GetMapping("/tables")
    fun getTables(): List<String> {
        return backupService.listTables()
    }
}