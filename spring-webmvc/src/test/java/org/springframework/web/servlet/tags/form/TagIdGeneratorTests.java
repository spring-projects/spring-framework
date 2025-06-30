/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.web.servlet.tags.form;

import java.util.stream.IntStream;

import jakarta.servlet.jsp.PageContext;
import org.junit.jupiter.api.Test;

import org.springframework.web.testfixture.servlet.MockPageContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rob Harrop
 * @author Sam Brannen
 * @since 2.0
 */
class TagIdGeneratorTests {

	@Test
	void nextId() {
		// Repeat a few times just to be sure...
		IntStream.rangeClosed(1, 5).forEach(i -> assertNextId());
	}

	private void assertNextId() {
		PageContext pageContext = new MockPageContext();
		assertThat(TagIdGenerator.nextId("foo", pageContext)).isEqualTo("foo1");
		assertThat(TagIdGenerator.nextId("foo", pageContext)).isEqualTo("foo2");
		assertThat(TagIdGenerator.nextId("foo", pageContext)).isEqualTo("foo3");
		assertThat(TagIdGenerator.nextId("foo", pageContext)).isEqualTo("foo4");
		assertThat(TagIdGenerator.nextId("bar", pageContext)).isEqualTo("bar1");
	}

}
