/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.support;

import org.springframework.core.style.DefaultToStringStyler;
import org.springframework.core.style.SimpleValueStyler;
import org.springframework.core.style.ToStringCreator;
import org.springframework.test.context.BootstrapContext;
import org.springframework.test.context.CacheAwareContextLoaderDelegate;
import org.springframework.util.Assert;

/**
 * Default implementation of the {@link BootstrapContext} interface.
 *
 * @author Sam Brannen
 * @since 4.1
 */
public class DefaultBootstrapContext implements BootstrapContext {

	private final Class<?> testClass;
	private final CacheAwareContextLoaderDelegate cacheAwareContextLoaderDelegate;


	/**
	 * Construct a new {@code DefaultBootstrapContext} from the supplied arguments.
	 * @param testClass the test class for this bootstrap context; never {@code null}
	 * @param cacheAwareContextLoaderDelegate the context loader delegate to use for
	 * transparent interaction with the {@code ContextCache}; never {@code null}
	 */
	public DefaultBootstrapContext(Class<?> testClass, CacheAwareContextLoaderDelegate cacheAwareContextLoaderDelegate) {
		Assert.notNull(testClass, "Test class must not be null");
		Assert.notNull(cacheAwareContextLoaderDelegate, "CacheAwareContextLoaderDelegate must not be null");
		this.testClass = testClass;
		this.cacheAwareContextLoaderDelegate = cacheAwareContextLoaderDelegate;
	}

	@Override
	public Class<?> getTestClass() {
		return this.testClass;
	}

	@Override
	public CacheAwareContextLoaderDelegate getCacheAwareContextLoaderDelegate() {
		return this.cacheAwareContextLoaderDelegate;
	}

	/**
	 * Provide a String representation of this bootstrap context's state.
	 */
	@Override
	public String toString() {
		return new ToStringCreator(this, new DefaultToStringStyler(new SimpleValueStyler()))
			.append("testClass", this.testClass)
			.append("cacheAwareContextLoaderDelegate", this.cacheAwareContextLoaderDelegate.getClass())
			.toString();
	}

}
