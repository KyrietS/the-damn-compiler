# The Damn Compiler

Prosty kompilator języka imperatywnego, który kompiluje język do wymyślonego Assmeblera. Skompilowany program może zostać uruchomiony na maszynie wirtualnej dołączonej w katalogu `tests/`.

## Wymagania

* Kompilator Javy (`javac`)
* Kompilator Kotlina (`kotlinc`)

Generatory JFlex i CUP zostały dołączone do projektu i można je uruchomić ręcznie poprzez skrypty `jflex` oraz `cup`. Zwykle nie ma takiej potrzeby, ponieważ są one automatycznie uruchamiane przez polecenia budujące projekt w `Makefile`.

## Makefile
`make build` - zbudowanie całego projektu.

`make jflex` - wygenerowanie lexera

`make cup` - wygenerowanie parsera

`make kotlin` - skompilowanie źródeł Kotlina

`make java` - skompilowanie źródeł Javy

`make run` - uruchomienie programu (musi być wcześniej skompilowany)

`make test` - skompilowanie pliku `test.damn` do `out.dasm` i uruchomienie na maszynie wirtualnej

`make clean` - usunięcie wszystkich skompilowanych oraz wygenerowanych źródeł. (Wygenerowany kod Javy również zostanie usunięty)

## Uruchomienie pod Linuxem

```
make build
./run test.damn out.dasm
```

## Uruchomienie pod Windowsem
TBD

## Testy

Aby zweryfikować poprawność działania kompilatora można uruchomić skrypt `test-all.sh`. Kompiluje on kolejne programy z katalogu `tests`, wyświetla jaki jest spodziewany rezultat, a następnie uruchamia program na maszynie wirtualnej.

```
./test-add.sh positive|negative
```

## Katalog `lib`

W katalogu `lib` znajdują się biblioteki **niezbędne do działania** CUP'a oraz Kotlin'a. Zwróć uwagę, że biblioteki te nie są potrzebne do skompilowania projektu.

## Katalog `build`

W katalogu `build` znajdują się biblioteki **niezbędne do zbudowania projektu**. Dokładniej mówiąc, umieszczone tam zostały skompilowane wersje JAR programów JFlex oraz CUP.

## Optymalizacje

Kompilator dokonuje wielu optymalizacji w trakcie produkowania kodu wynikowego.

* Wyrażenia `c1 PLUS c2`, `c1 MINUS c2`, `c1 TIMES c2`, gdzie `c1`,`c2` są stałymi dosłownymi, są obliczane w trakcie kompilacji i w miejsce wyrażenia jest wstawiany wynik.
* Dodawanie **PLUS**
  * Dodanie `a` i `a`, to pomnożenie przez 2 (patrz niżej)
  * Dodanie `a` i `1`, to inkrementacja `a`
  * Dodanie `a` i `-1`, to dekrementacja `a`
* Odejmowanie **MINUS**
  * Odjęcie `a` i `a`, to zawsze wynik `0`
  * Odjęcie `a` i `1`, to dekrementacja (`a` musi być po lewej stronie znaku `-`)
  * Odjęcie `a` i `-1`, to inkrementacja (`a` musi być po lewej stronie znaku `-`)
* Mnożenie **TIMES**
  * Mnożenie przez `2`, to przesunięcie. `a TIMES 2` -> `a << 1`
  * Mnożenie przez `-2`, to przesunięcie i zmiana znaku. `a TIMES -2` -> `a << 1`, `-a`
  * Mnożenie przez `1`, to brak działania
  * Mnożenie przez `-1`, to zmiana znaku
  * Mnożenie przez `0`, to wynik `0`
* Dzielenie **DIV**
  * Dzielenie przez `0`, to wynik `0`
  * Dzielenie przez `1`, to brak działania
  * Dzielenie przez `2`, to przesunięcie. `a DIV 2` -> `a >> 1`
  * Dzielenie `0` przez cokolwiek, to zawsze wynik `0`
  * Dzielenie `a` i `a` zawsze da wynik `1`, dla `a` różnego od `0`
* Reszta z dzielenia **MOD**
  * Modulo `2`, to sprawdzenie parzystości (ostatni bit liczby)
* Porównania `c1 EQ c2`, `c1 NEQ c2`, `c1 GE c2`, `c1 LE c2`, `c1 GEQ c2`, `c1 LEQ c2`, gdzie `c1` i `c2` są stałymi dosłownymi, są obliczanie w trakcie kompilacji i w miejsce ich użycia jest wstawiany wynik `1` (true) lub `0` (false)
* Porównanie wartości z zerem
  * `a == 0` | `0 == a`
  * `a != 0` | `0 != a`
  * `a > 0`  | `0 > a`
  * `a < 0`  | `0 < a`
  * `a >= 0` | `0 >= a`
  * `a <= 0` | `0 <= a`
