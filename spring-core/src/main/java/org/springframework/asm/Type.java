/***
 * ASM: a very small and fast Java bytecode manipulation framework
 * Copyright (c) 2000-2011 INRIA, France Telecom
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holders nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.springframework.asm;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * A Java field or method type. This class can be used to make it easier to
 * manipulate type and method descriptors.
 * 
 * @author Eric Bruneton
 * @author Chris Nokleberg
 */
public class Type {

    /**
     * The sort of the <tt>void</tt> type. See {@link #getSort getSort}.
     */
    public static final int VOID = 0;

    /**
     * The sort of the <tt>boolean</tt> type. See {@link #getSort getSort}.
     */
    public static final int BOOLEAN = 1;

    /**
     * The sort of the <tt>char</tt> type. See {@link #getSort getSort}.
     */
    public static final int CHAR = 2;

    /**
     * The sort of the <tt>byte</tt> type. See {@link #getSort getSort}.
     */
    public static final int BYTE = 3;

    /**
     * The sort of the <tt>short</tt> type. See {@link #getSort getSort}.
     */
    public static final int SHORT = 4;

    /**
     * The sort of the <tt>int</tt> type. See {@link #getSort getSort}.
     */
    public static final int INT = 5;

    /**
     * The sort of the <tt>float</tt> type. See {@link #getSort getSort}.
     */
    public static final int FLOAT = 6;

    /**
     * The sort of the <tt>long</tt> type. See {@link #getSort getSort}.
     */
    public static final int LONG = 7;

    /**
     * The sort of the <tt>double</tt> type. See {@link #getSort getSort}.
     */
    public static final int DOUBLE = 8;

    /**
     * The sort of array reference types. See {@link #getSort getSort}.
     */
    public static final int ARRAY = 9;

    /**
     * The sort of object reference types. See {@link #getSort getSort}.
     */
    public static final int OBJECT = 10;

    /**
     * The sort of method types. See {@link #getSort getSort}.
     */
    public static final int METHOD = 11;

    /**
     * The <tt>void</tt> type.
     */
    public static final Type VOID_TYPE = new Type(VOID, null, ('V' << 24)
            | (5 << 16) | (0 << 8) | 0, 1);

    /**
     * The <tt>boolean</tt> type.
     */
    public static final Type BOOLEAN_TYPE = new Type(BOOLEAN, null, ('Z' << 24)
            | (0 << 16) | (5 << 8) | 1, 1);

    /**
     * The <tt>char</tt> type.
     */
    public static final Type CHAR_TYPE = new Type(CHAR, null, ('C' << 24)
            | (0 << 16) | (6 << 8) | 1, 1);

    /**
     * The <tt>byte</tt> type.
     */
    public static final Type BYTE_TYPE = new Type(BYTE, null, ('B' << 24)
            | (0 << 16) | (5 << 8) | 1, 1);

    /**
     * The <tt>short</tt> type.
     */
    public static final Type SHORT_TYPE = new Type(SHORT, null, ('S' << 24)
            | (0 << 16) | (7 << 8) | 1, 1);

    /**
     * The <tt>int</tt> type.
     */
    public static final Type INT_TYPE = new Type(INT, null, ('I' << 24)
            | (0 << 16) | (0 << 8) | 1, 1);

    /**
     * The <tt>float</tt> type.
     */
    public static final Type FLOAT_TYPE = new Type(FLOAT, null, ('F' << 24)
            | (2 << 16) | (2 << 8) | 1, 1);

    /**
     * The <tt>long</tt> type.
     */
    public static final Type LONG_TYPE = new Type(LONG, null, ('J' << 24)
            | (1 << 16) | (1 << 8) | 2, 1);

    /**
     * The <tt>double</tt> type.
     */
    public static final Type DOUBLE_TYPE = new Type(DOUBLE, null, ('D' << 24)
            | (3 << 16) | (3 << 8) | 2, 1);

    // ------------------------------------------------------------------------
    // Fields
    // ------------------------------------------------------------------------

    /**
     * The sort of this Java type.
     */
    private final int sort;

