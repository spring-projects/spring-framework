/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.annotation;

import org.junit.jupiter.api.Test;

import org.springframework.context.annotation.DeferredImportSelector.Group;
import org.springframework.core.type.AnnotationMetadata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link DeferredImportSelector}.
 *
 * @author Stephane Nicoll
 */
class DeferredImportSelectorTests {

	@Test
	void entryEqualsSameInstance() {
		AnnotationMetadata metadata = mock();
		Group.Entry entry = new Group.Entry(metadata, "com.example.Test");
		assertThat(entry).isEqualTo(entry);
	}

	@Test
	void entryEqualsSameMetadataAndClassName() {
		AnnotationMetadata metadata = mock();
		assertThat(new Group.Entry(metadata, "com.example.Test")).isEqualTo(new Group.Entry(metadata, "com.example.Test"));
	}

	@Test
	void entryEqualDifferentMetadataAndSameClassName() {
		assertThat(new Group.Entry(mock(), "com.example.Test")).isNotEqualTo(new Group.Entry(mock(), "com.example.Test"));
	}

	@Test
	void entryEqualSameMetadataAnDifferentClassName() {
		AnnotationMetadata metadata = mock();
		assertThat(new Group.Entry(metadata, "com.example.AnotherTest")).isNotEqualTo(new Group.Entry(metadata, "com.example.Test"));
	}
}
