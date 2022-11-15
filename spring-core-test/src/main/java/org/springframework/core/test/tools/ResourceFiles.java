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
import java.util.stream.Stream;

import org.springframework.lang.Nullable;

/**
 * An immutable collection of {@link ResourceFile} instances.
 *
 * @author Phillip Webb
 * @since 6.0
 */
public final class ResourceFiles implements Iterable<ResourceFile> {

	private static final ResourceFiles NONE = new ResourceFiles(DynamicFiles.none());


	private final DynamicFiles<ResourceFile> files;


	private ResourceFiles(DynamicFiles<ResourceFile> files) {
		this.files = files;
	}


	/**
	 * Return a {@link DynamicFiles} instance with no items.
	 * @return the empty instance
	 */
	public static ResourceFiles none() {
		return NONE;
	}

	/**
	 * Factory method that can be used to create a {@link ResourceFiles}
	 * instance containing the specified files.
	 * @param ResourceFiles the files to include
	 * @return a {@link ResourceFiles} instance
	 */
	public static ResourceFiles of(ResourceFile... ResourceFiles) {
		return none().and(ResourceFiles);
	}

	/**
	 * Return a new {@link ResourceFiles} instance that merges files from
	 * another array of {@link ResourceFile} instances.
	 * @param resourceFiles the instances to merge
	 * @return a new {@link ResourceFiles} instance containing merged content
	 */
	public ResourceFiles and(ResourceFile... resourceFiles) {
		return new ResourceFiles(this.files.and(resourceFiles));
	}

	/**
	 * Return a new {@link ResourceFiles} instance that merges files from another iterable
	 * of {@link ResourceFiles} instances.
	 * @param resourceFiles the instances to merge
	 * @return a new {@link ResourceFiles} instance containing merged content
	 */
	public ResourceFiles and(Iterable<ResourceFile> resourceFiles) {
		return new ResourceFiles(this.files.and(resourceFiles));
	}

	/**
	 * Return a new {@link ResourceFiles} instance that merges files from
	 * another {@link ResourceFiles} instance.
	 * @param ResourceFiles the instance to merge
	 * @return a new {@link ResourceFiles} instance containing merged content
	 */
	public ResourceFiles and(ResourceFiles ResourceFiles) {
		return new ResourceFiles(this.files.and(ResourceFiles.files));
	}

	@Override
	public Iterator<ResourceFile> iterator() {
		return this.files.iterator();
	}

	/**
	 * Stream the {@link ResourceFile} instances contained in this collection.
	 * @return a stream of file instances
	 */
	public Stream<ResourceFile> stream() {
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
	 * Get the {@link ResourceFile} with the given
	 * {@linkplain  DynamicFile#getPath() path}.
	 * @param path the path to find
	 * @return a {@link ResourceFile} instance or {@code null}
	 */
	@Nullable
	public ResourceFile get(String path) {
		return this.files.get(path);
	}

	/**
	 * Return the single {@link ResourceFile} contained in the collection.
	 * @return the single file
	 * @throws IllegalStateException if the collection doesn't contain exactly
	 * one file
	 */
	public ResourceFile getSingle() throws IllegalStateException {
		return this.files.getSingle();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		return this.files.equals(((ResourceFiles) obj).files);
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
