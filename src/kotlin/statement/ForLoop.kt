package statement

import Visitor
import identifier.Identifier
import value.Value

abstract class ForLoop(val iter: Identifier,
                       val from: Value,
                       val to: Value,
                       statements: List<Statement>) : Loop(statements) {
    override fun accept(v: Visitor) {
        if(!v.preVisit(this)) return
        iter.accept(v)
        from.accept(v)
        to.accept(v)
        v.postVisit(this)
    }
    abstract override fun toString(): String
}