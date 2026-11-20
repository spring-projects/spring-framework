/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.reactive.resource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link FixedVersionStrategy}.
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
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
	void addVersionRelativePath() {
		String relativePath = "../" + PATH;
		assertThat(this.strategy.addVersion(relativePath, VERSION)).isEqualTo(relativePath);
	}

}
