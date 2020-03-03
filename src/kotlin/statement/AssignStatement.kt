package statement

import Visitor
import expression.Expression
import identifier.Identifier

class AssignStatement(val id: Identifier, val ex: Expression): Statement() {

    override fun accept(v: Visitor) {
        if(!v.preVisit(this)) return
        id.accept(v)
        ex.accept(v)
        v.postVisit(this)
    }

    override fun toString(): String {
        return "$id = $ex"
    }
}