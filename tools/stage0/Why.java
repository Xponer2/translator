/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 *
 * Licensed under the PolyForm Noncommercial License 1.0.0 (see LICENSE or
 * https://polyformproject.org/licenses/noncommercial/1.0.0): free to use, change and share for any noncommercial
 * purpose, with credit to the author. This notice stays in this file and in every copy or changed version of it.
 *
 * Required Notice: Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 */
import java.nio.file.*;
import java.util.*;

/**
 * Why a class or member isn't carried at a rung: its hard needs and its uncarried dependencies, down to the roots.
 *   java Why <rung index> <thing key ("C cls" | "M cls name desc")> <version args as for Convert>
 */
public class Why {
    public static void main(String[] args) throws Exception {
        int rung = Integer.parseInt(args[0]);
        String key = args[1];
        List<Stage0.Version> versions = new ArrayList<>();
        for (int i = 2; i < args.length; i++) {
            String[] p = args[i].split("=");
            List<Path> loader = new ArrayList<>();
            for (String line : Files.readAllLines(Paths.get(p[3]))) {
                if (line.endsWith(".jar")) {
                    loader.add(Paths.get(line.trim()));
                }
            }
            versions.add(Stage0.load(p[0], Paths.get(p[1]), Paths.get(p[2]), loader));
        }
        Stage0.Plan plan = Stage0.plan(versions.get(rung), versions.get(rung + 1));
        Deque<String> todo = new ArrayDeque<>(List.of(key));
        Set<String> seen = new HashSet<>();
        Map<String, Integer> roots = new TreeMap<>();
        while (!todo.isEmpty() && seen.size() < 400) {
            String k = todo.poll();
            if (!seen.add(k)) {
                continue;
            }
            Stage0.Needs n = plan.needs.get(k);
            if (n == null) {
                continue;
            }
            if (!n.hard.isEmpty()) {
                for (String h : n.hard) {
                    roots.merge(h, 1, Integer::sum);
                }
            }
            for (String d : n.deps) {
                if (plan.needs.containsKey(d) && !plan.carried.contains(d)) {
                    todo.add(d);
                }
            }
        }
        System.out.println(key + " carried: " + plan.carried.contains(key) + "; looked at " + seen.size() + " uncarried things");
        roots.forEach((r, c) -> System.out.println("  root: " + r));
    }
}
