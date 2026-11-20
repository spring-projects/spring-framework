/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.reactive.resource;

import reactor.core.publisher.Mono;

import org.springframework.core.io.Resource;

/**
 * A {@code VersionStrategy} that relies on a fixed version applied as a request
 * path prefix, for example, reduced SHA, version name, release date, etc.
 *
 * <p>This is useful for example when {@link ContentVersionStrategy} cannot be
 * used such as when using JavaScript module loaders which are in charge of
 * loading the JavaScript resources and need to know their relative paths.
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @since 5.0
 * @see VersionResourceResolver
 */
public class FixedVersionStrategy extends AbstractPrefixVersionStrategy {

	private final Mono<String> versionMono;


	/**
	 * Create a new FixedVersionStrategy with the given version string.
	 * @param version the fixed version string to use
	 */
	public FixedVersionStrategy(String version) {
		super(version);
		this.versionMono = Mono.just(version);
	}


	@Override
	public Mono<String> getResourceVersion(Resource resource) {
		return this.versionMono;
	}

}
