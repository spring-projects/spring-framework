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

package org.springframework.core;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rob Harrop
 * @author Sam Brannen
 * @since 2.0
 */
class AttributeAccessorSupportTests {

	private static final String NAME = "foo";

	private static final String VALUE = "bar";

	private AttributeAccessor attributeAccessor = new SimpleAttributeAccessorSupport();

	@Test
	void setAndGet() throws Exception {
		this.attributeAccessor.setAttribute(NAME, VALUE);
		assertThat(this.attributeAccessor.getAttribute(NAME)).isEqualTo(VALUE);
	}

	@Test
	void setAndHas() throws Exception {
		assertThat(this.attributeAccessor.hasAttribute(NAME)).isFalse();
		this.attributeAccessor.setAttribute(NAME, VALUE);
		assertThat(this.attributeAccessor.hasAttribute(NAME)).isTrue();
	}

	@Test
	void remove() throws Exception {
		assertThat(this.attributeAccessor.hasAttribute(NAME)).isFalse();
		this.attributeAccessor.setAttribute(NAME, VALUE);
		assertThat(this.attributeAccessor.removeAttribute(NAME)).isEqualTo(VALUE);
		assertThat(this.attributeAccessor.hasAttribute(NAME)).isFalse();
	}

	@Test
	void attributeNames() throws Exception {
		this.attributeAccessor.setAttribute(NAME, VALUE);
		this.attributeAccessor.setAttribute("abc", "123");
		String[] attributeNames = this.attributeAccessor.attributeNames();
		Arrays.sort(attributeNames);
		assertThat(Arrays.binarySearch(attributeNames, NAME) > -1).isTrue();
		assertThat(Arrays.binarySearch(attributeNames, "abc") > -1).isTrue();
	}

	@SuppressWarnings("serial")
	private static class SimpleAttributeAccessorSupport extends AttributeAccessorSupport {
	}

}
