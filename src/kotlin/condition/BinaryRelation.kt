package condition

import Visitor
import value.Value

abstract class BinaryRelation(val v1: Value, val relation: Int, val v2: Value) {
    open fun accept(v: Visitor) {
        if(!v.preVisit(this)) return
        v1.accept(v)
        v2.accept(v)
        v.postVisit(this)
    }

    abstract override fun toString(): String
}