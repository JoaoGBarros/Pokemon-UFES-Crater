package org.br;

import java.util.List;
import java.util.Map;

public record AvailablePokemon() {

    public static final Pokemon PIKACHU = new Pokemon("Pikachu", 100, Map.of(
            "hp", 274,
            "attack", 229,
            "defense", 196,
            "speed", 306,
            "specialAttack", 218,
            "specialDefense", 218
    ), "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/back/25.png", Map.of(
            "Thunderbolt", Map.of("power", 90, "type", "Electric", "category", "Special"),
            "Quick Attack", Map.of("power", 40, "type", "Normal", "category", "Physical")
    ), List.of("Electric"));

    public static final Pokemon CHARMANDER = new Pokemon("Charmander", 100, Map.of(
            "hp", 282,
            "attack", 223,
            "defense", 203,
            "speed", 251,
            "specialAttack", 240,
            "specialDefense", 218
    ),"https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/4.png", Map.of(
            "Flamethrower", Map.of("power", 90, "type", "Fire", "category", "Special"),
            "Scratch", Map.of("power", 40, "type", "Normal", "category", "Physical")
    ), List.of("Fire"));
}
