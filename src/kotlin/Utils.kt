@file:JvmName("Utils")

import java_cup.runtime.ComplexSymbolFactory
import java.io.File

object Config {
    lateinit var srcFilename: String
    lateinit var outFilename: String
    var numOfErrors = 0
    const val interpreterPath = "./lib/interpreter"
}

const val resetFont  = "\u001B[0m"
const val boldFont   = "\u001B[1m"
const val redColor   = "\u001B[31m"
const val greenColor = "\u001B[32m"

/**
 *  Drukowanie sformatowanego błędu. Wzór: filename:line:column: title: message.
 *  Np. plik.src:4:12 Syntax Error: unexpected token '$'.
 */
fun reportError(title: String?, message: String?,
                loc: ComplexSymbolFactory.Location? = null,
                loc2: ComplexSymbolFactory.Location? = null)
{
    Config.numOfErrors++

    var msg = "${redColor}${boldFont} $title: ${resetFont}${boldFont}${message}${resetFont}"
    if(loc != null) msg = "${loc.line}:${loc.column}:" + msg

    println("${Config.srcFilename}:$msg")

    if(loc != null) printLineFromFile(loc, loc2 ?: loc)
}

/** Drukowanie linii, w której wystąpił błąd. Np.
 *  int n = 5 + number;
 *              ^~~~~~
 */
private fun printLineFromFile(loc: ComplexSymbolFactory.Location, loc2: ComplexSymbolFactory.Location) {
    var lines = listOf<String>()
    File(Config.srcFilename).useLines {
        lines = it.filterIndexed { index, _ -> index in loc.line-3 until loc.line }.toList()
    }
    if(lines.isNotEmpty()) {
        lines.forEach { println(it) }
        val numOfTabs = lines.last().subSequence(0, loc2.column).count { it == '\t' }
        print(greenColor)
        println("\t".repeat(numOfTabs) + " ".repeat(loc.column-1 - numOfTabs) + "^" + "~".repeat(loc2.offset-loc.offset))
        print(resetFont)
    }
}