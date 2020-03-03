package expression

import Visitor
import value.Value

abstract class BinaryOperation(val v1: Value, val op: Int, val v2: Value)
    : Expression() {

    override fun accept(v: Visitor) {
        if(!v.preVisit(this)) return
        v1.accept(v)
        v2.accept(v)
        v.postVisit(this)
    }
}
