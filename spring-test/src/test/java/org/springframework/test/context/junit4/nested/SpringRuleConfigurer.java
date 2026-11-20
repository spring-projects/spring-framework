/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.junit4.nested;

import org.junit.ClassRule;
import org.junit.Rule;

import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

/**
 * Abstract base test class that preconfigures the {@link SpringClassRule} and
 * {@link SpringMethodRule}.
 *
 * @author Sam Brannen
 * @since 5.0
 */
@SuppressWarnings("deprecation")
public abstract class SpringRuleConfigurer {

	@ClassRule
	public static final SpringClassRule springClassRule = new SpringClassRule();

	@Rule
	public final SpringMethodRule springMethodRule = new SpringMethodRule();

}
