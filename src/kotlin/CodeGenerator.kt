import condition.BinaryRelation
import declaration.ArrayDeclaration
import declaration.ConstDeclaration
import declaration.NumberDeclaration
import expression.BinaryOperation
import identifier.ArrayNumIdentifier
import identifier.ArrayVarIdentifier
import identifier.Identifier
import identifier.VariableIdentifier
import statement.*
import sym.*
import value.IdentifierValue
import value.NumberValue
import java.io.File

/**
 * Generator kodu wynikowego w postaci Damn Asm (*.dasm)
 *
 * Rejestry:
 *  0       Akumulator
 *  1       Wartość 1 (stała)
 *  2       Rejestr adresowy
 *  3       Rejestr A dla operacji binarnych
 *  4       Rejestr B dla operacji binarnych
 *  5..15   Rejestry pomocnicze
 */
@Suppress("FunctionName", "SameParameterValue")
class CodeGenerator: Visitor() {
    private enum class VarType {
        NUMBER, ARRAY, CONST
    }
    @Suppress("unused")
    private inner class Variable (
        val name: String,
        val type: VarType,
        val begin: Long = 0,
        val end: Long = 0,
        val size: Long = end - begin + 1,
        var offset: Long = lastVarOffset,
        val isIterator: Boolean = false
    ) {
        init {
            lastVarOffset += size
        }
    }

    private inner class Labels(
        val labels: MutableMap<String, Int> = mutableMapOf(),
        val jumps: MutableMap<Int, String> = mutableMapOf()
    )

    companion object {
        var lastVarOffset: Long = 16
        // Rejestry
        /* private const val r_acc = 0 */
        private const val r_one =       1L
        private const val r_addr =      2L
        private const val r_binop_1 =   3L
        private const val r_binop_2 =   4L
        private const val r_tmp =       5L
        private const val r_1 =         6L
        private const val r_2 =         7L
        private const val r_3 =         8L
        private const val r_neg_one =   11L
    }

    private val dasm = mutableListOf<String>()              // Linie kodu wynikowego asemblera
    private val vars = mutableMapOf<String, Variable>()     // Zmienne zadeklarowane w programie
    private val comments = mutableMapOf<Int, String>()      // Komentarze do określonych linii programu
    private val allLabels = mutableListOf<Labels>()         // Lokalne zakresy etykiet

    var accIsZero = false                                   // Informacja, czy p0 trzyma zero

    private val labels: MutableMap<String, Int>             // Lokalne etykiety
        get() = allLabels[0].labels
    private val jumps: MutableMap<Int, String>              // Lokalne skoki do etykiet (nierozwiązane)
        get() = allLabels[0].jumps

    ////////////////////////////////////////////////////
    /// PROGRAM
    ////////////////////////////////////////////////////

    override fun preVisit(p: Program): Boolean {
        comment("BEGIN")
        SUB(0)         // Wyzeruj rejestr p0
        INC()            // Wpisz wartość 1 do rejestru r_one
        STORE(r_one)
        DEC()            // Wpisz wartość -1 do rejestru r_neg_one
        DEC()
        STORE(r_neg_one)
        return true
    }
    override fun postVisit(p: Program) {
        comment("END")
        HALT()

        // Dodawanie komentarzy do kodu wynikowego
        comments.keys.forEach {
            val commentsColumn = 20
            val spaces = commentsColumn - dasm[it].length
            dasm[it] = dasm[it] + " ".repeat(spaces) + " # ${comments[it]}"
        }

        val code = dasm.joinToString("\n")
        File(Config.outFilename).writeText(code + "\n")
    }

    ////////////////////////////////////////////////////
    /// DECLARATIONS
    ////////////////////////////////////////////////////

    override fun visit(n: NumberDeclaration) {
        val variable = Variable(n.name, VarType.NUMBER)
        vars[n.name] = variable
    }
    override fun visit(a: ArrayDeclaration) {
        val variable = Variable(a.name, VarType.ARRAY, a.begin, a.end+1)
        vars[a.name] = variable
        generateConst(variable.offset - variable.begin + 1)
        STORE(variable.offset)
    }
    override fun visit(c: ConstDeclaration) {
        val variable = Variable(c.name, VarType.CONST)
        vars[c.name] = variable
        if(!accIsZero)
            SUB(0)
        comment("Miejsce na stałą ${c.name}")
        STORE(variable.offset)
        accIsZero = true
    }

    ////////////////////////////////////////////////////
    /// STATEMENTS
    ////////////////////////////////////////////////////

