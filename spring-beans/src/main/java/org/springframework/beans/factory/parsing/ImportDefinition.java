/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.beans.factory.parsing;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

/**
 * Representation of an import that has been processed during the parsing process.
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see ReaderEventListener#importProcessed(ImportDefinition)
 */
public class ImportDefinition implements BeanMetadataElement {

	private final String importedResource;

	private final Resource[] actualResources;

	private final Object source;


	/**
	 * Create a new ImportDefinition.
	 * @param importedResource the location of the imported resource
	 */
	public ImportDefinition(String importedResource) {
		this(importedResource, null, null);
	}

	/**
	 * Create a new ImportDefinition.
	 * @param importedResource the location of the imported resource
	 * @param source the source object (may be {@code null})
	 */
	public ImportDefinition(String importedResource, Object source) {
		this(importedResource, null, source);
	}

	/**
	 * Create a new ImportDefinition.
	 * @param importedResource the location of the imported resource
	 * @param source the source object (may be {@code null})
	 */
	public ImportDefinition(String importedResource, Resource[] actualResources, Object source) {
		Assert.notNull(importedResource, "Imported resource must not be null");
		this.importedResource = importedResource;
		this.actualResources = actualResources;
		this.source = source;
	}


	/**
	 * Return the location of the imported resource.
	 */
	public final String getImportedResource() {
		return this.importedResource;
	}

	public final Resource[] getActualResources() {
		return this.actualResources;
	}

	public final Object getSource() {
		return this.source;
	}

}
