package value

import Visitor

class NumberValue(val n: Long): Value() {
    override fun accept(v: Visitor) {
        v.visit(this)
    }

    override fun toString(): String {
        return "$n"
    }
}