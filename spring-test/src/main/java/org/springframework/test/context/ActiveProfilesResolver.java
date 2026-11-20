/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context;

/**
 * Strategy interface for programmatically resolving which <em>active bean
 * definition profiles</em> should be used when loading an
 * {@link org.springframework.context.ApplicationContext ApplicationContext}
 * for a test class.
 *
 * <p>A custom {@code ActiveProfilesResolver} can be registered via the
 * {@link ActiveProfiles#resolver resolver} attribute of {@code @ActiveProfiles}.
 *
 * <p>Concrete implementations must provide a {@code public} no-args constructor.
 *
 * @author Sam Brannen
 * @author Michail Nikolaev
 * @since 4.0
 * @see ActiveProfiles @ActiveProfiles
 * @see org.springframework.test.context.support.DefaultActiveProfilesResolver DefaultActiveProfilesResolver
 */
@FunctionalInterface
public interface ActiveProfilesResolver {

	/**
	 * Resolve the <em>bean definition profiles</em> to use when loading an
	 * {@code ApplicationContext} for the given {@linkplain Class test class}.
	 * @param testClass the test class for which the profiles should be resolved;
	 * never {@code null}
	 * @return the bean definition profiles to use when loading the
	 * {@code ApplicationContext}; never {@code null} but potentially empty
	 * @see ActiveProfiles#resolver
	 * @see ActiveProfiles#inheritProfiles
	 */
	String[] resolve(Class<?> testClass);

}
