# Budowanie projektu combailera

.PHONY: build jflex cup kotlin java run test clean

# Generowanie lexera, parsera i kompilowanie źródeł.
build: kotlin java
	@echo "Kompilacja projektu przebiegła pomyślnie"

# Generowanie lexera
jflex:
	@echo "**************** JFlex ****************"
	./jflex src/jflex/Scanner.flex -d src/java/

# Generowanie parsera
cup:
	@echo "**************** CUP ****************"
	./cup -destdir src/java/ -interface -locations -parser Parser src/cup/Parser.cup

# Kompilowanie plików źródłowych *.kt
kotlin: jflex cup
	@echo "**************** Kotlin ****************"
	kotlinc -cp "lib/java-cup-11b-runtime.jar" $$(find ./src/kotlin -name "*.kt") src/java/*.java -d bin/

# Kompilowanie wygenerowanych plików *.java
java: kotlin
	@echo "**************** Java ****************"
	javac -cp "lib/java-cup-11b-runtime.jar:bin/" src/java/*.java -d bin/

# Uruchomienie skompilowanych źródeł (wymaga wcześniejszego skompilowania)
run:
	@echo "**************** RUN ****************"
	java -cp "lib/kotlin-stdlib.jar:lib/java-cup-11b-runtime.jar:bin/" MainKt

test:
	./run test.damn out.dasm
	./tests/maszyna-wirtualna out.dasm

# Czyszczenie projektu
clean:
	@echo "**************** CLEAN ****************"
	rm -Rf bin/*
	rm -Rf src/java/*
	rm -Rf out/
