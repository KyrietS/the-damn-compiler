package statement

import identifier.Identifier
import value.Value

class ForToLoop(iter: Identifier,
                from: Value,
                to: Value,
                statements: List<Statement>)
    : ForLoop(iter, from, to, statements) {

    override fun toString(): String {
        val sb = StringBuilder("FOR $iter FROM $from TO $to DO").append('\n')
        sb.append(intendStatements(statements))
        sb.append("ENDFOR")
        return sb.toString()
    }
}