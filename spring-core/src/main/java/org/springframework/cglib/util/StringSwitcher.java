/*
 * Copyright 2003 The Apache Software Foundation
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

package org.springframework.cglib.util;

import java.util.Arrays;
import java.util.List;

import org.springframework.asm.ClassVisitor;
import org.springframework.asm.Label;
import org.springframework.asm.Type;
import org.springframework.cglib.core.AbstractClassGenerator;
import org.springframework.cglib.core.ClassEmitter;
import org.springframework.cglib.core.CodeEmitter;
import org.springframework.cglib.core.Constants;
import org.springframework.cglib.core.EmitUtils;
import org.springframework.cglib.core.KeyFactory;
import org.springframework.cglib.core.ObjectSwitchCallback;
import org.springframework.cglib.core.ReflectUtils;
import org.springframework.cglib.core.Signature;
import org.springframework.cglib.core.TypeUtils;

/**
 * This class implements a simple String &rarr; int mapping for a fixed set of keys.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
abstract public class StringSwitcher {
    private static final Type STRING_SWITCHER =
      TypeUtils.parseType("org.springframework.cglib.util.StringSwitcher");
    private static final Signature INT_VALUE =
      TypeUtils.parseSignature("int intValue(String)");
    private static final StringSwitcherKey KEY_FACTORY =
      (StringSwitcherKey)KeyFactory.create(StringSwitcherKey.class);

    interface StringSwitcherKey {
        public Object newInstance(String[] strings, int[] ints, boolean fixedInput);
    }

    /**
     * Helper method to create a StringSwitcher.
     * For finer control over the generated instance, use a new instance of StringSwitcher.Generator
     * instead of this static method.
     * @param strings the array of String keys; must be the same length as the value array
     * @param ints the array of integer results; must be the same length as the key array
     * @param fixedInput if false, an unknown key will be returned from {@link #intValue} as <code>-1</code>; if true,
     * the result will be undefined, and the resulting code will be faster
     */
    public static StringSwitcher create(String[] strings, int[] ints, boolean fixedInput) {
        Generator gen = new Generator();
        gen.setStrings(strings);
        gen.setInts(ints);
        gen.setFixedInput(fixedInput);
        return gen.create();
    }

    protected StringSwitcher() {
    }

    /**
     * Return the integer associated with the given key.
     * @param s the key
     * @return the associated integer value, or <code>-1</code> if the key is unknown (unless
     * <code>fixedInput</code> was specified when this <code>StringSwitcher</code> was created,
     * in which case the return value for an unknown key is undefined)
     */
    abstract public int intValue(String s);

    public static class Generator extends AbstractClassGenerator {
        private static final Source SOURCE = new Source(StringSwitcher.class.getName());

        private String[] strings;
        private int[] ints;
        private boolean fixedInput;

        public Generator() {
            super(SOURCE);
        }

        /**
         * Set the array of recognized Strings.
         * @param strings the array of String keys; must be the same length as the value array
         * @see #setInts
         */
        public void setStrings(String[] strings) {
            this.strings = strings;
        }

        /**
         * Set the array of integer results.
         * @param ints the array of integer results; must be the same length as the key array
         * @see #setStrings
         */
        public void setInts(int[] ints) {
            this.ints = ints;
        }

        /**
         * Configure how unknown String keys will be handled.
         * @param fixedInput if false, an unknown key will be returned from {@link #intValue} as <code>-1</code>; if true,
         * the result will be undefined, and the resulting code will be faster
         */
        public void setFixedInput(boolean fixedInput) {
            this.fixedInput = fixedInput;
        }

        @Override
        protected ClassLoader getDefaultClassLoader() {
            return getClass().getClassLoader();
        }

        /**
         * Generate the <code>StringSwitcher</code>.
         */
        public StringSwitcher create() {
            setNamePrefix(StringSwitcher.class.getName());
            Object key = KEY_FACTORY.newInstance(strings, ints, fixedInput);
            return (StringSwitcher)super.create(key);
        }

        @Override
        public void generateClass(ClassVisitor v) throws Exception {
            ClassEmitter ce = new ClassEmitter(v);
            ce.begin_class(Constants.V1_8,
                           Constants.ACC_PUBLIC,
                           getClassName(),
                           STRING_SWITCHER,
                           null,
                           Constants.SOURCE_FILE);
            EmitUtils.null_constructor(ce);
            final CodeEmitter e = ce.begin_method(Constants.ACC_PUBLIC, INT_VALUE, null);
            e.load_arg(0);
            final List stringList = Arrays.asList(strings);
            int style = fixedInput ? Constants.SWITCH_STYLE_HASHONLY : Constants.SWITCH_STYLE_HASH;
            EmitUtils.string_switch(e, strings, style, new ObjectSwitchCallback() {
                @Override
                public void processCase(Object key, Label end) {
                    e.push(ints[stringList.indexOf(key)]);
                    e.return_value();
                }
                @Override
                public void processDefault() {
                    e.push(-1);
                    e.return_value();
                }
            });
            e.end_method();
            ce.end_class();
        }

        @Override
        protected Object firstInstance(Class type) {
            return ReflectUtils.newInstance(type);
        }

        @Override
        protected Object nextInstance(Object instance) {
            return instance;
        }
    }
}
