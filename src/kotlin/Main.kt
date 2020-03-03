import java.io.BufferedReader
import java.io.FileReader
import kotlin.system.exitProcess
import java_cup.runtime.ComplexSymbolFactory

fun main(args: Array<String>) {
    if (args.size != 2) {
        println("Niepoprawne uruchomienie programu")
        println("<source> <output>")
        exitProcess(-1)
    }

    Config.srcFilename = args[0]
    Config.outFilename = args[1]
    val reader = BufferedReader(FileReader(Config.srcFilename))
    val lexer = Lexer(reader)

    val symbolFactory = ComplexSymbolFactory()
    val parser = Parser(lexer, symbolFactory)

    try {
        val program = parser.parse().value as Program
        compile(program)
    } catch (e: CompilationError) {
        reportError(e.title, e.msg, e.loc, e.loc2)
    } catch(e: Throwable) {
        reportError("Error", e.message ?: e.javaClass.canonicalName)
    } finally {
        if(Config.numOfErrors == 1) {
            println("1 error occurred.")
            exitProcess(1)
        } else if(Config.numOfErrors > 1) {
            println("${Config.numOfErrors} errors occurred.")
            exitProcess(Config.numOfErrors)
        }
    }
}

fun compile(p: Program) {
    val staticAnalyzer = StaticAnalyzer(p)
    p.accept(staticAnalyzer)

    val codeGenerator = CodeGenerator()
    p.accept(codeGenerator)

    val codeExecutor = CodeExecutor()
    p.accept(codeExecutor)
}