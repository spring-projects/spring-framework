/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.web.servlet.resource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.DigestUtils;
import org.springframework.util.FileCopyUtils;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link org.springframework.web.servlet.resource.ContentBasedVersionStrategy}
 * @author Brian Clozel
 */
public class ContentBasedVersionStrategyTests {

	private List<Resource> locations;

	private ContentBasedVersionStrategy versionStrategy = new ContentBasedVersionStrategy();

	private ResourceResolverChain chain;

	@Before
	public void setup() {
		this.locations = new ArrayList<Resource>();
		this.locations.add(new ClassPathResource("test/", getClass()));
		this.locations.add(new ClassPathResource("testalternatepath/", getClass()));

		VersionResourceResolver versionResourceResolver = new VersionResourceResolver();
		versionResourceResolver.setVersionStrategyMap(Collections.singletonMap("/**", this.versionStrategy));
		this.chain = new DefaultResourceResolverChain(Arrays.asList(versionResourceResolver, new PathResourceResolver()));
	}

	@Test
	public void extractVersionFromPath() throws Exception {
		String hash = "7fbe76cdac6093784895bb4989203e5a";
		String path = "font-awesome/css/font-awesome.min-" + hash + ".css";

		assertEquals(hash, this.versionStrategy.extractVersionFromPath(path));
		assertEquals("", this.versionStrategy.extractVersionFromPath("foo/bar.css"));
	}

	@Test
	public void deleteVersionFromPath() throws Exception {
		String file = "font-awesome/css/font-awesome.min%s%s.css";
		String hash = "7fbe76cdac6093784895bb4989203e5a";

		assertEquals(String.format(file, "", ""), this.versionStrategy.deleteVersionFromPath(String.format(file, "-", hash), hash));
		assertEquals("", this.versionStrategy.extractVersionFromPath("foo/bar.css"));
	}

	@Test
	public void resourceVersionMatches() throws Exception {
		Resource expected = new ClassPathResource("test/bar.css", getClass());
		String hash = DigestUtils.md5DigestAsHex(FileCopyUtils.copyToByteArray(expected.getInputStream()));
		String wrongHash = "wronghash";

		assertTrue(this.versionStrategy.resourceVersionMatches(expected, hash));
		assertFalse(this.versionStrategy.resourceVersionMatches(expected, wrongHash));
	}

	@Test
	public void addVersionToUrl() throws Exception {
		String file = "bar.css";
		Resource expected = new ClassPathResource("test/" + file, getClass());
		String hash = DigestUtils.md5DigestAsHex(FileCopyUtils.copyToByteArray(expected.getInputStream()));
		String path = "bar-" + hash + ".css";
		String resultUrl = this.versionStrategy.addVersionToUrl(file, this.locations, this.chain);

		assertEquals(path, resultUrl);
	}

}
