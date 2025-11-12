package ru.vsuet.backend.controller

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import ru.vsuet.backend.model.dto.QueryRequest
import ru.vsuet.backend.model.dto.QueryResultResponse
import ru.vsuet.backend.model.dto.SQLQueryRequest
import ru.vsuet.backend.service.DatabaseService

@RestController
@RequestMapping("/api/data")
@CrossOrigin(origins = ["*"])
class DataController(
    private val databaseService: DatabaseService
) {

    @PostMapping("/query")
    fun executeQuery(@RequestBody queryRequest: QueryRequest): List<Map<String, Any>> {
        return databaseService.executeQuery(queryRequest)
    }

    @PostMapping("/sql-command")
    fun executeSQLQuery(@RequestBody request: SQLQueryRequest): ResponseEntity<QueryResultResponse> {
        return try {
            val result = databaseService.executeSQLQuery(request.query)
            ResponseEntity.ok(QueryResultResponse(data = result))
        } catch (e: Exception) {
            val errorResponse = QueryResultResponse(
                success = false,
                error = "Ошибка выполнения SQL-запроса: ${e.message ?: "Произошла неизвестная ошибка"}"
            )
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse)
        }
    }

    @PostMapping("/export")
    fun exportData(@RequestBody queryRequest: QueryRequest): ResponseEntity<ByteArray> {
        if(queryRequest.format == null)
            return ResponseEntity(HttpStatus.BAD_REQUEST)
        val format = queryRequest.format.lowercase()
        val bytes: ByteArray
        val filename: String
        val contentType: String

        when (format) {
            "xlsx" -> {
                bytes = databaseService.exportToExcel(queryRequest)
                filename = "data.xlsx"
                contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            }
            "csv" -> {
                bytes = databaseService.exportToCsv(queryRequest)
                filename = "data.csv"
                contentType = "text/csv"
            }
            else -> throw IllegalArgumentException("Неподдерживаемый формат: $format")
        }

        return ResponseEntity.ok()
            .header("Content-Disposition", "attachment; filename=$filename")
            .contentType(org.springframework.http.MediaType.parseMediaType(contentType))
            .body(bytes)
    }

    @GetMapping("/columns/{tableName}")
    fun getTableColumns(@PathVariable tableName: String): ResponseEntity<List<String>> {
        return try {
            val columns = databaseService.getTableColumns(tableName)
            ResponseEntity.ok(columns)
        } catch (e: IllegalArgumentException) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Таблица $tableName не найдена")
        } catch (e: Exception) {
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Ошибка получения столбцов для таблицы $tableName", e)
        }
    }
}