    /**
     * A buffer containing the internal name of this Java type. This field is
     * only used for reference types.
     */
    private final char[] buf;

    /**
     * The offset of the internal name of this Java type in {@link #buf buf} or,
     * for primitive types, the size, descriptor and getOpcode offsets for this
     * type (byte 0 contains the size, byte 1 the descriptor, byte 2 the offset
     * for IALOAD or IASTORE, byte 3 the offset for all other instructions).
     */
    private final int off;

    /**
     * The length of the internal name of this Java type.
     */
    private final int len;

    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------

    /**
     * Constructs a reference type.
     * 
     * @param sort
     *            the sort of the reference type to be constructed.
     * @param buf
     *            a buffer containing the descriptor of the previous type.
     * @param off
     *            the offset of this descriptor in the previous buffer.
     * @param len
     *            the length of this descriptor.
     */
    private Type(final int sort, final char[] buf, final int off, final int len) {
        this.sort = sort;
        this.buf = buf;
        this.off = off;
        this.len = len;
    }

    /**
     * Returns the Java type corresponding to the given type descriptor.
     * 
     * @param typeDescriptor
     *            a field or method type descriptor.
     * @return the Java type corresponding to the given type descriptor.
     */
    public static Type getType(final String typeDescriptor) {
        return getType(typeDescriptor.toCharArray(), 0);
    }

    /**
     * Returns the Java type corresponding to the given internal name.
     * 
     * @param internalName
     *            an internal name.
     * @return the Java type corresponding to the given internal name.
     */
    public static Type getObjectType(final String internalName) {
        char[] buf = internalName.toCharArray();
        return new Type(buf[0] == '[' ? ARRAY : OBJECT, buf, 0, buf.length);
    }

    /**
     * Returns the Java type corresponding to the given method descriptor.
     * Equivalent to <code>Type.getType(methodDescriptor)</code>.
     * 
     * @param methodDescriptor
     *            a method descriptor.
     * @return the Java type corresponding to the given method descriptor.
     */
    public static Type getMethodType(final String methodDescriptor) {
        return getType(methodDescriptor.toCharArray(), 0);
    }

    /**
     * Returns the Java method type corresponding to the given argument and
     * return types.
     * 
     * @param returnType
     *            the return type of the method.
     * @param argumentTypes
     *            the argument types of the method.
     * @return the Java type corresponding to the given argument and return
     *         types.
     */
    public static Type getMethodType(final Type returnType,
            final Type... argumentTypes) {
        return getType(getMethodDescriptor(returnType, argumentTypes));
    }

    /**
     * Returns the Java type corresponding to the given class.
     * 
     * @param c
     *            a class.
     * @return the Java type corresponding to the given class.
     */
    public static Type getType(final Class<?> c) {
        if (c.isPrimitive()) {
            if (c == Integer.TYPE) {
                return INT_TYPE;
            } else if (c == Void.TYPE) {
                return VOID_TYPE;
            } else if (c == Boolean.TYPE) {
                return BOOLEAN_TYPE;
            } else if (c == Byte.TYPE) {
                return BYTE_TYPE;
            } else if (c == Character.TYPE) {
                return CHAR_TYPE;
            } else if (c == Short.TYPE) {
                return SHORT_TYPE;
            } else if (c == Double.TYPE) {
                return DOUBLE_TYPE;
            } else if (c == Float.TYPE) {
                return FLOAT_TYPE;
            } else /* if (c == Long.TYPE) */{
                return LONG_TYPE;
            }
        } else {
            return getType(getDescriptor(c));
        }
    }

    /**
     * Returns the Java method type corresponding to the given constructor.
     * 
     * @param c
     *            a {@link Constructor Constructor} object.
     * @return the Java method type corresponding to the given constructor.
     */
    public static Type getType(final Constructor<?> c) {
        return getType(getConstructorDescriptor(c));
    }

    /**
     * Returns the Java method type corresponding to the given method.
     * 
     * @param m
     *            a {@link Method Method} object.
     * @return the Java method type corresponding to the given method.
     */
    public static Type getType(final Method m) {
        return getType(getMethodDescriptor(m));
    }

