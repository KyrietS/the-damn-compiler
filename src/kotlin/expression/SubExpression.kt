package expression

import sym.MINUS
import value.Value

class SubExpression(v1: Value, v2: Value)
    : BinaryOperation(v1, MINUS, v2 ) {

    override fun toString(): String {
        return "$v1 - $v2"
    }
}