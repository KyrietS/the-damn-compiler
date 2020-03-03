package expression

import sym.TIMES
import value.Value

class MulExpression(v1: Value, v2: Value)
    : BinaryOperation(v1, TIMES, v2 ) {

    override fun toString(): String {
        return "$v1 * $v2"
    }
}