    /**
     * Returns the Java types corresponding to the argument types of the given
     * method descriptor.
     * 
     * @param methodDescriptor
     *            a method descriptor.
     * @return the Java types corresponding to the argument types of the given
     *         method descriptor.
     */
    public static Type[] getArgumentTypes(final String methodDescriptor) {
        char[] buf = methodDescriptor.toCharArray();
        int off = 1;
        int size = 0;
        while (true) {
            char car = buf[off++];
            if (car == ')') {
                break;
            } else if (car == 'L') {
                while (buf[off++] != ';') {
                }
                ++size;
            } else if (car != '[') {
                ++size;
            }
        }
        Type[] args = new Type[size];
        off = 1;
        size = 0;
        while (buf[off] != ')') {
            args[size] = getType(buf, off);
            off += args[size].len + (args[size].sort == OBJECT ? 2 : 0);
            size += 1;
        }
        return args;
    }

    /**
     * Returns the Java types corresponding to the argument types of the given
     * method.
     * 
     * @param method
     *            a method.
     * @return the Java types corresponding to the argument types of the given
     *         method.
     */
    public static Type[] getArgumentTypes(final Method method) {
        Class<?>[] classes = method.getParameterTypes();
        Type[] types = new Type[classes.length];
        for (int i = classes.length - 1; i >= 0; --i) {
            types[i] = getType(classes[i]);
        }
        return types;
    }

    /**
     * Returns the Java type corresponding to the return type of the given
     * method descriptor.
     * 
     * @param methodDescriptor
     *            a method descriptor.
     * @return the Java type corresponding to the return type of the given
     *         method descriptor.
     */
    public static Type getReturnType(final String methodDescriptor) {
        char[] buf = methodDescriptor.toCharArray();
        return getType(buf, methodDescriptor.indexOf(')') + 1);
    }

    /**
     * Returns the Java type corresponding to the return type of the given
     * method.
     * 
     * @param method
     *            a method.
     * @return the Java type corresponding to the return type of the given
     *         method.
     */
    public static Type getReturnType(final Method method) {
        return getType(method.getReturnType());
    }

    /**
     * Computes the size of the arguments and of the return value of a method.
     * 
     * @param desc
     *            the descriptor of a method.
     * @return the size of the arguments of the method (plus one for the
     *         implicit this argument), argSize, and the size of its return
     *         value, retSize, packed into a single int i =
     *         <tt>(argSize &lt;&lt; 2) | retSize</tt> (argSize is therefore equal to
     *         <tt>i &gt;&gt; 2</tt>, and retSize to <tt>i &amp; 0x03</tt>).
     */
    public static int getArgumentsAndReturnSizes(final String desc) {
        int n = 1;
        int c = 1;
        while (true) {
            char car = desc.charAt(c++);
            if (car == ')') {
                car = desc.charAt(c);
                return n << 2
                        | (car == 'V' ? 0 : (car == 'D' || car == 'J' ? 2 : 1));
            } else if (car == 'L') {
                while (desc.charAt(c++) != ';') {
                }
                n += 1;
            } else if (car == '[') {
                while ((car = desc.charAt(c)) == '[') {
                    ++c;
                }
                if (car == 'D' || car == 'J') {
                    n -= 1;
                }
            } else if (car == 'D' || car == 'J') {
                n += 2;
            } else {
                n += 1;
            }
        }
    }

