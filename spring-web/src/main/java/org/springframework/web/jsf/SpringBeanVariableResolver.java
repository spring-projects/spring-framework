/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.web.jsf;

import javax.faces.context.FacesContext;
import javax.faces.el.EvaluationException;
import javax.faces.el.VariableResolver;

/**
 * This is a subclass of the JSF 1.1 {@link DelegatingVariableResolver},
 * letting Spring bean definitions override other attributes of the same name.
 *
 * <p>The main purpose of this class is to provide behavior that is analogous
 * to the JSF 1.2 {@link org.springframework.web.jsf.el.SpringBeanFacesELResolver}.
 *
 * @author Juergen Hoeller
 * @since 2.5
 * @see WebApplicationContextVariableResolver
 * @see FacesContextUtils#getRequiredWebApplicationContext
 */
public class SpringBeanVariableResolver extends DelegatingVariableResolver {

	public SpringBeanVariableResolver(VariableResolver originalVariableResolver) {
		super(originalVariableResolver);
	}

	@Override
	public Object resolveVariable(FacesContext facesContext, String name) throws EvaluationException {
		Object bean = resolveSpringBean(facesContext, name);
		if (bean != null) {
			return bean;
		}
		Object value = resolveOriginal(facesContext, name);
		if (value != null) {
			return value;
		}
		return null;
	}

}
