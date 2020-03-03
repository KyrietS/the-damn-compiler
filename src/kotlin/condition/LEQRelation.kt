package condition

import sym.LEQ
import value.Value

class LEQRelation(v1: Value, v2: Value)
    : BinaryRelation(v1, LEQ, v2) {
    override fun toString(): String {
        return "$v1 <= $v2"
    }
}