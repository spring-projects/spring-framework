/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.reactive.resource;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.DigestUtils;
import org.springframework.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ContentVersionStrategy}.
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 */
class ContentBasedVersionStrategyTests {

	private ContentVersionStrategy strategy = new ContentVersionStrategy();


	@BeforeEach
	void setup() {
		VersionResourceResolver versionResourceResolver = new VersionResourceResolver();
		versionResourceResolver.setStrategyMap(Collections.singletonMap("/**", this.strategy));
	}

	@Test
	void extractVersion() {
		String hash = "7fbe76cdac6093784895bb4989203e5a";
		String path = "font-awesome/css/font-awesome.min-" + hash + ".css";

		assertThat(this.strategy.extractVersion(path)).isEqualTo(hash);
		assertThat(this.strategy.extractVersion("foo/bar.css")).isNull();
	}

	@Test
	void extractVersionWithMultipleDashesAndDots() {
		assertThat(this.strategy.extractVersion("my-dir/foo-abc.min.css")).isEqualTo("abc.min");
		assertThat(this.strategy.extractVersion("foo-bar baz-abc.css")).isEqualTo("abc");
		assertThat(this.strategy.extractVersion("foo-bar/baz")).isNull();
	}

	@Test
	@Timeout(5)
	void extractVersionWithManyDashes() {
		assertThat(this.strategy.extractVersion("css/" + "-".repeat(100_000))).isNull();
	}

	@Test
	void removeVersion() {
		String hash = "7fbe76cdac6093784895bb4989203e5a";
		String path = "font-awesome/css/font-awesome.min%s%s.css";

		assertThat(this.strategy.removeVersion(String.format(path, "-", hash), hash)).isEqualTo(String.format(path, "", ""));
	}

	@Test
	void removeVersionOnlyOnce() {
		String hash = "sha";
		String path = "font-awesome/css%s%s/font-awesome.min%s%s.css";

		assertThat(this.strategy.removeVersion(String.format(path, "-", hash, "-", hash), hash)).isEqualTo(String.format(path, "-", hash, "", ""));
	}

	@Test
	void getResourceVersion() throws Exception {
		Resource expected = new ClassPathResource("test/bar.css", getClass());
		String hash = DigestUtils.md5DigestAsHex(FileCopyUtils.copyToByteArray(expected.getInputStream()));

		assertThat(this.strategy.getResourceVersion(expected).block()).isEqualTo(hash);
	}

	@Test
	void addVersionToUrl() {
		assertThat(this.strategy.addVersion("test/bar.css", "123")).isEqualTo("test/bar-123.css");
	}

}
