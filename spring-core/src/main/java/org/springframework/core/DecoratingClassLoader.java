/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;

/**
 * Base class for decorating ClassLoaders such as {@link OverridingClassLoader}
 * and {@link org.springframework.instrument.classloading.ShadowingClassLoader},
 * providing common handling of excluded packages and classes.
 *
 * @author Juergen Hoeller
 * @author Rod Johnson
 * @since 2.5.2
 */
public abstract class DecoratingClassLoader extends ClassLoader {

	static {
		ClassLoader.registerAsParallelCapable();
	}


	private final Set<String> excludedPackages = ConcurrentHashMap.newKeySet(8);

	private final Set<String> excludedClasses = ConcurrentHashMap.newKeySet(8);


	/**
	 * Create a new DecoratingClassLoader with no parent ClassLoader.
	 */
	public DecoratingClassLoader() {
	}

	/**
	 * Create a new DecoratingClassLoader using the given parent ClassLoader
	 * for delegation.
	 */
	public DecoratingClassLoader(@Nullable ClassLoader parent) {
		super(parent);
	}


	/**
	 * Add a package name to exclude from decoration (for example, overriding).
	 * <p>Any class whose fully-qualified name starts with the name registered
	 * here will be handled by the parent ClassLoader in the usual fashion.
	 * @param packageName the package name to exclude
	 */
	public void excludePackage(String packageName) {
		Assert.notNull(packageName, "Package name must not be null");
		this.excludedPackages.add(packageName);
	}

	/**
	 * Add a class name to exclude from decoration (for example, overriding).
	 * <p>Any class name registered here will be handled by the parent
	 * ClassLoader in the usual fashion.
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
