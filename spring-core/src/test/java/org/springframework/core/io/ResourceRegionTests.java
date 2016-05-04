/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.core.io;

import static org.mockito.Mockito.mock;

import org.junit.Test;

/**
 * Unit tests for the {@link ResourceRegion} class.
 *
 * @author Brian Clozel
 */
public class ResourceRegionTests {

	@Test(expected = IllegalArgumentException.class)
	public void shouldThrowExceptionWithNullResource() {
		new ResourceRegion(null, 0, 1);
	}

	@Test(expected = IllegalArgumentException.class)
	public void shouldThrowExceptionForNegativePosition() {
		new ResourceRegion(mock(Resource.class), -1, 1);
	}

	@Test(expected = IllegalArgumentException.class)
	public void shouldThrowExceptionForNegativeCount() {
		new ResourceRegion(mock(Resource.class), 0, -1);
	}
}
