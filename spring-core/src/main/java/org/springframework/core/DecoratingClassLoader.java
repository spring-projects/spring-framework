/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.core;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.util.Assert;

/**
 * Base class for decorating ClassLoaders such as {@link OverridingClassLoader}
 * and {@link org.springframework.instrument.classloading.ShadowingClassLoader},
 * providing common handling of excluded packages and classes.
 * <p>装饰ClassLoaders的基础类,例如OverridingClassLoader和ShadowingClassLoader
 *    提供通用的处理去排除给定包名和类名
 * @author Juergen Hoeller
 * @author Rod Johnson
 * @since 2.5.2
 */
public abstract class DecoratingClassLoader extends ClassLoader {

	static {
		ClassLoader.registerAsParallelCapable();
	}


	private final Set<String> excludedPackages = Collections.newSetFromMap(new ConcurrentHashMap<>(8));

	private final Set<String> excludedClasses = Collections.newSetFromMap(new ConcurrentHashMap<>(8));


	/**
	 * Create a new DecoratingClassLoader with no parent ClassLoader.
	 */
	public DecoratingClassLoader() {
	}

	/**
	 * Create a new DecoratingClassLoader using the given parent ClassLoader
	 * for delegation.
	 * 根据给定的父ClassLoader创建一个新的DecoratingClassLoader
	 */
	public DecoratingClassLoader(ClassLoader parent) {
		super(parent);
	}


	/**
	 * Add a package name to exclude from decoration (e.g. overriding).
	 * <p>Any class whose fully-qualified name starts with the name registered
	 * here will be handled by the parent ClassLoader in the usual fashion.
	 * 
	 * <p> 添加一个包名到移除集合,任何包名在排除集合里面的类都会被父类处理
	 * @param packageName the package name to exclude
	 */
	public void excludePackage(String packageName) {
		Assert.notNull(packageName, "Package name must not be null");
		this.excludedPackages.add(packageName);
	}

	/**
	 * Add a class name to exclude from decoration (e.g. overriding).
	 * <p>Any class name registered here will be handled by the parent
	 * ClassLoader in the usual fashion.
	 * <p>添加一个类名到排除集合
	 * @param className the class name to exclude
	 */
	public void excludeClass(String className) {
		Assert.notNull(className, "Class name must not be null");
		this.excludedClasses.add(className);
	}

	/**
	 * Determine whether the specified class is excluded from decoration
	 * by this class loader.
	 * <p>The default implementation checks against excluded packages and classes.
	 * <p> 判断给定的类是否在排除之外
	 * @param className the class name to check
	 * @return whether the specified class is eligible
	 * @see #excludePackage
	 * @see #excludeClass
	 */
	protected boolean isExcluded(String className) {
		if (this.excludedClasses.contains(className)) {
			return true;
		}
		for (String packageName : this.excludedPackages) {
			if (className.startsWith(packageName)) {
				return true;
			}
		}
		return false;
	}

}
