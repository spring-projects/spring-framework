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

package org.springframework.core.test.tools;

import java.io.IOException;
import java.io.OutputStream;
import java.util.EnumSet;

import javax.tools.FileObject;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileManager.Location;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.util.StreamUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;

/**
 * Tests for {@link DynamicJavaFileManager}.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 */
class DynamicJavaFileManagerTests {

	private static final byte[] DUMMY_BYTECODE = new byte[] { 'a' };

	private static final String DUMMY_RESOURCE = "a";

	@Mock
	private JavaFileManager parentFileManager;

	@Mock
	private Location location;

	private ClassLoader classLoader;

	private DynamicJavaFileManager fileManager;

	@BeforeEach
	void setup() {
		MockitoAnnotations.openMocks(this);
		this.classLoader = new ClassLoader() {};
		ClassFiles classFiles = ClassFiles.of(
				ClassFile.of("com.example.one.ClassOne", DUMMY_BYTECODE),
				ClassFile.of("com.example.two.ClassTwo", DUMMY_BYTECODE));
		ResourceFiles resourceFiles = ResourceFiles.of(
				ResourceFile.of("com/example/one/resource.one", DUMMY_RESOURCE),
				ResourceFile.of("com/example/two/resource.two", DUMMY_RESOURCE));
		this.fileManager = new DynamicJavaFileManager(this.parentFileManager, this.classLoader,
				classFiles, resourceFiles);
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
		writeDummyResource(fileObject1);
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
		writeDummyBytecode(this.fileManager.getJavaFileForOutput(this.location, "com.example.MyClass1",
				Kind.CLASS, null));
		writeDummyBytecode(this.fileManager.getJavaFileForOutput(this.location, "com.example.MyClass2",
				Kind.CLASS, null));
		assertThat(this.fileManager.getDynamicClassFiles()).containsKeys(
				"com.example.MyClass1", "com.example.MyClass2");
	}

	@Test
	void listWithoutRecurseReturnsClassesInRequestedPackage() throws IOException {
		Iterable<JavaFileObject> listed = this.fileManager.list(
				this.location, "com.example.one", EnumSet.allOf(Kind.class), false);
		assertThat(listed).hasSize(1);
		assertThat(listed).extracting(JavaFileObject::getName).containsExactly("/com/example/one/ClassOne.class");
	}

	@Test
	void listWithRecurseReturnsClassesInRequestedPackageAndSubpackages() throws IOException {
		Iterable<JavaFileObject> listed = this.fileManager.list(
				this.location, "com.example", EnumSet.allOf(Kind.class), true);
		assertThat(listed).hasSize(2);
		assertThat(listed).extracting(JavaFileObject::getName)
				.containsExactly("/com/example/one/ClassOne.class", "/com/example/two/ClassTwo.class");
	}

	@Test
	void listWithoutClassKindDoesNotReturnClasses() throws IOException {
		Iterable<JavaFileObject> listed = this.fileManager.list(
				this.location, "com.example", EnumSet.of(Kind.SOURCE), true);
		assertThat(listed).hasSize(0);
	}

	@Test
	void getFileForOutputReturnsDynamicResourceFile() {
		FileObject fileObject = this.fileManager.getFileForOutput(this.location,
				"", "META-INF/generated.properties", null);
		assertThat(fileObject).isInstanceOf(DynamicResourceFileObject.class);
	}

	@Test
	void getFileForOutputReturnsFile() throws Exception {
		writeDummyResource(this.fileManager.getFileForOutput(this.location, "", "META-INF/first.properties", null));
		writeDummyResource(this.fileManager.getFileForOutput(this.location, "", "META-INF/second.properties", null));
		assertThat(this.fileManager.getDynamicResourceFiles()).containsKeys("META-INF/first.properties",
				"META-INF/second.properties");
	}

	private void writeDummyBytecode(JavaFileObject fileObject) throws IOException {
		try (OutputStream outputStream = fileObject.openOutputStream()) {
			StreamUtils.copy(DUMMY_BYTECODE, outputStream);
		}
	}

	private void writeDummyResource(FileObject fileObject) throws IOException {
		try (OutputStream outputStream = fileObject.openOutputStream()) {
			StreamUtils.copy(DUMMY_RESOURCE.getBytes(), outputStream);
		}
	}

}
