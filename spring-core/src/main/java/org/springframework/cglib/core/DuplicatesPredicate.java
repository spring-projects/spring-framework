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

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.asm.ClassReader;
import org.springframework.asm.ClassVisitor;
import org.springframework.asm.MethodVisitor;
import org.springframework.asm.Opcodes;

@SuppressWarnings({"rawtypes", "unchecked"})
public class DuplicatesPredicate implements Predicate {
  private final Set unique;
  private final Set rejected;

  /**
   * Constructs a DuplicatesPredicate that will allow subclass bridge methods to be preferred over
   * superclass non-bridge methods.
   */
  public DuplicatesPredicate() {
    unique = new HashSet();
    rejected = Collections.emptySet();
  }

  /**
   * Constructs a DuplicatesPredicate that prefers using superclass non-bridge methods despite a
   * subclass method with the same signature existing (if the subclass is a bridge method).
   */
  public DuplicatesPredicate(List allMethods) {
    rejected = new HashSet();
    unique = new HashSet();

    // Traverse through the methods and capture ones that are bridge
    // methods when a subsequent method (from a non-interface superclass)
    // has the same signature but isn't a bridge. Record these so that
    // we avoid using them when filtering duplicates.
    Map scanned = new HashMap();
    Map suspects = new HashMap();
    for (Object o : allMethods) {
      Method method = (Method) o;
      Object sig = MethodWrapper.create(method);
      Method existing = (Method) scanned.get(sig);
      if (existing == null) {
        scanned.put(sig, method);
      } else if (!suspects.containsKey(sig) && existing.isBridge() && !method.isBridge()) {
        // TODO: this currently only will capture a single bridge. it will not work
        // if there's Child.bridge1 Middle.bridge2 Parent.concrete.  (we'd offer the 2nd bridge).
        // no idea if that's even possible tho...
        suspects.put(sig, existing);
      }
    }

    if (!suspects.isEmpty()) {
      Set classes = new HashSet();
      UnnecessaryBridgeFinder finder = new UnnecessaryBridgeFinder(rejected);
      for (Object o : suspects.values()) {
        Method m = (Method) o;
        classes.add(m.getDeclaringClass());
        finder.addSuspectMethod(m);
      }
      for (Object o : classes) {
        Class c = (Class) o;
        try {
          ClassLoader cl = getClassLoader(c);
          if (cl == null) {
            continue;
          }
          InputStream is = cl.getResourceAsStream(c.getName().replace('.', '/') + ".class");
          if (is == null) {
            continue;
          }
          try {
            new ClassReader(is).accept(finder, ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
          } finally {
            is.close();
          }
        } catch (IOException ignored) {
        }
      }
    }
  }

  @Override
  public boolean evaluate(Object arg) {
    return !rejected.contains(arg) && unique.add(MethodWrapper.create((Method) arg));
  }

  private static ClassLoader getClassLoader(Class c) {
    ClassLoader cl = c.getClassLoader();
    if (cl == null) {
      cl = DuplicatesPredicate.class.getClassLoader();
    }
    if (cl == null) {
      cl = Thread.currentThread().getContextClassLoader();
    }
    return cl;
  }

  private static class UnnecessaryBridgeFinder extends ClassVisitor {
    private final Set rejected;

    private Signature currentMethodSig = null;
    private Map methods = new HashMap();

    UnnecessaryBridgeFinder(Set rejected) {
      super(Constants.ASM_API);
      this.rejected = rejected;
    }

    void addSuspectMethod(Method m) {
      methods.put(ReflectUtils.getSignature(m), m);
    }

    @Override
    public void visit(
        int version,
        int access,
        String name,
        String signature,
        String superName,
        String[] interfaces) {}

    @Override
    public MethodVisitor visitMethod(
        int access, String name, String desc, String signature, String[] exceptions) {
      Signature sig = new Signature(name, desc);
      final Method currentMethod = (Method) methods.remove(sig);
      if (currentMethod != null) {
        currentMethodSig = sig;
        return new MethodVisitor(Constants.ASM_API) {
          @Override
          public void visitMethodInsn(
              int opcode, String owner, String name, String desc, boolean itf) {
            if (opcode == Opcodes.INVOKESPECIAL && currentMethodSig != null) {
              Signature target = new Signature(name, desc);
              if (target.equals(currentMethodSig)) {
                rejected.add(currentMethod);
              }
              currentMethodSig = null;
            }
          }
        };
      } else {
        return null;
      }
    }
  }
}
