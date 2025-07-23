package org.br;

import java.util.List;
import java.util.Map;

public record AvailablePokemon() {

    public static final Pokemon LUCARIO = new Pokemon("Lucario", 100, Map.of(
            "hp", 281,
            "attack", 350,
            "defense", 218,
            "speed", 306,
            "specialAttack", 361,
            "specialDefense", 218
    ), "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/448.png", Map.of(
            "Aura Sphere", Map.of("power", 80, "type", "Fighting", "category", "Special"),
            "Close Combat", Map.of("power", 120, "type", "Fighting", "category", "Physical"),
            "Flash Cannon", Map.of("power", 80, "type", "Steel", "category", "Special"),
            "Extreme Speed", Map.of("power", 80, "type", "Normal", "category", "Physical")
    ), List.of("Fighting", "Steel"));

    public static final Pokemon SCEPTILE = new Pokemon("Sceptile", 100, Map.of(
            "hp", 281,
            "attack", 262,
            "defense", 218,
            "speed", 372,
            "specialAttack", 328,
            "specialDefense", 239
    ), "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/254.png", Map.of(
            "Leaf Blade", Map.of("power", 90, "type", "Grass", "category", "Physical"),
            "Dragon Pulse", Map.of("power", 85, "type", "Dragon", "category", "Special"),
            "Energy Ball", Map.of("power", 90, "type", "Grass", "category", "Special"),
            "X-Scissor", Map.of("power", 80, "type", "Bug", "category", "Physical")
    ), List.of("Grass"));

    public static final Pokemon SALAMENCE = new Pokemon("Salamence", 100, Map.of(
            "hp", 341,
            "attack", 405,
            "defense", 284,
            "speed", 328,
            "specialAttack", 350,
            "specialDefense", 284
    ), "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/373.png", Map.of(
            "Dragon Claw", Map.of("power", 80, "type", "Dragon", "category", "Physical"),
            "Fly", Map.of("power", 90, "type", "Flying", "category", "Physical"),
            "Flamethrower", Map.of("power", 90, "type", "Fire", "category", "Special"),
            "Crunch", Map.of("power", 80, "type", "Dark", "category", "Physical")
    ), List.of("Dragon", "Flying"));

    public static final Pokemon GARDEVOIR = new Pokemon("Gardevoir", 100, Map.of(
            "hp", 324,
            "attack", 229,
            "defense", 229,
            "speed", 284,
            "specialAttack", 394,
            "specialDefense", 350
    ), "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/282.png", Map.of(
            "Psychic", Map.of("power", 90, "type", "Psychic", "category", "Special"),
            "Moonblast", Map.of("power", 95, "type", "Fairy", "category", "Special"),
            "Shadow Ball", Map.of("power", 80, "type", "Ghost", "category", "Special"),
            "Thunderbolt", Map.of("power", 90, "type", "Electric", "category", "Special")
    ), List.of("Psychic", "Fairy"));

    public static final Pokemon CHARIZARD = new Pokemon("Charizard", 100, Map.of(
            "hp", 360,
            "attack", 293,
            "defense", 280,
            "speed", 328,
            "specialAttack", 348,
            "specialDefense", 295
    ), "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/6.png", Map.of(
            "Flamethrower", Map.of("power", 90, "type", "Fire", "category", "Special"),
            "Air Slash", Map.of("power", 75, "type", "Flying", "category", "Special"),
            "Dragon Claw", Map.of("power", 80, "type", "Dragon", "category", "Physical"),
            "Solar Beam", Map.of("power", 120, "type", "Grass", "category", "Special")
    ), List.of("Fire", "Flying"));

    public static final Pokemon FROSLASS = new Pokemon("Froslass", 100, Map.of(
            "hp", 281,
            "attack", 262,
            "defense", 218,
            "speed", 350,
            "specialAttack", 306,
            "specialDefense", 262
    ), "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/478.png", Map.of(
            "Ice Beam", Map.of("power", 90, "type", "Ice", "category", "Special"),
            "Shadow Ball", Map.of("power", 80, "type", "Ghost", "category", "Special"),
            "Blizzard", Map.of("power", 110, "type", "Ice", "category", "Special"),
            "Crunch", Map.of("power", 80, "type", "Dark", "category", "Physical")
    ), List.of("Ice", "Ghost"));

    public static final Pokemon MAGNEZONE = new Pokemon("Magnezone", 100, Map.of(
            "hp", 304,
            "attack", 238,
            "defense", 350,
            "speed", 218,
            "specialAttack", 394,
            "specialDefense", 262
    ), "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/462.png", Map.of(
            "Thunderbolt", Map.of("power", 90, "type", "Electric", "category", "Special"),
            "Flash Cannon", Map.of("power", 80, "type", "Steel", "category", "Special"),
            "Volt Switch", Map.of("power", 70, "type", "Electric", "category", "Special"),
            "Tri Attack", Map.of("power", 80, "type", "Normal", "category", "Special")
    ), List.of("Electric", "Steel"));

    public static List<Pokemon> getAll() {
        return List.of(
                LUCARIO, SCEPTILE, SALAMENCE, GARDEVOIR, CHARIZARD, FROSLASS, MAGNEZONE
        );
    }
}
