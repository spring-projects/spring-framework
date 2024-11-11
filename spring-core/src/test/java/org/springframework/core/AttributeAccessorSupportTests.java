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

package org.springframework.core;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AttributeAccessorSupport}.
 *
 * @author Rob Harrop
 * @author Sam Brannen
 * @since 2.0
 */
class AttributeAccessorSupportTests {

	private static final String NAME = "name";

	private static final String VALUE = "value";

	private final AttributeAccessor attributeAccessor = new SimpleAttributeAccessorSupport();


	@Test
	void setAndGet() {
		this.attributeAccessor.setAttribute(NAME, VALUE);
		assertThat(this.attributeAccessor.getAttribute(NAME)).isEqualTo(VALUE);
	}

	@Test
	void setAndHas() {
		assertThat(this.attributeAccessor.hasAttribute(NAME)).isFalse();
		this.attributeAccessor.setAttribute(NAME, VALUE);
		assertThat(this.attributeAccessor.hasAttribute(NAME)).isTrue();
	}

	@Test
	void computeAttribute() {
		AtomicInteger atomicInteger = new AtomicInteger();
		Function<String, String> computeFunction = name -> "computed-" + atomicInteger.incrementAndGet();

		assertThat(this.attributeAccessor.hasAttribute(NAME)).isFalse();
		this.attributeAccessor.computeAttribute(NAME, computeFunction);
		assertThat(this.attributeAccessor.getAttribute(NAME)).isEqualTo("computed-1");
		this.attributeAccessor.computeAttribute(NAME, computeFunction);
		assertThat(this.attributeAccessor.getAttribute(NAME)).isEqualTo("computed-1");

		this.attributeAccessor.removeAttribute(NAME);
		assertThat(this.attributeAccessor.hasAttribute(NAME)).isFalse();
		this.attributeAccessor.computeAttribute(NAME, computeFunction);
		assertThat(this.attributeAccessor.getAttribute(NAME)).isEqualTo("computed-2");
	}

	@Test
	void remove() {
		assertThat(this.attributeAccessor.hasAttribute(NAME)).isFalse();
		this.attributeAccessor.setAttribute(NAME, VALUE);
		assertThat(this.attributeAccessor.removeAttribute(NAME)).isEqualTo(VALUE);
		assertThat(this.attributeAccessor.hasAttribute(NAME)).isFalse();
	}

	@Test
	void attributeNames() {
		this.attributeAccessor.setAttribute(NAME, VALUE);
		this.attributeAccessor.setAttribute("abc", "123");
		assertThat(this.attributeAccessor.attributeNames()).contains("abc", NAME);
	}

	@SuppressWarnings("serial")
	private static class SimpleAttributeAccessorSupport extends AttributeAccessorSupport {
	}

}
