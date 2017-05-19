/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.core.convert.support;

import org.junit.Test;
import org.springframework.core.convert.TypeDescriptor;

/**
 * @author Martin Macko
 */
public class ObjectToObjectConverterTests {

    @Test(expected = IllegalStateException.class)
    public void testPrivateMethodOrConstructor() {
        ObjectToObjectConverter converter = new ObjectToObjectConverter();
        converter.convert(new Bar(), TypeDescriptor.valueOf(Bar.class), TypeDescriptor.valueOf(Foo.class));
    }

    static class Foo {
        public Foo(){}
        private Foo(Bar bar) {}
        private static Foo valueOf(Bar bar) {
            return new Foo();
        }
        private static Foo of(Bar bar) {
            return new Foo();
        }
        private static Foo from(Bar bar) {
            return new Foo();
        }
    }

    static class Bar {
        private Foo toFoo() {
            return new Foo();
        }
    }
}
