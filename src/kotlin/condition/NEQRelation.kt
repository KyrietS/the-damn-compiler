package condition

import sym.NEQ
import value.Value

class NEQRelation(v1: Value, v2: Value)
    : BinaryRelation(v1, NEQ, v2) {
    override fun toString(): String {
        return "$v1 != $v2"
    }
}