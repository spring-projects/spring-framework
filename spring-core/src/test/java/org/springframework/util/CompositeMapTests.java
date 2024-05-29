/*
 * Copyright 2002-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.util;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.entry;

/**
 * @author Arjen Poutsma
 */
class CompositeMapTests {

	@Test
	void size() {
		Map<String, String> first = Map.of("foo", "bar", "baz", "qux");
		Map<String, String> second = Map.of("quux", "corge");
		CompositeMap<String, String> composite = new CompositeMap<>(first, second);

		assertThat(composite).hasSize(3);
	}

	@Test
	void isEmpty() {
		Map<String, String> first = Map.of("foo", "bar", "baz", "qux");
		Map<String, String> second = Map.of("quux", "corge");
		CompositeMap<String, String> composite = new CompositeMap<>(first, second);

		assertThat(composite).isNotEmpty();

		composite = new CompositeMap<>(Collections.emptyMap(), Collections.emptyMap());
		assertThat(composite).isEmpty();
	}

	@Test
	void containsKey() {
		Map<String, String> first = Map.of("foo", "bar", "baz", "qux");
		Map<String, String> second = Map.of("quux", "corge");
		CompositeMap<String, String> composite = new CompositeMap<>(first, second);

		assertThat(composite.containsKey("foo")).isTrue();
		assertThat(composite.containsKey("bar")).isFalse();
		assertThat(composite.containsKey("baz")).isTrue();
		assertThat(composite.containsKey("qux")).isFalse();
		assertThat(composite.containsKey("quux")).isTrue();
		assertThat(composite.containsKey("corge")).isFalse();
	}

	@Test
	void containsValue() {
		Map<String, String> first = Map.of("foo", "bar", "baz", "qux");
		Map<String, String> second = Map.of("quux", "corge");
		CompositeMap<String, String> composite = new CompositeMap<>(first, second);

		assertThat(composite.containsValue("foo")).isFalse();
		assertThat(composite.containsValue("bar")).isTrue();
		assertThat(composite.containsValue("baz")).isFalse();
		assertThat(composite.containsValue("qux")).isTrue();
		assertThat(composite.containsValue("quux")).isFalse();
		assertThat(composite.containsValue("corge")).isTrue();
	}

	@Test
	void get() {
		Map<String, String> first = Map.of("foo", "bar", "baz", "qux");
		Map<String, String> second = Map.of("quux", "corge");
		CompositeMap<String, String> composite = new CompositeMap<>(first, second);

		assertThat(composite.get("foo")).isEqualTo("bar");
		assertThat(composite.get("baz")).isEqualTo("qux");
		assertThat(composite.get("quux")).isEqualTo("corge");

		assertThat(composite.get("grault")).isNull();
	}

