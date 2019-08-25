/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.cache.interceptor;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;





/**
 * Tests for {@link SimpleKeyGenerator} and {@link SimpleKey}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
public class SimpleKeyGeneratorTests {

	private final SimpleKeyGenerator generator = new SimpleKeyGenerator();


	@Test
	public void noValues() {
		Object k1 = generateKey(new Object[] {});
		Object k2 = generateKey(new Object[] {});
		Object k3 = generateKey(new Object[] { "different" });
		assertThat(k1.hashCode()).isEqualTo(k2.hashCode());
		assertThat(k1.hashCode()).isNotEqualTo(k3.hashCode());
		assertThat(k1).isEqualTo(k2);
		assertThat(k1).isNotEqualTo(k3);
	}

	@Test
	public void singleValue(){
		Object k1 = generateKey(new Object[] { "a" });
		Object k2 = generateKey(new Object[] { "a" });
		Object k3 = generateKey(new Object[] { "different" });
		assertThat(k1.hashCode()).isEqualTo(k2.hashCode());
		assertThat(k1.hashCode()).isNotEqualTo(k3.hashCode());
		assertThat(k1).isEqualTo(k2);
		assertThat(k1).isNotEqualTo(k3);
		assertThat(k1).isEqualTo("a");
	}

	@Test
	public void multipleValues()  {
		Object k1 = generateKey(new Object[] { "a", 1, "b" });
		Object k2 = generateKey(new Object[] { "a", 1, "b" });
		Object k3 = generateKey(new Object[] { "b", 1, "a" });
		assertThat(k1.hashCode()).isEqualTo(k2.hashCode());
		assertThat(k1.hashCode()).isNotEqualTo(k3.hashCode());
		assertThat(k1).isEqualTo(k2);
		assertThat(k1).isNotEqualTo(k3);
	}

	@Test
	public void singleNullValue() {
		Object k1 = generateKey(new Object[] { null });
		Object k2 = generateKey(new Object[] { null });
		Object k3 = generateKey(new Object[] { "different" });
		assertThat(k1.hashCode()).isEqualTo(k2.hashCode());
		assertThat(k1.hashCode()).isNotEqualTo(k3.hashCode());
		assertThat(k1).isEqualTo(k2);
		assertThat(k1).isNotEqualTo(k3);
		assertThat(k1).isInstanceOf(SimpleKey.class);
	}

	@Test
	public void multipleNullValues() {
		Object k1 = generateKey(new Object[] { "a", null, "b", null });
		Object k2 = generateKey(new Object[] { "a", null, "b", null });
		Object k3 = generateKey(new Object[] { "a", null, "b" });
		assertThat(k1.hashCode()).isEqualTo(k2.hashCode());
		assertThat(k1.hashCode()).isNotEqualTo(k3.hashCode());
		assertThat(k1).isEqualTo(k2);
		assertThat(k1).isNotEqualTo(k3);
	}

	@Test
	public void plainArray() {
		Object k1 = generateKey(new Object[] { new String[]{"a", "b"} });
		Object k2 = generateKey(new Object[] { new String[]{"a", "b"} });
		Object k3 = generateKey(new Object[] { new String[]{"b", "a"} });
		assertThat(k1.hashCode()).isEqualTo(k2.hashCode());
		assertThat(k1.hashCode()).isNotEqualTo(k3.hashCode());
		assertThat(k1).isEqualTo(k2);
		assertThat(k1).isNotEqualTo(k3);
	}

	@Test
	public void arrayWithExtraParameter() {
		Object k1 = generateKey(new Object[] { new String[]{"a", "b"}, "c" });
		Object k2 = generateKey(new Object[] { new String[]{"a", "b"}, "c" });
		Object k3 = generateKey(new Object[] { new String[]{"b", "a"}, "c" });
		assertThat(k1.hashCode()).isEqualTo(k2.hashCode());
		assertThat(k1.hashCode()).isNotEqualTo(k3.hashCode());
		assertThat(k1).isEqualTo(k2);
		assertThat(k1).isNotEqualTo(k3);
	}


	private Object generateKey(Object[] arguments) {
		return this.generator.generate(null, null, arguments);
	}

}
