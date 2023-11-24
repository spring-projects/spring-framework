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

import java.io.IOException;

import org.springframework.asm.Attribute;
import org.springframework.asm.ClassReader;
import org.springframework.asm.ClassWriter;
import org.springframework.cglib.core.ClassGenerator;
import org.springframework.cglib.core.CodeGenerationException;
import org.springframework.cglib.core.DebuggingClassWriter;

@SuppressWarnings({"rawtypes", "unchecked"})
abstract public class AbstractClassLoader extends ClassLoader {
    private ClassFilter filter;
    private ClassLoader classPath;
    private static java.security.ProtectionDomain DOMAIN ;

    static{
        DOMAIN = AbstractClassLoader.class.getProtectionDomain();
     }

    protected AbstractClassLoader(ClassLoader parent, ClassLoader classPath, ClassFilter filter) {
        super(parent);
        this.filter = filter;
        this.classPath = classPath;
    }

    @Override
	public Class loadClass(String name) throws ClassNotFoundException {

        Class loaded = findLoadedClass(name);

        if( loaded != null ){
            if( loaded.getClassLoader() == this ){
               return loaded;
            }//else reload with this class loader
        }

        if (!filter.accept(name)) {
            return super.loadClass(name);
        }
        ClassReader r;
        try {

           java.io.InputStream is = classPath.getResourceAsStream(
                       name.replace('.','/') + ".class"
                  );

           if (is == null) {

              throw new ClassNotFoundException(name);

           }
           try {

              r = new ClassReader(is);

           } finally {

              is.close();

           }
        } catch (IOException e) {
            throw new ClassNotFoundException(name + ":" + e.getMessage());
        }

        try {
            DebuggingClassWriter w =
        	    new DebuggingClassWriter(ClassWriter.COMPUTE_FRAMES);
            getGenerator(r).generateClass(w);
            byte[] b = w.toByteArray();
            Class c = super.defineClass(name, b, 0, b.length, DOMAIN);
            postProcess(c);
            return c;
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Exception e) {
            throw new CodeGenerationException(e);
        }
    }

    protected ClassGenerator getGenerator(ClassReader r) {
        return new ClassReaderGenerator(r, attributes(), getFlags());
    }

    protected int getFlags() {
        return 0;
    }

    protected Attribute[] attributes() {
        return null;
    }

    protected void postProcess(Class c) {
    }
}
