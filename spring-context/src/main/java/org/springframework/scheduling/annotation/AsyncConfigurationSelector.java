/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.scheduling.annotation;

import org.springframework.context.annotation.AdviceMode;
import org.springframework.context.annotation.AdviceModeImportSelector;

/**
 * Selects which implementation of {@link AbstractAsyncConfiguration} should
 * be used based on the value of {@link EnableAsync#mode} on the importing
 * {@code @Configuration} class.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.1
 * @see EnableAsync
 * @see ProxyAsyncConfiguration
 */
public class AsyncConfigurationSelector extends AdviceModeImportSelector<EnableAsync> {

	private static final String ASYNC_EXECUTION_ASPECT_CONFIGURATION_CLASS_NAME =
			"org.springframework.scheduling.aspectj.AspectJAsyncConfiguration";


	/**
	 * Returns {@link ProxyAsyncConfiguration} or {@code AspectJAsyncConfiguration}
	 * for {@code PROXY} and {@code ASPECTJ} values of {@link EnableAsync#mode()},
	 * respectively.
	 */
	@Override
	public String[] selectImports(AdviceMode adviceMode) {
		return switch (adviceMode) {
			case PROXY -> new String[] {ProxyAsyncConfiguration.class.getName()};
			case ASPECTJ -> new String[] {ASYNC_EXECUTION_ASPECT_CONFIGURATION_CLASS_NAME};
		};
	}

}
