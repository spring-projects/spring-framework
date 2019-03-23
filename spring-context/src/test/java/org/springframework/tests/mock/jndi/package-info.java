/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * The simplest implementation of the JNDI SPI that could possibly work.
 *
 * <p>Useful for setting up a simple JNDI environment for test suites
 * or standalone applications. If e.g. JDBC DataSources get bound to the
 * same JNDI names as within a J2EE container, both application code and
 * configuration can me reused without changes.
 */
package org.springframework.tests.mock.jndi;
