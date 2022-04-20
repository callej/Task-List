package tasklist

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.datetime.*
import java.io.File
import kotlin.math.sign

enum class TABLE(val dim: Int, val header: String, val content: (TaskList, Task) -> String) {
    NUMBER(4, "N", { taskList, task -> (taskList.tasks().indexOf(task) + 1).toString() }),
    DATE(12, "Date", { _, task -> task.date }),
    TIME(7, "Time", { _, task -> task.time }),
    PRIO(3, "P", { _, task -> task.prio() }),
    DUE(3, "D", { _, task -> task.due() }),
    TASK(44, "Task", { _, task -> task.task })
}

enum class COLOR(val mark: String) {
    RED("\u001B[101m \u001B[0m"),
    YELLOW("\u001B[103m \u001B[0m"),
    GREEN("\u001B[102m \u001B[0m"),
    BLUE("\u001B[104m \u001B[0m"),
}

data class Task(var task: String, var priority: String, var date: String, var time: String) {

    fun due(): String {
        val (taskYear, taskMonth, taskDay) = date.split("-").map { it.toInt() }
        val taskDate = LocalDate(taskYear, taskMonth, taskDay)
        val currentDate = Clock.System.now().toLocalDateTime(TimeZone.of("UTC-8")).date
        return when (currentDate.daysUntil(taskDate).sign) {
            1 -> COLOR.GREEN.mark
            0 -> COLOR.YELLOW.mark
            else -> COLOR.RED.mark
        }
    }

    fun prio(): String {
        return when (priority) {
            "C" -> COLOR.RED.mark
            "H" -> COLOR.YELLOW.mark
            "N" -> COLOR.GREEN.mark
            else -> COLOR.BLUE.mark
        }
    }
}

class TaskList {
    private val taskList = emptyList<Task>().toMutableList()
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val type = Types.newParameterizedType(MutableList::class.java, Task::class.java)
    private val taskListAdapter = moshi.adapter<MutableList<Task>>(type)

    fun tasks() = taskList

    fun importJson(jsonFile: File) {
        taskListAdapter.fromJson(jsonFile.readText())!!.forEach { taskList.add(it) }
    }

    fun exportJson(jsonFile: File) {
        jsonFile.writeText(taskListAdapter.toJson(taskList))
    }

    fun addTask() {
        val priority = getPriority()
        val date = getDate()
        val time = getTime()
        val task = getTask()
        if (task == "") {
            println("The task is blank")
        } else {
            taskList.add(Task(task, priority, date, time))
        }
    }

    fun editTask() {
        println(this)
        if (taskList.isEmpty()) return
        val taskNumber = getTaskNumber()
        while (true) {
            println("Input a field to edit (priority, date, time, task):")
            when (readln()) {
                "priority" -> {taskList[taskNumber - 1].priority = getPriority(); break}
                "date" -> {taskList[taskNumber - 1].date = getDate(); break}
                "time" -> {taskList[taskNumber - 1].time = getTime(); break}
                "task" -> {taskList[taskNumber - 1].task = getTask(); break}
                else -> println("Invalid field")
            }
        }
        println("The task is changed")
    }

    fun deleteTask() {
        println(this)
        if (taskList.isEmpty()) return
        taskList.removeAt(getTaskNumber() - 1)
        println("The task is deleted")
    }

    private fun getTaskNumber(): Int {
        var taskNumber: String
        while (true) {
            println("Input the task number (1-${taskList.size}):")
            taskNumber = readln()
            if (Regex("\\d+").matches(taskNumber) && taskNumber.toInt() in 1..taskList.size) {
                break
            } else {
                println("Invalid task number")
            }
        }
        return taskNumber.toInt()
    }

    private fun getPriority(): String {
        var priority: String
        while (true) {
            println("Input the task priority (C, H, N, L):")
            priority = readln().uppercase()
            if (priority in listOf("C", "H", "N", "L")) break
        }
        return priority
    }

    private fun getDate(): String {
        var d: List<String>
        while (true) {
            println("Input the date (yyyy-mm-dd):")
            try {
                d = readln().split("-", limit = 3)
                LocalDate(d[0].toInt(), d[1].toInt(), d[2].toInt())
            } catch (e: Exception) {
                println("The input date is invalid")
                continue
            }
            if (d[0].length != 4) {
                println("The input date is invalid")
                continue
            }
            break
        }
        return "${d[0]}-${if (d[1].length == 1) "0" else ""}${d[1]}-${if (d[2].length == 1) "0" else ""}${d[2]}"
    }

    private fun getTime(): String {
        var t: List<String>
        while (true) {
            println("Input the time (hh:mm):")
            try {
                t = readln().split(":", limit = 2)
                if (!(t[0].toInt() in 0..23 && t[1].toInt() in 0..59)) throw Exception("Invalid time")
            } catch (e: Exception) {
                println("The input time is invalid")
                continue
            }
            break
        }
        return "${if (t[0].length == 1) "0" else ""}${t[0]}:${if (t[1].length == 1) "0" else ""}${t[1]}"
    }

    private fun getTask(): String {
        println("Input a new task (enter a blank line to end):")
        var task = ""
        while (true) {
            val input = readln().trim()
            if (input == "") break
            task += "$input\n"
        }
        return task.dropLast(1)
    }

    private fun divString(): String {
        var div = "+"
        TABLE.values().forEach { div += "-".repeat(it.dim) + "+" }
        return div
    }

    private fun tableHead(): String {
        var head = divString()
        head += "\n|"
        for (item in TABLE.values()) {
            val spaces = if (item.header == "Task") (item.dim - item.header.length) / 2 - 1 else (item.dim - item.header.length) / 2
            head += " ".repeat(spaces) + item.header
            head += " ".repeat(item.dim - (item.header.length + spaces)) + "|"
        }
        return head + "\n" + divString()
    }

    private fun newTaskLine(): String {
        var line = "|"
        TABLE.values().dropLast(1).forEach { line += " ".repeat(it.dim) + "|" }
        return line
    }

    override fun toString(): String {
        if (taskList.isEmpty()) {
            return "No tasks have been input"
        } else {
            var tStr = tableHead()
            for (item in taskList) {
                tStr += "\n|"
                for (f in TABLE.values().toList().dropLast(1)) {
                    val contentLength = if ("\u001B" in f.content(this, item)) 1 else f.content(this, item).length
                    tStr += " ".repeat((f.dim - contentLength) / 2)
                    tStr += f.content(this, item)
                    tStr += " ".repeat(f.dim - (contentLength + (f.dim - contentLength) / 2)
                    )
                    tStr += "|"
                }
                val task = item.task.split("\n").map { it.chunked(TABLE.TASK.dim) }.flatten()
                if (task.isEmpty()) {
                    tStr += " ".repeat(TABLE.TASK.dim) + "|"
                } else {
                    tStr += task[0] + " ".repeat(TABLE.TASK.dim - task[0].length) + "|"
                    for (taskLine in task.drop(1)) {
                        tStr += "\n" + newTaskLine()
                        tStr += taskLine + " ".repeat(TABLE.TASK.dim - taskLine.length) + "|"
                    }
                }
                tStr += "\n" + divString()
            }
            return tStr
        }
    }
}