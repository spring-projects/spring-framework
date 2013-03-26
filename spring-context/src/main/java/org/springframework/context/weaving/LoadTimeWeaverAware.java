/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.context.weaving;

import org.springframework.beans.factory.Aware;
import org.springframework.instrument.classloading.LoadTimeWeaver;

/**
 * Interface to be implemented by any object that wishes to be notified
 * of the application context's default {@link LoadTimeWeaver}.
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 2.5
 * @see org.springframework.context.ConfigurableApplicationContext#LOAD_TIME_WEAVER_BEAN_NAME
 */
public interface LoadTimeWeaverAware extends Aware {

	/**
	 * Set the {@link LoadTimeWeaver} of this object's containing
	 * {@link org.springframework.context.ApplicationContext ApplicationContext}.
	 * <p>Invoked after the population of normal bean properties but before an
	 * initialization callback like
	 * {@link org.springframework.beans.factory.InitializingBean InitializingBean's}
	 * {@link org.springframework.beans.factory.InitializingBean#afterPropertiesSet() afterPropertiesSet()}
	 * or a custom init-method. Invoked after
	 * {@link org.springframework.context.ApplicationContextAware ApplicationContextAware's}
	 * {@link org.springframework.context.ApplicationContextAware#setApplicationContext setApplicationContext(..)}.
	 * <p><b>NOTE:</b> This method will only be called if there actually is a
	 * {@code LoadTimeWeaver} available in the application context. If
	 * there is none, the method will simply not get invoked, assuming that the
	 * implementing object is able to activate its weaving dependency accordingly.
	 * @param loadTimeWeaver the {@code LoadTimeWeaver} instance (never {@code null})
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet
	 * @see org.springframework.context.ApplicationContextAware#setApplicationContext
	 */
	void setLoadTimeWeaver(LoadTimeWeaver loadTimeWeaver);

}
