/*
 * Copyright 2002-2025 the original author or authors.
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

package org.springframework.docs.integration.resthttpinterface.customresolver;

import java.util.List;

import org.springframework.core.MethodParameter;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.invoker.HttpRequestValues;
import org.springframework.web.service.invoker.HttpServiceArgumentResolver;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

public class CustomHttpServiceArgumentResolver {

	// tag::httpinterface[]
	interface RepositoryService {

		@GetExchange("/repos/search")
		List<Repository> searchRepository(Search search);

	}
	// end::httpinterface[]

	class Sample {

		void sample() {
			// tag::usage[]
			RestClient restClient = RestClient.builder().baseUrl("https://api.github.com/").build();
			RestClientAdapter adapter = RestClientAdapter.create(restClient);
			HttpServiceProxyFactory factory = HttpServiceProxyFactory
					.builderFor(adapter)
					.customArgumentResolver(new SearchQueryArgumentResolver())
					.build();
			RepositoryService repositoryService = factory.createClient(RepositoryService.class);

			Search search = Search.create()
					.owner("spring-projects")
					.language("java")
					.query("rest")
					.build();
			List<Repository> repositories = repositoryService.searchRepository(search);
			// end::usage[]
		}

	}

	// tag::argumentresolver[]
	static class SearchQueryArgumentResolver implements HttpServiceArgumentResolver {
		@Override
		public boolean resolve(Object argument, MethodParameter parameter, HttpRequestValues.Builder requestValues) {
			if (parameter.getParameterType().equals(Search.class)) {
				Search search = (Search) argument;
				requestValues.addRequestParameter("owner", search.owner());
				requestValues.addRequestParameter("language", search.language());
				requestValues.addRequestParameter("query", search.query());
				return true;
			}
			return false;
		}
	}
	// end::argumentresolver[]


	record Search (String query, String owner, String language) {

		static Builder create() {
			return new Builder();
		}

		static class Builder {

			Builder query(String query) { return this;}

			Builder owner(String owner) { return this;}

			Builder language(String language) { return this;}

			Search build() {
				return new Search(null, null, null);
			}
		}

	}

	record Repository(String name) {

	}

}
