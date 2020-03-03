import java_cup.runtime.ComplexSymbolFactory

class CompilationError(val title: String,
                       val msg: String,
                       val loc: ComplexSymbolFactory.Location? = null,
                       val loc2: ComplexSymbolFactory.Location? = null)
    : Error(msg) {

    constructor(title: String, msg: String): this(title, msg, null, null)
    constructor(error: Error, loc: ComplexSymbolFactory.Location, loc2: ComplexSymbolFactory.Location):
            this("Error", error.message!!, loc, loc2)
    constructor(cError: CompilationError): this(cError.title, cError.msg, cError.loc, cError.loc2)
}