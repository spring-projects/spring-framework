/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.web.servlet.config.annotation;

import org.springframework.web.servlet.view.InternalResourceViewResolver;

/**
 * Encapsulates information required to create an {@link InternalResourceViewResolver} bean.
 * Default configuration is "/WEB-INF/" prefix and ".jsp" suffix.
 *
 * @author Sebastien Deleuze
 * @since 4.1
 */
public class JspRegistration extends ViewResolutionRegistration<InternalResourceViewResolver> {

	public JspRegistration(ViewResolutionRegistry registry) {
		this(registry, "/WEB-INF/", ".jsp");
	}

	public JspRegistration(ViewResolutionRegistry registry, String prefix, String suffix) {
		super(registry, new InternalResourceViewResolver());
		this.viewResolver.setPrefix(prefix);
		this.viewResolver.setSuffix(suffix);
	}

}
