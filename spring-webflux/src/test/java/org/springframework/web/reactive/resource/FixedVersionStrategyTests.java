/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.web.reactive.resource;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link FixedVersionStrategy}.
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 */
public class FixedVersionStrategyTests {

	private static final String VERSION = "1df341f";

	private static final String PATH = "js/foo.js";


	private FixedVersionStrategy strategy;


	@Before
	public void setup() {
		this.strategy = new FixedVersionStrategy(VERSION);
	}


	@Test(expected = IllegalArgumentException.class)
	public void emptyPrefixVersion() {
		new FixedVersionStrategy("  ");
	}

	@Test
	public void extractVersion() {
		assertEquals(VERSION, this.strategy.extractVersion(VERSION + "/" + PATH));
		assertNull(this.strategy.extractVersion(PATH));
	}

	@Test
	public void removeVersion() {
		assertEquals("/" + PATH, this.strategy.removeVersion(VERSION + "/" + PATH, VERSION));
	}

	@Test
	public void addVersion() {
		assertEquals(VERSION + "/" + PATH, this.strategy.addVersion("/" + PATH, VERSION));
	}

	@Test  // SPR-13727
	public void addVersionRelativePath() {
		String relativePath = "../" + PATH;
		assertEquals(relativePath, this.strategy.addVersion(relativePath, VERSION));
	}

}
