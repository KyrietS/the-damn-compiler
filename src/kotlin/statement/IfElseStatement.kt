package statement

import Visitor
import condition.BinaryRelation

open class IfElseStatement(val cond: BinaryRelation,
                      val statements: List<Statement>,
                      val elseStatements: List<Statement>)
    : Statement() {
    override fun accept(v: Visitor) {
        if(!v.preVisit(this)) return
        cond.accept(v)
        statements.forEach { it.accept(v) }
        elseStatements.forEach { it.accept(v) }
        v.postVisit(this)
    }

    override fun toString(): String {
        val sb = StringBuilder("IF $cond THEN").append('\n')
        sb.append(intendStatements(statements))
        sb.append("ELSE")
        sb.append(intendStatements(elseStatements))
        sb.append("ENDIF")
        return sb.toString()
    }
}