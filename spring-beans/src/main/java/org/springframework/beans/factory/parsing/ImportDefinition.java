/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.factory.parsing;

import org.jspecify.annotations.Nullable;

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

	private final Resource @Nullable [] actualResources;

	private final @Nullable Object source;


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
	public ImportDefinition(String importedResource, @Nullable Object source) {
		this(importedResource, null, source);
	}

	/**
	 * Create a new ImportDefinition.
	 * @param importedResource the location of the imported resource
	 * @param source the source object (may be {@code null})
	 */
	public ImportDefinition(String importedResource, Resource @Nullable [] actualResources, @Nullable Object source) {
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

	public final Resource @Nullable [] getActualResources() {
		return this.actualResources;
	}

	@Override
	public final @Nullable Object getSource() {
		return this.source;
	}

}
