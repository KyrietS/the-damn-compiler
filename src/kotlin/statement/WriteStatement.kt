package statement

import Visitor
import value.Value

class WriteStatement(val value: Value) : Statement() {

    override fun accept(v: Visitor) {
        if(!v.preVisit(this)) return
        value.accept(v)
        v.postVisit(this)
    }

    override fun toString(): String {
        return "WRITE $value"
    }
}