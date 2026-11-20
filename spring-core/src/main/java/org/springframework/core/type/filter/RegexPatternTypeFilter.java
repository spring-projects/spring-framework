/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.type.filter;

import java.util.regex.Pattern;

import org.springframework.core.type.ClassMetadata;
import org.springframework.util.Assert;

/**
 * A simple filter for matching a fully-qualified class name with a regex {@link Pattern}.
 *
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @since 2.5
 */
public class RegexPatternTypeFilter extends AbstractClassTestingTypeFilter {

	private final Pattern pattern;


	public RegexPatternTypeFilter(Pattern pattern) {
		Assert.notNull(pattern, "Pattern must not be null");
		this.pattern = pattern;
	}


	@Override
	protected boolean match(ClassMetadata metadata) {
		return this.pattern.matcher(metadata.getClassName()).matches();
	}

}
