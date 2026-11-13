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
package io.github.xponer2.translator.runtime.redirect;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * The 1.21 {@code RandomSource} methods {@link Random} lacks (RandomSource is java.util.Random here): its factories
 * (Mining Gadgets renders with {@code RandomSource.create(42)}) and its helpers for ranges.
 */
public final class RandomSourceBridges {
	private RandomSourceBridges() {
	}

	public static Random create() {
		return new Random();
	}

	public static Random create(long seed) {
		return new Random(seed);
	}

	public static Random createNewThreadLocalInstance() {
		return new Random(ThreadLocalRandom.current().nextLong());
	}

	/** java.util.Random is thread safe already. */
	public static Random createThreadSafe() {
		return new Random();
	}

	public static Random fork(Random random) {
		return new Random(random.nextLong());
	}

	public static int nextInt(Random random, int origin, int bound) {
		if (origin >= bound) {
			throw new IllegalArgumentException("bound - origin is non positive");
		}
		return origin + random.nextInt(bound - origin);
	}

	public static int nextIntBetweenInclusive(Random random, int min, int max) {
		return random.nextInt(max - min + 1) + min;
	}

	public static double triangle(Random random, double mode, double deviation) {
		return mode + deviation * (random.nextDouble() - random.nextDouble());
	}

	/**
	 * 1.21 {@code random.forkPositional()}: a factory of randoms by position and by name (1.21's noises make their
	 * octaves so: PerlinNoise.create, NormalNoise; Twilight Forest's plants make one when registered). The randoms
	 * are the Translator's xoroshiro ones, seeded from this one as 1.21's are.
	 */
	public static Object forkPositional(Random random) {
		long seedLo = random.nextLong();
		long seedHi = random.nextLong();
		try {
			Class<?> type = Class.forName("net.minecraft.world.level.levelgen.PositionalRandomFactory", true,
					net.minecraft.launchwrapper.Launch.classLoader);
			return java.lang.reflect.Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, (proxy, method, args) -> {
				switch (method.getName()) {
					case "at":
						if (args.length == 3) {
							long position = net.minecraft.util.math.MathHelper.getCoordinateRandom((Integer) args[0], (Integer) args[1], (Integer) args[2]);
							return new io.github.xponer2.translator.util.random.XoroshiroRandomSource(position ^ seedLo, seedHi);
						}
						return new io.github.xponer2.translator.util.random.XoroshiroRandomSource(seedLo, seedHi);
					case "fromHashOf": {
						long[] hash = hashOf(String.valueOf(args[0]));
						return new io.github.xponer2.translator.util.random.XoroshiroRandomSource(hash[0] ^ seedLo, hash[1] ^ seedHi);
					}
					case "fromSeed":
						return new io.github.xponer2.translator.util.random.XoroshiroRandomSource((Long) args[0] ^ seedLo, (Long) args[0] ^ seedHi);
					case "parityConfigString":
						return ((StringBuilder) args[0]).append("seedLo: ").append(seedLo).append(", seedHi: ").append(seedHi);
					case "equals":
						return proxy == args[0];
					case "hashCode":
						return System.identityHashCode(proxy);
					default:
						return "PositionalRandomFactory[" + seedLo + ", " + seedHi + "]";
				}
			});
		} catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
			io.github.xponer2.translator.runtime.RuntimeStubs.failed("RandomSource.forkPositional", e);
			return null;
		}
	}

	/** 1.21's RandomSupport.seedFromHashOf: the MD5 of the name as two longs. */
	private static long[] hashOf(String name) {
		try {
			byte[] md5 = java.security.MessageDigest.getInstance("MD5").digest(name.getBytes(java.nio.charset.StandardCharsets.UTF_8));
			java.nio.ByteBuffer bytes = java.nio.ByteBuffer.wrap(md5);
			return new long[] {bytes.getLong(), bytes.getLong()};
		} catch (java.security.NoSuchAlgorithmException e) {
			return new long[] {name.hashCode(), (long) name.hashCode() << 32};
		}
	}

	/**
	 * 1.21 {@code WeightedRandom.getTotalWeight(list)} (an X line: 1.12.2's WeightedRandom has a method of the same
	 * name and descriptor that casts every entry to its own Item): the weights of 1.21 entries and of 1.12.2 items.
	 * Supplementaries' flute picked an animal with it and failed.
	 */
	public static int getTotalWeight(java.util.List<?> entries) {
		long total = 0;
		for (Object entry : entries) {
			if (entry instanceof io.github.xponer2.translator.util.random.WeightedEntry weighted) {
				total += weighted.getWeight().asInt();
			} else if (entry instanceof net.minecraft.util.WeightedRandom.Item item) {
				total += item.itemWeight;
			}
		}
		return (int) Math.min(total, Integer.MAX_VALUE);
	}

	public static void consumeCount(Random random, int count) {
		for (int i = 0; i < count; i++) {
			random.nextInt();
		}
	}
}
