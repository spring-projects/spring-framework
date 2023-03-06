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

import java.util.Iterator;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

/**
 * An immutable collection of {@link SourceFile} instances.
 *
 * @author Phillip Webb
 * @since 6.0
 */
public final class SourceFiles implements Iterable<SourceFile> {

	private static final SourceFiles NONE = new SourceFiles(DynamicFiles.none());


	private final DynamicFiles<SourceFile> files;


	private SourceFiles(DynamicFiles<SourceFile> files) {
		this.files = files;
	}


	/**
	 * Return a {@link SourceFiles} instance with no items.
	 * @return the empty instance
	 */
	public static SourceFiles none() {
		return NONE;
	}

	/**
	 * Factory method that can be used to create a {@link SourceFiles} instance
	 * containing the specified files.
	 * @param sourceFiles the files to include
	 * @return a {@link SourceFiles} instance
	 */
	public static SourceFiles of(SourceFile... sourceFiles) {
		return none().and(sourceFiles);
	}

	/**
	 * Return a new {@link SourceFiles} instance that merges files from another
	 * array of {@link SourceFile} instances.
	 * @param sourceFiles the instances to merge
	 * @return a new {@link SourceFiles} instance containing merged content
	 */
	public SourceFiles and(SourceFile... sourceFiles) {
		return new SourceFiles(this.files.and(sourceFiles));
	}

	/**
	 * Return a new {@link SourceFiles} instance that merges files from another
	 * array of {@link SourceFile} instances.
	 * @param sourceFiles the instances to merge
	 * @return a new {@link SourceFiles} instance containing merged content
	 */
	public SourceFiles and(Iterable<SourceFile> sourceFiles) {
		return new SourceFiles(this.files.and(sourceFiles));
	}

	/**
	 * Return a new {@link SourceFiles} instance that merges files from another
	 * {@link SourceFiles} instance.
	 * @param sourceFiles the instance to merge
	 * @return a new {@link SourceFiles} instance containing merged content
	 */
	public SourceFiles and(SourceFiles sourceFiles) {
		return new SourceFiles(this.files.and(sourceFiles.files));
	}

	@Override
	public Iterator<SourceFile> iterator() {
		return this.files.iterator();
	}

	/**
	 * Stream the {@link SourceFile} instances contained in this collection.
	 * @return a stream of file instances
	 */
	public Stream<SourceFile> stream() {
		return this.files.stream();
	}

	/**
	 * Returns {@code true} if this collection is empty.
	 * @return if this collection is empty
	 */
	public boolean isEmpty() {
		return this.files.isEmpty();
	}

	/**
	 * Get the {@link SourceFile} with the given
	 * {@linkplain  DynamicFile#getPath() path}.
	 * @param path the path to find
	 * @return a {@link SourceFile} instance or {@code null}
	 */
	@Nullable
	public SourceFile get(String path) {
		return this.files.get(path);
	}

	/**
	 * Return the single source file contained in the collection.
	 * @return the single file
	 * @throws IllegalStateException if the collection doesn't contain exactly
	 * one file
	 */
	public SourceFile getSingle() throws IllegalStateException {
		return this.files.getSingle();
	}

	/**
	 * Return the single matching source file contained in the collection.
	 * @return the single file
	 * @throws IllegalStateException if the collection doesn't contain exactly
	 * one file
	 */
	public SourceFile getSingle(String pattern) throws IllegalStateException {
		return getSingle(Pattern.compile(pattern));
	}

	private SourceFile getSingle(Pattern pattern) {
		return this.files.getSingle(
				candidate -> pattern.matcher(candidate.getClassName()).matches());
	}

	/**
	 * Return a single source file contained in the specified package.
	 * @return the single file
	 * @throws IllegalStateException if the collection doesn't contain exactly
	 * one file
	 */
	public SourceFile getSingleFromPackage(String packageName) {
		return this.files.getSingle(candidate -> Objects.equals(packageName,
				ClassUtils.getPackageName(candidate.getClassName())));
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		return this.files.equals(((SourceFiles) obj).files);
	}

	@Override
	public int hashCode() {
		return this.files.hashCode();
	}

	@Override
	public String toString() {
		return this.files.toString();
	}

}
