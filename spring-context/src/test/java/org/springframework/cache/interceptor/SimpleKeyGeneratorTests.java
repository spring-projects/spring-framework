/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cache.interceptor;

import org.junit.Test;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/**
 * Tests for {@link SimpleKeyGenerator} and {@link SimpleKey}.
 *
 * @author Phillip Webb
 */
public class SimpleKeyGeneratorTests {

	private SimpleKeyGenerator generator = new SimpleKeyGenerator();

	@Test
	public void noValues() throws Exception {
		Object k1 = generator.generate(null, null, new Object[] {});
		Object k2 = generator.generate(null, null, new Object[] {});
		Object k3 = generator.generate(null, null, new Object[] { "different" });
		assertThat(k1.hashCode(), equalTo(k2.hashCode()));
		assertThat(k1.hashCode(), not(equalTo(k3.hashCode())));
		assertThat(k1, equalTo(k2));
		assertThat(k1, not(equalTo(k3)));
	}

	@Test
	public void singleValue() throws Exception {
		Object k1 = generator.generate(null, null, new Object[] { "a" });
		Object k2 = generator.generate(null, null, new Object[] { "a" });
		Object k3 = generator.generate(null, null, new Object[] { "different" });
		assertThat(k1.hashCode(), equalTo(k2.hashCode()));
		assertThat(k1.hashCode(), not(equalTo(k3.hashCode())));
		assertThat(k1, equalTo(k2));
		assertThat(k1, not(equalTo(k3)));
		assertThat(k1, equalTo((Object) "a"));
	}

	@Test
	public void multipleValues() throws Exception {
		Object k1 = generator.generate(null, null, new Object[] { "a", 1, "b" });
		Object k2 = generator.generate(null, null, new Object[] { "a", 1, "b" });
		Object k3 = generator.generate(null, null, new Object[] { "b", 1, "a" });
		assertThat(k1.hashCode(), equalTo(k2.hashCode()));
		assertThat(k1.hashCode(), not(equalTo(k3.hashCode())));
		assertThat(k1, equalTo(k2));
		assertThat(k1, not(equalTo(k3)));
	}

	@Test
	public void singleNullValue() throws Exception {
		Object k1 = generator.generate(null, null, new Object[] { null });
		Object k2 = generator.generate(null, null, new Object[] { null });
		Object k3 = generator.generate(null, null, new Object[] { "different" });
		assertThat(k1.hashCode(), equalTo(k2.hashCode()));
		assertThat(k1.hashCode(), not(equalTo(k3.hashCode())));
		assertThat(k1, equalTo(k2));
		assertThat(k1, not(equalTo(k3)));
		assertThat(k1, instanceOf(SimpleKey.class));
	}

	@Test
	public void multiplNullValues() throws Exception {
		Object k1 = generator.generate(null, null, new Object[] { "a", null, "b", null });
		Object k2 = generator.generate(null, null, new Object[] { "a", null, "b", null });
		Object k3 = generator.generate(null, null, new Object[] { "a", null, "b" });
		assertThat(k1.hashCode(), equalTo(k2.hashCode()));
		assertThat(k1.hashCode(), not(equalTo(k3.hashCode())));
		assertThat(k1, equalTo(k2));
		assertThat(k1, not(equalTo(k3)));
	}
}
