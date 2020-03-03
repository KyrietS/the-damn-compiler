import declaration.Declaration
import statement.Statement

class Program(val statements: List<Statement>,
              val declarations: MutableList<Declaration> = mutableListOf()) {

    constructor(statements: List<Statement>) : this(statements, mutableListOf())

    fun accept(v: Visitor) {
        if( !v.preVisit(this) ) return
        declarations.forEach { it.accept(v) }
        statements.forEach { it.accept(v) }
        v.postVisit(this)
    }
}
