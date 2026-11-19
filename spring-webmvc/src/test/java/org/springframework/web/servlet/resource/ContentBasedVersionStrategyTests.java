/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.servlet.resource;

import java.io.IOException;
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
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 */
class ContentBasedVersionStrategyTests {

	private final ContentVersionStrategy versionStrategy = new ContentVersionStrategy();


	@BeforeEach
	void setup() {
		VersionResourceResolver versionResourceResolver = new VersionResourceResolver();
		versionResourceResolver.setStrategyMap(Collections.singletonMap("/**", this.versionStrategy));
	}

	@Test
	void extractVersion() {
		String hash = "7fbe76cdac6093784895bb4989203e5a";
		String path = "font-awesome/css/font-awesome.min-" + hash + ".css";

		assertThat(this.versionStrategy.extractVersion(path)).isEqualTo(hash);
		assertThat(this.versionStrategy.extractVersion("foo/bar.css")).isNull();
	}

	@Test
	void extractVersionWithMultipleDashesAndDots() {
		assertThat(this.versionStrategy.extractVersion("my-dir/foo-abc.min.css")).isEqualTo("abc.min");
		assertThat(this.versionStrategy.extractVersion("foo-bar baz-abc.css")).isEqualTo("abc");
		assertThat(this.versionStrategy.extractVersion("foo-bar/baz")).isNull();
	}

	@Test
	@Timeout(5)
	void extractVersionWithManyDashes() {
		assertThat(this.versionStrategy.extractVersion("css/" + "-".repeat(100_000))).isNull();
	}

	@Test
	void removeVersion() {
		String hash = "7fbe76cdac6093784895bb4989203e5a";
		String file = "font-awesome/css/font-awesome.min%s%s.css";

		assertThat(this.versionStrategy.removeVersion(String.format(file, "-", hash), hash)).isEqualTo(String.format(file, "", ""));
	}

	@Test
	void removeVersionOnlyOnce() {
		String hash = "sha";
		String file = "font-awesome/css%s%s/font-awesome.min%s%s.css";

		assertThat(this.versionStrategy.removeVersion(String.format(file, "-", hash, "-", hash), hash)).isEqualTo(String.format(file, "-", hash, "", ""));
	}

	@Test
	void getResourceVersion() throws IOException {
		Resource expected = new ClassPathResource("test/bar.css", getClass());
		String hash = DigestUtils.md5DigestAsHex(FileCopyUtils.copyToByteArray(expected.getInputStream()));

		assertThat(this.versionStrategy.getResourceVersion(expected)).isEqualTo(hash);
	}

	@Test
	void addVersionToUrl() {
		assertThat(this.versionStrategy.addVersion("test/bar.css", "123")).isEqualTo("test/bar-123.css");
	}

}
