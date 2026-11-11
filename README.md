# Zlá Kočka

Malá Java aplikace, která po nečinnosti krátce pohne kurzorem.

## Spuštění

Přenositelný soubor `ZlaKocka.jar` vyžaduje Javu 17 nebo novější a grafické prostředí s povoleným ovládáním kurzoru.

```sh
java -jar ZlaKocka.jar
```

Na macOS může systém požadovat povolení v **Nastavení systému → Soukromí a zabezpečení → Zpřístupnění**. Na Linuxu může Wayland blokovat ovládání kurzoru, které Java v relaci X11 podporuje. Na displejích se škálováním vrací JAR kurzor na stejné souřadnice, jaké poskytuje Java AWT.

Windows 10 a 11 (x64) mají také přenosnou verzi s přibalenou Javou.

1. Rozbalte celý archiv ZIP.
2. Ve složce `ZlaKocka` spusťte `ZlaKocka.exe`. Java je součástí balíčku.
3. Volitelně vyberte dobu automatického vypnutí a klikněte na **Zapnout pohyb kurzoru**.

Od zapnutí nebo posledního zaznamenaného pohybu myši aplikace čeká 59 sekund. Pak kurzor opíše krátkou spirálu do vzdálenosti nejvýše 100 px a vrátí se na původní souřadnice. Pokud během animace pohnete myší, spirála se přeruší a aplikace začne znovu měřit nečinnost. Nastavený odpočet se měří od zapnutí funkce. Při volbě **Bez časového omezení** běží funkce až do ručního vypnutí nebo ukončení aplikace.

Minimalizací se okno schová do oznamovací oblasti nebo do řádku nabídek na macOS. Dvojklik na ikonu kočky nebo nabídka **Zobrazit okno** okno znovu otevře. Pokud grafické prostředí neposkytuje Javě oznamovací oblast, okno se minimalizuje běžným způsobem. Křížek v okně a volba **Ukončit** aplikaci ukončí.

## Sestavení ze zdrojů

JAR sestaví skript `build-jar.sh` s JDK 17 nebo novějším.

```bash
./build-jar.sh
```

Skript `build-windows.sh` používá JDK 24 nebo novější, Zig 0.17.1 a rozbalenou distribuci Temurin JRE 25 pro Windows x64. Lze ho spustit například z Bashe na macOS nebo Linuxu.

```bash
./build-windows.sh /cesta/k/windows-x64-jre ./dist
```

Výstupem je složka `dist/ZlaKocka`. Distribuujte ji celou včetně složek `app` a `runtime`.

Logiku odpočtu a pohybu lze ověřit také bez Windows.

```bash
javac --release 17 -encoding UTF-8 -d test-build src/*.java test/*.java
java -Djava.awt.headless=true -cp test-build CursorMotionTest
```
