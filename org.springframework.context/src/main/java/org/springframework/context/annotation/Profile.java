/*
 * Copyright 2002-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.context.annotation;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.TYPE;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * TODO SPR-7508: document
 * 
 * Components not @Profile-annotated will always be registered
 * @Profile("default") means that beans will be registered unless other profile(s) are active
 * @Profile({"xyz,default"}) means that beans will be registered if 'xyz' is active or if no profile is active
 * ConfigurableEnvironment.setDefaultProfileName(String) customizes the name of the default profile
 * 'defaultSpringProfile' property customizes the name of the default profile (usually for use as a servlet context/init param)
 * ConfigurableEnvironment.setActiveProfiles(String...) sets which profiles are active
 * 'springProfiles' sets which profiles are active (typically as a -D system property)
 *
 * @author Chris Beams
 * @since 3.1
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({
	ANNOTATION_TYPE, // @Profile may be used as a meta-annotation
	TYPE             // In conjunction with @Component and its derivatives
})
public @interface Profile {

	/**
	 * @see #value()
	 */
	static final String CANDIDATE_PROFILES_ATTRIB_NAME = "value";

	/**
	 * TODO SPR-7508: document
	 */
	String[] value();
}
