package statement

import Visitor
import condition.BinaryRelation

class WhileDoLoop(val cond: BinaryRelation, statements: List<Statement>): Loop(statements) {
    override fun accept(v: Visitor) {
        if(!v.preVisit(this)) return
        cond.accept(v)
        statements.forEach { it.accept(v) }
        v.postVisit(this)
    }

    override fun toString(): String {
        val sb = StringBuilder("WHILE $cond DO").append('\n')
        sb.append(intendStatements(statements))
        sb.append("ENDWHILE")
        return sb.toString()
    }
}