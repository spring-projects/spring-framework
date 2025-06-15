/*
 * Copyright 2011 The Apache Software Foundation
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

package org.springframework.cglib.proxy;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.springframework.asm.ClassReader;
import org.springframework.asm.ClassVisitor;
import org.springframework.asm.MethodVisitor;
import org.springframework.asm.Opcodes;
import org.springframework.cglib.core.Constants;
import org.springframework.cglib.core.Signature;

/**
 * Uses bytecode reflection to figure out the targets of all bridge methods that use invokespecial
 * and invokeinterface, so that we can later rewrite them to use invokevirtual.
 *
 * <p>For interface bridges, using invokesuper will fail since the method being bridged to is in a
 * superinterface, not a superclass. Starting in Java 8, javac emits default bridge methods in
 * interfaces, which use invokeinterface to bridge to the target method.
 *
 * @author sberlin@gmail.com (Sam Berlin)
 */
@SuppressWarnings({"rawtypes", "unchecked"})
class BridgeMethodResolver {

    private final Map/* <Class, Set<Signature> */declToBridge;
    private final ClassLoader classLoader;

    public BridgeMethodResolver(Map declToBridge, ClassLoader classLoader) {
        this.declToBridge = declToBridge;
        this.classLoader = classLoader;
    }

    /**
     * Finds all bridge methods that are being called with invokespecial &
     * returns them.
     */
    public Map/*<Signature, Signature>*/resolveAll() {
        Map resolved = new HashMap();
        for (Iterator entryIter = declToBridge.entrySet().iterator(); entryIter.hasNext(); ) {
            Map.Entry entry = (Map.Entry) entryIter.next();
            Class owner = (Class) entry.getKey();
            Set bridges = (Set) entry.getValue();
            try {
                InputStream is = classLoader.getResourceAsStream(owner.getName().replace('.', '/') + ".class");
                if (is == null) {
                    return resolved;
                }
                try {
                    new ClassReader(is)
                            .accept(new BridgedFinder(bridges, resolved),
                                    ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
                } finally {
                    is.close();
                }
            } catch (IOException ignored) {}
        }
        return resolved;
    }

    private static class BridgedFinder extends ClassVisitor {
        private Map/*<Signature, Signature>*/ resolved;
        private Set/*<Signature>*/ eligibleMethods;

        private Signature currentMethod = null;

        BridgedFinder(Set eligibleMethods, Map resolved) {
            super(Constants.ASM_API);
            this.resolved = resolved;
            this.eligibleMethods = eligibleMethods;
        }

        @Override
		public void visit(int version, int access, String name,
                String signature, String superName, String[] interfaces) {
        }

        @Override
		public MethodVisitor visitMethod(int access, String name, String desc,
                String signature, String[] exceptions) {
            Signature sig = new Signature(name, desc);
            if (eligibleMethods.remove(sig)) {
                currentMethod = sig;
                return new MethodVisitor(Constants.ASM_API) {
                    @Override
					public void visitMethodInsn(
                            int opcode, String owner, String name, String desc, boolean itf) {
                        if ((opcode == Opcodes.INVOKESPECIAL
                                        || (itf && opcode == Opcodes.INVOKEINTERFACE))
                                && currentMethod != null) {
                            Signature target = new Signature(name, desc);
                            // If the target signature is the same as the current,
                            // we shouldn't change our bridge becaues invokespecial
                            // is the only way to make progress (otherwise we'll
                            // get infinite recursion).  This would typically
                            // only happen when a bridge method is created to widen
                            // the visibility of a superclass' method.
                            if (!target.equals(currentMethod)) {
                                resolved.put(currentMethod, target);
                            }
                            currentMethod = null;
                        }
                    }
                };
            } else {
                return null;
            }
        }
    }

}
