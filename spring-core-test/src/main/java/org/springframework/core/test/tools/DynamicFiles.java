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

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.springframework.lang.Nullable;

/**
 * Internal class used by {@link SourceFiles} and {@link ResourceFiles} to
 * manage {@link DynamicFile} instances.
 *
 * @author Phillip Webb
 * @since 6.0
 * @param <F> the {@link DynamicFile} type
 */
final class DynamicFiles<F extends DynamicFile> implements Iterable<F> {


	private static final DynamicFiles<?> NONE = new DynamicFiles<>(Collections.emptyMap());


	private final Map<String, F> files;


	private DynamicFiles(Map<String, F> files) {
		this.files = files;
	}


	@SuppressWarnings("unchecked")
	static <F extends DynamicFile> DynamicFiles<F> none() {
		return (DynamicFiles<F>) NONE;
	}

	DynamicFiles<F> and(Iterable<F> files) {
		Map<String, F> merged = new LinkedHashMap<>(this.files);
		files.forEach(file -> merged.put(file.getPath(), file));
		return new DynamicFiles<>(Collections.unmodifiableMap(merged));
	}

	DynamicFiles<F> and(F[] files) {
		Map<String, F> merged = new LinkedHashMap<>(this.files);
		Arrays.stream(files).forEach(file -> merged.put(file.getPath(), file));
		return new DynamicFiles<>(Collections.unmodifiableMap(merged));
	}

	DynamicFiles<F> and(DynamicFiles<F> files) {
		Map<String, F> merged = new LinkedHashMap<>(this.files);
		merged.putAll(files.files);
		return new DynamicFiles<>(Collections.unmodifiableMap(merged));
	}

	@Override
	public Iterator<F> iterator() {
		return this.files.values().iterator();
	}

	Stream<F> stream() {
		return this.files.values().stream();
	}

	boolean isEmpty() {
		return this.files.isEmpty();
	}

	@Nullable
	F get(String path) {
		return this.files.get(path);
	}

	F getSingle() {
		return getSingle(candidate -> true);
	}

	F getSingle(Predicate<F> filter) {
		List<F> files = this.files.values().stream().filter(filter).toList();
		if (files.size() != 1) {
			throw new IllegalStateException("No single file available");
		}
		return files.iterator().next();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		return this.files.equals(((DynamicFiles<?>) obj).files);
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