    override fun preVisit(a: AssignStatement): Boolean {
        comment(a)
        assignToId(a.id) {
            a.ex.accept(this)   // Ładowanie do rejestru wartości do przypisania
        }
        return false
    }

    override fun preVisit(r: ReadStatement): Boolean {
        comment(r)
        assignToId(r.id) {
            GET()                  // Ładowanie do rejestru wczytanej wartości
        }
        return false
    }

    override fun preVisit(w: WriteStatement): Boolean {
        comment(w)
        return true
    }
    override fun postVisit(w: WriteStatement) {
        PUT()
    }

    override fun preVisit(i: IfStatement): Boolean {
        preVisit(i as IfElseStatement)  // Uruchom visit dla If-Else z pustym Else
        return false
    }

    override fun preVisit(i: IfElseStatement): Boolean {
        comment("IF ${i.cond}")
        beginLabels()
        val isElse = i.elseStatements.isNotEmpty()

        i.cond.accept(this)     // Sprawdź warunek
        JZERO("else")

        i.statements.forEach { it.accept(this) }
        JUMP("end_if")

        if(isElse)
            comment("ELSE")
        LABEL("else")

        i.elseStatements.forEach { it.accept(this) }

        LABEL("end_if")
        endLabels()
        comment("ENDIF")
        return false
    }

    override fun preVisit(l: WhileDoLoop): Boolean {
        comment("WHILE ${l.cond}")
        beginLabels()

        LABEL("while")
        l.cond.accept(this)           // Sprawdź warunek
        JZERO("end_while")
        l.statements.forEach { it.accept(this) }
        JUMP("while")

        LABEL("end_while")
        comment("ENDWHILE")
        endLabels()
        return false
    }

    override fun preVisit(l: DoWhileLoop): Boolean {
        comment("DO")
        beginLabels()

        LABEL("do")
        l.statements.forEach { it.accept(this) }
        comment("WHILE ${l.cond}")
        l.cond.accept(this)         // Sprawdź warunek
        JZERO("end_do")
        JUMP("do")

        LABEL("end_do")
        comment("ENDDO")
        endLabels()
        return false
    }

    override fun preVisit(l: ForLoop): Boolean {
        beginLabels()
        comment("FOR")
        val isDownto = l is ForDowntoLoop

        // Deklaracja iteratora w pamięci
        val iterator = Variable(l.iter.name, VarType.NUMBER, 0, 1, isIterator = true)
        vars[l.iter.name] = iterator
        val iter = iterator.offset             // zmienna lokalna iteratora używana w pętli (readonly)
        val limit = iterator.offset + 1        // limit iteratora pętli
        // Uzupełnienie danych iteratora
        l.to.accept(this)           // załadowanie 'to' do pamięci
        STORE(limit)
        l.from.accept(this)         // załadowanie 'from' do pamięci
        STORE(iter)

        // while iter >= 0
        LABEL("for")
        SUB(limit)                          // iter - limit (from - to)
        if(isDownto)
            JNEG("end_for")                 // koniec pętli For-Downto
        else
            JPOS("end_for")                 // koniec pętli For-To

        l.statements.forEach { it.accept(this) }

        LOAD(iter)                     // iter++ | iter--
        if(isDownto) DEC()
        else         INC()
        STORE(iter)
        comment("ENDFOR")
        JUMP("for")
        LABEL("end_for")
        // Usunięcie iteratora z pamięci
        lastVarOffset -= iterator.size
        vars.remove(l.iter.name)
        endLabels()
        return false
    }

    ////////////////////////////////////////////////////
    /// CONDITION
    ////////////////////////////////////////////////////

    /**
     * Operacja porównania. Pozostawia w rejestrze p0
     * wartość 1 (true) lub 0 (false)
     */
    override fun preVisit(b: BinaryRelation): Boolean {
        if(optimiseBinaryRel(b)) {
            comment("Zoptymalizowano warunek")
            return false
        }
        beginLabels()

        b.v2.accept(this)
        STORE(r_binop_2)
        b.v1.accept(this)

        when(b.relation) {
            EQ -> { // v1 == v2
                SUB(r_binop_2)
                JZERO("true")
                JUMP("false")
            }
            NEQ -> { // v1 != v2
                SUB(r_binop_2)
                JUMP("end")     // jeśli różnica 0 - false, 1 - true
            }
            LT -> { // v1 < v2
                SUB(r_binop_2)
                JNEG("true")
                JUMP("false")
            }
            GT -> { // v1 > v2
                SUB(r_binop_2)
                JPOS("true")
                JUMP("false")
            }
            LEQ -> { // v1 <= v2
                SUB(r_binop_2)
                JNEG("true")
                JZERO("true")
                JUMP("false")
            }
            GEQ -> { // v1 >= v2
                SUB(r_binop_2)
                JPOS("true")
                JZERO("true")
                JUMP("false")
            }
            else -> throw CompilationError("Error", "Nieznany operator")
        }

        // false
        LABEL("false")
        SUB(0)
        JUMP("end")

        // true
        LABEL("true")
        LOAD(1)

        // koniec
        LABEL("end")

        endLabels()
        return false
    }

