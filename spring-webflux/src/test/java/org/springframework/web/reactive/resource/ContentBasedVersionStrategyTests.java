/*
 * Copyright 2002-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.web.reactive.resource;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.DigestUtils;
import org.springframework.util.FileCopyUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Unit tests for {@link ContentVersionStrategy}.
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 */
public class ContentBasedVersionStrategyTests {

	private ContentVersionStrategy strategy = new ContentVersionStrategy();


	@Before
	public void setup() {
		VersionResourceResolver versionResourceResolver = new VersionResourceResolver();
		versionResourceResolver.setStrategyMap(Collections.singletonMap("/**", this.strategy));
	}

	@Test
	public void extractVersion() throws Exception {
		String hash = "7fbe76cdac6093784895bb4989203e5a";
		String path = "font-awesome/css/font-awesome.min-" + hash + ".css";

		assertEquals(hash, this.strategy.extractVersion(path));
		assertNull(this.strategy.extractVersion("foo/bar.css"));
	}

	@Test
	public void removeVersion() throws Exception {
		String file = "font-awesome/css/font-awesome.min%s%s.css";
		String hash = "7fbe76cdac6093784895bb4989203e5a";

		assertEquals(String.format(file, "", ""),  this.strategy.removeVersion(String.format(file, "-", hash), hash));
		assertNull(this.strategy.extractVersion("foo/bar.css"));
	}

	@Test
	public void getResourceVersion() throws Exception {
		Resource expected = new ClassPathResource("test/bar.css", getClass());
		String hash = DigestUtils.md5DigestAsHex(FileCopyUtils.copyToByteArray(expected.getInputStream()));
		assertEquals(hash, this.strategy.getResourceVersion(expected).block());
	}

	@Test
	public void addVersionToUrl() throws Exception {
		String requestPath = "test/bar.css";
		String version = "123";
		assertEquals("test/bar-123.css", this.strategy.addVersion(requestPath, version));
	}

}
