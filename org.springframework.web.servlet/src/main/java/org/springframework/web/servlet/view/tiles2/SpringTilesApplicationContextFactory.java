/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.web.servlet.view.tiles2;

import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.servlet.ServletContext;

import org.apache.tiles.Initializable;
import org.apache.tiles.TilesApplicationContext;
import org.apache.tiles.context.AbstractTilesApplicationContextFactory;
import org.apache.tiles.servlet.context.wildcard.WildcardServletTilesApplicationContext;

/**
 * Spring-specific subclass of the standard Tiles AbstractTilesApplicationContextFactory,
 * passing given properties through as Tiles init-param map.
 *
 * @author Juergen Hoeller
 * @since 3.0
 * @see TilesConfigurer#setTilesProperties
 */
public class SpringTilesApplicationContextFactory extends AbstractTilesApplicationContextFactory
		implements Initializable {

	private Map<String, String> params;

	public void init(Map<String, String> params) {
		this.params = params;
	}

	public TilesApplicationContext createApplicationContext(Object context) {
		return new SpringWildcardServletTilesApplicationContext((ServletContext) context, this.params);
	}


	/**
	 * Custom subclass of the standard Tiles WildcardServletTilesApplicationContext,
	 * passing given properties through as Tiles init-param map.
	 */
	private static class SpringWildcardServletTilesApplicationContext extends WildcardServletTilesApplicationContext {

		private final Map<String, String> mergedInitParams;

		public SpringWildcardServletTilesApplicationContext(ServletContext servletContext, Map<String, String> params) {
			super(servletContext);
			this.mergedInitParams = new LinkedHashMap<String, String>();
			Enumeration initParamNames = servletContext.getInitParameterNames();
			while (initParamNames.hasMoreElements()) {
				String initParamName = (String) initParamNames.nextElement();
				this.mergedInitParams.put(initParamName, servletContext.getInitParameter(initParamName));
			}
			if (params != null) {
				this.mergedInitParams.putAll(params);
			}
		}

		@Override
		public Map<String, String> getInitParams() {
			return this.mergedInitParams;
		}
	}

}
