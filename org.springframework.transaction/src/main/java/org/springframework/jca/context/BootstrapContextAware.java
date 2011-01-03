/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.jca.context;

import javax.resource.spi.BootstrapContext;

import org.springframework.beans.factory.Aware;

/**
 * Interface to be implemented by any object that wishes to be
 * notified of the BootstrapContext (typically determined by the
 * {@link ResourceAdapterApplicationContext}) that it runs in.
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 2.5
 * @see javax.resource.spi.BootstrapContext
 */
public interface BootstrapContextAware extends Aware {

	/**
	 * Set the BootstrapContext that this object runs in.
	 * <p>Invoked after population of normal bean properties but before an init
	 * callback like InitializingBean's <code>afterPropertiesSet</code> or a
	 * custom init-method. Invoked after ApplicationContextAware's
	 * <code>setApplicationContext</code>.
	 * @param bootstrapContext BootstrapContext object to be used by this object
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet
	 * @see org.springframework.context.ApplicationContextAware#setApplicationContext
	 */
	void setBootstrapContext(BootstrapContext bootstrapContext);

}
