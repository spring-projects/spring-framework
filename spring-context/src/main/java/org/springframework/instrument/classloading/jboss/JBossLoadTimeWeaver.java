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

package org.springframework.instrument.classloading.jboss;

import java.lang.instrument.ClassFileTransformer;

import org.springframework.instrument.classloading.LoadTimeWeaver;
import org.springframework.instrument.classloading.SimpleThrowawayClassLoader;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * {@link LoadTimeWeaver} implementation for JBoss's instrumentable ClassLoader.
 * Autodetects the specific JBoss version at runtime: currently supports
 * JBoss AS 6 and 7, as well as WildFly 8 (as of Spring 4.0).
 *
 * <p><b>NOTE:</b> On JBoss 6, to avoid the container loading the classes before the
 * application actually starts, one needs to add a <tt>WEB-INF/jboss-scanning.xml</tt>
 * file to the application archive - with the following content:
 * <pre class="code">&lt;scanning xmlns="urn:jboss:scanning:1.0"/&gt;</pre>
 *
 * <p>Thanks to Ales Justin and Marius Bogoevici for the initial prototype.
 *
 * @author Costin Leau
 * @author Juergen Hoeller
 * @since 3.0
 */
public class JBossLoadTimeWeaver implements LoadTimeWeaver {

	private final JBossClassLoaderAdapter adapter;


	/**
	 * Create a new instance of the {@link JBossLoadTimeWeaver} class using
	 * the default {@link ClassLoader class loader}.
	 * @see org.springframework.util.ClassUtils#getDefaultClassLoader()
	 */
	public JBossLoadTimeWeaver() {
		this(ClassUtils.getDefaultClassLoader());
	}

	/**
	 * Create a new instance of the {@link JBossLoadTimeWeaver} class using
	 * the supplied {@link ClassLoader}.
	 * @param classLoader the {@code ClassLoader} to delegate to for weaving
	 * (must not be {@code null})
	 */
	public JBossLoadTimeWeaver(ClassLoader classLoader) {
		Assert.notNull(classLoader, "ClassLoader must not be null");
		if (classLoader.getClass().getName().startsWith("org.jboss.modules")) {
			// JBoss AS 7 or WildFly 8
			this.adapter = new JBossModulesAdapter(classLoader);
		}
		else {
			// JBoss AS 6
			this.adapter = new JBossMCAdapter(classLoader);
		}
	}


	@Override
	public void addTransformer(ClassFileTransformer transformer) {
		this.adapter.addTransformer(transformer);
	}

	@Override
	public ClassLoader getInstrumentableClassLoader() {
		return this.adapter.getInstrumentableClassLoader();
	}

	@Override
	public ClassLoader getThrowawayClassLoader() {
		return new SimpleThrowawayClassLoader(getInstrumentableClassLoader());
	}

}
