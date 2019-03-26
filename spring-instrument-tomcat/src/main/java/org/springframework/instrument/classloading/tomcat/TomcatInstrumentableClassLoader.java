/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.instrument.classloading.tomcat;

import java.lang.instrument.ClassFileTransformer;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.apache.catalina.loader.ResourceEntry;
import org.apache.catalina.loader.WebappClassLoader;

import org.springframework.instrument.classloading.WeavingTransformer;

/**
 * Extension of Tomcat's default class loader which adds instrumentation
 * to loaded classes without the need to use a VM-wide agent.
 *
 * <p>To be registered using a
 * <a href="https://tomcat.apache.org/tomcat-6.0-doc/config/loader.html">{@code Loader}</a> tag
 * in Tomcat's <a href="https://tomcat.apache.org/tomcat-6.0-doc/config/context.html">{@code Context}</a>
 * definition in the {@code server.xml} file, with the Spring-provided "spring-instrument-tomcat.jar"
 * file deployed into Tomcat's "lib" directory. The required configuration tag looks as follows:
 *
 * <pre class="code">&lt;Loader loaderClass="org.springframework.instrument.classloading.tomcat.TomcatInstrumentableClassLoader"/&gt;</pre>
 *
 * <p>Typically used in combination with a
 * {@link org.springframework.instrument.classloading.ReflectiveLoadTimeWeaver}
 * defined in the Spring application context. The {@code addTransformer} and
 * {@code getThrowawayClassLoader} methods mirror the corresponding methods
 * in the LoadTimeWeaver interface, as expected by ReflectiveLoadTimeWeaver.
 *
 * <p><b>NOTE:</b> Requires Apache Tomcat version 6.0 or higher, as of Spring 4.0.
 * This class is not intended to work on Tomcat 8.0+; please rely on Tomcat's own
 * {@code InstrumentableClassLoader} facility instead, as autodetected by Spring's
 * {@link org.springframework.instrument.classloading.tomcat.TomcatLoadTimeWeaver}.
 *
 * @author Costin Leau
 * @author Juergen Hoeller
 * @since 2.0
 * @see #addTransformer
 * @see #getThrowawayClassLoader
 * @see org.springframework.instrument.classloading.ReflectiveLoadTimeWeaver
 * @see org.springframework.instrument.classloading.tomcat.TomcatLoadTimeWeaver
 */
public class TomcatInstrumentableClassLoader extends WebappClassLoader {

	private static final String CLASS_SUFFIX = ".class";

	/** Use an internal WeavingTransformer */
	private final WeavingTransformer weavingTransformer;


	/**
	 * Create a new {@code TomcatInstrumentableClassLoader} using the
	 * current context class loader.
	 * @see #TomcatInstrumentableClassLoader(ClassLoader)
	 */
	public TomcatInstrumentableClassLoader() {
		super();
		this.weavingTransformer = new WeavingTransformer(this);
	}

	/**
	 * Create a new {@code TomcatInstrumentableClassLoader} with the
	 * supplied class loader as parent.
	 * @param parent the parent {@link ClassLoader} to be used
	 */
	public TomcatInstrumentableClassLoader(ClassLoader parent) {
		super(parent);
		this.weavingTransformer = new WeavingTransformer(this);
	}


	/**
	 * Delegate for LoadTimeWeaver's {@code addTransformer} method.
	 * Typically called through ReflectiveLoadTimeWeaver.
	 * @see org.springframework.instrument.classloading.LoadTimeWeaver#addTransformer
	 * @see org.springframework.instrument.classloading.ReflectiveLoadTimeWeaver
	 */
	public void addTransformer(ClassFileTransformer transformer) {
		this.weavingTransformer.addTransformer(transformer);
	}

	/**
	 * Delegate for LoadTimeWeaver's {@code getThrowawayClassLoader} method.
	 * Typically called through ReflectiveLoadTimeWeaver.
	 * @see org.springframework.instrument.classloading.LoadTimeWeaver#getThrowawayClassLoader
	 * @see org.springframework.instrument.classloading.ReflectiveLoadTimeWeaver
	 */
	public ClassLoader getThrowawayClassLoader() {
		WebappClassLoader tempLoader = new WebappClassLoader();
		// Use reflection to copy all the fields since they are not exposed any other way.
		shallowCopyFieldState(this, tempLoader);
		return tempLoader;
	}


	@Override  // overriding the pre-7.0.63 variant of findResourceInternal
	protected ResourceEntry findResourceInternal(String name, String path) {
		ResourceEntry entry = super.findResourceInternal(name, path);
		if (entry != null && entry.binaryContent != null && path.endsWith(CLASS_SUFFIX)) {
			String className = (name.endsWith(CLASS_SUFFIX) ? name.substring(0, name.length() - CLASS_SUFFIX.length()) : name);
			entry.binaryContent = this.weavingTransformer.transformIfNecessary(className, entry.binaryContent);
		}
		return entry;
	}

	@Override  // overriding the 7.0.63+ variant of findResourceInternal
	protected ResourceEntry findResourceInternal(String name, String path, boolean manifestRequired) {
		ResourceEntry entry = super.findResourceInternal(name, path, manifestRequired);
		if (entry != null && entry.binaryContent != null && path.endsWith(CLASS_SUFFIX)) {
			String className = (name.endsWith(CLASS_SUFFIX) ? name.substring(0, name.length() - CLASS_SUFFIX.length()) : name);
			entry.binaryContent = this.weavingTransformer.transformIfNecessary(className, entry.binaryContent);
		}
		return entry;
	}

	@Override
	public String toString() {
		return getClass().getName() + "\r\n" + super.toString();
	}


	// The code below is originally taken from ReflectionUtils and optimized for
	// local usage. There is no dependency on ReflectionUtils to keep this class
	// self-contained (since it gets deployed into Tomcat's server class loader).
	private static void shallowCopyFieldState(final WebappClassLoader src, final WebappClassLoader dest) {
		Class<?> targetClass = WebappClassLoader.class;
		// Keep backing up the inheritance hierarchy.
		do {
			Field[] fields = targetClass.getDeclaredFields();
			for (Field field : fields) {
				// Do not copy resourceEntries - it's a cache that holds class entries.
				if (!(Modifier.isStatic(field.getModifiers()) || Modifier.isFinal(field.getModifiers()) ||
						field.getName().equals("resourceEntries"))) {
					try {
						field.setAccessible(true);
						Object srcValue = field.get(src);
						field.set(dest, srcValue);
					}
					catch (IllegalAccessException ex) {
						throw new IllegalStateException(
								"Shouldn't be illegal to access field '" + field.getName() + "': " + ex);
					}
				}
			}
			targetClass = targetClass.getSuperclass();
		}
		while (targetClass != null && targetClass != Object.class);
	}

}
