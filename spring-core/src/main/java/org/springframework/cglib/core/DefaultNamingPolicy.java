/*
 * Copyright 2003,2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cglib.core;

/**
 * The default policy used by {@link AbstractClassGenerator}.
 * Generates names such as
 * <p><code>org.springframework.cglib.Foo$$EnhancerByCGLIB$$38272841</code><p>
 * This is composed of a prefix based on the name of the superclass, a fixed
 * string incorporating the CGLIB class responsible for generation, and a
 * hashcode derived from the parameters used to create the object. If the same
 * name has been previously been used in the same <code>ClassLoader</code>, a
 * suffix is added to ensure uniqueness.
 */
public class DefaultNamingPolicy implements NamingPolicy {
    public static final DefaultNamingPolicy INSTANCE = new DefaultNamingPolicy();

    /**
     * This allows to test collisions of {@code key.hashCode()}.
     */
    private final static boolean STRESS_HASH_CODE = Boolean.getBoolean("org.springframework.cglib.test.stressHashCodes");

    @Override
    public String getClassName(String prefix, String source, Object key, Predicate names) {
        if (prefix == null) {
            prefix = "org.springframework.cglib.empty.Object";
        } else if (prefix.startsWith("java")) {
            prefix = "$" + prefix;
        }
        String base =
            prefix + "$$" +
            source.substring(source.lastIndexOf('.') + 1) +
            getTag() + "$$" +
            Integer.toHexString(STRESS_HASH_CODE ? 0 : key.hashCode());
        String attempt = base;
        int index = 2;
        while (names.evaluate(attempt)) {
			attempt = base + "_" + index++;
		}
        return attempt;
    }

    /**
     * Returns a string which is incorporated into every generated class name.
     * By default returns "ByCGLIB"
     */
    protected String getTag() {
        return "ByCGLIB";
    }

    @Override
    public int hashCode() {
        return getTag().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof DefaultNamingPolicy defaultNamingPolicy) &&
                defaultNamingPolicy.getTag().equals(getTag());
    }
}