	@Test
	void putUnsupported() {
		Map<String, String> first = Map.of("foo", "bar");
		Map<String, String> second = Map.of("baz", "qux");
		CompositeMap<String, String> composite = new CompositeMap<>(first, second);

		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> composite.put("grault", "garply"));
	}
	@Test
	void putSupported() {
		Map<String, String> first = Map.of("foo", "bar");
		Map<String, String> second = Map.of("baz", "qux");
		CompositeMap<String, String> composite = new CompositeMap<>(first, second, (k,v) -> {
			assertThat(k).isEqualTo("quux");
			assertThat(v).isEqualTo("corge");
			return "grault";
		}, null);

		assertThat(composite.put("quux", "corge")).isEqualTo("grault");
	}

	@Test
	void remove() {
		Map<String, String> first = new HashMap<>(Map.of("foo", "bar", "baz", "qux"));
		Map<String, String> second = new HashMap<>(Map.of("quux", "corge"));
		CompositeMap<String, String> composite = new CompositeMap<>(first, second);

		assertThat(composite.remove("foo")).isEqualTo("bar");
		assertThat(composite.containsKey("foo")).isFalse();
		assertThat(first).containsExactly(entry("baz", "qux"));

		assertThat(composite.remove("grault")).isNull();
	}

	@Test
	void putAllUnsupported() {
		Map<String, String> first = Map.of("foo", "bar");
		Map<String, String> second = Map.of("baz", "qux");

		CompositeMap<String, String> composite = new CompositeMap<>(first, second);

		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> composite.putAll(Map.of("quux", "corge", "grault", "garply")));
	}

	@Test
	void putAllPutFunction() {
		Map<String, String> first = Map.of("foo", "bar");
		Map<String, String> second = Map.of("baz", "qux");

		AtomicBoolean functionInvoked = new AtomicBoolean();
		CompositeMap<String, String> composite = new CompositeMap<>(first, second, (k,v) -> {
			assertThat(k).isEqualTo("quux");
			assertThat(v).isEqualTo("corge");
			functionInvoked.set(true);
			return "grault";
		}, null);

		composite.putAll(Map.of("quux", "corge"));

		assertThat(functionInvoked).isTrue();
	}

	@Test
	void putAllPutAllFunction() {
		Map<String, String> first = Map.of("foo", "bar");
		Map<String, String> second = Map.of("baz", "qux");

		AtomicBoolean functionInvoked = new AtomicBoolean();
		Map<String, String> argument = Map.of("quux", "corge");
		CompositeMap<String, String> composite = new CompositeMap<>(first, second, null,
				m -> {
					assertThat(m).isSameAs(argument);
					functionInvoked.set(true);
				});

		composite.putAll(argument);

		assertThat(functionInvoked).isTrue();
	}

	@Test
	void clear() {
		Map<String, String> first = new HashMap<>(Map.of("foo", "bar", "baz", "qux"));
		Map<String, String> second = new HashMap<>(Map.of("quux", "corge"));
		CompositeMap<String, String> composite = new CompositeMap<>(first, second);

		composite.clear();

		assertThat(composite).isEmpty();
		assertThat(first).isEmpty();
		assertThat(second).isEmpty();
	}

	@Test
	void keySet() {
		Map<String, String> first = Map.of("foo", "bar");
		Map<String, String> second = Map.of("baz", "qux");
		CompositeMap<String, String> composite = new CompositeMap<>(first, second);

		Set<String> keySet = composite.keySet();
		assertThat(keySet).containsExactly("foo", "baz");
	}

	@Test
	void values() {
		Map<String, String> first = Map.of("foo", "bar");
		Map<String, String> second = Map.of("baz", "qux");
		CompositeMap<String, String> composite = new CompositeMap<>(first, second);

		Collection<String> values = composite.values();
		assertThat(values).containsExactly("bar", "qux");
	}

	@Test
	void entrySet() {
		Map<String, String> first = Map.of("foo", "bar");
		Map<String, String> second = Map.of("baz", "qux");
		CompositeMap<String, String> composite = new CompositeMap<>(first, second);

		Set<Map.Entry<String, String>> entries = composite.entrySet();
		assertThat(entries).containsExactly(entry("foo", "bar"), entry("baz", "qux"));
	}

	@Nested
	class CollisionTests {

		@Test
		void size() {
			Map<String, String> first = Map.of("foo", "bar", "baz", "qux");
			Map<String, String> second = Map.of("baz", "quux", "corge", "grault");
			CompositeMap<String, String> composite = new CompositeMap<>(first, second);

			assertThat(composite).hasSize(3);
		}

		@Test
		void containsValue() {
			Map<String, String> first = Map.of("foo", "bar", "baz", "qux");
			Map<String, String> second = Map.of("baz", "quux", "corge", "grault");
			CompositeMap<String, String> composite = new CompositeMap<>(first, second);

			assertThat(composite.containsValue("bar")).isTrue();
			assertThat(composite.containsValue("qux")).isTrue();
			assertThat(composite.containsValue("quux")).isFalse();
			assertThat(composite.containsValue("grault")).isTrue();
		}

		@Test
		void get() {
			Map<String, String> first = Map.of("foo", "bar", "baz", "qux");
			Map<String, String> second = Map.of("baz", "quux", "corge", "grault");
			CompositeMap<String, String> composite = new CompositeMap<>(first, second);

			assertThat(composite.get("foo")).isEqualTo("bar");
			assertThat(composite.get("baz")).isEqualTo("qux");
			assertThat(composite.get("corge")).isEqualTo("grault");
		}

		@Test
		void remove() {
			Map<String, String> first = new HashMap<>(Map.of("foo", "bar", "baz", "qux"));
			Map<String, String> second = new HashMap<>(Map.of("baz", "quux", "corge", "grault"));
			CompositeMap<String, String> composite = new CompositeMap<>(first, second);

			assertThat(composite.remove("baz")).isEqualTo("qux");
			assertThat(composite.containsKey("baz")).isFalse();
			assertThat(first).containsExactly(entry("foo", "bar"));
			assertThat(second).containsExactly(entry("corge", "grault"));
		}

		@Test
		void keySet() {
			Map<String, String> first = Map.of("foo", "bar", "baz", "qux");
			Map<String, String> second = Map.of("baz", "quux", "corge", "grault");
			CompositeMap<String, String> composite = new CompositeMap<>(first, second);

			Set<String> keySet = composite.keySet();
			assertThat(keySet).containsExactlyInAnyOrder("foo", "baz", "corge");
		}


		@Test
		void values() {
			Map<String, String> first = Map.of("foo", "bar", "baz", "qux");
			Map<String, String> second = Map.of("baz", "quux", "corge", "grault");
			CompositeMap<String, String> composite = new CompositeMap<>(first, second);

			Collection<String> values = composite.values();
			assertThat(values).containsExactlyInAnyOrder("bar", "qux", "grault");
		}

		@Test
		void entrySet() {
			Map<String, String> first = Map.of("foo", "bar", "baz", "qux");
			Map<String, String> second = Map.of("baz", "quux", "corge", "grault");
			CompositeMap<String, String> composite = new CompositeMap<>(first, second);

			Set<Map.Entry<String, String>> entries = composite.entrySet();
			assertThat(entries).containsExactlyInAnyOrder(entry("foo", "bar"), entry("baz", "qux"), entry("corge", "grault"));
		}


	}
}
