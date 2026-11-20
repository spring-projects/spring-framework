/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.type.filter;

import java.io.IOException;

import org.springframework.core.type.ClassMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;

/**
 * Type filter that exposes a
 * {@link org.springframework.core.type.ClassMetadata} object
 * to subclasses, for class testing purposes.
 *
 * @author Rod Johnson
 * @author Costin Leau
 * @author Juergen Hoeller
 * @since 2.5
 * @see #match(org.springframework.core.type.ClassMetadata)
 */
public abstract class AbstractClassTestingTypeFilter implements TypeFilter {

	@Override
	public final boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory)
			throws IOException {

		return match(metadataReader.getClassMetadata());
	}

	/**
	 * Determine a match based on the given ClassMetadata object.
	 * @param metadata the ClassMetadata object
	 * @return whether this filter matches on the specified type
	 */
	protected abstract boolean match(ClassMetadata metadata);

}