    /**
     * Returns the Java type corresponding to the given type descriptor. For
     * method descriptors, buf is supposed to contain nothing more than the
     * descriptor itself.
     * 
     * @param buf
     *            a buffer containing a type descriptor.
     * @param off
     *            the offset of this descriptor in the previous buffer.
     * @return the Java type corresponding to the given type descriptor.
     */
    private static Type getType(final char[] buf, final int off) {
        int len;
        switch (buf[off]) {
        case 'V':
            return VOID_TYPE;
        case 'Z':
            return BOOLEAN_TYPE;
        case 'C':
            return CHAR_TYPE;
        case 'B':
            return BYTE_TYPE;
        case 'S':
            return SHORT_TYPE;
        case 'I':
            return INT_TYPE;
        case 'F':
            return FLOAT_TYPE;
        case 'J':
            return LONG_TYPE;
        case 'D':
            return DOUBLE_TYPE;
        case '[':
            len = 1;
            while (buf[off + len] == '[') {
                ++len;
            }
            if (buf[off + len] == 'L') {
                ++len;
                while (buf[off + len] != ';') {
                    ++len;
                }
            }
            return new Type(ARRAY, buf, off, len + 1);
        case 'L':
            len = 1;
            while (buf[off + len] != ';') {
                ++len;
            }
            return new Type(OBJECT, buf, off + 1, len - 1);
            // case '(':
        default:
            return new Type(METHOD, buf, off, buf.length - off);
        }
    }

    // ------------------------------------------------------------------------
    // Accessors
    // ------------------------------------------------------------------------

    /**
     * Returns the sort of this Java type.
     * 
     * @return {@link #VOID VOID}, {@link #BOOLEAN BOOLEAN}, {@link #CHAR CHAR},
     *         {@link #BYTE BYTE}, {@link #SHORT SHORT}, {@link #INT INT},
     *         {@link #FLOAT FLOAT}, {@link #LONG LONG}, {@link #DOUBLE DOUBLE},
     *         {@link #ARRAY ARRAY}, {@link #OBJECT OBJECT} or {@link #METHOD
     *         METHOD}.
     */
    public int getSort() {
        return sort;
    }

    /**
     * Returns the number of dimensions of this array type. This method should
     * only be used for an array type.
     * 
     * @return the number of dimensions of this array type.
     */
    public int getDimensions() {
        int i = 1;
        while (buf[off + i] == '[') {
            ++i;
        }
        return i;
    }

    /**
     * Returns the type of the elements of this array type. This method should
     * only be used for an array type.
     * 
     * @return Returns the type of the elements of this array type.
     */
    public Type getElementType() {
        return getType(buf, off + getDimensions());
    }

    /**
     * Returns the binary name of the class corresponding to this type. This
     * method must not be used on method types.
     * 
     * @return the binary name of the class corresponding to this type.
     */
    public String getClassName() {
        switch (sort) {
        case VOID:
            return "void";
        case BOOLEAN:
            return "boolean";
        case CHAR:
            return "char";
        case BYTE:
            return "byte";
        case SHORT:
            return "short";
        case INT:
            return "int";
        case FLOAT:
            return "float";
        case LONG:
            return "long";
        case DOUBLE:
            return "double";
        case ARRAY:
            StringBuilder sb = new StringBuilder(getElementType().getClassName());
            for (int i = getDimensions(); i > 0; --i) {
                sb.append("[]");
            }
            return sb.toString();
        case OBJECT:
            return new String(buf, off, len).replace('/', '.');
        default:
            return null;
        }
    }

    /**
     * Returns the internal name of the class corresponding to this object or
     * array type. The internal name of a class is its fully qualified name (as
     * returned by Class.getName(), where '.' are replaced by '/'. This method
     * should only be used for an object or array type.
     * 
     * @return the internal name of the class corresponding to this object type.
     */
    public String getInternalName() {
        return new String(buf, off, len);
    }

    /**
     * Returns the argument types of methods of this type. This method should
     * only be used for method types.
     * 
     * @return the argument types of methods of this type.
     */
    public Type[] getArgumentTypes() {
        return getArgumentTypes(getDescriptor());
    }

    /**
     * Returns the return type of methods of this type. This method should only
     * be used for method types.
     * 
     * @return the return type of methods of this type.
     */
    public Type getReturnType() {
        return getReturnType(getDescriptor());
    }

    /**
     * Returns the size of the arguments and of the return value of methods of
     * this type. This method should only be used for method types.
     * 
     * @return the size of the arguments (plus one for the implicit this
     *         argument), argSize, and the size of the return value, retSize,
     *         packed into a single
     *         int i = <tt>(argSize &lt;&lt; 2) | retSize</tt>
     *         (argSize is therefore equal to <tt>i &gt;&gt; 2</tt>,
     *         and retSize to <tt>i &amp; 0x03</tt>).
     */
    public int getArgumentsAndReturnSizes() {
        return getArgumentsAndReturnSizes(getDescriptor());
    }

