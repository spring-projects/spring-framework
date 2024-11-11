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

package org.springframework.core.style;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ToStringCreator}.
 *
 * @author Keith Donald
 * @author Sam Brannen
 */
class ToStringCreatorTests {

	private final SomeObject s1 = new SomeObject() {
		@Override
		public String toString() {
			return "A";
		}
	};

	private final SomeObject s2 = new SomeObject() {
		@Override
		public String toString() {
			return "B";
		}
	};

	private final SomeObject s3 = new SomeObject() {
		@Override
		public String toString() {
			return "C";
		}
	};


	@Test
	void primitiveArray() {
		int[] integers = {0, 1, 2, 3, 4};
		String str = new ToStringCreator(integers).toString();
		assertThat(str).isEqualTo(
				"[@%s array<Integer>[0, 1, 2, 3, 4]]",
				ObjectUtils.getIdentityHexString(integers));
	}

	@Test
	void objectArray() {
		SomeObject[] array = new SomeObject[] {s1, s2, s3};
		String str = new ToStringCreator(array).toString();
		assertThat(str).isEqualTo(
				"[@%s array<ToStringCreatorTests.SomeObject>[A, B, C]]",
				ObjectUtils.getIdentityHexString(array));
	}

	@Test
	void appendTopLevelClass() {
		SomeObject object = new SomeObject();
		String str = new ToStringCreator(object)
				.append("myClass", Integer.class)
				.toString();
		assertThat(str).isEqualTo(
				"[ToStringCreatorTests.SomeObject@%s myClass = Integer]",
				ObjectUtils.getIdentityHexString(object));
	}

	@Test
	void appendNestedClass() {
		SomeObject object = new SomeObject();
		String str = new ToStringCreator(object)
				.append("myClass", object.getClass())
				.toString();
		assertThat(str).isEqualTo(
				"[ToStringCreatorTests.SomeObject@%s myClass = ToStringCreatorTests.SomeObject]",
				ObjectUtils.getIdentityHexString(object));
	}

	@Test
	void appendTopLevelMethod() throws Exception {
		SomeObject object = new SomeObject();
		String str = new ToStringCreator(object)
				.append("myMethod", ToStringCreatorTests.class.getDeclaredMethod("someMethod"))
				.toString();
		assertThat(str).isEqualTo(
				"[ToStringCreatorTests.SomeObject@%s myMethod = someMethod@ToStringCreatorTests]",
				ObjectUtils.getIdentityHexString(object));
	}

	@Test
	void appendNestedMethod() throws Exception {
		SomeObject object = new SomeObject();
		String str = new ToStringCreator(object)
				.append("myMethod", SomeObject.class.getDeclaredMethod("someMethod"))
				.toString();
		assertThat(str).isEqualTo(
				"[ToStringCreatorTests.SomeObject@%s myMethod = someMethod@ToStringCreatorTests.SomeObject]",
				ObjectUtils.getIdentityHexString(object));
	}

	@Test
	void appendList() {
		SomeObject object = new SomeObject();
		List<SomeObject> list = List.of(s1, s2, s3);
		String str = new ToStringCreator(object)
				.append("myLetters", list)
				.toString();
		assertThat(str).isEqualTo(
				"[ToStringCreatorTests.SomeObject@%s myLetters = list[A, B, C]]",
				ObjectUtils.getIdentityHexString(object));
	}

	@Test
	void appendSet() {
		SomeObject object = new SomeObject();
		Set<SomeObject> set = new LinkedHashSet<>();
		set.add(s1);
		set.add(s2);
		set.add(s3);
		String str = new ToStringCreator(object)
				.append("myLetters", set)
				.toString();
		assertThat(str).isEqualTo(
				"[ToStringCreatorTests.SomeObject@%s myLetters = set[A, B, C]]",
				ObjectUtils.getIdentityHexString(object));
	}

	@Test
	void appendMap() {
		Map<String, String> map = new LinkedHashMap<>() {{
			put("Keri", "Softball");
			put("Scot", "Fishing");
			put("Keith", "Flag Football");
		}};
		Object stringy = new Object() {
			@Override
			public String toString() {
				return new ToStringCreator(this)
						.append("familyFavoriteSport", map)
						.toString();
			}
		};
		assertThat(stringy.toString())
			.containsSubsequence(
				"[",
				ClassUtils.getShortName(stringy.getClass().getName()),
				"@",
				ObjectUtils.getIdentityHexString(stringy),
				"familyFavoriteSport = map['Keri' -> 'Softball', 'Scot' -> 'Fishing', 'Keith' -> 'Flag Football']",
				"]"
			);
	}


	private static class SomeObject {
		@SuppressWarnings("unused")
		private static void someMethod() {
		}
	}

	@SuppressWarnings("unused")
	private static void someMethod() {
	}

}
