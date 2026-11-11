# Zlá Kočka

Malá Java aplikace, která po nečinnosti krátce pohne kurzorem.

## Spuštění

Přenositelný soubor `ZlaKocka.jar` vyžaduje Javu 8 nebo novější a grafické prostředí s povoleným ovládáním kurzoru.

```sh
java -jar ZlaKocka.jar
```

Na macOS může systém požadovat povolení v **Nastavení systému → Soukromí a zabezpečení → Zpřístupnění**. Na Linuxu může Wayland blokovat ovládání kurzoru, které Java v relaci X11 podporuje. Na displejích se škálováním vrací JAR kurzor na stejné souřadnice, jaké poskytuje Java AWT.

Windows 10 a 11 (x64) mají také přenosnou verzi s přibalenou Javou.

1. Rozbalte celý archiv ZIP.
2. Ve složce `ZlaKocka` spusťte `ZlaKocka.exe`. Java je součástí balíčku.
3. V nabídce **Dej myšce milost za** volitelně vyberte dobu a klikněte na **Chytej myšku!**.

Od zapnutí nebo posledního zaznamenaného pohybu myši aplikace čeká 59 sekund. Pak kurzor opíše krátkou spirálu do vzdálenosti nejvýše 100 px a vrátí se na původní souřadnice. Pokud během animace pohnete myší, spirála se přeruší a aplikace začne znovu měřit nečinnost. Nastavený odpočet se měří od zapnutí funkce. Při volbě **Bez časového omezení** běží funkce až do ručního vypnutí nebo ukončení aplikace.

Okno přesunete tažením kočičí hlavy a minimalizujete tlačítkem **−**. V oznamovací oblasti nebo v řádku nabídek na macOS se oči ikony mění podle toho, zda je chytání zapnuté. Dvojklik na ikonu nebo volba **Zobrazit okno** okno znovu otevře.

Pravým kliknutím na ikonu můžete zapnout chytání bez omezení nebo na 1, 2 či 3 hodiny. Během chytání se v nabídce objeví **Nech myšku na pokoji**. Křížek v okně a volba **Ukončit** aplikaci ukončí.

Tvar a průhlednost okna závisí na grafickém prostředí. Pokud prostředí Javě neposkytuje oznamovací oblast, okno se minimalizuje běžným způsobem.

## Sestavení ze zdrojů

JAR sestaví skript `build-jar.sh` s JDK 17 nebo novějším. Výsledný soubor je kompatibilní s Javou 8.

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
javac --release 8 -encoding UTF-8 -d test-build src/*.java test/*.java
java -Djava.awt.headless=true -cp test-build CursorMotionTest
```
