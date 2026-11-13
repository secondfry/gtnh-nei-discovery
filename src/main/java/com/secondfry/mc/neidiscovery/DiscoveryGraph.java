package com.secondfry.mc.neidiscovery;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/** Ingredient-type discovery, independent of Minecraft and inventory quantities. */
final class DiscoveryGraph<K> {

    static final class Recipe<K> {

        final List<Set<K>> ingredients;
        final Set<K> outputs;
        final Set<K> workstations;

        Recipe(List<Set<K>> ingredients, Set<K> outputs) {
            this(ingredients, outputs, null);
        }

        Recipe(List<Set<K>> ingredients, Set<K> outputs, Set<K> workstations) {
            this.ingredients = new ArrayList<>();
            for (Set<K> alternatives : ingredients) this.ingredients.add(new HashSet<>(alternatives));
            this.outputs = new HashSet<>(outputs);
            // Share the handler's immutable catalyst set across its recipes.
            this.workstations = workstations;
        }

        boolean available(Set<K> known) {
            if (ingredients.isEmpty()) return false;
            for (Set<K> alternatives : ingredients) {
                if (Collections.disjoint(alternatives, known)) return false;
            }
            return true;
        }
    }

    static <K> Set<K> visible(Collection<K> seen, List<Recipe<K>> recipes, boolean chains) {
        return visible(seen, recipes, chains, Function.identity());
    }

    static <K> Set<K> visible(Collection<K> seen, List<Recipe<K>> recipes, boolean chains,
        Function<K, K> ingredientIdentity) {
        Set<K> visible = new HashSet<>(seen);
        Set<K> observed = new HashSet<>();
        for (K item : seen) {
            observed.add(item);
            observed.add(ingredientIdentity.apply(item));
        }
        Set<K> ingredients = new HashSet<>(observed);
        boolean changed;
        do {
            changed = false;
            for (Recipe<K> recipe : recipes) {
                if (recipe.workstations != null && Collections.disjoint(recipe.workstations, observed)) continue;
                if (recipe.available(ingredients)) changed |= visible.addAll(recipe.outputs);
            }
            if (chains) for (K item : visible) {
                ingredients.add(item);
                ingredients.add(ingredientIdentity.apply(item));
            }
        } while (chains && changed);
        return Collections.unmodifiableSet(visible);
    }
}