    ////////////////////////////////////////////////////
    /// EXPRESSION
    ////////////////////////////////////////////////////

    /**
     * Wykonuje operację i umieszcza wynik w rejestrze p0.
     */
    override fun preVisit(b: BinaryOperation): Boolean {
        comment(b)
        if(optimiseBinaryOp(b)) {
            comment("Zoptymalizowano działanie")
            return false
        }

        b.v2.accept(this)   // Ładuje prawą wartość do rejestru p0
        STORE(r_binop_2)       // Zapisuje prawą wartość do tymczasowego rejestru
        b.v1.accept(this)   // Ładuje lewą wartość do rejestru p0

        when(b.op) {
            PLUS ->  ADD(r_binop_2)
            MINUS -> SUB(r_binop_2)
            TIMES -> MUL(r_binop_2)
            DIV -> DIV(r_binop_2)
            MOD -> MOD(r_binop_2)
            else -> throw CompilationError("Error", "Nieznany operator")
        }
        return false
    }

    ////////////////////////////////////////////////////
    /// VALUES
    ////////////////////////////////////////////////////

    /**
     * Ładuje wartość zmiennej do rejestru
     */
    override fun preVisit(v: IdentifierValue): Boolean {
        // Tablica indeksowana zmienną
        if(v.id is ArrayVarIdentifier) {
            LOAD_ADDR(v.id)
            LOADI(0)
        } else {    // Adres docelowy obliczalny w trakcie kompilacji
            val variable = vars[v.id.name]!!
            var address = variable.offset
            if(v.id is ArrayNumIdentifier) {
                address += 1 + (v.id.index - variable.begin)    // +1 jest potrzebne, bo offset tablicy przechowuje
                                                                // przesunięty indeks początku (a.offset - a.begin)
            }
            LOAD(address)
        }

        return false
    }

    /**
     * Ładuje stałą do rejestru
     */
    override fun visit(v: NumberValue) {
        val constVariable = vars[v.n.toString()]!!
        val constAddress = constVariable.offset

        beginLabels()
        LOAD(constAddress)      // Wczytaj stałą z pamięci
        JZERO("gen_const")      // Jeśli wczytano zero, to stała nie była jeszcze wygenerowana
        JUMP("end")             // W przeciwnym przypadku stała znajduje się już w rejestrze
        LABEL("gen_const")
        generateConst(v.n, true) // Wygeneruj stałą
        STORE(constAddress)     // Zapisz ją w pamięci na później
        LABEL("end")
        endLabels()
    }

    private fun HALT() {
        dasm.add("HALT")
    }
    private fun GET() {
        dasm.add("GET")
    }
    private fun PUT() {
        dasm.add("PUT")
    }
    private fun LOADI(i: Long) {
        dasm.add("LOADI $i")
    }
    private fun LOAD(i: Long) {
        dasm.add("LOAD $i")
    }
    private fun STOREI(i: Long) {
        dasm.add("STOREI $i")
    }
    private fun STORE(i: Long) {
        dasm.add("STORE $i")
    }
    private fun ADD(i: Long) {
        dasm.add("ADD $i")
    }
    private fun SUB(i: Long) {
        dasm.add("SUB $i")
    }
    private fun SHIFT(i: Long) {
        dasm.add("SHIFT $i")
    }
    private fun INC() {
        dasm.add("INC")
    }
    private fun DEC() {
        dasm.add("DEC")
    }

    ////////////////////////////////////////////////////
    /// JUMPS
    ////////////////////////////////////////////////////
    @Suppress("unused")
    private fun JUMP(j: Int) {
        dasm.add("JUMP $j")
    }
    private fun JUMP(label: String) {
        jumps[dasm.size] = label
        dasm.add("JUMP %%")
    }

    @Suppress("unused")
    private fun JPOS(j: Int) {
        dasm.add("JPOS $j")
    }
    private fun JPOS(label: String) {
        jumps[dasm.size] = label
        dasm.add("JPOS %%")
    }

    @Suppress("unused")
    private fun JZERO(j: Int) {
        dasm.add("JZERO $j")
    }
    private fun JZERO(label: String) {
        jumps[dasm.size] = label
        dasm.add("JZERO %%")
    }

