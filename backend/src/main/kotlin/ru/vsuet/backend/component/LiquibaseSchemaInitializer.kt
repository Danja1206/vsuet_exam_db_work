package ru.vsuet.backend.component

import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import liquibase.integration.spring.SpringLiquibase
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import javax.sql.DataSource
import java.sql.DatabaseMetaData

@Component
class LiquibaseSchemaInitializer : BeanPostProcessor {

    override fun postProcessBeforeInitialization(bean: Any, beanName: String): Any {
        if (bean is SpringLiquibase) {
            logger.info("Инициализация схемы...")

            val dataSource = bean.dataSource
            val jdbcTemplate = JdbcTemplate(dataSource)

            val schemaName = extractSchemaFromDataSource(dataSource)
            if (schemaName.isNotBlank()) {
                initializeSchema(jdbcTemplate, schemaName)
            } else {
                logger.info("В переменной не указана схема, используется схема по умолчанию")
            }
        }
        return bean
    }

    private fun extractSchemaFromDataSource(dataSource: DataSource): String {
        return try {
            dataSource.connection.use { connection ->
                val metaData: DatabaseMetaData = connection.metaData
                val url = metaData.url
                extractCurrentSchemaFromUrl(url)
            }
        } catch (e: Exception) {
            logger.error("Ошибка извлечения схемы: ${e.message}")
            ""
        }
    }

    private fun extractCurrentSchemaFromUrl(url: String): String {
        return try {
            if (url.contains("?")) {
                val queryPart = url.substringAfter("?")
                val params = queryPart.split("&")
                val schemaParam = params.find { it.startsWith("currentSchema=") }
                schemaParam?.substringAfter("=") ?: ""
            } else {
                ""
            }
        } catch (e: Exception) {
            logger.error("Ошибка при извлечении из URL: ${e.message}")
            ""
        }
    }

    private fun initializeSchema(jdbcTemplate: JdbcTemplate, schemaName: String) {
        val schemaExists = checkSchemaExists(jdbcTemplate, schemaName)

        if (!schemaExists) {
            createSchema(jdbcTemplate, schemaName)
            logger.info("Схема '$schemaName' успешно создана")
        } else {
            logger.info("Схема '$schemaName' уже существует")
        }
    }

    private fun checkSchemaExists(jdbcTemplate: JdbcTemplate, schemaName: String): Boolean {
        return try {
            val sql = """
                SELECT COUNT(*) 
                FROM information_schema.schemata 
                WHERE schema_name = LOWER(?)
            """.trimIndent()

            val count = jdbcTemplate.queryForObject(sql, Int::class.java, schemaName.lowercase())
            count != null && count > 0
        } catch (e: Exception) {
            logger.error("Ошибка проверки существования схемы: ${e.message}")
            false
        }
    }

    private fun createSchema(jdbcTemplate: JdbcTemplate, schemaName: String) {
        try {
            val escapedSchemaName = "\"$schemaName\""
            val sql = "CREATE SCHEMA IF NOT EXISTS $escapedSchemaName"
            jdbcTemplate.execute(sql)
        } catch (e: Exception) {
            logger.error("Ошибка создания схемы '$schemaName': ${e.message}")
            throw e
        }
    }

    override fun postProcessAfterInitialization(bean: Any, beanName: String): Any {
        return bean
    }

    companion object {
        private val logger: Logger = LogManager.getLogger(LiquibaseSchemaInitializer::class.java)
    }
}