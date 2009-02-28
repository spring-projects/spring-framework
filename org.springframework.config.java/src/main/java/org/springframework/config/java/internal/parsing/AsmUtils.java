/*
 * Copyright 2002-2008 the original author or authors.
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
package org.springframework.config.java.internal.parsing;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.commons.EmptyVisitor;
import org.springframework.config.java.Util;


/**
 * Various utility methods commonly used when interacting with ASM.
 */
class AsmUtils {

    public static final EmptyVisitor EMPTY_VISITOR = new EmptyVisitor();

    private static final Log log = LogFactory.getLog(AsmUtils.class);
    
    /**
     * @param className a standard, dot-delimeted, fully-qualified Java class name
     * @return internal version of className, as per ASM guide section 2.1.2 "Internal Names"
     */
    public static String convertClassNameToInternalName(String className) {
        return className.replace('.', '/');
    }
    
    /**
     * Convert a type descriptor to a classname suitable for classloading
     * with Class.forName().
     *
     * @param typeDescriptor see ASM guide section 2.1.3
     */
    public static String convertTypeDescriptorToClassName(String typeDescriptor) {
        final String internalName; // See ASM guide section 2.1.2
        
        // TODO: SJC-242 should catch all possible cases. use case statement and switch on char
        // TODO: SJC-242 converting from primitive to object here won't be intuitive to users
        if("V".equals(typeDescriptor))
            return Void.class.getName();
        if("I".equals(typeDescriptor))
            return Integer.class.getName();
        if("Z".equals(typeDescriptor))
            return Boolean.class.getName();

        // strip the leading array/object/primitive identifier
        if(typeDescriptor.startsWith("[["))
            internalName = typeDescriptor.substring(3);
        else if(typeDescriptor.startsWith("["))
            internalName = typeDescriptor.substring(2);
        else
            internalName = typeDescriptor.substring(1);

        // convert slashes to dots
        String className = internalName.replace('/', '.');

        // and strip trailing semicolon (if present)
        if(className.endsWith(";"))
           className = className.substring(0, internalName.length()-1);

        return className;
    }

    /**
     * @param methodDescriptor see ASM guide section 2.1.4
     */
    public static String getReturnTypeFromMethodDescriptor(String methodDescriptor) {
        String returnTypeDescriptor = methodDescriptor.substring(methodDescriptor.indexOf(')')+1);
        return convertTypeDescriptorToClassName(returnTypeDescriptor);
    }
    
    /**
     * Creates a new ASM {@link ClassReader} for <var>pathToClass</var>.  Appends '.class'
     * to pathToClass before attempting to load.
     * 
     * @throws RuntimeException if <var>pathToClass</var>+.class cannot be found on the classpath
     * @throws RuntimeException if an IOException occurs when creating the new ClassReader
     */
    public static ClassReader newClassReader(String pathToClass) {
        InputStream is = Util.getClassAsStream(pathToClass);
        return newClassReader(is);
    }
    
    /**
     * Convenience method that simply returns a new ASM {@link ClassReader} instance based on
     * the supplied <var>bytes</var> byte array.  This method is exactly equivalent to calling
     * new ClassReader(byte[]), and is mainly provided for symmetry with usage of
     * {@link #newClassReader(InputStream)}.
     * 
     * @param bytes byte array that will be provided as input to the new ClassReader instance.
     * 
     * @return
     */
    public static ClassReader newClassReader(byte[] bytes) {
        return new ClassReader(bytes);
    }
    
    /**
     * Convenience method that creates and returns a new ASM {@link ClassReader} for the given
     * InputStream <var>is</var>, closing the InputStream after creating the ClassReader and rethrowing
     * any IOException thrown during ClassReader instantiation as an unchecked exception. Logs and ignores
     * any IOException thrown when closing the InputStream.
     * 
     * @param is InputStream that will be provided to the new ClassReader instance.
     */
    public static ClassReader newClassReader(InputStream is) {
        try {
            return new ClassReader(is);
        } catch (IOException ex) {
            throw new RuntimeException("An unexpected exception occurred while creating ASM ClassReader: " + ex);
        } finally {
            try {
                is.close();
            } catch (IOException ex) {
                log.error("Ignoring exception thrown while closing InputStream", ex);
            }
        }
    }

}
