/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.integration.resthttpserviceclient.customresolver

import org.springframework.core.MethodParameter
import org.springframework.web.client.RestClient
import org.springframework.web.client.support.RestClientAdapter
import org.springframework.web.service.annotation.GetExchange
import org.springframework.web.service.invoker.HttpRequestValues
import org.springframework.web.service.invoker.HttpServiceArgumentResolver
import org.springframework.web.service.invoker.HttpServiceProxyFactory

class CustomHttpServiceArgumentResolver {

	// tag::httpserviceclient[]
	interface RepositoryService {

		@GetExchange("/repos/search")
		fun searchRepository(search: Search): List<Repository>

	}
	// end::httpserviceclient[]

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