package expression

import sym.MOD
import value.Value

class ModExpression(v1: Value, v2: Value)
    : BinaryOperation(v1, MOD, v2 ) {

    override fun toString(): String {
        return "$v1 % $v2"
    }
}