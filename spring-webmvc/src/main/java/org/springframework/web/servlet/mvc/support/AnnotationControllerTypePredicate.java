/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.web.servlet.mvc.support;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Controller;

/**
 * Extension of {@link ControllerTypePredicate} that detects
 * annotated {@code @Controller} beans as well.
 *
 * @author Juergen Hoeller
 * @since 2.5.3
 */
class AnnotationControllerTypePredicate extends ControllerTypePredicate {

	@Override
	public boolean isControllerType(Class beanClass) {
		return (super.isControllerType(beanClass) ||
				AnnotationUtils.findAnnotation(beanClass, Controller.class) != null);
	}

	@Override
	public boolean isMultiActionControllerType(Class beanClass) {
		return (super.isMultiActionControllerType(beanClass) ||
				AnnotationUtils.findAnnotation(beanClass, Controller.class) != null);
	}

}
