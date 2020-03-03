package statement

import Visitor
import condition.BinaryRelation

class DoWhileLoop(val cond: BinaryRelation, statements: List<Statement>): Loop(statements) {
    override fun accept(v: Visitor) {
        if(!v.preVisit(this)) return
        cond.accept(v)
        statements.forEach { it.accept(v) }
        v.postVisit(this)
    }

    override fun toString(): String {
        val sb = StringBuilder("DO").append('\n')
        sb.append(intendStatements(statements))
        sb.append("WHILE $cond ENDDO")
        return sb.toString()
    }
}