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

package org.springframework.beans.factory.config;

import org.springframework.beans.BeanMetadataElement;

/**
 * Interface that exposes a reference to a bean name in an abstract fashion.
 * This interface does not necessarily imply a reference to an actual bean
 * instance; it just expresses a logical reference to the name of a bean.
 *
 * <p>Serves as common interface implemented by any kind of bean reference
 * holder, such as {@link RuntimeBeanReference RuntimeBeanReference} and
 * {@link RuntimeBeanNameReference RuntimeBeanNameReference}.
 *
 * @author Juergen Hoeller
 * @since 2.0
 */
public interface BeanReference extends BeanMetadataElement {

	/**
	 * Return the target bean name that this reference points to (never {@code null}).
	 */
	String getBeanName();

}
