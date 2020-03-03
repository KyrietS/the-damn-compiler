import java_cup.runtime.Symbol;
import java_cup.runtime.ComplexSymbolFactory;
import java_cup.runtime.ComplexSymbolFactory.Location;

%%

%class Lexer

%cup
%implements sym
%char
%line
%column

// Funkcje pomocnicze (tworzenie symboli z informacją o pozycji)
%{
    private ComplexSymbolFactory symbolFactory = new ComplexSymbolFactory();

    private Symbol symbol(String name, int id) {
        return symbol(name, id, null);
    }
    private Symbol symbol(String name, int id, Object val) {
        Location left = new Location(yyline+1, yycolumn+1, yychar+1);
        Location right = new Location(yyline+1, yycolumn+yylength(), yychar+yylength());
        Symbol complexSymbol = symbolFactory.newSymbol(name, id, left, right, val);
        return (ComplexSymbolFactory.ComplexSymbol)complexSymbol;
    }

    private void compilationError(String msg) {
        Location left = new Location(yyline+1, yycolumn+1, yychar+1);
        Location right = new Location(yyline+1, yycolumn+yylength(), yychar+yylength());
        throw new CompilationError("Error", msg, left, right);
    }
%}

// Symbol zwracany po dotarciu do końca pliku
%eofval{
     return symbolFactory.newSymbol("EOF", EOF, new Location(yyline+1,yycolumn+1,yychar), new Location(yyline+1,yycolumn+1,yychar+1));
%eofval}

NEW_LINE = \n|\r\n
ID       = [_a-z]+
NUMBER   = [+-]?[1-9][0-9]*|"0"

%%

/* Komentarz */
"[" [^"["]* "]"         { /* ignore */ }

/* Słowa kluczowe */
"DECLARE"               { return symbol("DECLARE", DECLARE); }
"BEGIN"                 { return symbol("BEGIN", BEGIN); }
"END"                   { return symbol("END", END); }

"IF"                    { return symbol("IF", IF); }
"THEN"                  { return symbol("THEN", THEN); }
"ELSE"                  { return symbol("ELSE", ELSE); }
"ENDIF"                 { return symbol("ENDIF", ENDIF); }

"WHILE"                 { return symbol("WHILE", WHILE); }
"DO"                    { return symbol("DO", DO); }
"ENDWHILE"              { return symbol("ENDWHILE", ENDWHILE); }
"ENDDO"                 { return symbol("ENDDO", ENDDO); }
"FOR"                   { return symbol("FOR", FOR); }
"FROM"                  { return symbol("FROM", FROM); }
"TO"                    { return symbol("TO", TO); }
"DOWNTO"                { return symbol("DOWNTO", DOWNTO); }
"ENDFOR"                { return symbol("ENDFOR", ENDFOR); }

"READ"                  { return symbol("READ", READ); }
"WRITE"                 { return symbol("WRITE", WRITE); }

/* Liczba (stała) */
{NUMBER}                { return symbol("number", NUM, Long.parseLong(yytext())); }

/* Nazwa (zmiennej) */
{ID}                    { return symbol(yytext(), ID, yytext()); }

/* Operatory logiczne */
"EQ"                    { return symbol("EQ", EQ); }
"NEQ"                   { return symbol("NEQ", NEQ); }
"GE"                    { return symbol("GE", GT); }
"LE"                    { return symbol("LE", LT); }
"GEQ"                   { return symbol("GEQ", GEQ); }
"LEQ"                   { return symbol("LEQ", LEQ); }

/* Operatory arytmetyczne */
"PLUS"                  { return symbol("PLUS", PLUS); }
"MINUS"                 { return symbol("MINUS", MINUS); }
"TIMES"                 { return symbol("TIMES", TIMES); }
"DIV"                   { return symbol("DIV", DIV); }
"MOD"                   { return symbol("MOD", MOD); }

/* Inne operatory */
"("                     { return symbol("(", LPAREN); }
")"                     { return symbol(")", RPAREN); }
"ASSIGN"                { return symbol("ASSIGN", ASSIGN); }
","                     { return symbol(",", COMMA); }
":"                     { return symbol(":", COLON); }
";"                     { return symbol(";", SEMICOLON); }

{NEW_LINE}              { /* ignore */ }
[ \t]                   { /* ignore */ }
.                       { compilationError("Unexpected character '" + yytext() + "'"); }
