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

package org.springframework.aot.test.generator.file;

import java.io.StringReader;

import javax.annotation.Nullable;

import com.thoughtworks.qdox.JavaProjectBuilder;
import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaPackage;
import com.thoughtworks.qdox.model.JavaSource;
import org.assertj.core.api.AssertProvider;
import org.assertj.core.util.Strings;

/**
 * {@link DynamicFile} that holds Java source code and provides
 * {@link SourceFileAssert} support. Usually created from an AOT generated type,
 * for example:
 *
 * <pre class="code">
 * SourceFile.of(generatedFile::writeTo)
 * </pre>
 *
 * @author Phillip Webb
 * @since 6.0
 */
public final class SourceFile extends DynamicFile
		implements AssertProvider<SourceFileAssert> {


	private final JavaSource javaSource;


	private SourceFile(String path, String content, JavaSource javaSource) {
		super(path, content);
		this.javaSource = javaSource;
	}


	/**
	 * Factory method to create a new {@link SourceFile} from the given
	 * {@link CharSequence}.
	 * @param charSequence a file containing the source contents
	 * @return a {@link SourceFile} instance
	 */
	public static SourceFile of(CharSequence charSequence) {
		return of(null, appendable -> appendable.append(charSequence));
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
		if (Strings.isNullOrEmpty(content)) {
			throw new IllegalStateException("WritableContent did not append any content");
		}
		JavaSource javaSource = parse(content);
		if (path == null || path.isEmpty()) {
			path = deducePath(javaSource);
		}
		return new SourceFile(path, content, javaSource);
	}

	private static JavaSource parse(String content) {
		JavaProjectBuilder builder = new JavaProjectBuilder();
		try {
			JavaSource javaSource = builder.addSource(new StringReader(content));
			if (javaSource.getClasses().size() != 1) {
				throw new IllegalStateException("Source must define a single class");
			}
			return javaSource;
		}
		catch (Exception ex) {
			throw new IllegalStateException(
					"Unable to parse source file content:\n\n" + content, ex);
		}
	}

	private static String deducePath(JavaSource javaSource) {
		JavaPackage javaPackage = javaSource.getPackage();
		JavaClass javaClass = javaSource.getClasses().get(0);
		String path = javaClass.getName() + ".java";
		if (javaPackage != null) {
			path = javaPackage.getName().replace('.', '/') + "/" + path;
		}
		return path;
	}

	JavaSource getJavaSource() {
		return this.javaSource;
	}

	/**
	 * Return the target class for this source file or {@code null}. The target
	 * class can be used if private lookup access is required.
	 * @return the target class
	 */
	@Nullable
	public Class<?> getTarget() {
		return null; // Not yet supported
	}

	public String getClassName() {
		return this.javaSource.getClasses().get(0).getFullyQualifiedName();
	}

	/**
	 * AssertJ {@code assertThat} support.
	 * @deprecated use {@code assertThat(sourceFile)} rather than calling this
	 * method directly.
	 */
	@Override
	@Deprecated
	public SourceFileAssert assertThat() {
		return new SourceFileAssert(this);
	}

}
