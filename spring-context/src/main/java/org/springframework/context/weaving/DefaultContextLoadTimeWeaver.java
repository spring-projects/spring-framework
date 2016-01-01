/*
 * Copyright 2002-2013 the original author or authors.
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

import java.lang.instrument.ClassFileTransformer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.instrument.InstrumentationSavingAgent;
import org.springframework.instrument.classloading.InstrumentationLoadTimeWeaver;
import org.springframework.instrument.classloading.LoadTimeWeaver;
import org.springframework.instrument.classloading.ReflectiveLoadTimeWeaver;
import org.springframework.instrument.classloading.glassfish.GlassFishLoadTimeWeaver;
import org.springframework.instrument.classloading.jboss.JBossLoadTimeWeaver;
import org.springframework.instrument.classloading.tomcat.TomcatLoadTimeWeaver;
import org.springframework.instrument.classloading.weblogic.WebLogicLoadTimeWeaver;
import org.springframework.instrument.classloading.websphere.WebSphereLoadTimeWeaver;

/**
 * Default {@link LoadTimeWeaver} bean for use in an application context,
 * decorating an automatically detected internal {@code LoadTimeWeaver}.
 *
 * <p>Typically registered for the default bean name
 * "{@code loadTimeWeaver}"; the most convenient way to achieve this is
 * Spring's {@code <context:load-time-weaver>} XML tag.
 *
 * <p>This class implements a runtime environment check for obtaining the
 * appropriate weaver implementation: As of Spring 4.0, it detects Oracle WebLogic 10,
 * GlassFish 3, Tomcat 6, 7 and 8, JBoss AS 5, 6 and 7, IBM WebSphere 7 and 8,
 * {@link InstrumentationSavingAgent Spring's VM agent}, and any {@link ClassLoader}
 * supported by Spring's {@link ReflectiveLoadTimeWeaver}.
 *
 * @author Juergen Hoeller
 * @author Ramnivas Laddad
 * @author Costin Leau
 * @since 2.5
 * @see org.springframework.context.ConfigurableApplicationContext#LOAD_TIME_WEAVER_BEAN_NAME
 */
public class DefaultContextLoadTimeWeaver implements LoadTimeWeaver, BeanClassLoaderAware, DisposableBean {

	protected final Log logger = LogFactory.getLog(getClass());

	private LoadTimeWeaver loadTimeWeaver;


	public DefaultContextLoadTimeWeaver() {
	}

	public DefaultContextLoadTimeWeaver(ClassLoader beanClassLoader) {
		setBeanClassLoader(beanClassLoader);
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		LoadTimeWeaver serverSpecificLoadTimeWeaver = createServerSpecificLoadTimeWeaver(classLoader);
		if (serverSpecificLoadTimeWeaver != null) {
			if (logger.isInfoEnabled()) {
				logger.info("Determined server-specific load-time weaver: " +
						serverSpecificLoadTimeWeaver.getClass().getName());
			}
			this.loadTimeWeaver = serverSpecificLoadTimeWeaver;
		}
		else if (InstrumentationLoadTimeWeaver.isInstrumentationAvailable()) {
			logger.info("Found Spring's JVM agent for instrumentation");
			this.loadTimeWeaver = new InstrumentationLoadTimeWeaver(classLoader);
		}
		else {
			try {
				this.loadTimeWeaver = new ReflectiveLoadTimeWeaver(classLoader);
				logger.info("Using a reflective load-time weaver for class loader: " +
						this.loadTimeWeaver.getInstrumentableClassLoader().getClass().getName());
			}
			catch (IllegalStateException ex) {
				throw new IllegalStateException(ex.getMessage() + " Specify a custom LoadTimeWeaver or start your " +
						"Java virtual machine with Spring's agent: -javaagent:org.springframework.instrument.jar");
			}
		}
	}

	/*
	 * This method never fails, allowing to try other possible ways to use an
	 * server-agnostic weaver. This non-failure logic is required since
	 * determining a load-time weaver based on the ClassLoader name alone may
	 * legitimately fail due to other mismatches. Specific case in point: the
	 * use of WebLogicLoadTimeWeaver works for WLS 10 but fails due to the lack
	 * of a specific method (addInstanceClassPreProcessor) for any earlier
	 * versions even though the ClassLoader name is the same.
	 */
	protected LoadTimeWeaver createServerSpecificLoadTimeWeaver(ClassLoader classLoader) {
		String name = classLoader.getClass().getName();
		try {
			if (name.startsWith("weblogic")) {
				return new WebLogicLoadTimeWeaver(classLoader);
			}
			else if (name.startsWith("org.glassfish")) {
				return new GlassFishLoadTimeWeaver(classLoader);
			}
			else if (name.startsWith("org.apache.catalina")) {
				return new TomcatLoadTimeWeaver(classLoader);
			}
			else if (name.startsWith("org.jboss")) {
				return new JBossLoadTimeWeaver(classLoader);
			}
			else if (name.startsWith("com.ibm")) {
				return new WebSphereLoadTimeWeaver(classLoader);
			}
		}
		catch (IllegalStateException ex) {
			logger.info("Could not obtain server-specific LoadTimeWeaver: " + ex.getMessage());
		}
		return null;
	}

	@Override
	public void destroy() {
		if (this.loadTimeWeaver instanceof InstrumentationLoadTimeWeaver) {
			logger.info("Removing all registered transformers for class loader: " +
					this.loadTimeWeaver.getInstrumentableClassLoader().getClass().getName());
			((InstrumentationLoadTimeWeaver) this.loadTimeWeaver).removeTransformers();
		}
	}


	@Override
	public void addTransformer(ClassFileTransformer transformer) {
		this.loadTimeWeaver.addTransformer(transformer);
	}

	@Override
	public ClassLoader getInstrumentableClassLoader() {
		return this.loadTimeWeaver.getInstrumentableClassLoader();
	}

	@Override
	public ClassLoader getThrowawayClassLoader() {
		return this.loadTimeWeaver.getThrowawayClassLoader();
	}

}
