package statement

import Visitor

abstract class Statement {
    abstract fun accept(v: Visitor)
    abstract override fun toString(): String

    protected fun intendStatements(statements: List<Statement>, size: Int = 4): String {
        val sb = StringBuilder()
        statements.forEach {
            val str = "$it".replace("\n", "\n" + " ".repeat(size))
            sb.append(" ".repeat(size) + "$str\n")
        }
        return sb.toString()
    }
}