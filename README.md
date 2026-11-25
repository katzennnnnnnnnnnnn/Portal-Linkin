# Portal Link Helper (Fabric, Minecraft 26.1.x, Einzelspieler)

Zwei Nether-Portale sind **automatisch verbunden**, wenn in den vier Rahmen-Ecken
dieselben Glazed-Terracotta-Blöcke stehen (Reihenfolge/Position der Ecken egal).
Keine Koordinaten, kein Rechnen. Kein Glazed in den Ecken = ganz normales Vanilla-Portal.

Beispiel: Overworld-Portal mit 2x blauem + 2x rotem Glazed in den Ecken -> führt zu dem Nether-Portal,
das ebenfalls 2x blau + 2x rot hat, egal wie weit weg.

Wichtig: Die Ecken sind die 4 Blöcke diagonal außen am Rahmen (dort, wo man sonst keinen Obsidian braucht).
Ein Portal wird gemerkt, sobald du einmal in seiner Nähe (16 Blöcke) warst oder es benutzt.

Wird in der Welt in `portal-link-helper.txt` gespeichert.

Bauen: GitHub Actions (siehe .github/workflows/build.yml), Jar `portal-link-helper-1.0.0.jar`, braucht Fabric API.
