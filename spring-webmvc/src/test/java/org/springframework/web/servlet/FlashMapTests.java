/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.web.servlet;

import org.junit.jupiter.api.Test;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test fixture for {@link FlashMap} tests.
 *
 * @author Rossen Stoyanchev
 */
public class FlashMapTests {

	@Test
	public void isExpired() throws InterruptedException {
		assertThat(new FlashMap().isExpired()).isFalse();

		FlashMap flashMap = new FlashMap();
		flashMap.startExpirationPeriod(0);
		Thread.sleep(100);

		assertThat(flashMap.isExpired()).isTrue();
	}

	@Test
	public void notExpired() throws InterruptedException {
		FlashMap flashMap = new FlashMap();
		flashMap.startExpirationPeriod(10);
		Thread.sleep(100);

		assertThat(flashMap.isExpired()).isFalse();
	}

	@Test
	public void compareTo() {
		FlashMap flashMap1 = new FlashMap();
		FlashMap flashMap2 = new FlashMap();
		assertThat(flashMap1.compareTo(flashMap2)).isEqualTo(0);

		flashMap1.setTargetRequestPath("/path1");
		assertThat(flashMap1.compareTo(flashMap2)).isEqualTo(-1);
		assertThat(flashMap2.compareTo(flashMap1)).isEqualTo(1);

		flashMap2.setTargetRequestPath("/path2");
		assertThat(flashMap1.compareTo(flashMap2)).isEqualTo(0);

		flashMap1.addTargetRequestParam("id", "1");
		assertThat(flashMap1.compareTo(flashMap2)).isEqualTo(-1);
		assertThat(flashMap2.compareTo(flashMap1)).isEqualTo(1);

		flashMap2.addTargetRequestParam("id", "2");
		assertThat(flashMap1.compareTo(flashMap2)).isEqualTo(0);
	}

	@Test
	public void addTargetRequestParamNullValue() {
		FlashMap flashMap = new FlashMap();
		flashMap.addTargetRequestParam("text", "abc");
		flashMap.addTargetRequestParam("empty", " ");
		flashMap.addTargetRequestParam("null", null);

		assertThat(flashMap.getTargetRequestParams().size()).isEqualTo(1);
		assertThat(flashMap.getTargetRequestParams().getFirst("text")).isEqualTo("abc");
	}

	@Test
	public void addTargetRequestParamsNullValue() {
		MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
		params.add("key", "abc");
		params.add("key", " ");
		params.add("key", null);

		FlashMap flashMap = new FlashMap();
		flashMap.addTargetRequestParams(params);

		assertThat(flashMap.getTargetRequestParams().size()).isEqualTo(1);
		assertThat(flashMap.getTargetRequestParams().get("key").size()).isEqualTo(1);
		assertThat(flashMap.getTargetRequestParams().getFirst("key")).isEqualTo("abc");
	}

	@Test
	public void addTargetRequestParamNullKey() {
		FlashMap flashMap = new FlashMap();
		flashMap.addTargetRequestParam(" ", "abc");
		flashMap.addTargetRequestParam(null, "abc");

		assertThat(flashMap.getTargetRequestParams().isEmpty()).isTrue();
	}

	@Test
	public void addTargetRequestParamsNullKey() {
		MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
		params.add(" ", "abc");
		params.add(null, " ");

		FlashMap flashMap = new FlashMap();
		flashMap.addTargetRequestParams(params);

		assertThat(flashMap.getTargetRequestParams().isEmpty()).isTrue();
	}

}
