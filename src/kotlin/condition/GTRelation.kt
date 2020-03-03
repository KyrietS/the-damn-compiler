package condition

import sym.GT
import value.Value

class GTRelation(v1: Value, v2: Value)
    : BinaryRelation(v1, GT, v2) {
    override fun toString(): String {
        return "$v1 > $v2"
    }
}