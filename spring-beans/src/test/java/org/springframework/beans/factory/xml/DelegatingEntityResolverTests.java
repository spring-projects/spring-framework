/*
 * Copyright 2002-2006 the original author or authors.
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

package org.springframework.beans.factory.xml;

import org.junit.Test;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

/**
 * Unit tests for the {@link DelegatingEntityResolver} class.
 *
 * @author Rick Evans
 * @author Chris Beams
 */
public final class DelegatingEntityResolverTests {

	@Test(expected=IllegalArgumentException.class)
	public void testCtorWhereDtdEntityResolverIsNull() throws Exception {
		new DelegatingEntityResolver(null, new NoOpEntityResolver());
	}

	@Test(expected=IllegalArgumentException.class)
	public void testCtorWhereSchemaEntityResolverIsNull() throws Exception {
		new DelegatingEntityResolver(new NoOpEntityResolver(), null);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testCtorWhereEntityResolversAreBothNull() throws Exception {
		new DelegatingEntityResolver(null, null);
	}


	private static final class NoOpEntityResolver implements EntityResolver {
		@Override
		public InputSource resolveEntity(String publicId, String systemId) {
			return null;
		}
	}

}
