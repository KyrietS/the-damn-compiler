package declaration

import Visitor
import java_cup.runtime.ComplexSymbolFactory


class ConstDeclaration(name: String)
    : Declaration(name, ComplexSymbolFactory.Location(0,0), ComplexSymbolFactory.Location(0,0)){
    override fun accept(v: Visitor) {
        super.accept(v)
        v.visit(this)
    }

    override fun toString(): String {
        return ""
    }
}