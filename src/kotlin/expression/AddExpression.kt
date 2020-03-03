package expression

import sym.PLUS
import value.Value

class AddExpression(v1: Value, v2: Value)
    : BinaryOperation(v1, PLUS, v2 ) {

    override fun toString(): String {
        return "$v1 + $v2"
    }
}