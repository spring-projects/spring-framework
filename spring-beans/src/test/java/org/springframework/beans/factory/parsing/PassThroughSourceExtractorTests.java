/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.beans.factory.parsing;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PassThroughSourceExtractor}.
 *
 * @author Rick Evans
 * @author Chris Beams
 */
class PassThroughSourceExtractorTests {

	@Test
	void testPassThroughContract() {
		Object source = new Object();
		Object extractedSource = new PassThroughSourceExtractor().extractSource(source, null);
		assertThat(extractedSource).as("The contract of PassThroughSourceExtractor states that the supplied " +
				"source object *must* be returned as-is").isSameAs(source);
	}

	@Test
	void testPassThroughContractEvenWithNull() {
		Object extractedSource = new PassThroughSourceExtractor().extractSource(null, null);
		assertThat(extractedSource).as("The contract of PassThroughSourceExtractor states that the supplied " +
				"source object *must* be returned as-is (even if null)").isNull();
	}

}
