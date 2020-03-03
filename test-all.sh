#!/bin/bash

# trap ctrl-c and call ctrl_c()
trap ctrl_c INT

function ctrl_c() {
    cleanup
    exit 1
}

function cleanup() {
    if [ "$mode" == "positive" ] || [ "$mode" == "manual" ] ; then
        printf "\nCzyszczenie po testach...\n"
        rm -f "$path"/*."$outext"
    fi
}

# Skrypt uruchamiające testy
ext="damn"      # Rozszerzenie pliku źródłowego
outext="dasm"   # Rozszerzenie pliku wynikowego
mode="$1"

if [ "$mode" == "positive" ] ; then
	path="tests/positive"
elif [ "$mode" == "negative" ] ; then
	path="tests/negative"
elif [ "$mode" == "manual" ] ; then
    path="tests/manual"
    ext="imp"
else
	echo "uruchamianie: podaj 'positive' lub 'negative' jako parametr"
	exit 1
fi

for FILE in "$path"/*."$ext"; do
    printf '\n%.0s' {1..20}
    echo "$FILE"
	echo
	FN="${FILE%%.*}"
	cat "$FN.$ext"
    echo "------------"
    echo "[1mExpected:[0m"
    head -n 1 "$FN.$ext"
    echo "------------"
    ./run "$FN.$ext" "$FN.$outext"
    if [ "$mode" == "positive" ] || [ "$mode" == "manual" ] ; then
	    ./tests/maszyna-wirtualna "$FN.$outext"
    fi
    #./_exercise/maszyna-rejestrowa-cln "$FN.mr"
    read -n 1 -s -r -p "Nacisnij dowolny klawisz, aby testowac dalej... "
done

    cleanup
    echo
