/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.annotation.spr10546.scanpackage;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.spr10546.ParentConfig;

/**
 * Note the name of {@link AEnclosingConfig} is chosen to help ensure scanning picks up
 * the enclosing configuration prior to {@link ChildConfig} to demonstrate this can happen
 * with classpath scanning.
 *
 * @author Rob Winch
 */
@Configuration
public class AEnclosingConfig {
	@Configuration
	public static class ChildConfig extends ParentConfig {}
}
