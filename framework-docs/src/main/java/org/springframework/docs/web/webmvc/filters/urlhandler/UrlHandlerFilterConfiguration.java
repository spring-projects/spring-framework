/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.web.webmvc.filters.urlhandler;

import org.springframework.http.HttpStatus;
import org.springframework.web.filter.UrlHandlerFilter;

public class UrlHandlerFilterConfiguration {

	public void configureUrlHandlerFilter() {
		// tag::config[]
		UrlHandlerFilter urlHandlerFilter = UrlHandlerFilter
				// will HTTP 308 redirect "/blog/my-blog-post/" -> "/blog/my-blog-post"
				.trailingSlashHandler("/blog/**").redirect(HttpStatus.PERMANENT_REDIRECT)
				// will wrap the request to "/admin/user/account/" and make it as "/admin/user/account"
				.trailingSlashHandler("/admin/**").wrapRequest()
				.build();
		// end::config[]
	}
}
