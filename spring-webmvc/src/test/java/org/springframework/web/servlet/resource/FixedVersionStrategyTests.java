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

package org.springframework.web.servlet.resource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link FixedVersionStrategy}.
 *
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 */
class FixedVersionStrategyTests {

	private static final String VERSION = "1df341f";

	private static final String PATH = "js/foo.js";


	private FixedVersionStrategy strategy;


	@BeforeEach
	void setup() {
		this.strategy = new FixedVersionStrategy(VERSION);
	}


	@Test
	void emptyPrefixVersion() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new FixedVersionStrategy("  "));
	}

	@Test
	void extractVersion() {
		assertThat(this.strategy.extractVersion(VERSION + "/" + PATH)).isEqualTo(VERSION);
		assertThat(this.strategy.extractVersion(PATH)).isNull();
	}

	@Test
	void removeVersion() {
		assertThat(this.strategy.removeVersion(VERSION + "/" + PATH, VERSION)).isEqualTo(("/" + PATH));
	}

	@Test
	void addVersion() {
		assertThat(this.strategy.addVersion("/" + PATH, VERSION)).isEqualTo((VERSION + "/" + PATH));
	}

	@Test  // SPR-13727
	public void addVersionRelativePath() {
		String relativePath = "../" + PATH;
		assertThat(this.strategy.addVersion(relativePath, VERSION)).isEqualTo(relativePath);
	}

}
