/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
