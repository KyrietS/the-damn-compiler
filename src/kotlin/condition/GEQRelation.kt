package condition

import sym.GEQ
import value.Value

class GEQRelation(v1: Value, v2: Value)
    : BinaryRelation(v1, GEQ, v2) {
    override fun toString(): String {
        return "$v1 >= $v2"
    }
}