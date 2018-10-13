/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.context.annotation;

import org.junit.Test;

import org.springframework.context.annotation.DeferredImportSelector.Group;
import org.springframework.core.type.AnnotationMetadata;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link DeferredImportSelector}.
 *
 * @author Stephane Nicoll
 */
public class DeferredImportSelectorTests {

	@Test
	public void entryEqualsSameInstance() {
		AnnotationMetadata metadata = mock(AnnotationMetadata.class);
		Group.Entry entry = new Group.Entry(metadata, "com.example.Test");
		assertEquals(entry, entry);
	}

	@Test
	public void entryEqualsSameMetadataAndClassName() {
		AnnotationMetadata metadata = mock(AnnotationMetadata.class);
		assertEquals(new Group.Entry(metadata, "com.example.Test"),
				new Group.Entry(metadata, "com.example.Test"));
	}

	@Test
	public void entryEqualDifferentMetadataAndSameClassName() {
		assertNotEquals(
				new Group.Entry(mock(AnnotationMetadata.class), "com.example.Test"),
				new Group.Entry(mock(AnnotationMetadata.class), "com.example.Test"));
	}

	@Test
	public void entryEqualSameMetadataAnDifferentClassName() {
		AnnotationMetadata metadata = mock(AnnotationMetadata.class);
		assertNotEquals(new Group.Entry(metadata, "com.example.Test"),
				new Group.Entry(metadata, "com.example.AnotherTest"));
	}
}
