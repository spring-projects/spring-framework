/*
 * Copyright 2011 the original author or authors.
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

package org.springframework.test.context.junit4;

import org.junit.Ignore;
import org.springframework.test.context.ContextConfiguration;

/**
 * TODO [SPR-6184] Document ConfigurationClassSpringJUnit4ClassRunnerAppCtxTests.
 * 
 * @author Sam Brannen
 * @since 3.1
 */
@Ignore("[SPR-6184] Work in Progress")
// TODO [SPR-6184] Set ContextLoader: ConfigurationClassContextLoader.class.
@ContextConfiguration(classes = ConfigurationClassSpringJUnit4ClassRunnerAppCtxTestsConfig.class, inheritLocations = false)
public class ConfigurationClassSpringJUnit4ClassRunnerAppCtxTests extends SpringJUnit4ClassRunnerAppCtxTests {

}
