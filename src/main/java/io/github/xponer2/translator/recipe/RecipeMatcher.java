/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 *
 * Licensed under the PolyForm Noncommercial License 1.0.0 (see LICENSE or
 * https://polyformproject.org/licenses/noncommercial/1.0.0): free to use, change and share for any noncommercial
 * purpose, with credit to the author. This notice stays in this file and in every copy or changed version of it.
 *
 * Required Notice: Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 */
package io.github.xponer2.translator.recipe;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

/**
 * Matches items to ingredients in any order (1.21 NeoForge {@code RecipeMatcher}); 1.12.2 ingredients are
 * predicates of stacks.
 */
public final class RecipeMatcher {
    private RecipeMatcher() { }

    /**
     * For each input, the index of the ingredient it was matched to; null if the inputs don't match the ingredients
     * one to one.
     */
    @Nullable
    public static <T> int[] findMatches(List<T> inputs, List<? extends Predicate<T>> tests) {
        int size = inputs.size();
        if (size != tests.size()) {
            return null;
        }
        var ret = new int[size];
        Arrays.fill(ret, -1);
        var testOwner = new int[size];
        Arrays.fill(testOwner, -1);
        for (int input = 0; input < size; input++) {
            if (!assign(input, inputs, tests, ret, testOwner, new boolean[size])) {
                return null;
            }
        }
        return ret;
    }

    // augmenting path of a bipartite matching (inputs -> tests)
    private static <T> boolean assign(int input, List<T> inputs, List<? extends Predicate<T>> tests, int[] ret, int[] testOwner, boolean[] visited) {
        for (int test = 0; test < tests.size(); test++) {
            if (visited[test] || !tests.get(test).test(inputs.get(input))) {
                continue;
            }
            visited[test] = true;
            if (testOwner[test] < 0 || assign(testOwner[test], inputs, tests, ret, testOwner, visited)) {
                testOwner[test] = input;
                ret[input] = test;
                return true;
            }
        }
        return false;
    }
}
