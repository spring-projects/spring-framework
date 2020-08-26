/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.context.index.test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;

import javax.annotation.processing.Processor;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

/**
 * Wrapper to make the {@link JavaCompiler} easier to use in tests.
 *
 * @author Stephane Nicoll
 * @author Sam Brannen
 */
public class TestCompiler {

	public static final File ORIGINAL_SOURCE_FOLDER = new File("src/test/java");

	private final JavaCompiler compiler;

	private final StandardJavaFileManager fileManager;

	private final File outputLocation;


	public TestCompiler(Path tempDir) throws IOException {
		this(ToolProvider.getSystemJavaCompiler(), tempDir);
	}

	public TestCompiler(JavaCompiler compiler, Path tempDir) throws IOException {
		this.compiler = compiler;
		this.fileManager = compiler.getStandardFileManager(null, null, null);
		this.outputLocation = tempDir.toFile();
		Iterable<? extends File> temp = Collections.singletonList(this.outputLocation);
		this.fileManager.setLocation(StandardLocation.CLASS_OUTPUT, temp);
		this.fileManager.setLocation(StandardLocation.SOURCE_OUTPUT, temp);
	}


	public TestCompilationTask getTask(Class<?>... types) {
		return getTask(Arrays.stream(types).map(Class::getName).toArray(String[]::new));
	}

	public TestCompilationTask getTask(String... types) {
		Iterable<? extends JavaFileObject> javaFileObjects = getJavaFileObjects(types);
		return getTask(javaFileObjects);
	}

	private TestCompilationTask getTask(Iterable<? extends JavaFileObject> javaFileObjects) {
		return new TestCompilationTask(
				this.compiler.getTask(null, this.fileManager, null, null, null, javaFileObjects));
	}

	public File getOutputLocation() {
		return this.outputLocation;
	}

	private Iterable<? extends JavaFileObject> getJavaFileObjects(String... types) {
		File[] files = new File[types.length];
		for (int i = 0; i < types.length; i++) {
			files[i] = getFile(types[i]);
		}
		return this.fileManager.getJavaFileObjects(files);
	}

	private File getFile(String type) {
		return new File(getSourceFolder(), sourcePathFor(type));
	}

	private static String sourcePathFor(String type) {
		return type.replace(".", "/") + ".java";
	}

	private File getSourceFolder() {
		return ORIGINAL_SOURCE_FOLDER;
	}


	/**
	 * A compilation task.
	 */
	public static class TestCompilationTask {

		private final JavaCompiler.CompilationTask task;

		public TestCompilationTask(JavaCompiler.CompilationTask task) {
			this.task = task;
		}

		public void call(Processor... processors) {
			this.task.setProcessors(Arrays.asList(processors));
			if (!this.task.call()) {
				throw new IllegalStateException("Compilation failed");
			}
		}
	}

}
