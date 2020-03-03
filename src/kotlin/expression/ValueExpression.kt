package expression

import Visitor
import value.Value

class ValueExpression(val value: Value): Expression() {
    override fun accept(v: Visitor) {
        if(!v.preVisit(this)) return
        value.accept(v)
        v.postVisit(this)
    }

    override fun toString(): String {
        return "$value"
    }
}