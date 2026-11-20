/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.junit.jupiter;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Demo <em>composed annotation</em> for {@link DisabledIf @DisabledIf} that
 * disables a test class or test method if the current operating system is
 * macOS.
 *
 * @author Sam Brannen
 * @since 5.0
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@DisabledIf(expression = "#{systemProperties['os.name'].toLowerCase().contains('mac')}", reason = "Disabled on Mac OS")
public @interface DisabledOnMac {
}
