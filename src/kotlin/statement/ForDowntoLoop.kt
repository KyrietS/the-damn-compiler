package statement

import identifier.Identifier
import value.Value

class ForDowntoLoop(iter: Identifier,
                    from: Value,
                    to: Value,
                    statements: List<Statement>)
    : ForLoop(iter, from, to, statements) {

    override fun toString(): String {
        val sb = StringBuilder("FOR $iter FROM $from DOWNTO $to DO").append('\n')
        sb.append(intendStatements(statements))
        sb.append("ENDFOR")
        return sb.toString()
    }
}