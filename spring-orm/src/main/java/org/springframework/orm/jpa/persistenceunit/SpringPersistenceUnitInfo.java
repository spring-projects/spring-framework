/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.orm.jpa.persistenceunit;

import jakarta.persistence.spi.ClassTransformer;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.core.DecoratingClassLoader;
import org.springframework.instrument.classloading.LoadTimeWeaver;
import org.springframework.instrument.classloading.SimpleThrowawayClassLoader;
import org.springframework.util.Assert;

/**
 * Subclass of {@link MutablePersistenceUnitInfo} that adds instrumentation hooks based on
 * Spring's {@link org.springframework.instrument.classloading.LoadTimeWeaver} abstraction.
 *
 * <p>This class is restricted to package visibility, in contrast to its superclass.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Costin Leau
 * @since 2.0
 * @see PersistenceUnitManager
 */
class SpringPersistenceUnitInfo extends MutablePersistenceUnitInfo {

	private @Nullable LoadTimeWeaver loadTimeWeaver;

	private @Nullable ClassLoader classLoader;


	/**
	 * Initialize this PersistenceUnitInfo with the LoadTimeWeaver SPI interface
	 * used by Spring to add instrumentation to the current class loader.
	 */
	public void init(LoadTimeWeaver loadTimeWeaver) {
		Assert.notNull(loadTimeWeaver, "LoadTimeWeaver must not be null");
		this.loadTimeWeaver = loadTimeWeaver;
		this.classLoader = loadTimeWeaver.getInstrumentableClassLoader();
	}

	/**
	 * Initialize this PersistenceUnitInfo with the current class loader
	 * (instead of with a LoadTimeWeaver).
	 */
	public void init(@Nullable ClassLoader classLoader) {
		this.classLoader = classLoader;
	}


	/**
	 * This implementation returns the LoadTimeWeaver's instrumentable ClassLoader,
	 * if specified.
	 */
	@Override
	public @Nullable ClassLoader getClassLoader() {
		return this.classLoader;
	}

	/**
	 * This implementation delegates to the LoadTimeWeaver, if specified.
	 */
	@Override
	public void addTransformer(ClassTransformer classTransformer) {
		if (this.loadTimeWeaver != null) {
			this.loadTimeWeaver.addTransformer(new ClassFileTransformerAdapter(classTransformer));
		}
		else {
			LogFactory.getLog(getClass()).info("No LoadTimeWeaver setup: ignoring JPA class transformer");
		}
	}

	/**
	 * This implementation delegates to the LoadTimeWeaver, if specified.
	 */
	@Override
	public ClassLoader getNewTempClassLoader() {
		ClassLoader tcl = (this.loadTimeWeaver != null ? this.loadTimeWeaver.getThrowawayClassLoader() :
				new SimpleThrowawayClassLoader(this.classLoader));
		String packageToExclude = getPersistenceProviderPackageName();
		if (packageToExclude != null && tcl instanceof DecoratingClassLoader dcl) {
			dcl.excludePackage(packageToExclude);
		}
		return tcl;
	}

}
