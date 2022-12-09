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
package org.springframework.cglib.core;

import org.springframework.asm.ClassWriter;

public class DefaultGeneratorStrategy implements GeneratorStrategy {
    public static final DefaultGeneratorStrategy INSTANCE = new DefaultGeneratorStrategy();

    @Override
	public byte[] generate(ClassGenerator cg) throws Exception {
        DebuggingClassWriter cw = getClassVisitor();
        transform(cg).generateClass(cw);
        return transform(cw.toByteArray());
    }

    protected DebuggingClassWriter getClassVisitor() throws Exception {
      return new DebuggingClassWriter(ClassWriter.COMPUTE_FRAMES);
    }

    protected final ClassWriter getClassWriter() {
	// Cause compile / runtime errors for people who implemented the old
	// interface without using @Override
	throw new UnsupportedOperationException("You are calling " +
		"getClassWriter, which no longer exists in this cglib version.");
    }

    protected byte[] transform(byte[] b) throws Exception {
        return b;
    }

    protected ClassGenerator transform(ClassGenerator cg) throws Exception {
        return cg;
    }
}
