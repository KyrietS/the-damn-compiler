package identifier

import Visitor
import java_cup.runtime.ComplexSymbolFactory

class ArrayNumIdentifier(name: String,
                         left: ComplexSymbolFactory.Location,
                         right: ComplexSymbolFactory.Location,
                         val index: Long)
    : Identifier(name, left, right) {

    override fun accept(v: Visitor) {
        super.accept(v)
        v.visit(this)
    }

    override fun toString(): String {
        return "$name($index)"
    }
}