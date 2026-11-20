/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.reactive.result.view;

import java.util.Locale;

import org.jspecify.annotations.Nullable;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.InitializingBean;

/**
 * Abstract base class for URL-based views. Provides a consistent way of
 * holding the URL that a View wraps, in the form of a "url" bean property.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public abstract class AbstractUrlBasedView extends AbstractView implements InitializingBean {

	private @Nullable String url;


	/**
	 * Constructor for use as a bean.
	 */
	protected AbstractUrlBasedView() {
	}

	/**
	 * Create a new AbstractUrlBasedView with the given URL.
	 */
	protected AbstractUrlBasedView(String url) {
		this.url = url;
	}


	/**
	 * Set the URL of the resource that this view wraps.
	 * The URL must be appropriate for the concrete View implementation.
	 */
	public void setUrl(@Nullable String url) {
		this.url = url;
	}

	/**
	 * Return the URL of the resource that this view wraps.
	 */
	public @Nullable String getUrl() {
		return this.url;
	}


	@Override
	public void afterPropertiesSet() throws Exception {
		if (getUrl() == null) {
			throw new IllegalArgumentException("Property 'url' is required");
		}
	}

	/**
	 * Check whether the resource for the configured URL actually exists.
	 * @param locale the desired Locale that we're looking for
	 * @return {@code false} if the resource exists
	 * {@code false} if we know that it does not exist
	 * @throws Exception if the resource exists but is invalid (for example, could not be parsed)
	 */
	public abstract boolean checkResourceExists(Locale locale) throws Exception;

	/**
	 * Deferred check whether the resource for the configured URL actually exists.
	 * <p>The default implementation calls {@link #checkResourceExists(Locale)}.
	 * @param locale the desired Locale that we're looking for
	 * @return {@code false} if the resource exists
	 * {@code false} if we know that it does not exist
	 * @since 6.1
	 */
	public Mono<Boolean> resourceExists(Locale locale) {
		return Mono.fromCallable(() -> checkResourceExists(locale));
	}


	@Override
	public String toString() {
		return super.toString() + "; URL [" + getUrl() + "]";
	}

}