    @Suppress("unused")
    private fun JNEG(j: Int) {
        dasm.add("JNEG $j")
    }
    private fun JNEG(label: String) {
        jumps[dasm.size] = label
        dasm.add("JNEG %%")
    }

    ////////////////////////////////////////////////////
    /// INSTRUKCJE POMOCNICZE
    ////////////////////////////////////////////////////

    /**
     * Mnoży n * x
     * r_binop_1 := n
     * r_binop_2 := x
     * r_1       := y - wynik mnożenia
     *
     */
    private fun MUL(x: Long = r_binop_2) {
        beginLabels()
        val n = r_binop_1
        val y = r_1
        // Obecnie w acc znajduje się wartość n
        STORE(n)
        JPOS("n_is_not_negative")           // if n < 0
        JZERO("n_is_not_negative")
        NEG()                               // n := -n
        STORE(n)
        LOAD(x)                             // x := -x
        NEG()
        STORE(x)
        LABEL("n_is_not_negative")

        SUB(0)
        STORE(y)                            // y := 0

        LOAD(x)
        JPOS("x_is_positive")               // if m < 0 then acc = -m
        NEG()
        LABEL("x_is_positive")
        SUB(n)                              // x - n   < 0
        JPOS("no_swap")                     // if n > x then swap(n,m);
        SWAP(n, x)                          // swap(n,m)
        LABEL("no_swap")

        LOAD(n)
        LABEL("while")                      // while n != 0
        JZERO("end_while")
        // n % 2
        SHIFT(r_neg_one)                    // n / 2
        SHIFT(r_one)                        // n * 2
        SUB(n)                              // (n/2)*2 - n   jeśli parzysta, to zostanie zero
        JZERO("n_is_even")
        // n % 2 != 0
        LOAD(y)                             // y := y + x
        ADD(x)
        STORE(y)
        LOAD(x)                             // x := x + x
        SHIFT(r_one)
        STORE(x)
        LOAD(n)                             // n := (n-1)/2
        DEC()
        SHIFT(r_neg_one)
        STORE(n)
        JUMP("while")                       // w tym momencie acc = n

        LABEL("n_is_even")                  // n % 2 == 0
        LOAD(x)                             // x := x + x
        SHIFT(r_one)
        STORE(x)
        LOAD(n)                             // n := n / 2
        SHIFT(r_neg_one)
        STORE(n)
        JUMP("while")                       // w tym momencie acc = n

        LABEL("end_while")

        LOAD(y)
        endLabels()
    }

    /**
     * Dzielenie n / m
     * r_binop_1 := n
     * r_binop_2 := m
     * r_1       := y - wynik dzielenia
     * r_2       := c - zmienna pomocnicza (counter)
     * r_3       := neg - czy wynik będzie ujemny
     */
    @Suppress("DuplicatedCode")
    private fun DIV(m: Long = r_binop_2) {
        beginLabels()
        val n = r_binop_1
        val y = r_1
        val c = r_2
        val neg = r_3
        // Obecnie w acc znajduje się wartość n
        STORE(n)
        SUB(0)
        STORE(y)                            // y := 0
        SUB(0)                            // c := 1
        INC()
        STORE(c)
        DEC()
        STORE(neg)                          // neg = false (0)

        LOAD(m)
        JZERO("return")                     // if m = 0 then return 0

        INC()                               // if m == -1 then return -n
        JZERO("m == -1")
        JUMP("m != -1")
        LABEL("m == -1")
        LOAD(n)                             //      return -n
        NEG()
        JUMP("return")
        LABEL("m != -1")
        DEC()
        DEC()                               // if m == 1 then return n
        JZERO("m == 1")
        JUMP("m != 1")
        LABEL("m == 1")
        LOAD(n)                             //      return n
        JUMP("return")

        LABEL("m != 1")
        INC()

        JPOS("end_if_m_1")                  // if m < 0
        NEG()                               // m := -m
        STORE(m)
        LOAD(neg)                           // neg++
        INC()
        STORE(neg)
        LABEL("end_if_m_1")

        LOAD(n)
        JZERO("return")                     // if n == 0 then return 0;
        JPOS("end_if_n")                    // if n < 0
        NEG()                               // n := -n
        STORE(n)
        LOAD(neg)                           // neg--
        DEC()
        STORE(neg)
        LABEL("end_if_n")

        LABEL("while_1")
        LOAD(n)                             // while n >= m
        SUB(m)
        JNEG("end_while_1")
        LOAD(m)                             // m := m * 2
        SHIFT(r_one)
        STORE(m)
        LOAD(c)                             // c := c * 2
        SHIFT(r_one)
        STORE(c)
        JUMP("while_1")
        LABEL("end_while_1")

        LOAD(c)                             // while c != 0
        LABEL("while_2")
        JZERO("end_div")
        LOAD(n)                             // if m <= n
        SUB(m)
        JNEG("end_if")
        STORE(n)                            // n := n - m
        LOAD(y)                             // y := y + c
        ADD(c)
        STORE(y)
        LABEL("end_if")

        LOAD(m)                             // m := m / 2
        SHIFT(r_neg_one)
        STORE(m)
        LOAD(c)                             // c := c / 2
        SHIFT(r_neg_one)
        STORE(c)
        JUMP("while_2")

        LABEL("end_div")

        LOAD(neg)
        JZERO("neg")                        // if(neg == 0)
            LOAD(y)
            NEG()                           // y := -y - 1
            DEC()
            JUMP("return")
        LABEL("neg")
        LOAD(y)

        LABEL("return")                     // return y
        endLabels()
    }

