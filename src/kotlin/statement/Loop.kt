package statement

import Visitor

abstract class Loop(val statements: List<Statement>) : Statement() {
    abstract override fun accept(v: Visitor)
    abstract override fun toString(): String
}