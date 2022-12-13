/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.aot.nativex.substitution;

import java.beans.Customizer;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * {@link java.beans.Introspector} substitution with a refined {@code findCustomizerClass} implementation
 * designed to avoid thousands of AWT classes to be included in the native image.
 *
 * TODO Remove once Spring Framework requires GraalVM 23.0+, see <a href="https://github.com/oracle/graal/pull/5224">graal#5224</a>.
 *
 * @author Sebastien Deleuze
 * @since 6.0
 */
@TargetClass(className = "java.beans.Introspector")
final class Target_Introspector {

	@Substitute
	private static Class<?> findCustomizerClass(Class<?> type) {
		String name = type.getName() + "Customizer";
		try {
			type = Target_ClassFinder.findClass(name, type.getClassLoader());
			if (Customizer.class.isAssignableFrom(type)) {
				Class<?> c = type;
				do {
					c = c.getSuperclass();
					if (c.getName().equals("java.awt.Component")) {
						return type;
					}
				} while (!c.getName().equals("java.lang.Object"));
			}
		}
		catch (Exception exception) {
		}
		return null;
	}

}
