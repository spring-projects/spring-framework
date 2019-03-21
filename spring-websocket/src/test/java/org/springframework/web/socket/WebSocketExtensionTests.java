/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.web.socket;

import java.util.List;

import org.hamcrest.Matchers;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Test fixture for {@link WebSocketExtension}
 * @author Brian Clozel
 */
public class WebSocketExtensionTests {

	@Test
	public void parseHeaderSingle() {
		List<WebSocketExtension> extensions = WebSocketExtension.parseExtensions("x-test-extension ; foo=bar ; bar=baz");
		assertThat(extensions, Matchers.hasSize(1));
		WebSocketExtension extension = extensions.get(0);

		assertEquals("x-test-extension", extension.getName());
		assertEquals(2, extension.getParameters().size());
		assertEquals("bar", extension.getParameters().get("foo"));
		assertEquals("baz", extension.getParameters().get("bar"));
	}

	@Test
	public void parseHeaderMultiple() {
		List<WebSocketExtension> extensions = WebSocketExtension.parseExtensions("x-foo-extension, x-bar-extension");
		assertThat(extensions, Matchers.hasSize(2));
		assertEquals("x-foo-extension", extensions.get(0).getName());
		assertEquals("x-bar-extension", extensions.get(1).getName());
	}

}
