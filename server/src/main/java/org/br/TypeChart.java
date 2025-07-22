package org.br;

import java.util.List;
import java.util.Map;

public class TypeChart {
    public static final Map<String, Map<String, List<String>>> TYPE_RELATIONS = Map.ofEntries(
            Map.entry("Normal", Map.of(
                    "vantagens", List.of(),
                    "desvantagens", List.of("Rock", "Steel"),
                    "imune", List.of("Ghost")
            )),
            Map.entry("Fire", Map.of(
                    "vantagens", List.of("Grass", "Bug", "Ice", "Steel"),
                    "desvantagens", List.of("Fire", "Water", "Rock", "Dragon"),
                    "imune", List.of()
            )),
            Map.entry("Water", Map.of(
                    "vantagens", List.of("Fire", "Ground", "Rock"),
                    "desvantagens", List.of("Water", "Grass", "Dragon"),
                    "imune", List.of()
            )),
            Map.entry("Electric", Map.of(
                    "vantagens", List.of("Water", "Flying"),
                    "desvantagens", List.of("Electric", "Grass", "Dragon"),
                    "imune", List.of("Ground")
            )),
            Map.entry("Grass", Map.of(
                    "vantagens", List.of("Water", "Ground", "Rock"),
                    "desvantagens", List.of("Fire", "Grass", "Poison", "Flying", "Bug", "Dragon", "Steel"),
                    "imune", List.of()
            )),
            Map.entry("Ice", Map.of(
                    "vantagens", List.of("Grass", "Ground", "Flying", "Dragon"),
                    "desvantagens", List.of("Fire", "Water", "Ice", "Steel"),
                    "imune", List.of()
            )),
            Map.entry("Fighting", Map.of(
                    "vantagens", List.of("Normal", "Rock", "Steel", "Ice", "Dark"),
                    "desvantagens", List.of("Poison", "Flying", "Psychic", "Bug", "Fairy"),
                    "imune", List.of("Ghost")
            )),
            Map.entry("Poison", Map.of(
                    "vantagens", List.of("Grass", "Fairy"),
                    "desvantagens", List.of("Poison", "Ground", "Rock", "Ghost"),
                    "imune", List.of("Steel")
            )),
            Map.entry("Ground", Map.of(
                    "vantagens", List.of("Fire", "Electric", "Poison", "Rock", "Steel"),
                    "desvantagens", List.of("Grass", "Ice"),
                    "imune", List.of("Flying")
            )),
            Map.entry("Flying", Map.of(
                    "vantagens", List.of("Grass", "Fighting", "Bug"),
                    "desvantagens", List.of("Electric", "Rock", "Steel"),
                    "imune", List.of()
            )),
            Map.entry("Psychic", Map.of(
                    "vantagens", List.of("Fighting", "Poison"),
                    "desvantagens", List.of("Psychic", "Steel"),
                    "imune", List.of("Dark")
            )),
            Map.entry("Bug", Map.of(
                    "vantagens", List.of("Grass", "Psychic", "Dark"),
                    "desvantagens", List.of("Fire", "Fighting", "Flying", "Ghost", "Steel", "Fairy"),
                    "imune", List.of()
            )),
            Map.entry("Rock", Map.of(
                    "vantagens", List.of("Fire", "Ice", "Flying", "Bug"),
                    "desvantagens", List.of("Fighting", "Ground", "Steel"),
                    "imune", List.of()
            )),
            Map.entry("Ghost", Map.of(
                    "vantagens", List.of("Psychic", "Ghost"),
                    "desvantagens", List.of("Dark"),
                    "imune", List.of("Normal")
            )),
            Map.entry("Dragon", Map.of(
                    "vantagens", List.of("Dragon"),
                    "desvantagens", List.of("Steel"),
                    "imune", List.of("Fairy")
            )),
            Map.entry("Dark", Map.of(
                    "vantagens", List.of("Psychic", "Ghost"),
                    "desvantagens", List.of("Fighting", "Dark", "Fairy"),
                    "imune", List.of()
            )),
            Map.entry("Steel", Map.of(
                    "vantagens", List.of("Ice", "Rock", "Fairy"),
                    "desvantagens", List.of("Fire", "Water", "Electric", "Steel"),
                    "imune", List.of()
            )),
            Map.entry("Fairy", Map.of(
                    "vantagens", List.of("Fighting", "Dragon", "Dark"),
                    "desvantagens", List.of("Fire", "Poison", "Steel"),
                    "imune", List.of()
            ))
    );
}