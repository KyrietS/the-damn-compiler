package declaration

import Visitor
import java_cup.runtime.ComplexSymbolFactory

abstract class Declaration(val name: String,
                           val left: ComplexSymbolFactory.Location,
                           val right: ComplexSymbolFactory.Location) {
    open fun accept(v: Visitor) {
        v.visit(this)
    }
    abstract override fun toString(): String
}