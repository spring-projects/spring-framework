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

package org.springframework.aot.test.generate.file;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.springframework.lang.Nullable;

/**
 * An immutable collection of {@link ClassFile} instances.
 *
 * @author Stephane Nicoll
 * @since 6.0
 */
public final class ClassFiles implements Iterable<ClassFile> {

	private static final ClassFiles NONE = new ClassFiles(Collections.emptyMap());

	private final Map<String, ClassFile> files;

	private ClassFiles(Map<String, ClassFile> files) {
		this.files = files;
	}


	/**
	 * Return a {@link ClassFiles} instance with no items.
	 * @return the empty instance
	 */
	public static ClassFiles none() {
		return NONE;
	}

	/**
	 * Factory method that can be used to create a {@link ClassFiles}
	 * instance containing the specified classes.
	 * @param ClassFiles the classes to include
	 * @return a {@link ClassFiles} instance
	 */
	public static ClassFiles of(ClassFile... ClassFiles) {
		return none().and(ClassFiles);
	}

	/**
	 * Return a new {@link ClassFiles} instance that merges classes from
	 * another array of {@link ClassFile} instances.
	 * @param classFiles the instances to merge
	 * @return a new {@link ClassFiles} instance containing merged content
	 */
	public ClassFiles and(ClassFile... classFiles) {
		Map<String, ClassFile> merged = new LinkedHashMap<>(this.files);
		Arrays.stream(classFiles).forEach(file -> merged.put(file.getName(), file));
		return new ClassFiles(Collections.unmodifiableMap(merged));
	}

	/**
	 * Return a new {@link ClassFiles} instance that merges classes from another
	 * iterable of {@link ClassFiles} instances.
	 * @param classFiles the instances to merge
	 * @return a new {@link ClassFiles} instance containing merged content
	 */
	public ClassFiles and(Iterable<ClassFile> classFiles) {
		Map<String, ClassFile> merged = new LinkedHashMap<>(this.files);
		classFiles.forEach(file -> merged.put(file.getName(), file));
		return new ClassFiles(Collections.unmodifiableMap(merged));
	}

	@Override
	public Iterator<ClassFile> iterator() {
		return this.files.values().iterator();
	}

	/**
	 * Stream the {@link ClassFile} instances contained in this collection.
	 * @return a stream of classes
	 */
	public Stream<ClassFile> stream() {
		return this.files.values().stream();
	}

	/**
	 * Returns {@code true} if this collection is empty.
	 * @return if this collection is empty
	 */
	public boolean isEmpty() {
		return this.files.isEmpty();
	}

	/**
	 * Get the {@link ClassFile} with the given class name.
	 * @param name the fully qualified name to find
	 * @return a {@link ClassFile} instance or {@code null}
	 */
	@Nullable
	public ClassFile get(String name) {
		return this.files.get(name);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		return this.files.equals(((ClassFiles) obj).files);
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
