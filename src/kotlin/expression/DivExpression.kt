package expression

import sym.DIV
import value.Value

class DivExpression(v1: Value, v2: Value)
    : BinaryOperation(v1, DIV, v2 ) {

    override fun toString(): String {
        return "$v1 / $v2"
    }
}