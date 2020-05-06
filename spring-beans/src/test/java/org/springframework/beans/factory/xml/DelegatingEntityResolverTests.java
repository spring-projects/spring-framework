/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.beans.factory.xml;

import org.junit.jupiter.api.Test;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for the {@link DelegatingEntityResolver} class.
 *
 * @author Rick Evans
 * @author Chris Beams
 */
public class DelegatingEntityResolverTests {

	@Test
	public void testCtorWhereDtdEntityResolverIsNull() throws Exception {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new DelegatingEntityResolver(null, new NoOpEntityResolver()));
	}

	@Test
	public void testCtorWhereSchemaEntityResolverIsNull() throws Exception {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new DelegatingEntityResolver(new NoOpEntityResolver(), null));
	}

	@Test
	public void testCtorWhereEntityResolversAreBothNull() throws Exception {
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
