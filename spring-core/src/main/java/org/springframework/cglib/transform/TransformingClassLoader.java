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

package org.springframework.cglib.transform;

import org.springframework.asm.ClassReader;
import org.springframework.cglib.core.ClassGenerator;
import org.springframework.cglib.core.ClassTransformer;

public class TransformingClassLoader extends AbstractClassLoader {
    private final ClassTransformerFactory t;

    public TransformingClassLoader(ClassLoader parent, ClassFilter filter, ClassTransformerFactory t) {
        super(parent, parent, filter);
        this.t = t;
    }

    @Override
    protected ClassGenerator getGenerator(ClassReader r) {
        ClassTransformer t2 = t.newInstance();
        return new TransformingClassGenerator(super.getGenerator(r), t2);
    }
}
