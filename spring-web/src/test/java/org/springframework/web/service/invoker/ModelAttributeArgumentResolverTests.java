/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.web.service.invoker;

import org.junit.jupiter.api.Test;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.BindParam;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.PostExchange;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ModelAttributeArgumentResolver}.
 *
 * <p>Additional tests for this resolver:
 * <ul>
 * <li>Base class functionality in {@link NamedValueArgumentResolverTests}
 * <li>Form data vs query params in {@link HttpRequestValuesTests}
 * </ul>
 *
 * @author Hermann Pencole
 */
class ModelAttributeArgumentResolverTests {

	private final TestExchangeAdapter client = new TestExchangeAdapter();

	private final HttpServiceProxyFactory.Builder builder = HttpServiceProxyFactory.builderFor(this.client);


	@Test
	void requestParam() {
		Service service = builder.build().createClient(Service.class);
		service.postForm(new MyBean1_2("value 1", true),new MyBean3_4("value 3", List.of("1", "2", "3")));

		Object body = this.client.getRequestValues().getBodyValue();
		assertThat(body).isInstanceOf(MultiValueMap.class);
		assertThat((MultiValueMap<String, String>) body).hasSize(4)
				.containsEntry("param.1", List.of("value 1"))
				.containsEntry("param2", List.of("true"))
				.containsEntry("param.3", List.of("value 3"))
				.containsEntry("param4", List.of("1,2,3"));
	}

	@Test
	void requestParamWithDisabledFormattingCollectionValue() {
		Service service = builder.build().createClient(Service.class);
		service.getWithParams(new MyBean1_2("value 1", true),new MyBean3_4("value 3",  List.of("1", "2", "3")));

		HttpRequestValues values = this.client.getRequestValues();
		String uriTemplate = values.getUriTemplate();
		Map<String, String> uriVariables = values.getUriVariables();
		UriComponents uri = UriComponentsBuilder.fromUriString(uriTemplate).buildAndExpand(uriVariables).encode();
		assertThat(uri.getQuery()).isEqualTo("param.1=value%201&param2=true&param.3=value%203&param4=1,2,3");
	}

	private interface Service {

		@PostExchange(contentType = "application/x-www-form-urlencoded")
		void postForm(@ModelAttribute MyBean1_2 param1_2, @ModelAttribute MyBean3_4 param3_4);

		@GetExchange
		void getWithParams(@ModelAttribute MyBean1_2 param1_2, @ModelAttribute MyBean3_4 param3_4);
	}

	private record MyBean1_2 (
			@BindParam("param.1") String param1,
			Boolean param2
	){}

	private record MyBean3_4 (
			@BindParam("param.3") String param3,
			List<String> param4
	){}

}
