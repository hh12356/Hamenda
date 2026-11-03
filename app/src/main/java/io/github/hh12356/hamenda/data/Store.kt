package io.github.hh12356.hamenda.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/** 单文件 JSON 存储：数据量小，读写全在主线程，保持简单。 */
class Store(private val context: Context) {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }
    private val file = File(context.filesDir, "hamenda.json")

    var data: AppData by mutableStateOf(load())
        private set

    private fun load(): AppData = try {
        if (file.exists()) json.decodeFromString<AppData>(file.readText()) else AppData()
    } catch (e: Exception) {
        AppData()
    }

    // 先写临时文件再改名，避免崩溃留下半个文件。
    private fun save() {
        val tmp = File(context.filesDir, "hamenda.json.tmp")
        tmp.writeText(json.encodeToString(data))
        if (!tmp.renameTo(file)) {
            file.delete()
            tmp.renameTo(file)
        }
    }

    fun update(transform: (AppData) -> AppData) {
        data = transform(data)
        save()
    }

    fun updateSettings(transform: (AppSettings) -> AppSettings) =
        update { it.copy(settings = transform(it.settings)) }

    fun upsertBlock(block: CourseBlock) = update { d ->
        val index = d.blocks.indexOfFirst { it.id == block.id }
        val blocks = if (index >= 0) {
            d.blocks.toMutableList().also { it[index] = block }
        } else {
            d.blocks + block
        }
        d.copy(blocks = blocks)
    }

    fun addBlocks(blocks: List<CourseBlock>) = update { d -> d.copy(blocks = d.blocks + blocks) }

    fun deleteBlock(id: Long) = update { d -> d.copy(blocks = d.blocks.filterNot { it.id == id }) }

    /** 只删这一周的这一节，其他周不动。 */
    fun skipWeek(block: CourseBlock, week: Int) =
        upsertBlock(block.copy(skippedWeeks = block.skippedWeeks + week))

    fun deleteByName(name: String) = update { d -> d.copy(blocks = d.blocks.filterNot { it.name == name }) }

    fun exportJson(): String = json.encodeToString(data)

    fun importJson(text: String): Boolean = try {
        data = json.decodeFromString<AppData>(text)
        save()
        true
    } catch (e: Exception) {
        false
    }
}