    /**
     * Modulo n i m
     * r_binop_1 := n
     * r_binop_2 := m
     * r_1       := m_original - niezmieniona wartość 'm'
     * r_2       := c - zmienna pomocnicza (counter)
     * r_3       := neg - czy wynik będzie ujemny
     */
    @Suppress("DuplicatedCode")
    private fun MOD(m: Long = r_binop_2) {
        beginLabels()
        val n = r_binop_1
        val mOriginal = r_1
        val c = r_2
        val neg = r_3
        // Obecnie w acc znajduje się wartość n
        STORE(n)
        SUB(0)                            // c := 1
        INC()
        STORE(c)
        DEC()
        STORE(neg)                          // neg = false (0)

        LOAD(m)
        STORE(mOriginal)

        JZERO("return")                     // if m == 0 then return 0

        INC()                               // if m == -1 then return 0
        JZERO("return")
        DEC()
        DEC()                               // if m == 1 then return 0
        JZERO("return")
        INC()

        JPOS("end_if_m")                    // if m < 0
        NEG()                               // m := -m
        STORE(m)
        LOAD(neg)                           // neg++
        INC()
        STORE(neg)
        LABEL("end_if_m")

        LOAD(n)
        JZERO("return")                     // if n == 0 then return 0
        JPOS("end_if_n")                    // if n < 0
        NEG()                               // n := -n
        STORE(n)
        LOAD(neg)                           // neg++
        DEC()
        STORE(neg)
        LABEL("end_if_n")

        LABEL("while_1")
        LOAD(n)                             // while n >= m
        SUB(m)
        JNEG("end_while_1")
        LOAD(m)                             // m := m * 2
        SHIFT(r_one)
        STORE(m)
        LOAD(c)                             // c := c * 2
        SHIFT(r_one)
        STORE(c)
        JUMP("while_1")
        LABEL("end_while_1")

        LOAD(c)                             // while c != 0
        LABEL("while_2")
        JZERO("end_mod")
        LOAD(n)                             // if m <= n
        SUB(m)
        JNEG("end_if")
        STORE(n)                            // n := n - m

        LABEL("end_if")

        LOAD(m)                             // m := m / 2
        SHIFT(r_neg_one)
        STORE(m)
        LOAD(c)                             // c := c / 2
        SHIFT(r_neg_one)
        STORE(c)

        JUMP("while_2")

        LABEL("end_mod")

        LOAD(neg)
        JZERO("neg")                        // if(neg == 0)

        LOAD(mOriginal)
        JNEG("neg_m")                       //    if(m_original > 0)
        SUB(n)                              //    n := m_original - n
        JUMP("return")

        LABEL("neg_m")                      //    else
        ADD(n)                              //    n := n + m_original
        JUMP("return")

        LABEL("neg")                        // else
        LOAD(mOriginal)
        JPOS("load_n")                      //    if(m_original < 0)
        LOAD(n)                             //    n := -n
        NEG()
        JUMP("return")

        LABEL("load_n")
        LOAD(n)

        LABEL("return")                     // return n

        endLabels()
    }

    /**
     * Odwraca wartość w rejestrze p0 używając rejestru p15
     */
    private fun NEG() {
        comment("NEG")
        STORE(r_tmp)
        SUB(r_tmp)
        SUB(r_tmp)
    }

    /**
     * Zamienia miejscami wartość rejestru reg1 z rejestrem reg2.
     * Po operacji acc = LOAD(reg1)
     * reg1Loaded: pozwala uniknąć niepotrzebnego ładowania reg1, jeśli już znajduje się w acc.
     */
    private fun SWAP(reg1: Long, reg2: Long, reg1Loaded: Boolean = false) {
        comment("SWAP")
        if(!reg1Loaded)
            LOAD(reg1)
        STORE(r_tmp)
        LOAD(reg2)
        STORE(reg1)
        LOAD(r_tmp)
        STORE(reg2)
    }