    // ------------------------------------------------------------------------
    // Conversion to type descriptors
    // ------------------------------------------------------------------------

    /**
     * Returns the descriptor corresponding to this Java type.
     * 
     * @return the descriptor corresponding to this Java type.
     */
    public String getDescriptor() {
        StringBuilder sb = new StringBuilder();
        getDescriptor(sb);
        return sb.toString();
    }

    /**
     * Returns the descriptor corresponding to the given argument and return
     * types.
     * 
     * @param returnType
     *            the return type of the method.
     * @param argumentTypes
     *            the argument types of the method.
     * @return the descriptor corresponding to the given argument and return
     *         types.
     */
    public static String getMethodDescriptor(final Type returnType,
            final Type... argumentTypes) {
		StringBuilder sb = new StringBuilder();
        sb.append('(');
        for (int i = 0; i < argumentTypes.length; ++i) {
            argumentTypes[i].getDescriptor(sb);
        }
        sb.append(')');
        returnType.getDescriptor(sb);
        return sb.toString();
    }

    /**
     * Appends the descriptor corresponding to this Java type to the given
     * string builder.
     * 
     * @param sb
     *            the string builder to which the descriptor must be appended.
     */
    private void getDescriptor(final StringBuilder sb) {
        if (this.buf == null) {
            // descriptor is in byte 3 of 'off' for primitive types (buf ==
            // null)
            sb.append((char) ((off & 0xFF000000) >>> 24));
        } else if (sort == OBJECT) {
            sb.append('L');
            sb.append(this.buf, off, len);
            sb.append(';');
        } else { // sort == ARRAY || sort == METHOD
            sb.append(this.buf, off, len);
        }
    }

    // ------------------------------------------------------------------------
    // Direct conversion from classes to type descriptors,
    // without intermediate Type objects
    // ------------------------------------------------------------------------

    /**
     * Returns the internal name of the given class. The internal name of a
     * class is its fully qualified name, as returned by Class.getName(), where
     * '.' are replaced by '/'.
     * 
     * @param c
     *            an object or array class.
     * @return the internal name of the given class.
     */
    public static String getInternalName(final Class<?> c) {
        return c.getName().replace('.', '/');
    }

    /**
     * Returns the descriptor corresponding to the given Java type.
     * 
     * @param c
     *            an object class, a primitive class or an array class.
     * @return the descriptor corresponding to the given class.
     */
    public static String getDescriptor(final Class<?> c) {
		StringBuilder sb = new StringBuilder();
        getDescriptor(sb, c);
        return sb.toString();
    }

    /**
     * Returns the descriptor corresponding to the given constructor.
     * 
     * @param c
     *            a {@link Constructor Constructor} object.
     * @return the descriptor of the given constructor.
     */
    public static String getConstructorDescriptor(final Constructor<?> c) {
        Class<?>[] parameters = c.getParameterTypes();
		StringBuilder sb = new StringBuilder();
        sb.append('(');
        for (int i = 0; i < parameters.length; ++i) {
            getDescriptor(sb, parameters[i]);
        }
        return sb.append(")V").toString();
    }

    /**
     * Returns the descriptor corresponding to the given method.
     * 
     * @param m
     *            a {@link Method Method} object.
     * @return the descriptor of the given method.
     */
    public static String getMethodDescriptor(final Method m) {
        Class<?>[] parameters = m.getParameterTypes();
		StringBuilder sb = new StringBuilder();
        sb.append('(');
        for (int i = 0; i < parameters.length; ++i) {
            getDescriptor(sb, parameters[i]);
        }
        sb.append(')');
        getDescriptor(sb, m.getReturnType());
        return sb.toString();
    }

