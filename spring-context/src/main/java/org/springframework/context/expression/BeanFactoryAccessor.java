/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.context.expression;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypedValue;

/**
 * EL property accessor that knows how to traverse the beans of a
 * Spring {@link org.springframework.beans.factory.BeanFactory}.
 *
 * @author Juergen Hoeller
 * @author Andy Clement
 * @since 3.0
 */
public class BeanFactoryAccessor implements PropertyAccessor {

	@Override
	public boolean canRead(EvaluationContext context, Object target, String name) throws AccessException {
		return (((BeanFactory) target).containsBean(name));
	}

	@Override
	public TypedValue read(EvaluationContext context, Object target, String name) throws AccessException {
		return new TypedValue(((BeanFactory) target).getBean(name));
	}

	@Override
	public boolean canWrite(EvaluationContext context, Object target, String name) throws AccessException {
		return false;
	}

	@Override
	public void write(EvaluationContext context, Object target, String name, Object newValue) throws AccessException {
		throw new AccessException("Beans in a BeanFactory are read-only");
	}

	@Override
	public Class[] getSpecificTargetClasses() {
		return new Class[] {BeanFactory.class};
	}

}