    /**
     * Ładuje adres elementu tablicy (tab(a)) do rejestru p0
     */
    private fun LOAD_ADDR(i: ArrayVarIdentifier) {
        val array = vars[i.name]!!                           // tablica
        LOAD(array.offset)                                   // adres przesuniętego początka tablicy
        val index = vars[i.index.name]!!                     // indeks tablicy (zmienna)
        ADD(index.offset)
    }

    /**
     * Utworzenie etykiety.
     */
    private fun LABEL(label: String) {
        labels[label] = dasm.size
    }

    /**
     * Generuje stałą w rejestrze p0
     */
    private fun generateConst(n: Long, accZero: Boolean = false) {
        var num = n
        if(!accZero)
            SUB(0)
        val l = mutableListOf<String>()
        while(num != 0L) {
            when {
                num % 2 == 0L -> {
                    num /= 2
                    l.add("SHIFT 1")

                }
                n > 0 -> {
                    num -= 1
                    l.add("INC")
                }
                n < 0 -> {
                    num += 1
                    l.add("DEC")
                }
            }
        }
        l.reverse()
        dasm.addAll(l)
    }

    /**
     * Funkcja wstawia do zmiennej 'id' wartość, która znajduje
     * się w rejestrze p0.
     * Lambda 'action' powinna wstawić do rejestru odpowiednią
     * wartość, która ma zostać umieszczona w zmiennej.
     * UWAGA nie wolno w niej zmieniać wartości tymczasowego rejestru p2.
     */
    private fun assignToId(id: Identifier, action: () -> Unit) {
        if(id is ArrayVarIdentifier) {
            LOAD_ADDR(id)
            STORE(r_addr)       // Zapisz adres komórki tablicy do rejestru adresowego
            action()
            STOREI(r_addr)      // Zapisz acc do numeru komórki z rejestru adresowego
        } else {
            val variable = vars[id.name]!!
            var address = variable.offset
            if(id is ArrayNumIdentifier) {
                address += 1 + (id.index - variable.begin)  // +1 jest potrzebne, bo offset tablicy przechowuje
                                                            // przesunięty indeks początku (a.offset - a.begin)
            }
            action()
            STORE(address)
        }
    }

    /**
     * Dodanie komentarze do kolejnej instrukcji.
     */
    private fun comment(msg: Any) {
        if(comments[dasm.size] == null) {
            comments[dasm.size] = "$msg"
        } else {
            comments[dasm.size] += " | $msg"
        }
    }

    /**
     * Tworzy lokalny zakres dla etykiet.
     */
    private fun beginLabels() {
        allLabels.add(0, Labels())
    }

    /**
     * Usuwa bieżący lokalny zakres dla etykiet.
     */
    private fun endLabels() {
        // Mapowanie skoków do odpowiadających linii

        labels.forEach { (label, line) ->
            // Podmień wszystkie skoki do tej etykiety na właściwe numery wiersza
            jumps.filterValues { it == label }.keys.forEach {
                dasm[it] = dasm[it].replace("%%", "$line")
            }
        }

        allLabels.removeAt(0)
    }

    ////////////////////////////////////////////////////
    /// OPTYMALIZACJE
    ////////////////////////////////////////////////////

    /**
     * Wykonuje w trakcie kompilacji obliczenie.
     */
    private fun executeOperation(b: BinaryOperation): Boolean {
        // Jeśli jest to operacja na stałych, to wykonaj ją w trakcie kompilacji
        if(b.v1 is NumberValue && b.v2 is NumberValue) {
            val result = when(b.op) {
                PLUS -> b.v1.n + b.v2.n
                MINUS -> b.v1.n - b.v2.n
                TIMES -> b.v1.n * b.v2.n
                else -> return false
            }
            comment("$b = $result: Obliczono w trakcie kompilacji")
            generateConst(result)
            return true
        }
        return false
    }

    private fun getAddressOfId(id: Identifier): Long {
        val variable = vars[id.name]!!
        var address = variable.offset
        if(id is ArrayNumIdentifier) {
            address += 1 + (id.index - variable.begin)    // +1 jest potrzebne, bo offset tablicy przechowuje
                                                          // przesunięty indeks początku (a.offset - a.begin)
        }
        return address
    }

