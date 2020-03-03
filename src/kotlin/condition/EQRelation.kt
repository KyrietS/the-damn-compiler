package condition

import sym.EQ
import value.Value

class EQRelation(v1: Value, v2: Value)
    : BinaryRelation(v1, EQ, v2) {
    override fun toString(): String {
        return "$v1 == $v2"
    }
}