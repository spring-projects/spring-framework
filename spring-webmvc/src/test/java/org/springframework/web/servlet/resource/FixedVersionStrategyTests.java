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

import java.util.Collections;
import java.util.concurrent.Callable;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

import org.springframework.core.io.Resource;

/**
 * Unit tests for {@link org.springframework.web.servlet.resource.FixedVersionStrategy}
 * @author Brian Clozel
 */
public class FixedVersionStrategyTests {

	private final String version = "1df341f";

	private final String resourceId = "js/foo.js";

	private FixedVersionStrategy versionStrategy;

	@Before
	public void setup() {
		this.versionStrategy = new FixedVersionStrategy(this.version);
	}

	@Test(expected = IllegalArgumentException.class)
	public void constructWithEmptyPrefixVersion() throws Exception {

		FixedVersionStrategy versionStrategy = new FixedVersionStrategy("  ");
	}

	@Test(expected = IllegalArgumentException.class)
	public void constructWithEmptyCallableVersion() throws Exception {

		FixedVersionStrategy versionStrategy = new FixedVersionStrategy(
				new Callable<String>() {
					@Override
					public String call() throws Exception {
						return "  ";
					}
				});
	}

	@Test
	public void extractVersionFromPath() throws Exception {
		assertEquals(this.version + "/", this.versionStrategy.extractVersionFromPath(this.version + "/" + this.resourceId));
		assertEquals("", this.versionStrategy.extractVersionFromPath(this.resourceId));
	}

	@Test
	public void deleteVersionFromPath() throws Exception {
		assertEquals(this.resourceId,
				this.versionStrategy.deleteVersionFromPath(this.version + "/" + this.resourceId, this.version + "/"));
	}

	@Test
	public void addVersionToUrl() throws Exception {
		assertEquals(this.version + "/" + this.resourceId,
				this.versionStrategy.addVersionToUrl(this.resourceId, Collections.<Resource>emptyList(), null));

	}
}
