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

package org.springframework.core.test.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.thoughtworks.qdox.JavaProjectBuilder;
import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaSource;
import org.assertj.core.api.AssertProvider;
import org.jspecify.annotations.Nullable;

import org.springframework.core.io.InputStreamSource;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;

/**
 * {@link DynamicFile} that holds Java source code and provides
 * {@link SourceFileAssert} support. Usually created from an AOT generated
 * type, for example:
 * <pre class="code">
 * SourceFile.of(generatedFile::writeTo)
 * </pre>
 *
 * @author Phillip Webb
 * @author Scott Frederick
 * @since 6.0
 */
public final class SourceFile extends DynamicFile implements AssertProvider<SourceFileAssert> {

	private static final File TEST_SOURCE_DIRECTORY = new File("src/test/java");

	private final String className;


	private SourceFile(String path, String content, String className) {
		super(path, content);
		this.className = className;
	}


	/**
	 * Factory method to create a new {@link SourceFile} by looking up source
	 * for the given test {@code Class}.
	 * @param type the class file to get the source from
	 * @return a {@link SourceFile} instance
	 */
	public static SourceFile forTestClass(Class<?> type) {
		return forClass(TEST_SOURCE_DIRECTORY, type);
	}

	/**
	 * Factory method to create a new {@link SourceFile} by looking up source
	 * for the given {@code Class}.
	 * @param sourceDirectory the source directory
	 * @param type the class file to get the source from
	 * @return a {@link SourceFile} instance
	 */
	public static SourceFile forClass(File sourceDirectory, Class<?> type) {
		String sourceFileName = type.getName().replace('.', '/');
		File sourceFile = new File(sourceDirectory, sourceFileName + ".java");
		return SourceFile.of(() -> new FileInputStream(sourceFile));
	}

	/**
	 * Factory method to create a new {@link SourceFile} from the given
	 * {@link CharSequence}.
	 * @param charSequence a file containing the source contents
	 * @return a {@link SourceFile} instance
	 */
	public static SourceFile of(CharSequence charSequence) {
		return of(null, charSequence);
	}

	/**
	 * Factory method to create a new {@link SourceFile} from the given
	 * {@link CharSequence}.
	 * @param path the relative path of the file or {@code null} to have the
	 * path deduced
	 * @param charSequence a file containing the source contents
	 * @return a {@link SourceFile} instance
	 */
	public static SourceFile of(@Nullable String path, CharSequence charSequence) {
		return of(path, appendable -> appendable.append(charSequence));
	}

	/**
	 * Factory method to create a new {@link SourceFile} from the given
	 * {@link InputStreamSource}.
	 * @param inputStreamSource the source for the file
	 * @return a {@link SourceFile} instance
	 */
	public static SourceFile of(InputStreamSource inputStreamSource) {
		return of(null, inputStreamSource);
	}

	/**
	 * Factory method to create a new {@link SourceFile} from the given
	 * {@link InputStreamSource}.
	 * @param path the relative path of the file or {@code null} to have the
	 * path deduced
	 * @param inputStreamSource the source for the file
	 * @return a {@link SourceFile} instance
	 */
	public static SourceFile of(@Nullable String path, InputStreamSource inputStreamSource) {
		return of(path, appendable -> appendable.append(copyToString(inputStreamSource)));
	}

	private static String copyToString(InputStreamSource inputStreamSource) throws IOException {
		InputStreamReader reader = new InputStreamReader(inputStreamSource.getInputStream(), StandardCharsets.UTF_8);
		return FileCopyUtils.copyToString(reader);
	}

	/**
	 * Factory method to create a new {@link SourceFile} from the given
	 * {@link WritableContent}.
	 * @param writableContent the content to write to the file
	 * @return a {@link SourceFile} instance
	 */
	public static SourceFile of(WritableContent writableContent) {
		return of(null, writableContent);
	}

	/**
	 * Factory method to create a new {@link SourceFile} from the given
	 * {@link WritableContent}.
	 * @param path the relative path of the file or {@code null} to have the
	 * path deduced
	 * @param writableContent the content to write to the file
	 * @return a {@link SourceFile} instance
	 */
	public static SourceFile of(@Nullable String path, WritableContent writableContent) {
		String content = toString(writableContent);
		Assert.state(StringUtils.hasLength(content), "WritableContent did not append any content");
		String className = getClassName(content);
		if (!StringUtils.hasLength(path)) {
			path = ClassUtils.convertClassNameToResourcePath(className) + ".java";
		}
		return new SourceFile(path, content, className);
	}

	/**
	 * Return the fully-qualified class name.
	 * @return the fully qualified class name
	 */
	public String getClassName() {
		return this.className;
	}

	private static String getClassName(String content) {
		JavaProjectBuilder builder = new JavaProjectBuilder();
		try {
			JavaSource javaSource = builder.addSource(new StringReader(content));
			if (javaSource.getClasses().isEmpty()) {
				// QDOX doesn't let us inspect records yet, but we only need the
				// class name so lets make the content look like a class
				javaSource = builder.addSource(new StringReader(makeRecordsLookLikeClasses(content)));
			}
			Assert.state(javaSource.getClasses().size() == 1, "Source must define a single class");
			JavaClass javaClass = javaSource.getClasses().get(0);
			return (javaSource.getPackage() != null) ?
					(javaSource.getPackageName() + "." + javaClass.getName()) : javaClass.getName();
		}
		catch (Exception ex) {
			throw new IllegalStateException(
					"Unable to parse source file content:\n\n" + content, ex);
		}
	}

	private static String makeRecordsLookLikeClasses(String content) {
		Pattern pattern = Pattern.compile("record\\s(\\S+)\\(");
		Matcher matcher = pattern.matcher(content);
		if (matcher.find()) {
			StringBuilder result = new StringBuilder();
			result.append(content.substring(0, matcher.start()) + "class");
			result.append(content.substring(matcher.start() + 6, matcher.end() - 1));
			int parenthesesCount = 1;
			for (int i = matcher.end(); i < content.length(); i++) {
				char ch = content.charAt(i);
				if (parenthesesCount > 0) {
					if (ch == '(') {
						parenthesesCount++;
					}
					else if (ch == ')') {
						parenthesesCount--;
					}
				}
				else {
					result.append(ch);
				}
			}
			return makeRecordsLookLikeClasses(result.toString());
		}
		return content;
	}

	/**
	 * Use {@code assertThat(sourceFile)} rather than calling this method
	 * directly.
	 */
	@Override
	public SourceFileAssert assertThat() {
		return new SourceFileAssert(this);
	}

}
