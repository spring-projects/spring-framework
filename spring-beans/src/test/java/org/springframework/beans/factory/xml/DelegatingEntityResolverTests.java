/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.factory.xml;

import org.junit.jupiter.api.Test;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link DelegatingEntityResolver}.
 *
 * @author Rick Evans
 * @author Chris Beams
 */
class DelegatingEntityResolverTests {

	@Test
	void ctorWhereDtdEntityResolverIsNull() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new DelegatingEntityResolver(null, new NoOpEntityResolver()));
	}

	@Test
	void ctorWhereSchemaEntityResolverIsNull() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new DelegatingEntityResolver(new NoOpEntityResolver(), null));
	}

	@Test
	void ctorWhereEntityResolversAreBothNull() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new DelegatingEntityResolver(null, null));
	}


	private static final class NoOpEntityResolver implements EntityResolver {
		@Override
		public InputSource resolveEntity(String publicId, String systemId) {
			return null;
		}
	}

}
