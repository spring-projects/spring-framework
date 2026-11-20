/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
