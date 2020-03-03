package identifier

import Visitor
import java_cup.runtime.ComplexSymbolFactory

class ArrayVarIdentifier(name: String,
                         left: ComplexSymbolFactory.Location,
                         right: ComplexSymbolFactory.Location,
                         val index: VariableIdentifier)
    : Identifier(name, left, right) {

    override fun accept(v: Visitor) {
        super.accept(v)
        if(!v.preVisit(this)) return
        index.accept(v)
        v.postVisit(this)
    }

    override fun toString(): String {
        return "$name($index)"
    }

}