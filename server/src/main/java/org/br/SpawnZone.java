package org.br;

import java.awt.Rectangle;

// classe da zona de spawn
public class SpawnZone {
    private final Rectangle area;
    private final double encounterChance;

    public SpawnZone(Rectangle area, double encounterChance) {
        this.area = area;
        this.encounterChance = encounterChance;
    }

    public Rectangle getArea() {
        return area;
    }

    public double getEncounterChance() {
        return encounterChance;
    }
}
