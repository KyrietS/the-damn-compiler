import condition.*
import declaration.ArrayDeclaration
import declaration.ConstDeclaration
import declaration.Declaration
import declaration.NumberDeclaration
import expression.*
import identifier.ArrayNumIdentifier
import identifier.ArrayVarIdentifier
import identifier.Identifier
import identifier.VariableIdentifier
import statement.*
import value.IdentifierValue
import value.NumberValue

@Suppress("UNUSED_PARAMETER")
abstract class Visitor {
    /* Program */
    open fun preVisit(p: Program): Boolean { return true }
    open fun postVisit(p: Program) {}

    /* Declaration */
    open fun visit(d: Declaration) {}
    open fun visit(n: NumberDeclaration) {}
    open fun visit(a: ArrayDeclaration) {}
    open fun visit(c: ConstDeclaration) {}

    /* Statement */
    open fun preVisit(a: AssignStatement): Boolean { return true }
    open fun postVisit(a: AssignStatement) {}

    /* If */
    open fun preVisit(i: IfElseStatement): Boolean { return true }
    open fun postVisit(i: IfElseStatement) {}
    open fun preVisit(i: IfStatement): Boolean { return true }
    open fun postVisit(i: IfStatement) {}

    /* Loop */
    open fun preVisit(l: WhileDoLoop): Boolean { return true }
    open fun postVisit(l: WhileDoLoop) {}
    open fun preVisit(l: DoWhileLoop): Boolean { return true }
    open fun postVisit(l: DoWhileLoop) {}
    open fun preVisit(l: ForLoop): Boolean { return true }
    open fun postVisit(l: ForLoop) {}

    open fun preVisit(r: ReadStatement): Boolean { return true }
    open fun postVisit(r: ReadStatement) {}
    open fun preVisit(w: WriteStatement): Boolean { return true }
    open fun postVisit(w: WriteStatement) {}

    /* Condition */
    open fun preVisit(b: BinaryRelation): Boolean { return true }
    open fun postVisit(b: BinaryRelation) {}

    /* Expression */
    open fun preVisit(b: BinaryOperation): Boolean { return true }
    open fun postVisit(b: BinaryOperation) {}
    open fun preVisit(v: ValueExpression): Boolean { return true }
    open fun postVisit(v: ValueExpression) {}

    /* Identifier */
    open fun visit(i: Identifier) {}
    open fun visit(i: VariableIdentifier) {}
    open fun visit(i: ArrayNumIdentifier) {}
    open fun preVisit(i: ArrayVarIdentifier): Boolean { return true }
    open fun postVisit(i: ArrayVarIdentifier) {}

    /* Value */
    open fun visit(v: NumberValue) {}
    open fun preVisit(v: IdentifierValue): Boolean { return true }
    open fun postVisit(v: IdentifierValue) {}
}