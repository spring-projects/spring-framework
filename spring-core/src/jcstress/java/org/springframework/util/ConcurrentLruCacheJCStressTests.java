package org.springframework.util;

import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.Arbiter;
import org.openjdk.jcstress.annotations.Description;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.I_Result;
import org.openjdk.jcstress.infra.results.ZZI_Result;
import org.openjdk.jcstress.infra.results.Z_Result;

import java.util.function.Function;

import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE;
import static org.openjdk.jcstress.annotations.Expect.FORBIDDEN;

public class ConcurrentLruCacheJCStressTests {

	@JCStressTest
	@Description("Testing size feature")
	@Outcome(id = {"0", "1", "2"}, expect = ACCEPTABLE, desc = "Valid sizes")
	@Outcome(id = "3", expect = FORBIDDEN, desc = "Invalid Size")
	@State
	public static class Case1 {

		int sizeLimit = 2;
		Function<String, String> generator = (key) -> key + "-value";
		private final ConcurrentLruCache<String, String> cache = new ConcurrentLruCache<>(sizeLimit, generator);

		@Actor
		public void actor1(I_Result r) {
			r.r1 = cache.size();
		}

		@Actor
		public void actor2(I_Result r) {
			this.cache.get("k1");
			r.r1 = cache.size();
		}

		@Actor
		public void actor3(I_Result r) {
			this.cache.get("k2");
			r.r1 = cache.size();
		}

		@Actor
		public void actor4(I_Result r) {
			this.cache.get("k3");
			r.r1 = cache.size();
		}
	}

	@JCStressTest
	@Description("Testing the internal movement of a LRU Cache")
	@Outcome(id = "true, false, 1", expect = ACCEPTABLE, desc = "Valid existence")
	@Outcome(id = "false, true, 1", expect = ACCEPTABLE, desc = "Valid existence")
	@Outcome(id = "true, true, 2", expect = FORBIDDEN, desc = "Invalid existence")
	@State
	public static class Case2 {

		int sizeLimit = 1;
		Function<Integer, Integer> generator = (key) -> key;
		private final ConcurrentLruCache<Integer, Integer> cache = new ConcurrentLruCache<>(sizeLimit, generator);

		@Actor
		public void actor1() {
			this.cache.get(1);
		}

		@Actor
		public void actor2() {
			this.cache.get(2);
		}

		@Arbiter
		public void arbiter(ZZI_Result r) {
			r.r1 = cache.contains(1);
			r.r2 = cache.contains(2);
			r.r3 = cache.size();
		}
	}

	@JCStressTest
	@Description("Testing value feature")
	@Outcome(id = "true", expect = ACCEPTABLE, desc = "Valid values")
	@Outcome(id = "false", expect = ACCEPTABLE, desc = "Valid values")
	@State
	public static class Case3 {

		int sizeLimit = 1;
		Function<Integer, Integer> generator = (key) -> key;
		private final ConcurrentLruCache<Integer, Integer> cache = new ConcurrentLruCache<>(sizeLimit, generator);

		@Actor
		public void actor1(Z_Result r) {
			r.r1 = (this.cache.get(1) == 1);
		}

		@Actor
		public void actor2(Z_Result r) {
			r.r1 = this.cache.contains(1);
		}
	}

	@JCStressTest
	@Description("Testing remove feature")
	@Outcome(id = {"0", "1"}, expect = ACCEPTABLE, desc = "Valid values")
	@State
	public static class Case4 {

		int sizeLimit = 2;
		Function<Integer, Integer> generator = (key) -> key;
		private final ConcurrentLruCache<Integer, Integer> cache = new ConcurrentLruCache<>(sizeLimit, generator);

		@Actor
		public void actor1() {
			this.cache.get(1);
		}

		@Actor
		public void actor2() {
			this.cache.remove(1);
		}

		@Arbiter
		public void arbiter(I_Result r) {
			r.r1 = cache.size();
		}
	}

	@JCStressTest
	@Description("Testing clear feature")
	@Outcome(id = {"0", "1", "2"}, expect = ACCEPTABLE, desc = "Valid results")
	@State
	public static class Case5 {

		int sizeLimit = 2;
		Function<Integer, Integer> generator = (key) -> key;
		private final ConcurrentLruCache<Integer, Integer> cache = new ConcurrentLruCache<>(sizeLimit, generator);

		@Actor
		public void actor1() {
			this.cache.get(1);
		}

		@Actor
		public void actor2() {
			this.cache.get(2);
		}

		@Actor
		public void actor3() {
			this.cache.clear();
		}

		@Arbiter
		public void arbiter(I_Result r) {
			r.r1 = cache.size();
		}
	}
}
