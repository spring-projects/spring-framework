/*
 * Copyright 2002-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.core.type.classreading;

import java.io.IOException;

import org.springframework.core.io.Resource;

/**
 * Factory interface for {@link MetadataReader} instances.
 * Allows for caching a MetadataReader per original resource.
 *
 * @author Juergen Hoeller
 * @since 2.5
 * @see SimpleMetadataReaderFactory
 * @see CachingMetadataReaderFactory
 */
public interface MetadataReaderFactory {

	/**
	 * Obtain a MetadataReader for the given class name.
	 * @param className the class name (to be resolved to a ".class" file)
	 * @return a holder for the ClassReader instance (never {@code null})
	 * @throws IOException in case of I/O failure
	 */
	MetadataReader getMetadataReader(String className) throws IOException;

	/**
	 * Obtain a MetadataReader for the given resource.
	 * @param resource the resource (pointing to a ".class" file)
	 * @return a holder for the ClassReader instance (never {@code null})
	 * @throws IOException in case of I/O failure
	 */
	MetadataReader getMetadataReader(Resource resource) throws IOException;

}
