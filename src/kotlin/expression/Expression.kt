package expression

import Visitor

abstract class Expression {
    abstract fun accept(v: Visitor)
    abstract override fun toString(): String
}
