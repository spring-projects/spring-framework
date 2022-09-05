/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.aot.test.generate.compile;

import javax.tools.JavaFileManager;
import javax.tools.JavaFileManager.Location;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;

/**
 * Tests for {@link DynamicJavaFileManager}.
 *
 * @author Phillip Webb
 * @since 6.0
 */
class DynamicJavaFileManagerTests {

	@Mock
	private JavaFileManager parentFileManager;

	@Mock
	private Location location;

	private ClassLoader classLoader;

	private DynamicJavaFileManager fileManager;

	@BeforeEach
	void setup() {
		MockitoAnnotations.openMocks(this);
		this.classLoader = new ClassLoader() {
		};
		this.fileManager = new DynamicJavaFileManager(this.parentFileManager,
				this.classLoader);
	}

	@Test
	void getClassLoaderReturnsClassLoader() {
		assertThat(this.fileManager.getClassLoader(this.location)).isSameAs(
				this.classLoader);
	}

	@Test
	void getJavaFileForOutputWhenClassKindReturnsDynamicClassFile() throws Exception {
		JavaFileObject fileObject = this.fileManager.getJavaFileForOutput(this.location,
				"com.example.MyClass", Kind.CLASS, null);
		assertThat(fileObject).isInstanceOf(DynamicClassFileObject.class);
	}

	@Test
	void getJavaFileForOutputWhenClassKindAndAlreadySeenReturnsSameDynamicClassFile()
			throws Exception {
		JavaFileObject fileObject1 = this.fileManager.getJavaFileForOutput(this.location,
				"com.example.MyClass", Kind.CLASS, null);
		JavaFileObject fileObject2 = this.fileManager.getJavaFileForOutput(this.location,
				"com.example.MyClass", Kind.CLASS, null);
		assertThat(fileObject1).isSameAs(fileObject2);
	}

	@Test
	void getJavaFileForOutputWhenNotClassKindDelegatesToParentFileManager()
			throws Exception {
		this.fileManager.getJavaFileForOutput(this.location, "com.example.MyClass",
				Kind.SOURCE, null);
		then(this.parentFileManager).should().getJavaFileForOutput(this.location,
				"com.example.MyClass", Kind.SOURCE, null);
	}

	@Test
	void getClassFilesReturnsClassFiles() throws Exception {
		this.fileManager.getJavaFileForOutput(this.location, "com.example.MyClass1",
				Kind.CLASS, null);
		this.fileManager.getJavaFileForOutput(this.location, "com.example.MyClass2",
				Kind.CLASS, null);
		assertThat(this.fileManager.getClassFiles()).containsKeys("com.example.MyClass1",
				"com.example.MyClass2");
	}

}
