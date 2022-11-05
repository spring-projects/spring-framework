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

package org.springframework.aot.generate;

import org.springframework.core.io.InputStreamSource;
import org.springframework.javapoet.JavaFile;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.function.ThrowingConsumer;

/**
 * Interface that can be used to add {@link Kind#SOURCE source},
 * {@link Kind#RESOURCE resource}, or {@link Kind#CLASS class} files generated
 * during ahead-of-time processing. Source and resource files are written using
 * UTF-8 encoding.
 *
 * @author Phillip Webb
 * @author Brian Clozel
 * @author Stephane Nicoll
 * @since 6.0
 * @see InMemoryGeneratedFiles
 * @see FileSystemGeneratedFiles
 */
public interface GeneratedFiles {

	/**
	 * Add a generated {@link Kind#SOURCE source file} with content from the
	 * given {@link JavaFile}.
	 * @param javaFile the java file to add
	 */
	default void addSourceFile(JavaFile javaFile) {
		String className = javaFile.packageName + "." + javaFile.typeSpec.name;
		addSourceFile(className, javaFile::writeTo);
	}

	/**
	 * Add a generated {@link Kind#SOURCE source file} with content from the
	 * given {@link CharSequence}.
	 * @param className the class name that should be used to determine the path
	 * of the file
	 * @param content the contents of the file
	 */
	default void addSourceFile(String className, CharSequence content) {
		addSourceFile(className, appendable -> appendable.append(content));
	}

	/**
	 * Add a generated {@link Kind#SOURCE source file} with content written to
	 * an {@link Appendable} passed to the given {@link ThrowingConsumer}.
	 * @param className the class name that should be used to determine the path
	 * of the file
	 * @param content a {@link ThrowingConsumer} that accepts an
	 * {@link Appendable} which will receive the file contents
	 */
	default void addSourceFile(String className, ThrowingConsumer<Appendable> content) {
		addFile(Kind.SOURCE, getClassNamePath(className), content);
	}

	/**
	 * Add a generated {@link Kind#SOURCE source file} with content from the
	 * given {@link InputStreamSource}.
	 * @param className the class name that should be used to determine the path
	 * of the file
	 * @param content an {@link InputStreamSource} that will provide an input
	 * stream containing the file contents
	 */
	default void addSourceFile(String className, InputStreamSource content) {
		addFile(Kind.SOURCE, getClassNamePath(className), content);
	}

	/**
	 * Add a generated {@link Kind#RESOURCE resource file} with content from the
	 * given {@link CharSequence}.
	 * @param path the relative path of the file
	 * @param content the contents of the file
	 */
	default void addResourceFile(String path, CharSequence content) {
		addResourceFile(path, appendable -> appendable.append(content));
	}

	/**
	 * Add a generated {@link Kind#RESOURCE resource file} with content written
	 * to an {@link Appendable} passed to the given {@link ThrowingConsumer}.
	 * @param path the relative path of the file
	 * @param content a {@link ThrowingConsumer} that accepts an
	 * {@link Appendable} which will receive the file contents
	 */
	default void addResourceFile(String path, ThrowingConsumer<Appendable> content) {
		addFile(Kind.RESOURCE, path, content);
	}

	/**
	 * Add a generated {@link Kind#RESOURCE resource file} with content from the
	 * given {@link InputStreamSource}.
	 * @param path the relative path of the file
	 * @param content an {@link InputStreamSource} that will provide an input
	 * stream containing the file contents
	 */
	default void addResourceFile(String path, InputStreamSource content) {
		addFile(Kind.RESOURCE, path, content);
	}

	/**
	 * Add a generated {@link Kind#CLASS class file} with content from the given
	 * {@link InputStreamSource}.
	 * @param path the relative path of the file
	 * @param content an {@link InputStreamSource} that will provide an input
	 * stream containing the file contents
	 */
	default void addClassFile(String path, InputStreamSource content) {
		addFile(Kind.CLASS, path, content);
	}

	/**
	 * Add a generated file of the specified {@link Kind} with content from the
	 * given {@link CharSequence}.
	 * @param kind the kind of file being written
	 * @param path the relative path of the file
	 * @param content the contents of the file
	 */
	default void addFile(Kind kind, String path, CharSequence content) {
		addFile(kind, path, appendable -> appendable.append(content));
	}

	/**
	 * Add a generated file of the specified {@link Kind} with content written
	 * to an {@link Appendable} passed to the given {@link ThrowingConsumer}.
	 * @param kind the kind of file being written
	 * @param path the relative path of the file
	 * @param content a {@link ThrowingConsumer} that accepts an
	 * {@link Appendable} which will receive the file contents
	 */
	default void addFile(Kind kind, String path, ThrowingConsumer<Appendable> content) {
		Assert.notNull(content, "'content' must not be null");
		addFile(kind, path, new AppendableConsumerInputStreamSource(content));
	}

	/**
	 * Add a generated file of the specified {@link Kind} with content from the
	 * given {@link InputStreamSource}.
	 * @param kind the kind of file being written
	 * @param path the relative path of the file
	 * @param content an {@link InputStreamSource} that will provide an input
	 * stream containing the file contents
	 */
	void addFile(Kind kind, String path, InputStreamSource content);

	private static String getClassNamePath(String className) {
		Assert.hasLength(className, "'className' must not be empty");
		Assert.isTrue(isJavaIdentifier(className), "'className' must be a valid identifier");
		return ClassUtils.convertClassNameToResourcePath(className) + ".java";
	}

	private static boolean isJavaIdentifier(String className) {
		char[] chars = className.toCharArray();
		for (int i = 0; i < chars.length; i++) {
			if (i == 0 && !Character.isJavaIdentifierStart(chars[i])) {
				return false;
			}
			if (i > 0 && chars[i] != '.' && !Character.isJavaIdentifierPart(chars[i])) {
				return false;
			}
		}
		return true;
	}


	/**
	 * The various kinds of generated files that are supported.
	 */
	enum Kind {

		/**
		 * A source file containing Java code that should be compiled.
		 */
		SOURCE,

		/**
		 * A resource file that should be directly added to the final application.
		 * For example, a {@code .properties} file.
		 */
		RESOURCE,

		/**
		 * A class file containing bytecode. For example, the result of a proxy
		 * generated using CGLIB.
		 */
		CLASS

	}

}
