package value

import Visitor
import identifier.Identifier
import java_cup.runtime.ComplexSymbolFactory

class IdentifierValue(val id: Identifier): Value() {
    override fun accept(v: Visitor) {
        if(!v.preVisit(this)) return
        id.accept(v)
        v.postVisit(this)
    }

    override fun toString(): String {
        return "$id"
    }
}