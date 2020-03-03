package statement

import Visitor
import condition.BinaryRelation

class IfStatement(cond: BinaryRelation, statements: List<Statement>)
    : IfElseStatement(cond, statements, emptyList()) {
    override fun accept(v: Visitor) {
        if(!v.preVisit(this)) return
        cond.accept(v)
        statements.forEach { it.accept(v) }
        v.postVisit(this)
    }

    override fun toString(): String {
        val sb = StringBuilder("IF $cond THEN").append('\n')
        sb.append(intendStatements(statements))
        sb.append("ENDIF")
        return sb.toString()
    }
}