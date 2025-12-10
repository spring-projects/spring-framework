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
package org.springframework.cglib.transform;

import org.springframework.asm.ClassVisitor;
import org.springframework.asm.MethodVisitor;
import org.springframework.cglib.core.ClassTransformer;

public class ClassTransformerChain extends AbstractClassTransformer {
    private ClassTransformer[] chain;

    public ClassTransformerChain(ClassTransformer[] chain) {
        this.chain = chain.clone();
    }

    @Override
	public void setTarget(ClassVisitor v) {
        super.setTarget(chain[0]);
        ClassVisitor next = v;
        for (int i = chain.length - 1; i >= 0; i--) {
            chain[i].setTarget(next);
            next = chain[i];
        }
    }

    @Override
	public MethodVisitor visitMethod(int access,
                                     String name,
                                     String desc,
                                     String signature,
                                     String[] exceptions) {
        return cv.visitMethod(access, name, desc, signature, exceptions);
    }

    @Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
        sb.append("ClassTransformerChain{");
        for (int i = 0; i < chain.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(chain[i].toString());
        }
        sb.append("}");
        return sb.toString();
    }
}
