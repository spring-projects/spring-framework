/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.web.webflux.filters.urlhandler

import org.springframework.http.HttpStatus
import org.springframework.web.filter.reactive.UrlHandlerFilter

class UrlHandlerFilterConfiguration {

	@Suppress("UNUSED_VARIABLE")
	fun configureUrlHandlerFilter() {
		// tag::config[]
		val urlHandlerFilter = UrlHandlerFilter
			// will HTTP 308 redirect "/blog/my-blog-post/" -> "/blog/my-blog-post"
			.trailingSlashHandler("/blog/**").redirect(HttpStatus.PERMANENT_REDIRECT)
			// will mutate the request to "/admin/user/account/" and make it as "/admin/user/account"
			.trailingSlashHandler("/admin/**").mutateRequest()
			.build()
		// end::config[]
	}
}