import statement.ReadStatement
import statement.WriteStatement
import java.io.File
import java.lang.Exception
import java.util.concurrent.TimeUnit

class CodeExecutor: Visitor() {
    var hasNoRead = true

    val dasm = mutableListOf<String>()

    override fun postVisit(r: ReadStatement) {
        hasNoRead = false
    }

    override fun preVisit(p: Program): Boolean {
        val header = "Program został wykonany w trakcie kompilacji"
        dasm.add("SUB 0        # $header")
        dasm.add("INC")
        dasm.add("STORE 1")
        return true
    }

    /**
     * Wygenerowanie programu wypisującego stałe.
     */
    override fun postVisit(p: Program) {
        if(hasNoRead) {
            val output = executeProgram() ?: return
            val numbers: List<Long> = output
                .lines()
                .filter { it.isNotEmpty() }
                .map { it.toLongOrNull() ?: return@postVisit }

            if(numbers.size > 15) return

            numbers.forEach {
                generateConst(it)
                dasm.add("PUT")
            }
            dasm.add("HALT")
            val code = dasm.joinToString("\n")
            File(Config.outFilename).writeText(code + "\n")
        }
    }

    /**
     * Generuje stałą w rejestrze p0
     */
    private fun generateConst(n: Long) {
        var num = n
        dasm.add("SUB 0")
        val list = mutableListOf<String>()
        while(num != 0L) {
            when {
                num % 2 == 0L -> {
                    num /= 2
                    list.add("SHIFT 1")

                }
                n > 0 -> {
                    num -= 1
                    list.add("INC")
                }
                n < 0 -> {
                    num += 1
                    list.add("DEC")
                }
            }
        }
        list.reverse()
        dasm.addAll(list)
    }

    /**
     * Uruchamia interpreter na skompilowanym kodzie i zwraca ciąg wartości,
     * które zostały wypisane (każda zakończona znakiem '\n')
     */
    private fun executeProgram(): String? {
        val command = "${Config.interpreterPath} ${Config.outFilename}"
        try {
            val parts = command.split("\\s".toRegex())
            val proc = ProcessBuilder(*parts.toTypedArray())
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .redirectError(ProcessBuilder.Redirect.PIPE)
                .start()

            if (!proc.waitFor(5, TimeUnit.SECONDS)) {
                proc.destroy()
                return null
            }
            if (proc.exitValue() != 0) {
                return null
            }

            return proc.inputStream.bufferedReader().readText()
        } catch(e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}