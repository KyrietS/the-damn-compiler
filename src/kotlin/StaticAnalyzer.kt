import declaration.ArrayDeclaration
import declaration.ConstDeclaration
import declaration.Declaration
import declaration.NumberDeclaration
import identifier.ArrayNumIdentifier
import identifier.ArrayVarIdentifier
import identifier.Identifier
import identifier.VariableIdentifier
import statement.AssignStatement
import statement.ForLoop
import statement.ReadStatement
import value.IdentifierValue
import value.NumberValue

/**
 * Analizator statyczny zgłaszający błędy semantyczne.
 *
 * @throws [CompilationError] gdy analiza się nie powiedzie.
 */
class StaticAnalyzer(val program: Program): Visitor() {

    enum class SymbolType {
        NUMBER, ARRAY
    }
    enum class SymbolScope {
        GLOBAL, LOOP
    }
    data class Symbol(
        val name: String,
        val type: SymbolType,
        val scope: SymbolScope,
        var assigned: Boolean = false
    )

    private val symbolTable = HashSet<Symbol>()

    /**
     * Redeklaracja zmiennej.
     */
    override fun visit(d: Declaration) {
        if(symbolTable.any { it.name == d.name }) {
            throw CompilationError(
                "Error",
                "Variable '${d.name}' is already declared.", d.left, d.right
            )
        }
    }

    /**
     * Deklaracja zmiennej tablicowej.
     * Niepoprawny zakres indeksów tablicy.
     */
    override fun visit(a: ArrayDeclaration) {
        if(a.begin > a.end) {
            throw CompilationError(
                "Error",
                "Array index range is incorrect.", a.left, a.right
            )
        }

        val arraySymbol = Symbol(a.name, SymbolType.ARRAY, SymbolScope.GLOBAL)
        symbolTable.add(arraySymbol)
    }

    /**
     * Deklaracja zmiennej liczbowej.
     */
    override fun visit(n: NumberDeclaration) {
        val numberSymbol = Symbol(n.name, SymbolType.NUMBER, SymbolScope.GLOBAL)
        symbolTable.add(numberSymbol)
    }

    /**
     * Użycie stałej.
     */
    override fun visit(v: NumberValue) {
        val declaration = ConstDeclaration(v.n.toString())
        program.declarations.add(declaration)
    }

    /**
     * Iterator pętli przesłania wcześniej zadeklarowaną zmienną
     */
    override fun preVisit(l: ForLoop): Boolean {
        // Nie zezwalam na przesłonięcie nazwy zmiennej przez iterator pętli
        if (symbolTable.any { it.name == l.iter.name }) {
            throw CompilationError(
                "Error", "For-Loop iterator '${l.iter.name}'" +
                        " is already declared.", l.iter.left, l.iter.right
            )
        }
        l.from.accept(this)
        l.to.accept(this)
        // Tworzę zmienną lokalną
        val iterSymbol = Symbol(l.iter.name, SymbolType.NUMBER, SymbolScope.LOOP, true)
        symbolTable.add(iterSymbol)

        l.statements.forEach { it.accept(this) }

        // Usuwam zmienną lokalną
        symbolTable.remove(iterSymbol)
        return false
    }

    /**
     * Użycie niezadeklarowanej zmiennej.
     */
    override fun visit(i: Identifier) {
        if(symbolTable.none { it.name == i.name })
            throw CompilationError("Error", "Undeclared variable: '${i.name}'", i.left, i.right)
    }

    /**
     * Użycie tablicy w kontekście liczby.
     */
    override fun visit(i: VariableIdentifier) {
        // Użycie tablicy w kontekście liczby.
        if(symbolTable.any { it.name == i.name && it.type != SymbolType.NUMBER }) {
            throw CompilationError(
                "Error",
                "Array variable '${i.name}' used in a context of a number.", i.left, i.right
            )
        }
    }

    /**
     * Użycie liczby w kontekście tablicy.
     */
    override fun visit(i: ArrayNumIdentifier) {
        if(symbolTable.any { it.name == i.name && it.type != SymbolType.ARRAY }) {
            throw CompilationError(
                "Error",
                "Number variable '${i.name}' used in a context of an array.", i.left, i.right
            )
        }
    }

    /**
     * Użycie liczby w kontekście tablicy.
     */
    override fun postVisit(i: ArrayVarIdentifier) {
        if(symbolTable.any { it.name == i.name && it.type != SymbolType.ARRAY }) {
            throw CompilationError(
                "Error",
                "Number variable '${i.name}' used in a context of an array.", i.left, i.right
            )
        }
    }

    /**
     * Wpisanie wartości do zmiennej
     * Wczytanie wartości (READ) do iteratora pętli.
     * Zmiana iteratora pętli FOR.
     */
    override fun postVisit(r: ReadStatement) {
        val readTarget = r.id
        val readTargetSymbol = symbolTable.find { it.name == readTarget.name }
        if(readTargetSymbol?.scope == SymbolScope.LOOP) {
            throw CompilationError(
                "Error",
                "Cannot READ value into For-Loop iterator.", readTarget.left, readTarget.right
            )
        }
        // Zmienna została zainicjalizowana
        readTargetSymbol?.assigned = true
    }

    /**
     * Wpisanie wartości do zmiennej
     * Zmiana iteratora pętli FOR.
     */
    override fun postVisit(a: AssignStatement) {
        val assignTarget = a.id
        val assignTargetSymbol = symbolTable.find { it.name == assignTarget.name }
        if(assignTargetSymbol?.scope == SymbolScope.LOOP) {
            throw CompilationError(
                "Error",
                "Cannot ASSIGN value into For-Loop iterator.", assignTarget.left, assignTarget.right
            )
        }
        // Zmienna została zainicjalizowana
        assignTargetSymbol?.assigned = true
    }

    /**
     * Odczyt niezainicjalizowanej wartości
     */
    override fun postVisit(v: IdentifierValue) {
        val readTarget = v.id
        val usedSymbol = symbolTable.find { it.name == readTarget.name }
        // Użycie niezainicjalizowanej zmiennej
        if(usedSymbol?.assigned == false) {
           throw CompilationError(
               "Error",
               "Variable '${usedSymbol.name}' is used before being assigned.", readTarget.left, readTarget.right
           )
        }

        // Użycie niezainicjalizowanej zmiennej jako indeks tablicy
        if(readTarget is ArrayVarIdentifier) {
            val usedIndexSymbol = symbolTable.find { it.name == readTarget.index.name }
            if(usedIndexSymbol?.assigned == false) {
                throw CompilationError(
                    "Error",
                    "Variable '${usedIndexSymbol.name}' is used before being assigned.", readTarget.index.left, readTarget.index.right
                )
            }
        }
    }
}