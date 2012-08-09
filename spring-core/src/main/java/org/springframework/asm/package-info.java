/*
 * Copyright 2002-2012 the original author or authors.
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

/**
 * Spring's repackaging of <a href="http://asm.ow2.org">org.objectweb.asm 4</a> (for
 * internal use only).
 * <p>This repackaging technique avoids any potential conflicts with
 * dependencies on ASM at the application level or from other third-party
 * libraries and frameworks.
 * <p>As this repackaging happens at the classfile level, sources and Javadoc
 * are not available here. See the original ObjectWeb
 * <a href="http://asm.ow2.org/asm40/javadoc/user">ASM 4 Javadoc</a>
 * for details when working with these classes.
 *
 * @since 3.2
 */
package org.springframework.asm;
