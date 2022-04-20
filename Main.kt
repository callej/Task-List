package tasklist

import java.io.File

fun main() {
    val myTasks = TaskList()
    val jsonFile = File("tasklist.json")
    if (jsonFile.exists()) myTasks.importJson(jsonFile)
    while (true) {
        println("Input an action (add, print, edit, delete, end):")
        when (readln().trim()) {
            "add" -> myTasks.addTask()
            "print" -> println(myTasks)
            "edit" -> myTasks.editTask()
            "delete" -> myTasks.deleteTask()
            "end" -> break
            else -> println("The input action is invalid")
        }
    }
    myTasks.exportJson(jsonFile)
    println("Tasklist exiting!")
}