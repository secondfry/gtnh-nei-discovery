package com.secondfry.mc.neidiscovery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

public class DiscoveryGraphTest {

    private Set<String> set(String... values) {
        return new HashSet<>(Arrays.asList(values));
    }

    @SafeVarargs
    private final DiscoveryGraph.Recipe<String> recipe(String output, Set<String>... inputs) {
        return new DiscoveryGraph.Recipe<>(Arrays.asList(inputs), set(output));
    }

    @Test
    public void machineRecipesRequireAnObservedWorkstationEvenWithChains() {
        List<DiscoveryGraph.Recipe<String>> recipes = Arrays.asList(
            recipe("macerator", set("ore")),
            new DiscoveryGraph.Recipe<>(Arrays.asList(set("ore")), set("tanzanite_dust"), set("macerator")),
            new DiscoveryGraph.Recipe<>(Arrays.asList(set("ore")), set("unknown_machine_dust"), set()));
        assertFalse(
            DiscoveryGraph.visible(set("ore"), recipes, true)
                .contains("tanzanite_dust"));
        Set<String> result = DiscoveryGraph.visible(set("ore", "macerator"), recipes, true);
        assertTrue(result.contains("tanzanite_dust"));
        assertFalse(result.contains("unknown_machine_dust"));
        assertTrue(
            DiscoveryGraph.visible(set("unknown_machine_dust"), recipes, false)
                .contains("unknown_machine_dust"));
    }

    @Test
    public void pickaxeSearchOnlyIncludesKnownMaterialsAndAlternativeIngredients() {
        List<DiscoveryGraph.Recipe<String>> recipes = Arrays.asList(
            recipe("wood_pickaxe", set("oak_plank", "birch_plank"), set("stick")),
            recipe("iron_pickaxe", set("iron_ingot"), set("stick")));
        Set<String> result = DiscoveryGraph.visible(set("birch_plank", "stick", "seen_pickaxe"), recipes, false);
        assertTrue(result.contains("wood_pickaxe"));
        assertTrue(result.contains("seen_pickaxe"));
        assertFalse(result.contains("iron_pickaxe"));
    }

    @Test
    public void chainsNeedASeedAndRespectTheToggle() {
        List<DiscoveryGraph.Recipe<String>> recipes = Arrays.asList(
            recipe("wood_pickaxe", set("plank"), set("stick")),
            recipe("stick", set("plank")),
            recipe("plank", set("log")),
            recipe("a", set("b")),
            recipe("b", set("a")),
            recipe("free_item"));
        assertFalse(
            DiscoveryGraph.visible(set("log"), recipes, false)
                .contains("wood_pickaxe"));
        Set<String> result = DiscoveryGraph.visible(set("log"), recipes, true);
        assertTrue(result.contains("wood_pickaxe"));
        assertFalse(result.contains("a"));
        assertFalse(result.contains("free_item"));
        assertEquals(Collections.emptySet(), DiscoveryGraph.visible(set(), recipes, true));
    }

    @Test
    public void worldsDoNotShareKnowledgeAndSnapshotsAreImmutable() {
        List<DiscoveryGraph.Recipe<String>> recipes = Collections.singletonList(recipe("pickaxe", set("iron")));
        Set<String> oldWorld = DiscoveryGraph.visible(set("iron"), recipes, true);
        assertTrue(oldWorld.contains("pickaxe"));
        assertFalse(
            DiscoveryGraph.visible(set("copper"), recipes, true)
                .contains("pickaxe"));
        try {
            oldWorld.add("leak");
            throw new AssertionError("Mutable filter snapshot");
        } catch (UnsupportedOperationException expected) {
            // NEI worker threads can safely retain this snapshot.
        }
    }
}
