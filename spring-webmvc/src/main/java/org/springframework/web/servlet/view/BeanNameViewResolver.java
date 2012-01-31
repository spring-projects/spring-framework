/*
 * Copyright 2002-2006 the original author or authors.
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

package org.springframework.web.servlet.view;

import java.util.Locale;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.web.context.support.WebApplicationObjectSupport;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;

/**
 * Simple implementation of ViewResolver that interprets a view name
 * as bean name in the current application context, i.e. in the XML
 * file of the executing DispatcherServlet.
 *
 * <p>This resolver can be handy for small applications, keeping all
 * definitions ranging from controllers to views in the same place.
 * For normal applications, XmlViewResolver will be the better choice, as
 * it separates the XML view bean definitions into a dedicated views file.
 * View beans should virtually never have references to any other
 * application beans - such a separation will make this clear.
 *
 * <p>This ViewResolver does not support internationalization.
 * Conside ResourceBundleViewResolver if you need to apply different
 * view resources per locale.
 *
 * <p>Note: This ViewResolver implements the Ordered interface to allow for
 * flexible participation in ViewResolver chaining. For example, some special
 * views could be defined via this ViewResolver (giving it 0 as "order" value),
 * while all remaining views could be resolved by a UrlBasedViewResolver.
 *
 * @author Juergen Hoeller
 * @since 18.06.2003
 * @see XmlViewResolver
 * @see ResourceBundleViewResolver
 * @see UrlBasedViewResolver
 */
public class BeanNameViewResolver extends WebApplicationObjectSupport implements ViewResolver, Ordered {

	private int order = Integer.MAX_VALUE;  // default: same as non-Ordered


	public void setOrder(int order) {
		this.order = order;
	}

	public int getOrder() {
		return order;
	}


	public View resolveViewName(String viewName, Locale locale) throws BeansException {
		ApplicationContext context = getApplicationContext();
		if (!context.containsBean(viewName)) {
			// Allow for ViewResolver chaining.
			return null;
		}
		return (View) context.getBean(viewName, View.class);
	}

}
