/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.type;

/**
 * @author Chris Beams
 * @since 3.1
 * @see AbstractClassMetadataMemberClassTests
 */
class StandardClassMetadataMemberClassTests extends AbstractClassMetadataMemberClassTests {

	@Override
	@SuppressWarnings("deprecation")
	public ClassMetadata getClassMetadataFor(Class<?> clazz) {
		return new StandardClassMetadata(clazz);
	}

}
