/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import org.springframework.objenesis.ObjenesisException;
import org.springframework.objenesis.ObjenesisStd;

/**
 * Objenesis based extension of {@link CglibAopProxy} to create proxy instances without
 * invoking the constructor of the class.
 *
 * @author Oliver Gierke
 * @since 4.0
 */
class ObjenesisCglibAopProxy extends CglibAopProxy {

	private static final Log logger = LogFactory.getLog(ObjenesisCglibAopProxy.class);

	private final ObjenesisStd objenesis;


	/**
	 * Creates a new {@link ObjenesisCglibAopProxy} using the given {@link AdvisedSupport}.
	 * @param config must not be {@literal null}.
	 */
	public ObjenesisCglibAopProxy(AdvisedSupport config) {
		super(config);
		this.objenesis = new ObjenesisStd(true);
	}


	@Override
	@SuppressWarnings("unchecked")
	protected Object createProxyClassAndInstance(Enhancer enhancer, Callback[] callbacks) {
		try {
			Factory factory = (Factory) objenesis.newInstance(enhancer.createClass());
			factory.setCallbacks(callbacks);
			return factory;
		}
		catch (ObjenesisException ex) {
			// Fallback to Cglib on unsupported JVMs
			if (logger.isDebugEnabled()) {
				logger.debug("Unable to instantiate proxy using Objenesis, falling back "
						+ "to regular proxy construction", ex);
			}
			return super.createProxyClassAndInstance(enhancer, callbacks);
		}
	}

}
