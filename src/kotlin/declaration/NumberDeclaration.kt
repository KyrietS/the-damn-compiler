package declaration

import Visitor
import java_cup.runtime.ComplexSymbolFactory


class NumberDeclaration(name: String,
                        left: ComplexSymbolFactory.Location,
                        right: ComplexSymbolFactory.Location)
    : Declaration(name, left, right){
    override fun accept(v: Visitor) {
        super.accept(v)
        v.visit(this)
    }

    override fun toString(): String {
        return name
    }
}