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
package org.springframework.instrument.classloading.jboss;

import java.lang.instrument.ClassFileTransformer;
import java.lang.reflect.Method;

import org.jboss.classloader.spi.base.BaseClassLoader;

import org.springframework.instrument.classloading.LoadTimeWeaver;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * {@link LoadTimeWeaver} implementation for JBoss5's instrumentable ClassLoader.
 *
 * @author Ales Justin
 */
public class JBoss5LoadTimeWeaver extends ReflectionHelper implements LoadTimeWeaver {

	private JBoss5ClassLoader classLoader;

	public JBoss5LoadTimeWeaver() {
		this(ClassUtils.getDefaultClassLoader());
	}

	public JBoss5LoadTimeWeaver(ClassLoader classLoader) {
		Assert.notNull(classLoader, "ClassLoader must not be null");
		BaseClassLoader bcl = determineClassLoader(classLoader);
		if (bcl == null) {
			throw new IllegalArgumentException(
					classLoader + " and its parents are not suitable ClassLoaders: " + "An [" +
							BaseClassLoader.class.getName() + "] implementation is required.");
		}
		this.classLoader = createClassLoaderWrapper(bcl);
	}

	/**
	 * Create a JBoss5 classloader wrapper based on the underlying JBossAS version.
	 *
	 * @param bcl the base classloader
	 * @return new JBoss5 classloader wrapper
	 */
	protected JBoss5ClassLoader createClassLoaderWrapper(BaseClassLoader bcl) {
		int versionNumber = 0;
		String tag;

		try {
			// BCL should see Version class
			Class<?> versionClass = bcl.loadClass("org.jboss.Version");
			Method getInstance = getMethod(versionClass, "getInstance");
			Object version = getInstance.invoke(null); // static method

			Method getMajor = getMethod(versionClass, "getMajor");
			versionNumber += 100 * invokeMethod(getMajor, version, Integer.class);
			Method getMinor = getMethod(versionClass, "getMinor");
			versionNumber += 10 * invokeMethod(getMinor, version, Integer.class);
			Method getRevision = getMethod(versionClass, "getRevision");
			versionNumber += invokeMethod(getRevision, version, Integer.class);
			Method getTag = getMethod(versionClass, "getTag");
			tag = invokeMethod(getTag, version, String.class);
		}
		catch (Exception e) {
			//log.warn("Exception creating JBoss5 CL wrapper: " + e + ", falling back to JBoss50ClassLoader wrapper.");
			return new JBoss50ClassLoader(bcl);
		}

		if (versionNumber < 500) // this only works on new MC code
		{
			throw new IllegalArgumentException(
					"JBoss5LoadTimeWeaver can only be used on new JBoss Microcontainer ClassLoader.");
		}
		else if (versionNumber <= 501 || (versionNumber == 510 && "Beta1".equals(tag))) {
			return new JBoss50ClassLoader(bcl);
		}
		else {
			return new JBoss51ClassLoader(bcl);
		}
	}

	/**
	 * Find first BaseClassLoader implementation.
	 *
	 * @param classLoader the classloader
	 * @return BaseClassLoader instance or null if not found
	 */
	private BaseClassLoader determineClassLoader(ClassLoader classLoader) {
		for (ClassLoader cl = classLoader; cl != null; cl = cl.getParent()) {
			if (cl instanceof BaseClassLoader) {
				return (BaseClassLoader) cl;
			}
		}
		return null;
	}

	public void addTransformer(ClassFileTransformer transformer) {
		classLoader.addTransformer(transformer);
	}

	public ClassLoader getInstrumentableClassLoader() {
		return classLoader.getInternalClassLoader();
	}

	public ClassLoader getThrowawayClassLoader() {
		return classLoader.getThrowawayClassLoader();
	}
}
