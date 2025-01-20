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

package org.springframework.docs.integration.resthttpinterface.customresolver

import org.springframework.core.MethodParameter
import org.springframework.web.client.RestClient
import org.springframework.web.client.support.RestClientAdapter
import org.springframework.web.service.annotation.GetExchange
import org.springframework.web.service.invoker.HttpRequestValues
import org.springframework.web.service.invoker.HttpServiceArgumentResolver
import org.springframework.web.service.invoker.HttpServiceProxyFactory

class CustomHttpServiceArgumentResolver {

	// tag::httpinterface[]
	interface RepositoryService {

		@GetExchange("/repos/search")
		fun searchRepository(search: Search): List<Repository>

	}
	// end::httpinterface[]

	class Sample {
		fun sample() {
			// tag::usage[]
			val restClient = RestClient.builder().baseUrl("https://api.github.com/").build()
			val adapter = RestClientAdapter.create(restClient)
			val factory = HttpServiceProxyFactory
				.builderFor(adapter)
				.customArgumentResolver(SearchQueryArgumentResolver())
				.build()
			val repositoryService = factory.createClient<RepositoryService>(RepositoryService::class.java)

			val search = Search(owner = "spring-projects", language = "java", query = "rest")
			val repositories = repositoryService.searchRepository(search)
			// end::usage[]
			repositories.size
		}
	}

	// tag::argumentresolver[]
	class SearchQueryArgumentResolver : HttpServiceArgumentResolver {
		override fun resolve(
			argument: Any?,
			parameter: MethodParameter,
			requestValues: HttpRequestValues.Builder
		): Boolean {
			if (parameter.getParameterType() == Search::class.java) {
				val search = argument as Search
				requestValues.addRequestParameter("owner", search.owner)
					.addRequestParameter("language", search.language)
					.addRequestParameter("query", search.query)
				return true
			}
			return false
		}
	}
	// end::argumentresolver[]

	data class Search(val query: String, val owner: String, val language: String)

	data class Repository(val name: String)
}