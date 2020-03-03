package condition

import sym.LT
import value.Value

class LTRelation(v1: Value, v2: Value)
    : BinaryRelation(v1, LT, v2) {
    override fun toString(): String {
        return "$v1 < $v2"
    }
}