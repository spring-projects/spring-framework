/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.aop.framework;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.cglib.proxy.Callback;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.Factory;
import org.springframework.objenesis.Objenesis;
import org.springframework.objenesis.ObjenesisException;
import org.springframework.objenesis.ObjenesisStd;
import org.springframework.objenesis.SpringObjenesis;

/**
 * Objenesis-based extension of {@link CglibAopProxy} to create proxy instances
 * without invoking the constructor of the class.
 *
 * @author Oliver Gierke
 * @author Juergen Hoeller
 * @since 4.0
 */
@SuppressWarnings("serial")
class ObjenesisCglibAopProxy extends CglibAopProxy {

	private static final Log logger = LogFactory.getLog(ObjenesisCglibAopProxy.class);

	private static final Objenesis cachedObjenesis = new SpringObjenesis();

	private static final Objenesis nonCachedObjenesis = new ObjenesisStd(false);


	/**
	 * Create a new ObjenesisCglibAopProxy for the given AOP configuration.
	 * @param config the AOP configuration as AdvisedSupport object
	 */
	public ObjenesisCglibAopProxy(AdvisedSupport config) {
		super(config);
	}


	@Override
	@SuppressWarnings("unchecked")
	protected Object createProxyClassAndInstance(Enhancer enhancer, Callback[] callbacks) {
		try {
			Objenesis objenesis = (enhancer.getUseCache() ? cachedObjenesis : nonCachedObjenesis);
			Factory factory = (Factory) objenesis.newInstance(enhancer.createClass());
			factory.setCallbacks(callbacks);
			return factory;
		}
		catch (ObjenesisException ex) {
			// Fallback to regular proxy construction on unsupported JVMs
			logger.debug("Unable to instantiate proxy using Objenesis, falling back to regular proxy construction", ex);
			return super.createProxyClassAndInstance(enhancer, callbacks);
		}
	}

}
