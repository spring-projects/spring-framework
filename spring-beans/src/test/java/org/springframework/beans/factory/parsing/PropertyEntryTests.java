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

package org.springframework.beans.factory.parsing;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for {@link PropertyEntry}.
 *
 * @author Rick Evans
 * @author Chris Beams
 */
public class PropertyEntryTests {

	@Test
	public void testCtorBailsOnNullPropertyNameArgument() throws Exception {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new PropertyEntry(null));
	}

	@Test
	public void testCtorBailsOnEmptyPropertyNameArgument() throws Exception {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new PropertyEntry(""));
	}

	@Test
	public void testCtorBailsOnWhitespacedPropertyNameArgument() throws Exception {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new PropertyEntry("\t   "));
	}

}