    /**
     * Appends the descriptor of the given class to the given string builder.
     * 
     * @param sb
     *            the string buffer to which the descriptor must be appended.
     * @param c
     *            the class whose descriptor must be computed.
     */
    private static void getDescriptor(final StringBuilder sb, final Class<?> c) {
        Class<?> d = c;
        while (true) {
            if (d.isPrimitive()) {
                char car;
                if (d == Integer.TYPE) {
                    car = 'I';
                } else if (d == Void.TYPE) {
                    car = 'V';
                } else if (d == Boolean.TYPE) {
                    car = 'Z';
                } else if (d == Byte.TYPE) {
                    car = 'B';
                } else if (d == Character.TYPE) {
                    car = 'C';
                } else if (d == Short.TYPE) {
                    car = 'S';
                } else if (d == Double.TYPE) {
                    car = 'D';
                } else if (d == Float.TYPE) {
                    car = 'F';
                } else /* if (d == Long.TYPE) */{
                    car = 'J';
                }
                sb.append(car);
                return;
            } else if (d.isArray()) {
                sb.append('[');
                d = d.getComponentType();
            } else {
                sb.append('L');
                String name = d.getName();
                int len = name.length();
                for (int i = 0; i < len; ++i) {
                    char car = name.charAt(i);
                    sb.append(car == '.' ? '/' : car);
                }
                sb.append(';');
                return;
            }
        }
    }

    // ------------------------------------------------------------------------
    // Corresponding size and opcodes
    // ------------------------------------------------------------------------

    /**
     * Returns the size of values of this type. This method must not be used for
     * method types.
     * 
     * @return the size of values of this type, i.e., 2 for <tt>long</tt> and
     *         <tt>double</tt>, 0 for <tt>void</tt> and 1 otherwise.
     */
    public int getSize() {
        // the size is in byte 0 of 'off' for primitive types (buf == null)
        return buf == null ? (off & 0xFF) : 1;
    }

    /**
     * Returns a JVM instruction opcode adapted to this Java type. This method
     * must not be used for method types.
     * 
     * @param opcode
     *            a JVM instruction opcode. This opcode must be one of ILOAD,
     *            ISTORE, IALOAD, IASTORE, IADD, ISUB, IMUL, IDIV, IREM, INEG,
     *            ISHL, ISHR, IUSHR, IAND, IOR, IXOR and IRETURN.
     * @return an opcode that is similar to the given opcode, but adapted to
     *         this Java type. For example, if this type is <tt>float</tt> and
     *         <tt>opcode</tt> is IRETURN, this method returns FRETURN.
     */
    public int getOpcode(final int opcode) {
        if (opcode == Opcodes.IALOAD || opcode == Opcodes.IASTORE) {
            // the offset for IALOAD or IASTORE is in byte 1 of 'off' for
            // primitive types (buf == null)
            return opcode + (buf == null ? (off & 0xFF00) >> 8 : 4);
        } else {
            // the offset for other instructions is in byte 2 of 'off' for
            // primitive types (buf == null)
            return opcode + (buf == null ? (off & 0xFF0000) >> 16 : 4);
        }
    }

    // ------------------------------------------------------------------------
    // Equals, hashCode and toString
    // ------------------------------------------------------------------------

    /**
     * Tests if the given object is equal to this type.
     * 
     * @param o
     *            the object to be compared to this type.
     * @return <tt>true</tt> if the given object is equal to this type.
     */
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Type)) {
            return false;
        }
        Type t = (Type) o;
        if (sort != t.sort) {
            return false;
        }
        if (sort >= ARRAY) {
            if (len != t.len) {
                return false;
            }
            for (int i = off, j = t.off, end = i + len; i < end; i++, j++) {
                if (buf[i] != t.buf[j]) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Returns a hash code value for this type.
     * 
     * @return a hash code value for this type.
     */
    @Override
    public int hashCode() {
        int hc = 13 * sort;
        if (sort >= ARRAY) {
            for (int i = off, end = i + len; i < end; i++) {
                hc = 17 * (hc + buf[i]);
            }
        }
        return hc;
    }

    /**
     * Returns a string representation of this type.
     * 
     * @return the descriptor of this type.
     */
    @Override
    public String toString() {
        return getDescriptor();
    }
}
