/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.http;

import org.hamcrest.Matchers;
import org.junit.Test;

import static org.junit.Assert.*;

import java.util.concurrent.TimeUnit;

/**
 * @author Brian Clozel
 */
public class CacheControlTests {

	@Test
	public void emptyCacheControl() throws Exception {
		CacheControl cc = CacheControl.empty();
		assertThat(cc.getHeaderValue(), Matchers.nullValue());
	}

	@Test
	public void maxAge() throws Exception {
		CacheControl cc = CacheControl.maxAge(1, TimeUnit.HOURS);
		assertThat(cc.getHeaderValue(), Matchers.equalTo("max-age=3600"));
	}

	@Test
	public void maxAgeAndDirectives() throws Exception {
		CacheControl cc = CacheControl.maxAge(3600, TimeUnit.SECONDS).cachePublic().noTransform();
		assertThat(cc.getHeaderValue(), Matchers.equalTo("max-age=3600, no-transform, public"));
	}

	@Test
	public void maxAgeAndSMaxAge() throws Exception {
		CacheControl cc = CacheControl.maxAge(1, TimeUnit.HOURS).sMaxAge(30, TimeUnit.MINUTES);
		assertThat(cc.getHeaderValue(), Matchers.equalTo("max-age=3600, s-maxage=1800"));
	}

	@Test
	public void noCachePrivate() throws Exception {
		CacheControl cc = CacheControl.noCache().cachePrivate();
		assertThat(cc.getHeaderValue(), Matchers.equalTo("no-cache, private"));
	}

	@Test
	public void noStore() throws Exception {
		CacheControl cc = CacheControl.noStore();
		assertThat(cc.getHeaderValue(), Matchers.equalTo("no-store"));
	}

	@Test
	public void staleIfError() throws Exception {
		CacheControl cc = CacheControl.maxAge(1, TimeUnit.HOURS).staleIfError(2, TimeUnit.HOURS);
		assertThat(cc.getHeaderValue(), Matchers.equalTo("max-age=3600, stale-if-error=7200"));
	}

	@Test
	public void staleWhileRevalidate() throws Exception {
		CacheControl cc = CacheControl.maxAge(1, TimeUnit.HOURS).staleWhileRevalidate(2, TimeUnit.HOURS);
		assertThat(cc.getHeaderValue(), Matchers.equalTo("max-age=3600, stale-while-revalidate=7200"));
	}

}
