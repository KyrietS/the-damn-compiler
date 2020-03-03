package value

import Visitor

abstract class Value {
    abstract fun accept(v: Visitor)
    abstract override fun toString(): String
}