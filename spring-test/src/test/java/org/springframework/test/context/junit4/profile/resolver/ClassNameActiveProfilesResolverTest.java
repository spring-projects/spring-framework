/*
 *
 *  * Copyright 2002-2013 the original author or authors.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.springframework.test.context.junit4.profile.resolver;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import javax.inject.Inject;

import java.util.Arrays;

import static org.junit.Assert.assertTrue;

/**
 * @author Michail Nikolaev
 * @since 3.2.2
 */
@ActiveProfiles(resolver = ClassNameActiveProfilesResolver.class)
@ContextConfiguration(classes = ClassNameActiveProfilesConfig.class, loader = AnnotationConfigContextLoader.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class ClassNameActiveProfilesResolverTest {
	@Inject
	private ApplicationContext applicationContext;
	@Test
	public void test() {
		assertTrue(Arrays.asList(applicationContext.getEnvironment().getActiveProfiles
				()).contains(getClass().getSimpleName().toLowerCase()));
	}
}
