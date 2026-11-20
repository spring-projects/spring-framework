/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.test.tools;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DynamicJavaFileObject}.
 *
 * @author Phillip Webb
 */
class DynamicJavaFileObjectTests {

	private static final String CONTENT = "package com.example; public class Hello {}";

	@Test
	void getUriReturnsPath() {
		DynamicJavaFileObject fileObject = new DynamicJavaFileObject(SourceFile.of(CONTENT));
		assertThat(fileObject.toUri()).hasToString("java:///com/example/Hello.java");
	}

	@Test
	void getCharContentReturnsContent() {
		DynamicJavaFileObject fileObject = new DynamicJavaFileObject(SourceFile.of(CONTENT));
		assertThat(fileObject.getCharContent(true)).isEqualTo(CONTENT);
	}

}
