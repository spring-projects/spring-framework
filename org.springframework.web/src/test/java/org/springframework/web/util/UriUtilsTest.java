/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.web.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.UnsupportedEncodingException;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/** @author Arjen Poutsma */
public class UriUtilsTest {

	@Test
	public void encode() throws UnsupportedEncodingException {
		assertEquals("Invalid encoded URI", "foobar", UriUtils.encode("foobar", "UTF-8"));
		assertEquals("Invalid encoded URI", "foo%20bar", UriUtils.encode("foo bar", "UTF-8"));
		assertEquals("Invalid encoded URI", "foo%2Bbar", UriUtils.encode("foo+bar", "UTF-8"));
	}

	@Test
	public void decode() throws UnsupportedEncodingException {
		assertEquals("Invalid encoded URI", "foobar", UriUtils.decode("foobar", "UTF-8"));
		assertEquals("Invalid encoded URI", "foo bar", UriUtils.decode("foo%20bar", "UTF-8"));
		assertEquals("Invalid encoded URI", "foo+bar", UriUtils.decode("foo%2bbar", "UTF-8"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void decodeInvalidSequence() throws UnsupportedEncodingException {
		UriUtils.decode("foo%2", "UTF-8");
	}

}
