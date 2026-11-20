/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.service.invoker;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.PostExchange;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link RequestParamArgumentResolver}.
 *
 * <p>Additional tests for this resolver:
 * <ul>
 * <li>Base class functionality in {@link NamedValueArgumentResolverTests}
 * <li>Form data vs query params in {@link HttpRequestValuesTests}
 * </ul>
 *
 * @author Rossen Stoyanchev
 */
class RequestParamArgumentResolverTests {

	private final TestExchangeAdapter client = new TestExchangeAdapter();

	private final HttpServiceProxyFactory.Builder builder = HttpServiceProxyFactory.builderFor(this.client);


	@Test
	@SuppressWarnings("unchecked")
	void requestParam() {
		Service service = builder.build().createClient(Service.class);
		service.postForm("value 1", "value 2");

		Object body = this.client.getRequestValues().getBodyValue();
		assertThat(body).isInstanceOf(MultiValueMap.class);
		assertThat((MultiValueMap<String, String>) body).hasSize(2)
				.containsEntry("param1", List.of("value 1"))
				.containsEntry("param2", List.of("value 2"));
	}

	@Test
	void requestParamWithDisabledFormattingCollectionValue() {
		RequestParamArgumentResolver resolver = new RequestParamArgumentResolver(new DefaultConversionService());
		resolver.setFavorSingleValue(true);

		Service service = builder.customArgumentResolver(resolver).build().createClient(Service.class);
		service.getWithParams("value 1", List.of("1", "2", "3"));

		HttpRequestValues values = this.client.getRequestValues();
		String uriTemplate = values.getUriTemplate();
		Map<String, String> uriVariables = values.getUriVariables();
		UriComponents uri = UriComponentsBuilder.fromUriString(uriTemplate).buildAndExpand(uriVariables).encode();
		assertThat(uri.getQuery()).isEqualTo("param1=value%201&param2=1,2,3");
	}

	private interface Service {

		@PostExchange(contentType = "application/x-www-form-urlencoded")
		void postForm(@RequestParam String param1, @RequestParam String param2);

		@GetExchange
		void getWithParams(@RequestParam String param1, @RequestParam List<String> param2);
	}

}