    /**
     * Optymalizuje operację jeśli to możliwe.
     * Zwraca true jeśli operacja została wykonana.
     */
    private fun optimiseBinaryOp(b: BinaryOperation): Boolean {

        if(executeOperation(b)) return true

        when(b.op) {
            TIMES -> {
                // Mnożenie zmiennej przez stałą c * id
                if(b.v1 is NumberValue || b.v2 is NumberValue) {
                    val numberValue = if(b.v1 is NumberValue) b.v1 else b.v2 as NumberValue
                    val elseValue = if(b.v1 is NumberValue) b.v2 else b.v1
                    // Mnożenie przez 2: przesunięcie w lewo
                    if(numberValue.n == 2L) {
                        elseValue.accept(this)
                        SHIFT(r_one)
                        return true
                    }
                    // Mnożenie przez -2: przesuń w lewo i zmień znak
                    if(numberValue.n == -2L) {
                        elseValue.accept(this)
                        SHIFT(r_one)
                        NEG()
                        return true
                    }
                    // Mnożenie przez 1: brak działania
                    if(numberValue.n == 1L) {
                        elseValue.accept(this)
                        return true
                    }
                    // Mnożenie przez -1: zmiana znaku
                    if(numberValue.n == -1L) {
                        elseValue.accept(this)
                        NEG()
                        return true
                    }
                    // Mnożenie przez 0: wynik 0
                    if(numberValue.n == 0L) {
                        SUB(0)
                        return true
                    }
                }
            }
            DIV -> {
                // id / c
                if(b.v2 is NumberValue) {
                    // Dzielenie przez 0 daje 0
                    if(b.v2.n == 0L) {
                        SUB(0)
                        return true
                    }
                    // Dzielenie przez 1 nie zmienia liczby
                    if(b.v2.n == 1L) {
                        b.v1.accept(this)
                        return true
                    }
                    // Dzielenie przez 2 przesuwa liczbę
                    if(b.v2.n == 2L) {
                        b.v1.accept(this)
                        SHIFT(r_neg_one)
                        return true
                    }
                }
                if(b.v1 is NumberValue) {
                    // Dzielenie zera zawsze da zero
                    if(b.v1.n == 0L) {
                        SUB(0)
                        return true
                    }
                }
                if(b.v1 is NumberValue && b.v2 is NumberValue) {
                    // Dzielenie a / a zawsze da 1
                    if(b.v1.n == b.v2.n && b.v1.n != 0L && b.v2.n != 0L) {
                        SUB(0)
                        INC()
                        return true
                    }
                }
            }
            MOD -> {
                // id % c
                if(b.v2 is NumberValue) {
                    // Sprawdzenie parzystości a % 2
                    if(b.v2.n == 2L) {
                        b.v1.accept(this)
                        comment("Sprawdzenie parzystości")
                        STORE(r_tmp)
                        SHIFT(r_neg_one)
                        SHIFT(r_one)
                        SUB(r_tmp)
                        NEG()
                        return true
                    }
                }
            }
            PLUS -> {
                // id + id (gdzie id nie jest tablicą)
                if(b.v1 is IdentifierValue && b.v2 is IdentifierValue &&
                        b.v1.id is VariableIdentifier && b.v2.id is VariableIdentifier) {
                    // Dodanie a + a: pomnożenie przez 2 (przesunięcie)
                    if(b.v1.id.name == b.v2.id.name) {
                        b.v1.accept(this)
                        SHIFT(r_one)
                        return true
                    }
                }
                if(b.v1 is NumberValue || b.v2 is NumberValue) {
                    val numberValue = if(b.v1 is NumberValue) b.v1 else b.v2 as NumberValue
                    val elseValue = if(b.v1 is NumberValue) b.v2 else b.v1
                    // Dodanie jedynki to inkrementacja
                    if(numberValue.n == 1L) {
                        elseValue.accept(this)
                        INC()
                        return true
                    }
                    // Dodanie -1 to dekrementacja
                    if(numberValue.n == -1L) {
                        elseValue.accept(this)
                        DEC()
                        return true
                    }
                }
            }
            MINUS -> {
                // id - id (gdzie id nie jest tablicą)
                if(b.v1 is IdentifierValue && b.v2 is IdentifierValue &&
                        b.v1.id is VariableIdentifier && b.v2.id is VariableIdentifier) {
                    // Odjęcie a - a: zawsze da wynik 0
                    if(b.v1.id.name == b.v2.id.name) {
                        SUB(0)
                        return true
                    }
                }
                if(b.v2 is NumberValue) {
                    // Odjęcie 1 to dekrementacja
                    if(b.v2.n == 1L) {
                        b.v1.accept(this)
                        DEC()
                        return true
                    }
                    // Odjęcie -1 to inkrementacja
                    if(b.v2.n == -1L) {
                        b.v1.accept(this)
                        INC()
                        return true
                    }
                }
            }
        }

        // Dodanie / Odjęcie zmiennej bez ładowania jej do pamięci: LOAD(v1) ADD/SUB (v2.address)
        if(b.v2 is IdentifierValue && b.v2.id !is ArrayVarIdentifier) {
            val address = getAddressOfId(b.v2.id)

            when(b.op) {
                PLUS -> {
                    b.v1.accept(this)
                    ADD(address)
                    return true
                }
                MINUS ->  {
                    b.v1.accept(this)
                    SUB(address)
                    return true
                }
            }
        }

        // Dodanie zmiennej bez ładowania jej do pamięci: LOAD(v2) ADD (v1.address)
        if(b.v1 is IdentifierValue && b.v1.id !is ArrayVarIdentifier) {
            val address = getAddressOfId(b.v1.id)

            when(b.op) {
                PLUS -> {
                    b.v2.accept(this)
                    ADD(address)
                    return true
                }
            }
        }

        return false
    }

