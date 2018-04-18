/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.web.socket;

import org.hamcrest.Matchers;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Test fixture for {@link TextMessage}.
 *
 * @author Shinobu Aoki
 * @author Juergen Hoeller
 */
public class TextMessageTests {

	@Test
	public void toStringWithAscii() {
		String expected = "foo,bar";
		TextMessage actual = new TextMessage(expected);
		assertThat(actual.getPayload(), Matchers.is(expected));
		assertThat(actual.toString(), Matchers.containsString(expected));
	}

	@Test
	public void toStringWithMultibyteString() {
		String expected = "\u3042\u3044\u3046\u3048\u304a";
		TextMessage actual = new TextMessage(expected);
		assertThat(actual.getPayload(), Matchers.is(expected));
		assertThat(actual.toString(), Matchers.containsString(expected));
	}

}
