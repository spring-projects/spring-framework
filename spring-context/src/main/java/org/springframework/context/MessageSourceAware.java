/*
 * Copyright 2002-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.context;

import org.springframework.beans.factory.Aware;

/**
 * Interface to be implemented by any object that wishes to be notified of the
 * {@link MessageSource} (typically the ApplicationContext) that it runs in.
 *
 * <p>Note that the {@code MessageSource} can usually also be passed in as a bean
 * reference (via arbitrary bean properties or constructor arguments), because
 * it is defined as a bean with name {@code "messageSource"} in the application
 * context.
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 1.1.1
 * @see ApplicationContextAware
 */
public interface MessageSourceAware extends Aware {

	/**
	 * Set the {@link MessageSource} that this object runs in.
	 * <p>Invoked after population of normal bean properties but before an init
	 * callback like InitializingBean's afterPropertiesSet or a custom init-method.
	 * Invoked before ApplicationContextAware's setApplicationContext.
	 * @param messageSource message source to be used by this object
	 */
	void setMessageSource(MessageSource messageSource);

}
