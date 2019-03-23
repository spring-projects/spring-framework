/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.reactive.resource;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Unit tests for {@link FixedVersionStrategy}.
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 */
public class FixedVersionStrategyTests {

	private final String version = "1df341f";

	private final String path = "js/foo.js";

	private FixedVersionStrategy strategy;


	@Before
	public void setup() {
		this.strategy = new FixedVersionStrategy(this.version);
	}


	@Test(expected = IllegalArgumentException.class)
	public void emptyPrefixVersion() throws Exception {
		new FixedVersionStrategy("  ");
	}

	@Test
	public void extractVersion() throws Exception {
		assertEquals(this.version, this.strategy.extractVersion(this.version + "/" + this.path));
		assertNull(this.strategy.extractVersion(this.path));
	}

	@Test
	public void removeVersion() throws Exception {
		assertEquals("/" + this.path, this.strategy.removeVersion(this.version + "/" + this.path, this.version));
	}

	@Test
	public void addVersion() throws Exception {
		assertEquals(this.version + "/" + this.path, this.strategy.addVersion("/" + this.path, this.version));
	}

	@Test  // SPR-13727
	public void addVersionRelativePath() throws Exception {
		String relativePath = "../" + this.path;
		assertEquals(relativePath, this.strategy.addVersion(relativePath, this.version));
	}

}
