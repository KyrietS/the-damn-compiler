package identifier

import Visitor
import java_cup.runtime.ComplexSymbolFactory

class VariableIdentifier(name: String,
                         left: ComplexSymbolFactory.Location,
                         right: ComplexSymbolFactory.Location)
    : Identifier(name, left, right) {

    override fun accept(v: Visitor) {
        super.accept(v)
        v.visit(this)
    }

    override fun toString(): String {
        return name
    }

}