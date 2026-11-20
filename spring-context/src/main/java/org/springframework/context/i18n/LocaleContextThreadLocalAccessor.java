/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.i18n;

import io.micrometer.context.ThreadLocalAccessor;
import org.jspecify.annotations.Nullable;

/**
 * Adapt {@link LocaleContextHolder} to the {@link ThreadLocalAccessor} contract
 * to assist the Micrometer Context Propagation library with {@link LocaleContext}
 * propagation.
 *
 * @author Tadaya Tsuyukubo
 * @since 6.2
 */
public class LocaleContextThreadLocalAccessor implements ThreadLocalAccessor<LocaleContext> {

	/**
	 * Key under which this accessor is registered in
	 * {@link io.micrometer.context.ContextRegistry}.
	 */
	public static final String KEY = LocaleContextThreadLocalAccessor.class.getName() + ".KEY";

	@Override
	public Object key() {
		return KEY;
	}

	@Override
	public @Nullable LocaleContext getValue() {
		return LocaleContextHolder.getLocaleContext();
	}

	@Override
	public void setValue(LocaleContext value) {
		LocaleContextHolder.setLocaleContext(value);
	}

	@Override
	public void setValue() {
		LocaleContextHolder.resetLocaleContext();
	}

}
