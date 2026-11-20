/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.accept;

import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;

/**
 * Simple container of the API version for a request (possibly {@code null}),
 * or an exception that resulted from trying to resolve, parse, and validate
 * the version.
 *
 * <p>While an API version needs to be initialized early, given that each
 * {@code HandlerMapping} may or may not expect an API version, it is important
 * to defer raising API version errors until it is known if the
 * {@code HandlerMapping} will handle the request.
 *
 * @author Rossen Stoyanchev
 * @since 7.0
 */
public final class ApiVersionHolder {

	/** Static instance for a request without an API version. */
	public static final ApiVersionHolder EMPTY = ApiVersionHolder.fromVersion(null);


	private final @Nullable Comparable<?> version;

	private final @Nullable RuntimeException exception;


	private ApiVersionHolder(@Nullable Comparable<?> version, @Nullable RuntimeException ex) {
		this.version = version;
		this.exception = ex;
	}


	public boolean hasVersion() {
		return (this.version != null);
	}

	public boolean hasError() {
		return (this.exception != null);
	}

	public Comparable<?> getVersion() {
		Assert.state(this.version != null, "No version");
		return this.version;
	}

	public @Nullable Comparable<?> getVersionIfPresent() {
		return this.version;
	}

	public RuntimeException getError() {
		Assert.state(this.exception != null, "No error");
		return this.exception;
	}


	public static ApiVersionHolder fromVersion(@Nullable Comparable<?> version) {
		return new ApiVersionHolder(version, null);
	}

	public static ApiVersionHolder fromError(@Nullable RuntimeException ex) {
		return new ApiVersionHolder(null, ex);
	}

}
