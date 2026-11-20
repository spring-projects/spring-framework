/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.testfixture;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

/**
 * {@code @EnabledForTestGroups} is used to enable the annotated test class or
 * test method for one or more {@link TestGroup} {@linkplain #value values}.
 *
 * @author Sam Brannen
 * @since 5.2
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@ExtendWith(TestGroupsCondition.class)
public @interface EnabledForTestGroups {

	/**
	 * One or more {@link TestGroup}s that must be active.
	 */
	TestGroup[] value();

}
