/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.web.reactive.result.view;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.http.HttpHeaders;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link DefaultRenderingBuilder}.
 *
 * @author Rossen Stoyanchev
 */
public class DefaultRenderingBuilderTests {

	@Test
	public void defaultValues() {
		Rendering rendering = Rendering.view("abc").build();

		assertThat(rendering.view()).isEqualTo("abc");
		assertThat(rendering.modelAttributes()).isEqualTo(Collections.emptyMap());
		assertThat(rendering.status()).isNull();
		assertThat(rendering.headers()).isEmpty();
	}

	@Test
	public void defaultValuesForRedirect() throws Exception {
		Rendering rendering = Rendering.redirectTo("abc").build();

		Object view = rendering.view();
		assertThat(view.getClass()).isEqualTo(RedirectView.class);
		assertThat(((RedirectView) view).getUrl()).isEqualTo("abc");
		assertThat(((RedirectView) view).isContextRelative()).isTrue();
		assertThat(((RedirectView) view).isPropagateQuery()).isFalse();
	}


	@Test
	public void viewName() {
		Rendering rendering = Rendering.view("foo").build();
		assertThat(rendering.view()).isEqualTo("foo");
	}

	@Test
	public void modelAttribute() throws Exception {
		Foo foo = new Foo();
		Rendering rendering = Rendering.view("foo").modelAttribute(foo).build();

		assertThat(rendering.modelAttributes()).isEqualTo(Collections.singletonMap("foo", foo));
	}

	@Test
	public void modelAttributes() throws Exception {
		Foo foo = new Foo();
		Bar bar = new Bar();
		Rendering rendering = Rendering.view("foo").modelAttributes(foo, bar).build();

		Map<String, Object> map = new LinkedHashMap<>(2);
		map.put("foo", foo);
		map.put("bar", bar);
		assertThat(rendering.modelAttributes()).isEqualTo(map);
	}

	@Test
	public void model() throws Exception {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("foo", new Foo());
		map.put("bar", new Bar());
		Rendering rendering = Rendering.view("foo").model(map).build();

		assertThat(rendering.modelAttributes()).isEqualTo(map);
	}

	@Test
	public void header() throws Exception {
		Rendering rendering = Rendering.view("foo").header("foo", "bar").build();

		assertThat(rendering.headers()).hasSize(1);
		assertThat(rendering.headers().get("foo")).isEqualTo(Collections.singletonList("bar"));
	}

	@Test
	public void httpHeaders() throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.add("foo", "bar");
		Rendering rendering = Rendering.view("foo").headers(headers).build();

		assertThat(rendering.headers()).isEqualTo(headers);
	}

	@Test
	public void redirectWithAbsoluteUrl() throws Exception {
		Rendering rendering = Rendering.redirectTo("foo").contextRelative(false).build();

		Object view = rendering.view();
		assertThat(view.getClass()).isEqualTo(RedirectView.class);
		assertThat(((RedirectView) view).isContextRelative()).isFalse();
	}

	@Test
	public void redirectWithPropagateQuery() throws Exception {
		Rendering rendering = Rendering.redirectTo("foo").propagateQuery(true).build();

		Object view = rendering.view();
		assertThat(view.getClass()).isEqualTo(RedirectView.class);
		assertThat(((RedirectView) view).isPropagateQuery()).isTrue();
	}


	private static class Foo {}

	private static class Bar {}

}