    /**
     * Wykonuje w trakcie kompilacji obliczenie.
     */
    private fun executeRelation(b: BinaryRelation): Boolean {
        // Jeśli jest to relacja na stałych, to wykonaj ją w trakcie kompilacji
        if(b.v1 is NumberValue && b.v2 is NumberValue) {
            val result = when(b.relation) {
                EQ -> b.v1.n == b.v2.n
                NEQ -> b.v1.n != b.v2.n
                GT -> b.v1.n > b.v2.n
                LT -> b.v1.n < b.v2.n
                GEQ -> b.v1.n >= b.v2.n
                LEQ -> b.v1.n <= b.v2.n
                else -> return false
            }
            comment("$b = $result: Obliczono w trakcie kompilacji")
            SUB(0)      // false
            if(result) {
                INC()     // true
            }
            return true
        }
        return false
    }

    private fun optimiseBinaryRel(b: BinaryRelation): Boolean {
        if(executeRelation(b)) return true

        beginLabels()

        // Kod optymalizujący. Zwraca 'true' jeśli wykonano optymalizację.
        // W przeciwnym przypadku 'false'.
        val optimised =  run optimised@{
            // Porównanie ze stałą a == 0
            if (b.v1 is NumberValue || b.v2 is NumberValue) {
                val numberValue = if (b.v2 is NumberValue) b.v2 else b.v1 as NumberValue
                val elseValue = if (b.v2 is NumberValue) b.v1 else b.v2
                var relation = b.relation

                elseValue.accept(this)  // załaduj zmienną 'a'
                // Porównanie z zerem
                if (numberValue.n == 0L) {
                    comment("Optymalizacja relacji")
                    if (b.v1 is NumberValue && b.v1.n == 0L) { // Przy porównaniu chcemy zero po prawej stronie
                        relation = when (relation) {
                            GT -> LT        // 0 > a --> a < 0
                            LT -> GT        // 0 < a --> a > 0
                            GEQ -> LEQ      // 0 >= a --> a <= 0
                            LEQ -> GEQ      // 0 <= a --> a >= 0
                            else -> relation
                        }
                    }
                    when (relation) {
                        EQ -> { // a == 0
                            JZERO("inc")    // true  - acc = 0++
                            JUMP("false")   // false - acc = <>0
                        }
                        NEQ -> { // a != 0
                            JUMP("end")     // 0 - false, else - true
                        }
                        GT -> { // a > 0
                            JPOS("end")     // true - acc > 0
                            JUMP("false")   // false
                        }
                        LT -> { // a < 0
                            JNEG("end")     // true - acc < 0
                            JUMP("false")
                        }
                        GEQ -> { // a >= 0
                            JPOS("end")     // true - acc > 0
                            JZERO("inc")    // true - acc = 0++
                            JUMP("false")   // false - acc = <>0
                        }
                        LEQ -> { // a <= 0
                            JNEG("end")     // true - acc < 0
                            JZERO("inc")    // true - acc = 0++
                            JUMP("false")   // false - acc = <>0
                        }
                        else -> return@optimised false  // nieznana operacja nie będzie zoptymalizowana
                    }
                    return@optimised true
                }
            }
            return@optimised false
        }

        LABEL("false")
        SUB(0)
        JUMP("end")
        LABEL("true")   // ustaw wartość 1 i zwróć
        SUB(0)
        LABEL("inc")    // inkrementuj acc i zwróć
        INC()

        LABEL("end")

        endLabels()
        return optimised
    }

}