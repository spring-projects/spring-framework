/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.http;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * Tests for {@link CacheControl}.
 * @author Brian Clozel
 */
class CacheControlTests {

	@Test
	void emptyCacheControl() {
		CacheControl cc = CacheControl.empty();
		assertThat(cc.getHeaderValue()).isNull();
	}

	@Test
	void maxAge() {
		CacheControl cc = CacheControl.maxAge(1, TimeUnit.HOURS);
		assertThat(cc.getHeaderValue()).isEqualTo("max-age=3600");
	}

	@Test
	void maxAge_duration() {
		CacheControl cc = CacheControl.maxAge(Duration.ofHours(1));
		assertThat(cc.getHeaderValue()).isEqualTo("max-age=3600");
	}

	@Test
	void maxAgeAndDirectives() {
		CacheControl cc = CacheControl.maxAge(3600, TimeUnit.SECONDS).cachePublic().noTransform();
		assertThat(cc.getHeaderValue()).isEqualTo("max-age=3600, no-transform, public");
	}

	@Test
	void maxAgeAndSMaxAge() {
		CacheControl cc = CacheControl.maxAge(1, TimeUnit.HOURS).sMaxAge(30, TimeUnit.MINUTES);
		assertThat(cc.getHeaderValue()).isEqualTo("max-age=3600, s-maxage=1800");
	}

	@Test
	void maxAgeAndSMaxAge_duration() {
		CacheControl cc = CacheControl.maxAge(Duration.ofHours(1)).sMaxAge(Duration.ofMinutes(30));
		assertThat(cc.getHeaderValue()).isEqualTo("max-age=3600, s-maxage=1800");
	}

	@Test
	void noCachePrivate() {
		CacheControl cc = CacheControl.noCache().cachePrivate();
		assertThat(cc.getHeaderValue()).isEqualTo("no-cache, private");
	}

	@Test
	void noStore() {
		CacheControl cc = CacheControl.noStore();
		assertThat(cc.getHeaderValue()).isEqualTo("no-store");
	}

	@Test
	void staleIfError() {
		CacheControl cc = CacheControl.maxAge(1, TimeUnit.HOURS).staleIfError(2, TimeUnit.HOURS);
		assertThat(cc.getHeaderValue()).isEqualTo("max-age=3600, stale-if-error=7200");
	}

	@Test
	void staleIfError_duration() {
		CacheControl cc = CacheControl.maxAge(Duration.ofHours(1)).staleIfError(2, TimeUnit.HOURS);
		assertThat(cc.getHeaderValue()).isEqualTo("max-age=3600, stale-if-error=7200");
	}

	@Test
	void staleWhileRevalidate() {
		CacheControl cc = CacheControl.maxAge(1, TimeUnit.HOURS).staleWhileRevalidate(2, TimeUnit.HOURS);
		assertThat(cc.getHeaderValue()).isEqualTo("max-age=3600, stale-while-revalidate=7200");
	}

	@Test
	void staleWhileRevalidate_duration() {
		CacheControl cc = CacheControl.maxAge(Duration.ofHours(1)).staleWhileRevalidate(2, TimeUnit.HOURS);
		assertThat(cc.getHeaderValue()).isEqualTo("max-age=3600, stale-while-revalidate=7200");
	}

	@Test
	void immutable() {
		CacheControl cc = CacheControl.maxAge(Duration.ofHours(1)).immutable();
		assertThat(cc.getHeaderValue()).isEqualTo("max-age=3600, immutable");
	}

}
