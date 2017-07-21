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

import java.io.IOException;
import java.io.InputStream;

/**
 * A Java class parser to make a {@link ClassVisitor} visit an existing class.
 * This class parses a byte array conforming to the Java class file format and
 * calls the appropriate visit methods of a given class visitor for each field,
 * method and bytecode instruction encountered.
 * 
 * @author Eric Bruneton
 * @author Eugene Kuleshov
 */
public class ClassReader {

    /**
     * True to enable signatures support.
     */
    static final boolean SIGNATURES = true;

    /**
     * True to enable annotations support.
     */
    static final boolean ANNOTATIONS = true;

    /**
     * True to enable stack map frames support.
     */
    static final boolean FRAMES = true;

    /**
     * True to enable bytecode writing support.
     */
    static final boolean WRITER = true;

    /**
     * True to enable JSR_W and GOTO_W support.
     */
    static final boolean RESIZE = true;

    /**
     * Flag to skip method code. If this class is set <code>CODE</code>
     * attribute won't be visited. This can be used, for example, to retrieve
     * annotations for methods and method parameters.
     */
    public static final int SKIP_CODE = 1;

    /**
     * Flag to skip the debug information in the class. If this flag is set the
     * debug information of the class is not visited, i.e. the
     * {@link MethodVisitor#visitLocalVariable visitLocalVariable} and
     * {@link MethodVisitor#visitLineNumber visitLineNumber} methods will not be
     * called.
     */
    public static final int SKIP_DEBUG = 2;

    /**
     * Flag to skip the stack map frames in the class. If this flag is set the
     * stack map frames of the class is not visited, i.e. the
     * {@link MethodVisitor#visitFrame visitFrame} method will not be called.
     * This flag is useful when the {@link ClassWriter#COMPUTE_FRAMES} option is
     * used: it avoids visiting frames that will be ignored and recomputed from
     * scratch in the class writer.
     */
    public static final int SKIP_FRAMES = 4;

    /**
     * Flag to expand the stack map frames. By default stack map frames are
     * visited in their original format (i.e. "expanded" for classes whose
     * version is less than V1_6, and "compressed" for the other classes). If
     * this flag is set, stack map frames are always visited in expanded format
     * (this option adds a decompression/recompression step in ClassReader and
     * ClassWriter which degrades performances quite a lot).
     */
    public static final int EXPAND_FRAMES = 8;

    /**
     * Flag to expand the ASM pseudo instructions into an equivalent sequence of
     * standard bytecode instructions. When resolving a forward jump it may
     * happen that the signed 2 bytes offset reserved for it is not sufficient
     * to store the bytecode offset. In this case the jump instruction is
     * replaced with a temporary ASM pseudo instruction using an unsigned 2
     * bytes offset (see Label#resolve). This internal flag is used to re-read
     * classes containing such instructions, in order to replace them with
     * standard instructions. In addition, when this flag is used, GOTO_W and
     * JSR_W are <i>not</i> converted into GOTO and JSR, to make sure that
     * infinite loops where a GOTO_W is replaced with a GOTO in ClassReader and
     * converted back to a GOTO_W in ClassWriter cannot occur.
     */
    static final int EXPAND_ASM_INSNS = 256;

    /**
     * The class to be parsed. <i>The content of this array must not be
     * modified. This field is intended for {@link Attribute} sub classes, and
     * is normally not needed by class generators or adapters.</i>
     */
    public final byte[] b;

    /**
     * The start index of each constant pool item in {@link #b b}, plus one. The
     * one byte offset skips the constant pool item tag that indicates its type.
     */
    private final int[] items;

    /**
     * The String objects corresponding to the CONSTANT_Utf8 items. This cache
     * avoids multiple parsing of a given CONSTANT_Utf8 constant pool item,
     * which GREATLY improves performances (by a factor 2 to 3). This caching
     * strategy could be extended to all constant pool items, but its benefit
     * would not be so great for these items (because they are much less
     * expensive to parse than CONSTANT_Utf8 items).
     */
    private final String[] strings;

    /**
     * Maximum length of the strings contained in the constant pool of the
     * class.
     */
    private final int maxStringLength;

    /**
     * Start index of the class header information (access, name...) in
     * {@link #b b}.
     */
    public final int header;

    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------

    /**
     * Constructs a new {@link ClassReader} object.
     * 
     * @param b
     *            the bytecode of the class to be read.
     */
    public ClassReader(final byte[] b) {
        this(b, 0, b.length);
    }

    /**
     * Constructs a new {@link ClassReader} object.
     * 
     * @param b
     *            the bytecode of the class to be read.
     * @param off
     *            the start offset of the class data.
     * @param len
     *            the length of the class data.
     */
    public ClassReader(final byte[] b, final int off, final int len) {
        this.b = b;
        // checks the class version
		/* SPRING PATCH: REMOVED FOR FORWARD COMPATIBILITY WITH JDK 9
        if (readShort(off + 6) > Opcodes.V1_8) {
            throw new IllegalArgumentException();
        }
		*/
        // parses the constant pool
        items = new int[readUnsignedShort(off + 8)];
        int n = items.length;
        strings = new String[n];
        int max = 0;
        int index = off + 10;
        for (int i = 1; i < n; ++i) {
            items[i] = index + 1;
            int size;
            switch (b[index]) {
            case ClassWriter.FIELD:
            case ClassWriter.METH:
            case ClassWriter.IMETH:
            case ClassWriter.INT:
            case ClassWriter.FLOAT:
            case ClassWriter.NAME_TYPE:
            case ClassWriter.INDY:
                size = 5;
                break;
            case ClassWriter.LONG:
            case ClassWriter.DOUBLE:
                size = 9;
                ++i;
                break;
            case ClassWriter.UTF8:
                size = 3 + readUnsignedShort(index + 1);
                if (size > max) {
                    max = size;
                }
                break;
            case ClassWriter.HANDLE:
                size = 4;
                break;
            // case ClassWriter.CLASS:
            // case ClassWriter.STR:
            // case ClassWriter.MTYPE
            default:
                size = 3;
                break;
            }
            index += size;
        }
        maxStringLength = max;
        // the class header information starts just after the constant pool
        header = index;
    }

    /**
     * Returns the class's access flags (see {@link Opcodes}). This value may
     * not reflect Deprecated and Synthetic flags when bytecode is before 1.5
     * and those flags are represented by attributes.
     * 
     * @return the class access flags
     * 
     * @see ClassVisitor#visit(int, int, String, String, String, String[])
     */
    public int getAccess() {
        return readUnsignedShort(header);
    }

    /**
     * Returns the internal name of the class (see
     * {@link Type#getInternalName() getInternalName}).
     * 
     * @return the internal class name
     * 
     * @see ClassVisitor#visit(int, int, String, String, String, String[])
     */
    public String getClassName() {
        return readClass(header + 2, new char[maxStringLength]);
    }

    /**
     * Returns the internal of name of the super class (see
     * {@link Type#getInternalName() getInternalName}). For interfaces, the
     * super class is {@link Object}.
     * 
     * @return the internal name of super class, or <tt>null</tt> for
     *         {@link Object} class.
     * 
     * @see ClassVisitor#visit(int, int, String, String, String, String[])
     */
    public String getSuperName() {
        return readClass(header + 4, new char[maxStringLength]);
    }

    /**
     * Returns the internal names of the class's interfaces (see
     * {@link Type#getInternalName() getInternalName}).
     * 
     * @return the array of internal names for all implemented interfaces or
     *         <tt>null</tt>.
     * 
     * @see ClassVisitor#visit(int, int, String, String, String, String[])
     */
    public String[] getInterfaces() {
        int index = header + 6;
        int n = readUnsignedShort(index);
        String[] interfaces = new String[n];
        if (n > 0) {
            char[] buf = new char[maxStringLength];
            for (int i = 0; i < n; ++i) {
                index += 2;
                interfaces[i] = readClass(index, buf);
            }
        }
        return interfaces;
    }

    /**
     * Copies the constant pool data into the given {@link ClassWriter}. Should
     * be called before the {@link #accept(ClassVisitor,int)} method.
     * 
     * @param classWriter
     *            the {@link ClassWriter} to copy constant pool into.
     */
    void copyPool(final ClassWriter classWriter) {
        char[] buf = new char[maxStringLength];
        int ll = items.length;
        Item[] items2 = new Item[ll];
        for (int i = 1; i < ll; i++) {
            int index = items[i];
            int tag = b[index - 1];
            Item item = new Item(i);
            int nameType;
            switch (tag) {
            case ClassWriter.FIELD:
            case ClassWriter.METH:
            case ClassWriter.IMETH:
                nameType = items[readUnsignedShort(index + 2)];
                item.set(tag, readClass(index, buf), readUTF8(nameType, buf),
                        readUTF8(nameType + 2, buf));
                break;
            case ClassWriter.INT:
                item.set(readInt(index));
                break;
            case ClassWriter.FLOAT:
                item.set(Float.intBitsToFloat(readInt(index)));
                break;
            case ClassWriter.NAME_TYPE:
                item.set(tag, readUTF8(index, buf), readUTF8(index + 2, buf),
                        null);
                break;
            case ClassWriter.LONG:
                item.set(readLong(index));
                ++i;
                break;
            case ClassWriter.DOUBLE:
                item.set(Double.longBitsToDouble(readLong(index)));
                ++i;
                break;
            case ClassWriter.UTF8: {
                String s = strings[i];
                if (s == null) {
                    index = items[i];
                    s = strings[i] = readUTF(index + 2,
                            readUnsignedShort(index), buf);
                }
                item.set(tag, s, null, null);
                break;
            }
            case ClassWriter.HANDLE: {
                int fieldOrMethodRef = items[readUnsignedShort(index + 1)];
                nameType = items[readUnsignedShort(fieldOrMethodRef + 2)];
                item.set(ClassWriter.HANDLE_BASE + readByte(index),
                        readClass(fieldOrMethodRef, buf),
                        readUTF8(nameType, buf), readUTF8(nameType + 2, buf));
                break;
            }
            case ClassWriter.INDY:
                if (classWriter.bootstrapMethods == null) {
                    copyBootstrapMethods(classWriter, items2, buf);
                }
                nameType = items[readUnsignedShort(index + 2)];
                item.set(readUTF8(nameType, buf), readUTF8(nameType + 2, buf),
                        readUnsignedShort(index));
                break;
            // case ClassWriter.STR:
            // case ClassWriter.CLASS:
            // case ClassWriter.MTYPE
            default:
                item.set(tag, readUTF8(index, buf), null, null);
                break;
            }

            int index2 = item.hashCode % items2.length;
            item.next = items2[index2];
            items2[index2] = item;
        }

        int off = items[1] - 1;
        classWriter.pool.putByteArray(b, off, header - off);
        classWriter.items = items2;
        classWriter.threshold = (int) (0.75d * ll);
        classWriter.index = ll;
    }

    /**
     * Copies the bootstrap method data into the given {@link ClassWriter}.
     * Should be called before the {@link #accept(ClassVisitor,int)} method.
     * 
     * @param classWriter
     *            the {@link ClassWriter} to copy bootstrap methods into.
     */
    private void copyBootstrapMethods(final ClassWriter classWriter,
            final Item[] items, final char[] c) {
        // finds the "BootstrapMethods" attribute
        int u = getAttributes();
        boolean found = false;
        for (int i = readUnsignedShort(u); i > 0; --i) {
            String attrName = readUTF8(u + 2, c);
            if ("BootstrapMethods".equals(attrName)) {
                found = true;
                break;
            }
            u += 6 + readInt(u + 4);
        }
        if (!found) {
            return;
        }
        // copies the bootstrap methods in the class writer
        int boostrapMethodCount = readUnsignedShort(u + 8);
        for (int j = 0, v = u + 10; j < boostrapMethodCount; j++) {
            int position = v - u - 10;
            int hashCode = readConst(readUnsignedShort(v), c).hashCode();
            for (int k = readUnsignedShort(v + 2); k > 0; --k) {
                hashCode ^= readConst(readUnsignedShort(v + 4), c).hashCode();
                v += 2;
            }
            v += 4;
            Item item = new Item(j);
            item.set(position, hashCode & 0x7FFFFFFF);
            int index = item.hashCode % items.length;
            item.next = items[index];
            items[index] = item;
        }
        int attrSize = readInt(u + 4);
        ByteVector bootstrapMethods = new ByteVector(attrSize + 62);
        bootstrapMethods.putByteArray(b, u + 10, attrSize - 2);
        classWriter.bootstrapMethodsCount = boostrapMethodCount;
        classWriter.bootstrapMethods = bootstrapMethods;
    }

    /**
     * Constructs a new {@link ClassReader} object.
     * 
     * @param is
     *            an input stream from which to read the class.
     * @throws IOException
     *             if a problem occurs during reading.
     */
    public ClassReader(final InputStream is) throws IOException {
        this(readClass(is, false));
    }

    /**
     * Constructs a new {@link ClassReader} object.
     * 
     * @param name
     *            the binary qualified name of the class to be read.
     * @throws IOException
     *             if an exception occurs during reading.
     */
    public ClassReader(final String name) throws IOException {
        this(readClass(
                ClassLoader.getSystemResourceAsStream(name.replace('.', '/')
                        + ".class"), true));
    }

    /**
     * Reads the bytecode of a class.
     * 
     * @param is
     *            an input stream from which to read the class.
     * @param close
     *            true to close the input stream after reading.
     * @return the bytecode read from the given input stream.
     * @throws IOException
     *             if a problem occurs during reading.
     */
    private static byte[] readClass(final InputStream is, boolean close)
            throws IOException {
        if (is == null) {
            throw new IOException("Class not found");
        }
        try {
            byte[] b = new byte[is.available()];
            int len = 0;
            while (true) {
                int n = is.read(b, len, b.length - len);
                if (n == -1) {
                    if (len < b.length) {
                        byte[] c = new byte[len];
                        System.arraycopy(b, 0, c, 0, len);
                        b = c;
                    }
                    return b;
                }
                len += n;
                if (len == b.length) {
                    int last = is.read();
                    if (last < 0) {
                        return b;
                    }
                    byte[] c = new byte[b.length + 1000];
                    System.arraycopy(b, 0, c, 0, len);
                    c[len++] = (byte) last;
                    b = c;
                }
            }
        } finally {
            if (close) {
                is.close();
            }
        }
    }

    // ------------------------------------------------------------------------
    // Public methods
    // ------------------------------------------------------------------------

    /**
     * Makes the given visitor visit the Java class of this {@link ClassReader}
     * . This class is the one specified in the constructor (see
     * {@link #ClassReader(byte[]) ClassReader}).
     * 
     * @param classVisitor
     *            the visitor that must visit this class.
     * @param flags
     *            option flags that can be used to modify the default behavior
     *            of this class. See {@link #SKIP_DEBUG}, {@link #EXPAND_FRAMES}
     *            , {@link #SKIP_FRAMES}, {@link #SKIP_CODE}.
     */
    public void accept(final ClassVisitor classVisitor, final int flags) {
        accept(classVisitor, new Attribute[0], flags);
    }

    /**
     * Makes the given visitor visit the Java class of this {@link ClassReader}.
     * This class is the one specified in the constructor (see
     * {@link #ClassReader(byte[]) ClassReader}).
     * 
     * @param classVisitor
     *            the visitor that must visit this class.
     * @param attrs
     *            prototypes of the attributes that must be parsed during the
     *            visit of the class. Any attribute whose type is not equal to
     *            the type of one the prototypes will not be parsed: its byte
     *            array value will be passed unchanged to the ClassWriter.
     *            <i>This may corrupt it if this value contains references to
     *            the constant pool, or has syntactic or semantic links with a
     *            class element that has been transformed by a class adapter
     *            between the reader and the writer</i>.
     * @param flags
     *            option flags that can be used to modify the default behavior
     *            of this class. See {@link #SKIP_DEBUG}, {@link #EXPAND_FRAMES}
     *            , {@link #SKIP_FRAMES}, {@link #SKIP_CODE}.
     */
    public void accept(final ClassVisitor classVisitor,
            final Attribute[] attrs, final int flags) {
        int u = header; // current offset in the class file
        char[] c = new char[maxStringLength]; // buffer used to read strings

        Context context = new Context();
        context.attrs = attrs;
        context.flags = flags;
        context.buffer = c;

        // reads the class declaration
        int access = readUnsignedShort(u);
        String name = readClass(u + 2, c);
        String superClass = readClass(u + 4, c);
        String[] interfaces = new String[readUnsignedShort(u + 6)];
        u += 8;
        for (int i = 0; i < interfaces.length; ++i) {
            interfaces[i] = readClass(u, c);
            u += 2;
        }

        // reads the class attributes
        String signature = null;
        String sourceFile = null;
        String sourceDebug = null;
        String enclosingOwner = null;
        String enclosingName = null;
        String enclosingDesc = null;
        int anns = 0;
        int ianns = 0;
        int tanns = 0;
        int itanns = 0;
        int innerClasses = 0;
        Attribute attributes = null;

        u = getAttributes();
        for (int i = readUnsignedShort(u); i > 0; --i) {
            String attrName = readUTF8(u + 2, c);
            // tests are sorted in decreasing frequency order
            // (based on frequencies observed on typical classes)
            if ("SourceFile".equals(attrName)) {
                sourceFile = readUTF8(u + 8, c);
            } else if ("InnerClasses".equals(attrName)) {
                innerClasses = u + 8;
            } else if ("EnclosingMethod".equals(attrName)) {
                enclosingOwner = readClass(u + 8, c);
                int item = readUnsignedShort(u + 10);
                if (item != 0) {
                    enclosingName = readUTF8(items[item], c);
                    enclosingDesc = readUTF8(items[item] + 2, c);
                }
            } else if (SIGNATURES && "Signature".equals(attrName)) {
                signature = readUTF8(u + 8, c);
            } else if (ANNOTATIONS
                    && "RuntimeVisibleAnnotations".equals(attrName)) {
                anns = u + 8;
            } else if (ANNOTATIONS
                    && "RuntimeVisibleTypeAnnotations".equals(attrName)) {
                tanns = u + 8;
            } else if ("Deprecated".equals(attrName)) {
                access |= Opcodes.ACC_DEPRECATED;
            } else if ("Synthetic".equals(attrName)) {
                access |= Opcodes.ACC_SYNTHETIC
                        | ClassWriter.ACC_SYNTHETIC_ATTRIBUTE;
            } else if ("SourceDebugExtension".equals(attrName)) {
                int len = readInt(u + 4);
                sourceDebug = readUTF(u + 8, len, new char[len]);
            } else if (ANNOTATIONS
                    && "RuntimeInvisibleAnnotations".equals(attrName)) {
                ianns = u + 8;
            } else if (ANNOTATIONS
                    && "RuntimeInvisibleTypeAnnotations".equals(attrName)) {
                itanns = u + 8;
            } else if ("BootstrapMethods".equals(attrName)) {
                int[] bootstrapMethods = new int[readUnsignedShort(u + 8)];
                for (int j = 0, v = u + 10; j < bootstrapMethods.length; j++) {
                    bootstrapMethods[j] = v;
                    v += 2 + readUnsignedShort(v + 2) << 1;
                }
                context.bootstrapMethods = bootstrapMethods;
            } else {
                Attribute attr = readAttribute(attrs, attrName, u + 8,
                        readInt(u + 4), c, -1, null);
                if (attr != null) {
                    attr.next = attributes;
                    attributes = attr;
                }
            }
            u += 6 + readInt(u + 4);
        }

        // visits the class declaration
        classVisitor.visit(readInt(items[1] - 7), access, name, signature,
                superClass, interfaces);

        // visits the source and debug info
        if ((flags & SKIP_DEBUG) == 0
                && (sourceFile != null || sourceDebug != null)) {
            classVisitor.visitSource(sourceFile, sourceDebug);
        }

        // visits the outer class
        if (enclosingOwner != null) {
            classVisitor.visitOuterClass(enclosingOwner, enclosingName,
                    enclosingDesc);
        }

        // visits the class annotations and type annotations
        if (ANNOTATIONS && anns != 0) {
            for (int i = readUnsignedShort(anns), v = anns + 2; i > 0; --i) {
                v = readAnnotationValues(v + 2, c, true,
                        classVisitor.visitAnnotation(readUTF8(v, c), true));
            }
        }
        if (ANNOTATIONS && ianns != 0) {
            for (int i = readUnsignedShort(ianns), v = ianns + 2; i > 0; --i) {
                v = readAnnotationValues(v + 2, c, true,
                        classVisitor.visitAnnotation(readUTF8(v, c), false));
            }
        }
        if (ANNOTATIONS && tanns != 0) {
            for (int i = readUnsignedShort(tanns), v = tanns + 2; i > 0; --i) {
                v = readAnnotationTarget(context, v);
                v = readAnnotationValues(v + 2, c, true,
                        classVisitor.visitTypeAnnotation(context.typeRef,
                                context.typePath, readUTF8(v, c), true));
            }
        }
        if (ANNOTATIONS && itanns != 0) {
            for (int i = readUnsignedShort(itanns), v = itanns + 2; i > 0; --i) {
                v = readAnnotationTarget(context, v);
                v = readAnnotationValues(v + 2, c, true,
                        classVisitor.visitTypeAnnotation(context.typeRef,
                                context.typePath, readUTF8(v, c), false));
            }
        }

        // visits the attributes
        while (attributes != null) {
            Attribute attr = attributes.next;
            attributes.next = null;
            classVisitor.visitAttribute(attributes);
            attributes = attr;
        }

        // visits the inner classes
        if (innerClasses != 0) {
            int v = innerClasses + 2;
            for (int i = readUnsignedShort(innerClasses); i > 0; --i) {
                classVisitor.visitInnerClass(readClass(v, c),
                        readClass(v + 2, c), readUTF8(v + 4, c),
                        readUnsignedShort(v + 6));
                v += 8;
            }
        }

        // visits the fields and methods
        u = header + 10 + 2 * interfaces.length;
        for (int i = readUnsignedShort(u - 2); i > 0; --i) {
            u = readField(classVisitor, context, u);
        }
        u += 2;
        for (int i = readUnsignedShort(u - 2); i > 0; --i) {
            u = readMethod(classVisitor, context, u);
        }

        // visits the end of the class
        classVisitor.visitEnd();
    }

    /**
     * Reads a field and makes the given visitor visit it.
     * 
     * @param classVisitor
     *            the visitor that must visit the field.
     * @param context
     *            information about the class being parsed.
     * @param u
     *            the start offset of the field in the class file.
     * @return the offset of the first byte following the field in the class.
     */
    private int readField(final ClassVisitor classVisitor,
            final Context context, int u) {
        // reads the field declaration
        char[] c = context.buffer;
        int access = readUnsignedShort(u);
        String name = readUTF8(u + 2, c);
        String desc = readUTF8(u + 4, c);
        u += 6;

        // reads the field attributes
        String signature = null;
        int anns = 0;
        int ianns = 0;
        int tanns = 0;
        int itanns = 0;
        Object value = null;
        Attribute attributes = null;

        for (int i = readUnsignedShort(u); i > 0; --i) {
            String attrName = readUTF8(u + 2, c);
            // tests are sorted in decreasing frequency order
            // (based on frequencies observed on typical classes)
            if ("ConstantValue".equals(attrName)) {
                int item = readUnsignedShort(u + 8);
                value = item == 0 ? null : readConst(item, c);
            } else if (SIGNATURES && "Signature".equals(attrName)) {
                signature = readUTF8(u + 8, c);
            } else if ("Deprecated".equals(attrName)) {
                access |= Opcodes.ACC_DEPRECATED;
            } else if ("Synthetic".equals(attrName)) {
                access |= Opcodes.ACC_SYNTHETIC
                        | ClassWriter.ACC_SYNTHETIC_ATTRIBUTE;
            } else if (ANNOTATIONS
                    && "RuntimeVisibleAnnotations".equals(attrName)) {
                anns = u + 8;
            } else if (ANNOTATIONS
                    && "RuntimeVisibleTypeAnnotations".equals(attrName)) {
                tanns = u + 8;
            } else if (ANNOTATIONS
                    && "RuntimeInvisibleAnnotations".equals(attrName)) {
                ianns = u + 8;
            } else if (ANNOTATIONS
                    && "RuntimeInvisibleTypeAnnotations".equals(attrName)) {
                itanns = u + 8;
            } else {
                Attribute attr = readAttribute(context.attrs, attrName, u + 8,
                        readInt(u + 4), c, -1, null);
                if (attr != null) {
                    attr.next = attributes;
                    attributes = attr;
                }
            }
            u += 6 + readInt(u + 4);
        }
        u += 2;

        // visits the field declaration
        FieldVisitor fv = classVisitor.visitField(access, name, desc,
                signature, value);
        if (fv == null) {
            return u;
        }

        // visits the field annotations and type annotations
        if (ANNOTATIONS && anns != 0) {
            for (int i = readUnsignedShort(anns), v = anns + 2; i > 0; --i) {
                v = readAnnotationValues(v + 2, c, true,
                        fv.visitAnnotation(readUTF8(v, c), true));
            }
        }
        if (ANNOTATIONS && ianns != 0) {
            for (int i = readUnsignedShort(ianns), v = ianns + 2; i > 0; --i) {
                v = readAnnotationValues(v + 2, c, true,
                        fv.visitAnnotation(readUTF8(v, c), false));
            }
        }
        if (ANNOTATIONS && tanns != 0) {
            for (int i = readUnsignedShort(tanns), v = tanns + 2; i > 0; --i) {
                v = readAnnotationTarget(context, v);
                v = readAnnotationValues(v + 2, c, true,
                        fv.visitTypeAnnotation(context.typeRef,
                                context.typePath, readUTF8(v, c), true));
            }
        }
        if (ANNOTATIONS && itanns != 0) {
            for (int i = readUnsignedShort(itanns), v = itanns + 2; i > 0; --i) {
                v = readAnnotationTarget(context, v);
                v = readAnnotationValues(v + 2, c, true,
                        fv.visitTypeAnnotation(context.typeRef,
                                context.typePath, readUTF8(v, c), false));
            }
        }

        // visits the field attributes
        while (attributes != null) {
            Attribute attr = attributes.next;
            attributes.next = null;
            fv.visitAttribute(attributes);
            attributes = attr;
        }

        // visits the end of the field
        fv.visitEnd();

        return u;
    }

    /**
     * Reads a method and makes the given visitor visit it.
     * 
     * @param classVisitor
     *            the visitor that must visit the method.
     * @param context
     *            information about the class being parsed.
     * @param u
     *            the start offset of the method in the class file.
     * @return the offset of the first byte following the method in the class.
     */
    private int readMethod(final ClassVisitor classVisitor,
            final Context context, int u) {
        // reads the method declaration
        char[] c = context.buffer;
        context.access = readUnsignedShort(u);
        context.name = readUTF8(u + 2, c);
        context.desc = readUTF8(u + 4, c);
        u += 6;

        // reads the method attributes
        int code = 0;
        int exception = 0;
        String[] exceptions = null;
        String signature = null;
        int methodParameters = 0;
        int anns = 0;
        int ianns = 0;
        int tanns = 0;
        int itanns = 0;
        int dann = 0;
        int mpanns = 0;
        int impanns = 0;
        int firstAttribute = u;
        Attribute attributes = null;

        for (int i = readUnsignedShort(u); i > 0; --i) {
            String attrName = readUTF8(u + 2, c);
            // tests are sorted in decreasing frequency order
            // (based on frequencies observed on typical classes)
            if ("Code".equals(attrName)) {
                if ((context.flags & SKIP_CODE) == 0) {
                    code = u + 8;
                }
            } else if ("Exceptions".equals(attrName)) {
                exceptions = new String[readUnsignedShort(u + 8)];
                exception = u + 10;
                for (int j = 0; j < exceptions.length; ++j) {
                    exceptions[j] = readClass(exception, c);
                    exception += 2;
                }
            } else if (SIGNATURES && "Signature".equals(attrName)) {
                signature = readUTF8(u + 8, c);
            } else if ("Deprecated".equals(attrName)) {
                context.access |= Opcodes.ACC_DEPRECATED;
            } else if (ANNOTATIONS
                    && "RuntimeVisibleAnnotations".equals(attrName)) {
                anns = u + 8;
            } else if (ANNOTATIONS
                    && "RuntimeVisibleTypeAnnotations".equals(attrName)) {
                tanns = u + 8;
            } else if (ANNOTATIONS && "AnnotationDefault".equals(attrName)) {
                dann = u + 8;
            } else if ("Synthetic".equals(attrName)) {
                context.access |= Opcodes.ACC_SYNTHETIC
                        | ClassWriter.ACC_SYNTHETIC_ATTRIBUTE;
            } else if (ANNOTATIONS
                    && "RuntimeInvisibleAnnotations".equals(attrName)) {
                ianns = u + 8;
            } else if (ANNOTATIONS
                    && "RuntimeInvisibleTypeAnnotations".equals(attrName)) {
                itanns = u + 8;
            } else if (ANNOTATIONS
                    && "RuntimeVisibleParameterAnnotations".equals(attrName)) {
                mpanns = u + 8;
            } else if (ANNOTATIONS
                    && "RuntimeInvisibleParameterAnnotations".equals(attrName)) {
                impanns = u + 8;
            } else if ("MethodParameters".equals(attrName)) {
                methodParameters = u + 8;
            } else {
                Attribute attr = readAttribute(context.attrs, attrName, u + 8,
                        readInt(u + 4), c, -1, null);
                if (attr != null) {
                    attr.next = attributes;
                    attributes = attr;
                }
            }
            u += 6 + readInt(u + 4);
        }
        u += 2;

        // visits the method declaration
        MethodVisitor mv = classVisitor.visitMethod(context.access,
                context.name, context.desc, signature, exceptions);
        if (mv == null) {
            return u;
        }

        /*
         * if the returned MethodVisitor is in fact a MethodWriter, it means
         * there is no method adapter between the reader and the writer. If, in
         * addition, the writer's constant pool was copied from this reader
         * (mw.cw.cr == this), and the signature and exceptions of the method
         * have not been changed, then it is possible to skip all visit events
         * and just copy the original code of the method to the writer (the
         * access, name and descriptor can have been changed, this is not
         * important since they are not copied as is from the reader).
         */
        if (WRITER && mv instanceof MethodWriter) {
            MethodWriter mw = (MethodWriter) mv;
            if (mw.cw.cr == this &&
					(signature != null ? signature.equals(mw.signature) : mw.signature == null)) {
                boolean sameExceptions = false;
                if (exceptions == null) {
                    sameExceptions = mw.exceptionCount == 0;
                } else if (exceptions.length == mw.exceptionCount) {
                    sameExceptions = true;
                    for (int j = exceptions.length - 1; j >= 0; --j) {
                        exception -= 2;
                        if (mw.exceptions[j] != readUnsignedShort(exception)) {
                            sameExceptions = false;
                            break;
                        }
                    }
                }
                if (sameExceptions) {
                    /*
                     * we do not copy directly the code into MethodWriter to
                     * save a byte array copy operation. The real copy will be
                     * done in ClassWriter.toByteArray().
                     */
                    mw.classReaderOffset = firstAttribute;
                    mw.classReaderLength = u - firstAttribute;
                    return u;
                }
            }
        }

        // visit the method parameters
        if (methodParameters != 0) {
            for (int i = b[methodParameters] & 0xFF, v = methodParameters + 1; i > 0; --i, v = v + 4) {
                mv.visitParameter(readUTF8(v, c), readUnsignedShort(v + 2));
            }
        }

        // visits the method annotations
        if (ANNOTATIONS && dann != 0) {
            AnnotationVisitor dv = mv.visitAnnotationDefault();
            readAnnotationValue(dann, c, null, dv);
            if (dv != null) {
                dv.visitEnd();
            }
        }
        if (ANNOTATIONS && anns != 0) {
            for (int i = readUnsignedShort(anns), v = anns + 2; i > 0; --i) {
                v = readAnnotationValues(v + 2, c, true,
                        mv.visitAnnotation(readUTF8(v, c), true));
            }
        }
        if (ANNOTATIONS && ianns != 0) {
            for (int i = readUnsignedShort(ianns), v = ianns + 2; i > 0; --i) {
                v = readAnnotationValues(v + 2, c, true,
                        mv.visitAnnotation(readUTF8(v, c), false));
            }
        }
        if (ANNOTATIONS && tanns != 0) {
            for (int i = readUnsignedShort(tanns), v = tanns + 2; i > 0; --i) {
                v = readAnnotationTarget(context, v);
                v = readAnnotationValues(v + 2, c, true,
                        mv.visitTypeAnnotation(context.typeRef,
                                context.typePath, readUTF8(v, c), true));
            }
        }
        if (ANNOTATIONS && itanns != 0) {
            for (int i = readUnsignedShort(itanns), v = itanns + 2; i > 0; --i) {
                v = readAnnotationTarget(context, v);
                v = readAnnotationValues(v + 2, c, true,
                        mv.visitTypeAnnotation(context.typeRef,
                                context.typePath, readUTF8(v, c), false));
            }
        }
        if (ANNOTATIONS && mpanns != 0) {
            readParameterAnnotations(mv, context, mpanns, true);
        }
        if (ANNOTATIONS && impanns != 0) {
            readParameterAnnotations(mv, context, impanns, false);
        }

        // visits the method attributes
        while (attributes != null) {
            Attribute attr = attributes.next;
            attributes.next = null;
            mv.visitAttribute(attributes);
            attributes = attr;
        }

        // visits the method code
        if (code != 0) {
            mv.visitCode();
            readCode(mv, context, code);
        }

        // visits the end of the method
        mv.visitEnd();

        return u;
    }

    /**
     * Reads the bytecode of a method and makes the given visitor visit it.
     * 
     * @param mv
     *            the visitor that must visit the method's code.
     * @param context
     *            information about the class being parsed.
     * @param u
     *            the start offset of the code attribute in the class file.
     */
    private void readCode(final MethodVisitor mv, final Context context, int u) {
        // reads the header
        byte[] b = this.b;
        char[] c = context.buffer;
        int maxStack = readUnsignedShort(u);
        int maxLocals = readUnsignedShort(u + 2);
        int codeLength = readInt(u + 4);
        u += 8;

        // reads the bytecode to find the labels
        int codeStart = u;
        int codeEnd = u + codeLength;
        Label[] labels = context.labels = new Label[codeLength + 2];
        readLabel(codeLength + 1, labels);
        while (u < codeEnd) {
            int offset = u - codeStart;
            int opcode = b[u] & 0xFF;
            switch (ClassWriter.TYPE[opcode]) {
            case ClassWriter.NOARG_INSN:
            case ClassWriter.IMPLVAR_INSN:
                u += 1;
                break;
            case ClassWriter.LABEL_INSN:
                readLabel(offset + readShort(u + 1), labels);
                u += 3;
                break;
            case ClassWriter.ASM_LABEL_INSN:
                readLabel(offset + readUnsignedShort(u + 1), labels);
                u += 3;
                break;
            case ClassWriter.LABELW_INSN:
                readLabel(offset + readInt(u + 1), labels);
                u += 5;
                break;
            case ClassWriter.WIDE_INSN:
                opcode = b[u + 1] & 0xFF;
                if (opcode == Opcodes.IINC) {
                    u += 6;
                } else {
                    u += 4;
                }
                break;
            case ClassWriter.TABL_INSN:
                // skips 0 to 3 padding bytes
                u = u + 4 - (offset & 3);
                // reads instruction
                readLabel(offset + readInt(u), labels);
                for (int i = readInt(u + 8) - readInt(u + 4) + 1; i > 0; --i) {
                    readLabel(offset + readInt(u + 12), labels);
                    u += 4;
                }
                u += 12;
                break;
            case ClassWriter.LOOK_INSN:
                // skips 0 to 3 padding bytes
                u = u + 4 - (offset & 3);
                // reads instruction
                readLabel(offset + readInt(u), labels);
                for (int i = readInt(u + 4); i > 0; --i) {
                    readLabel(offset + readInt(u + 12), labels);
                    u += 8;
                }
                u += 8;
                break;
            case ClassWriter.VAR_INSN:
            case ClassWriter.SBYTE_INSN:
            case ClassWriter.LDC_INSN:
                u += 2;
                break;
            case ClassWriter.SHORT_INSN:
            case ClassWriter.LDCW_INSN:
            case ClassWriter.FIELDORMETH_INSN:
            case ClassWriter.TYPE_INSN:
            case ClassWriter.IINC_INSN:
                u += 3;
                break;
            case ClassWriter.ITFMETH_INSN:
            case ClassWriter.INDYMETH_INSN:
                u += 5;
                break;
            // case MANA_INSN:
            default:
                u += 4;
                break;
            }
        }

        // reads the try catch entries to find the labels, and also visits them
        for (int i = readUnsignedShort(u); i > 0; --i) {
            Label start = readLabel(readUnsignedShort(u + 2), labels);
            Label end = readLabel(readUnsignedShort(u + 4), labels);
            Label handler = readLabel(readUnsignedShort(u + 6), labels);
            String type = readUTF8(items[readUnsignedShort(u + 8)], c);
            mv.visitTryCatchBlock(start, end, handler, type);
            u += 8;
        }
        u += 2;

        // reads the code attributes
        int[] tanns = null; // start index of each visible type annotation
        int[] itanns = null; // start index of each invisible type annotation
        int tann = 0; // current index in tanns array
        int itann = 0; // current index in itanns array
        int ntoff = -1; // next visible type annotation code offset
        int nitoff = -1; // next invisible type annotation code offset
        int varTable = 0;
        int varTypeTable = 0;
        boolean zip = true;
        boolean unzip = (context.flags & EXPAND_FRAMES) != 0;
        int stackMap = 0;
        int stackMapSize = 0;
        int frameCount = 0;
        Context frame = null;
        Attribute attributes = null;

        for (int i = readUnsignedShort(u); i > 0; --i) {
            String attrName = readUTF8(u + 2, c);
            if ("LocalVariableTable".equals(attrName)) {
                if ((context.flags & SKIP_DEBUG) == 0) {
                    varTable = u + 8;
                    for (int j = readUnsignedShort(u + 8), v = u; j > 0; --j) {
                        int label = readUnsignedShort(v + 10);
                        if (labels[label] == null) {
                            readLabel(label, labels).status |= Label.DEBUG;
                        }
                        label += readUnsignedShort(v + 12);
                        if (labels[label] == null) {
                            readLabel(label, labels).status |= Label.DEBUG;
                        }
                        v += 10;
                    }
                }
            } else if ("LocalVariableTypeTable".equals(attrName)) {
                varTypeTable = u + 8;
            } else if ("LineNumberTable".equals(attrName)) {
                if ((context.flags & SKIP_DEBUG) == 0) {
                    for (int j = readUnsignedShort(u + 8), v = u; j > 0; --j) {
                        int label = readUnsignedShort(v + 10);
                        if (labels[label] == null) {
                            readLabel(label, labels).status |= Label.DEBUG;
                        }
                        Label l = labels[label];
                        while (l.line > 0) {
                            if (l.next == null) {
                                l.next = new Label();
                            }
                            l = l.next;
                        }
                        l.line = readUnsignedShort(v + 12);
                        v += 4;
                    }
                }
            } else if (ANNOTATIONS
                    && "RuntimeVisibleTypeAnnotations".equals(attrName)) {
                tanns = readTypeAnnotations(mv, context, u + 8, true);
                ntoff = tanns.length == 0 || readByte(tanns[0]) < 0x43 ? -1
                        : readUnsignedShort(tanns[0] + 1);
            } else if (ANNOTATIONS
                    && "RuntimeInvisibleTypeAnnotations".equals(attrName)) {
                itanns = readTypeAnnotations(mv, context, u + 8, false);
                nitoff = itanns.length == 0 || readByte(itanns[0]) < 0x43 ? -1
                        : readUnsignedShort(itanns[0] + 1);
            } else if (FRAMES && "StackMapTable".equals(attrName)) {
                if ((context.flags & SKIP_FRAMES) == 0) {
                    stackMap = u + 10;
                    stackMapSize = readInt(u + 4);
                    frameCount = readUnsignedShort(u + 8);
                }
                /*
                 * here we do not extract the labels corresponding to the
                 * attribute content. This would require a full parsing of the
                 * attribute, which would need to be repeated in the second
                 * phase (see below). Instead the content of the attribute is
                 * read one frame at a time (i.e. after a frame has been
                 * visited, the next frame is read), and the labels it contains
                 * are also extracted one frame at a time. Thanks to the
                 * ordering of frames, having only a "one frame lookahead" is
                 * not a problem, i.e. it is not possible to see an offset
                 * smaller than the offset of the current insn and for which no
                 * Label exist.
                 */
                /*
                 * This is not true for UNINITIALIZED type offsets. We solve
                 * this by parsing the stack map table without a full decoding
                 * (see below).
                 */
            } else if (FRAMES && "StackMap".equals(attrName)) {
                if ((context.flags & SKIP_FRAMES) == 0) {
                    zip = false;
                    stackMap = u + 10;
                    stackMapSize = readInt(u + 4);
                    frameCount = readUnsignedShort(u + 8);
                }
                /*
                 * IMPORTANT! here we assume that the frames are ordered, as in
                 * the StackMapTable attribute, although this is not guaranteed
                 * by the attribute format.
                 */
            } else {
                for (int j = 0; j < context.attrs.length; ++j) {
                    if (context.attrs[j].type.equals(attrName)) {
                        Attribute attr = context.attrs[j].read(this, u + 8,
                                readInt(u + 4), c, codeStart - 8, labels);
                        if (attr != null) {
                            attr.next = attributes;
                            attributes = attr;
                        }
                    }
                }
            }
            u += 6 + readInt(u + 4);
        }
        u += 2;

        // generates the first (implicit) stack map frame
        if (FRAMES && stackMap != 0) {
            /*
             * for the first explicit frame the offset is not offset_delta + 1
             * but only offset_delta; setting the implicit frame offset to -1
             * allow the use of the "offset_delta + 1" rule in all cases
             */
            frame = context;
            frame.offset = -1;
            frame.mode = 0;
            frame.localCount = 0;
            frame.localDiff = 0;
            frame.stackCount = 0;
            frame.local = new Object[maxLocals];
            frame.stack = new Object[maxStack];
            if (unzip) {
                getImplicitFrame(context);
            }
            /*
             * Finds labels for UNINITIALIZED frame types. Instead of decoding
             * each element of the stack map table, we look for 3 consecutive
             * bytes that "look like" an UNINITIALIZED type (tag 8, offset
             * within code bounds, NEW instruction at this offset). We may find
             * false positives (i.e. not real UNINITIALIZED types), but this
             * should be rare, and the only consequence will be the creation of
             * an unneeded label. This is better than creating a label for each
             * NEW instruction, and faster than fully decoding the whole stack
             * map table.
             */
            for (int i = stackMap; i < stackMap + stackMapSize - 2; ++i) {
                if (b[i] == 8) { // UNINITIALIZED FRAME TYPE
                    int v = readUnsignedShort(i + 1);
                    if (v >= 0 && v < codeLength) {
                        if ((b[codeStart + v] & 0xFF) == Opcodes.NEW) {
                            readLabel(v, labels);
                        }
                    }
                }
            }
        }
        if ((context.flags & EXPAND_ASM_INSNS) != 0) {
            // Expanding the ASM pseudo instructions can introduce F_INSERT
            // frames, even if the method does not currently have any frame.
            // Also these inserted frames must be computed by simulating the
            // effect of the bytecode instructions one by one, starting from the
            // first one and the last existing frame (or the implicit first
            // one). Finally, due to the way MethodWriter computes this (with
            // the compute = INSERTED_FRAMES option), MethodWriter needs to know
            // maxLocals before the first instruction is visited. For all these
            // reasons we always visit the implicit first frame in this case
            // (passing only maxLocals - the rest can be and is computed in
            // MethodWriter).
            mv.visitFrame(Opcodes.F_NEW, maxLocals, null, 0, null);
        }

        // visits the instructions
        int opcodeDelta = (context.flags & EXPAND_ASM_INSNS) == 0 ? -33 : 0;
        u = codeStart;
        while (u < codeEnd) {
            int offset = u - codeStart;

            // visits the label and line number for this offset, if any
            Label l = labels[offset];
            if (l != null) {
                Label next = l.next;
                l.next = null;
                mv.visitLabel(l);
                if ((context.flags & SKIP_DEBUG) == 0 && l.line > 0) {
                    mv.visitLineNumber(l.line, l);
                    while (next != null) {
                        mv.visitLineNumber(next.line, l);
                        next = next.next;
                    }
                }
            }

            // visits the frame for this offset, if any
            while (FRAMES && frame != null
                    && (frame.offset == offset || frame.offset == -1)) {
                // if there is a frame for this offset, makes the visitor visit
                // it, and reads the next frame if there is one.
                if (frame.offset != -1) {
                    if (!zip || unzip) {
                        mv.visitFrame(Opcodes.F_NEW, frame.localCount,
                                frame.local, frame.stackCount, frame.stack);
                    } else {
                        mv.visitFrame(frame.mode, frame.localDiff, frame.local,
                                frame.stackCount, frame.stack);
                    }
                }
                if (frameCount > 0) {
                    stackMap = readFrame(stackMap, zip, unzip, frame);
                    --frameCount;
                } else {
                    frame = null;
                }
            }

            // visits the instruction at this offset
            int opcode = b[u] & 0xFF;
            switch (ClassWriter.TYPE[opcode]) {
            case ClassWriter.NOARG_INSN:
                mv.visitInsn(opcode);
                u += 1;
                break;
            case ClassWriter.IMPLVAR_INSN:
                if (opcode > Opcodes.ISTORE) {
                    opcode -= 59; // ISTORE_0
                    mv.visitVarInsn(Opcodes.ISTORE + (opcode >> 2),
                            opcode & 0x3);
                } else {
                    opcode -= 26; // ILOAD_0
                    mv.visitVarInsn(Opcodes.ILOAD + (opcode >> 2), opcode & 0x3);
                }
                u += 1;
                break;
            case ClassWriter.LABEL_INSN:
                mv.visitJumpInsn(opcode, labels[offset + readShort(u + 1)]);
                u += 3;
                break;
            case ClassWriter.LABELW_INSN:
                mv.visitJumpInsn(opcode + opcodeDelta, labels[offset
                        + readInt(u + 1)]);
                u += 5;
                break;
            case ClassWriter.ASM_LABEL_INSN: {
                // changes temporary opcodes 202 to 217 (inclusive), 218
                // and 219 to IFEQ ... JSR (inclusive), IFNULL and
                // IFNONNULL
                opcode = opcode < 218 ? opcode - 49 : opcode - 20;
                Label target = labels[offset + readUnsignedShort(u + 1)];
                // replaces GOTO with GOTO_W, JSR with JSR_W and IFxxx
                // <l> with IFNOTxxx <l'> GOTO_W <l>, where IFNOTxxx is
                // the "opposite" opcode of IFxxx (i.e., IFNE for IFEQ)
                // and where <l'> designates the instruction just after
                // the GOTO_W.
                if (opcode == Opcodes.GOTO || opcode == Opcodes.JSR) {
                    mv.visitJumpInsn(opcode + 33, target);
                } else {
                    opcode = opcode <= 166 ? ((opcode + 1) ^ 1) - 1
                            : opcode ^ 1;
                    Label endif = new Label();
                    mv.visitJumpInsn(opcode, endif);
                    mv.visitJumpInsn(200, target); // GOTO_W
                    mv.visitLabel(endif);
                    // since we introduced an unconditional jump instruction we
                    // also need to insert a stack map frame here, unless there
                    // is already one. The actual frame content will be computed
                    // in MethodWriter.
                    if (FRAMES && stackMap != 0
                            && (frame == null || frame.offset != offset + 3)) {
                        mv.visitFrame(ClassWriter.F_INSERT, 0, null, 0, null);
                    }
                }
                u += 3;
                break;
            }
            case ClassWriter.WIDE_INSN:
                opcode = b[u + 1] & 0xFF;
                if (opcode == Opcodes.IINC) {
                    mv.visitIincInsn(readUnsignedShort(u + 2), readShort(u + 4));
                    u += 6;
                } else {
                    mv.visitVarInsn(opcode, readUnsignedShort(u + 2));
                    u += 4;
                }
                break;
            case ClassWriter.TABL_INSN: {
                // skips 0 to 3 padding bytes
                u = u + 4 - (offset & 3);
                // reads instruction
                int label = offset + readInt(u);
                int min = readInt(u + 4);
                int max = readInt(u + 8);
                Label[] table = new Label[max - min + 1];
                u += 12;
                for (int i = 0; i < table.length; ++i) {
                    table[i] = labels[offset + readInt(u)];
                    u += 4;
                }
                mv.visitTableSwitchInsn(min, max, labels[label], table);
                break;
            }
            case ClassWriter.LOOK_INSN: {
                // skips 0 to 3 padding bytes
                u = u + 4 - (offset & 3);
                // reads instruction
                int label = offset + readInt(u);
                int len = readInt(u + 4);
                int[] keys = new int[len];
                Label[] values = new Label[len];
                u += 8;
                for (int i = 0; i < len; ++i) {
                    keys[i] = readInt(u);
                    values[i] = labels[offset + readInt(u + 4)];
                    u += 8;
                }
                mv.visitLookupSwitchInsn(labels[label], keys, values);
                break;
            }
            case ClassWriter.VAR_INSN:
                mv.visitVarInsn(opcode, b[u + 1] & 0xFF);
                u += 2;
                break;
            case ClassWriter.SBYTE_INSN:
                mv.visitIntInsn(opcode, b[u + 1]);
                u += 2;
                break;
            case ClassWriter.SHORT_INSN:
                mv.visitIntInsn(opcode, readShort(u + 1));
                u += 3;
                break;
            case ClassWriter.LDC_INSN:
                mv.visitLdcInsn(readConst(b[u + 1] & 0xFF, c));
                u += 2;
                break;
            case ClassWriter.LDCW_INSN:
                mv.visitLdcInsn(readConst(readUnsignedShort(u + 1), c));
                u += 3;
                break;
            case ClassWriter.FIELDORMETH_INSN:
            case ClassWriter.ITFMETH_INSN: {
                int cpIndex = items[readUnsignedShort(u + 1)];
                boolean itf = b[cpIndex - 1] == ClassWriter.IMETH;
                String iowner = readClass(cpIndex, c);
                cpIndex = items[readUnsignedShort(cpIndex + 2)];
                String iname = readUTF8(cpIndex, c);
                String idesc = readUTF8(cpIndex + 2, c);
                if (opcode < Opcodes.INVOKEVIRTUAL) {
                    mv.visitFieldInsn(opcode, iowner, iname, idesc);
                } else {
                    mv.visitMethodInsn(opcode, iowner, iname, idesc, itf);
                }
                if (opcode == Opcodes.INVOKEINTERFACE) {
                    u += 5;
                } else {
                    u += 3;
                }
                break;
            }
            case ClassWriter.INDYMETH_INSN: {
                int cpIndex = items[readUnsignedShort(u + 1)];
                int bsmIndex = context.bootstrapMethods[readUnsignedShort(cpIndex)];
                Handle bsm = (Handle) readConst(readUnsignedShort(bsmIndex), c);
                int bsmArgCount = readUnsignedShort(bsmIndex + 2);
                Object[] bsmArgs = new Object[bsmArgCount];
                bsmIndex += 4;
                for (int i = 0; i < bsmArgCount; i++) {
                    bsmArgs[i] = readConst(readUnsignedShort(bsmIndex), c);
                    bsmIndex += 2;
                }
                cpIndex = items[readUnsignedShort(cpIndex + 2)];
                String iname = readUTF8(cpIndex, c);
                String idesc = readUTF8(cpIndex + 2, c);
                mv.visitInvokeDynamicInsn(iname, idesc, bsm, bsmArgs);
                u += 5;
                break;
            }
            case ClassWriter.TYPE_INSN:
                mv.visitTypeInsn(opcode, readClass(u + 1, c));
                u += 3;
                break;
            case ClassWriter.IINC_INSN:
                mv.visitIincInsn(b[u + 1] & 0xFF, b[u + 2]);
                u += 3;
                break;
            // case MANA_INSN:
            default:
                mv.visitMultiANewArrayInsn(readClass(u + 1, c), b[u + 3] & 0xFF);
                u += 4;
                break;
            }

            // visit the instruction annotations, if any
            while (tanns != null && tann < tanns.length && ntoff <= offset) {
                if (ntoff == offset) {
                    int v = readAnnotationTarget(context, tanns[tann]);
                    readAnnotationValues(v + 2, c, true,
                            mv.visitInsnAnnotation(context.typeRef,
                                    context.typePath, readUTF8(v, c), true));
                }
                ntoff = ++tann >= tanns.length || readByte(tanns[tann]) < 0x43 ? -1
                        : readUnsignedShort(tanns[tann] + 1);
            }
            while (itanns != null && itann < itanns.length && nitoff <= offset) {
                if (nitoff == offset) {
                    int v = readAnnotationTarget(context, itanns[itann]);
                    readAnnotationValues(v + 2, c, true,
                            mv.visitInsnAnnotation(context.typeRef,
                                    context.typePath, readUTF8(v, c), false));
                }
                nitoff = ++itann >= itanns.length
                        || readByte(itanns[itann]) < 0x43 ? -1
                        : readUnsignedShort(itanns[itann] + 1);
            }
        }
        if (labels[codeLength] != null) {
            mv.visitLabel(labels[codeLength]);
        }

        // visits the local variable tables
        if ((context.flags & SKIP_DEBUG) == 0 && varTable != 0) {
            int[] typeTable = null;
            if (varTypeTable != 0) {
                u = varTypeTable + 2;
                typeTable = new int[readUnsignedShort(varTypeTable) * 3];
                for (int i = typeTable.length; i > 0;) {
                    typeTable[--i] = u + 6; // signature
                    typeTable[--i] = readUnsignedShort(u + 8); // index
                    typeTable[--i] = readUnsignedShort(u); // start
                    u += 10;
                }
            }
            u = varTable + 2;
            for (int i = readUnsignedShort(varTable); i > 0; --i) {
                int start = readUnsignedShort(u);
                int length = readUnsignedShort(u + 2);
                int index = readUnsignedShort(u + 8);
                String vsignature = null;
                if (typeTable != null) {
                    for (int j = 0; j < typeTable.length; j += 3) {
                        if (typeTable[j] == start && typeTable[j + 1] == index) {
                            vsignature = readUTF8(typeTable[j + 2], c);
                            break;
                        }
                    }
                }
                mv.visitLocalVariable(readUTF8(u + 4, c), readUTF8(u + 6, c),
                        vsignature, labels[start], labels[start + length],
                        index);
                u += 10;
            }
        }

        // visits the local variables type annotations
        if (tanns != null) {
            for (int i = 0; i < tanns.length; ++i) {
                if ((readByte(tanns[i]) >> 1) == (0x40 >> 1)) {
                    int v = readAnnotationTarget(context, tanns[i]);
                    v = readAnnotationValues(v + 2, c, true,
                            mv.visitLocalVariableAnnotation(context.typeRef,
                                    context.typePath, context.start,
                                    context.end, context.index, readUTF8(v, c),
                                    true));
                }
            }
        }
        if (itanns != null) {
            for (int i = 0; i < itanns.length; ++i) {
                if ((readByte(itanns[i]) >> 1) == (0x40 >> 1)) {
                    int v = readAnnotationTarget(context, itanns[i]);
                    v = readAnnotationValues(v + 2, c, true,
                            mv.visitLocalVariableAnnotation(context.typeRef,
                                    context.typePath, context.start,
                                    context.end, context.index, readUTF8(v, c),
                                    false));
                }
            }
        }

        // visits the code attributes
        while (attributes != null) {
            Attribute attr = attributes.next;
            attributes.next = null;
            mv.visitAttribute(attributes);
            attributes = attr;
        }

        // visits the max stack and max locals values
        mv.visitMaxs(maxStack, maxLocals);
    }

    /**
     * Parses a type annotation table to find the labels, and to visit the try
     * catch block annotations.
     * 
     * @param u
     *            the start offset of a type annotation table.
     * @param mv
     *            the method visitor to be used to visit the try catch block
     *            annotations.
     * @param context
     *            information about the class being parsed.
     * @param visible
     *            if the type annotation table to parse contains runtime visible
     *            annotations.
     * @return the start offset of each type annotation in the parsed table.
     */
    private int[] readTypeAnnotations(final MethodVisitor mv,
            final Context context, int u, boolean visible) {
        char[] c = context.buffer;
        int[] offsets = new int[readUnsignedShort(u)];
        u += 2;
        for (int i = 0; i < offsets.length; ++i) {
            offsets[i] = u;
            int target = readInt(u);
            switch (target >>> 24) {
            case 0x00: // CLASS_TYPE_PARAMETER
            case 0x01: // METHOD_TYPE_PARAMETER
            case 0x16: // METHOD_FORMAL_PARAMETER
                u += 2;
                break;
            case 0x13: // FIELD
            case 0x14: // METHOD_RETURN
            case 0x15: // METHOD_RECEIVER
                u += 1;
                break;
            case 0x40: // LOCAL_VARIABLE
            case 0x41: // RESOURCE_VARIABLE
                for (int j = readUnsignedShort(u + 1); j > 0; --j) {
                    int start = readUnsignedShort(u + 3);
                    int length = readUnsignedShort(u + 5);
                    readLabel(start, context.labels);
                    readLabel(start + length, context.labels);
                    u += 6;
                }
                u += 3;
                break;
            case 0x47: // CAST
            case 0x48: // CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT
            case 0x49: // METHOD_INVOCATION_TYPE_ARGUMENT
            case 0x4A: // CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT
            case 0x4B: // METHOD_REFERENCE_TYPE_ARGUMENT
                u += 4;
                break;
            // case 0x10: // CLASS_EXTENDS
            // case 0x11: // CLASS_TYPE_PARAMETER_BOUND
            // case 0x12: // METHOD_TYPE_PARAMETER_BOUND
            // case 0x17: // THROWS
            // case 0x42: // EXCEPTION_PARAMETER
            // case 0x43: // INSTANCEOF
            // case 0x44: // NEW
            // case 0x45: // CONSTRUCTOR_REFERENCE
            // case 0x46: // METHOD_REFERENCE
            default:
                u += 3;
                break;
            }
            int pathLength = readByte(u);
            if ((target >>> 24) == 0x42) {
                TypePath path = pathLength == 0 ? null : new TypePath(b, u);
                u += 1 + 2 * pathLength;
                u = readAnnotationValues(u + 2, c, true,
                        mv.visitTryCatchAnnotation(target, path,
                                readUTF8(u, c), visible));
            } else {
                u = readAnnotationValues(u + 3 + 2 * pathLength, c, true, null);
            }
        }
        return offsets;
    }

    /**
     * Parses the header of a type annotation to extract its target_type and
     * target_path (the result is stored in the given context), and returns the
     * start offset of the rest of the type_annotation structure (i.e. the
     * offset to the type_index field, which is followed by
     * num_element_value_pairs and then the name,value pairs).
     * 
     * @param context
     *            information about the class being parsed. This is where the
     *            extracted target_type and target_path must be stored.
     * @param u
     *            the start offset of a type_annotation structure.
     * @return the start offset of the rest of the type_annotation structure.
     */
    private int readAnnotationTarget(final Context context, int u) {
        int target = readInt(u);
        switch (target >>> 24) {
        case 0x00: // CLASS_TYPE_PARAMETER
        case 0x01: // METHOD_TYPE_PARAMETER
        case 0x16: // METHOD_FORMAL_PARAMETER
            target &= 0xFFFF0000;
            u += 2;
            break;
        case 0x13: // FIELD
        case 0x14: // METHOD_RETURN
        case 0x15: // METHOD_RECEIVER
            target &= 0xFF000000;
            u += 1;
            break;
        case 0x40: // LOCAL_VARIABLE
        case 0x41: { // RESOURCE_VARIABLE
            target &= 0xFF000000;
            int n = readUnsignedShort(u + 1);
            context.start = new Label[n];
            context.end = new Label[n];
            context.index = new int[n];
            u += 3;
            for (int i = 0; i < n; ++i) {
                int start = readUnsignedShort(u);
                int length = readUnsignedShort(u + 2);
                context.start[i] = readLabel(start, context.labels);
                context.end[i] = readLabel(start + length, context.labels);
                context.index[i] = readUnsignedShort(u + 4);
                u += 6;
            }
            break;
        }
        case 0x47: // CAST
        case 0x48: // CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT
        case 0x49: // METHOD_INVOCATION_TYPE_ARGUMENT
        case 0x4A: // CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT
        case 0x4B: // METHOD_REFERENCE_TYPE_ARGUMENT
            target &= 0xFF0000FF;
            u += 4;
            break;
        // case 0x10: // CLASS_EXTENDS
        // case 0x11: // CLASS_TYPE_PARAMETER_BOUND
        // case 0x12: // METHOD_TYPE_PARAMETER_BOUND
        // case 0x17: // THROWS
        // case 0x42: // EXCEPTION_PARAMETER
        // case 0x43: // INSTANCEOF
        // case 0x44: // NEW
        // case 0x45: // CONSTRUCTOR_REFERENCE
        // case 0x46: // METHOD_REFERENCE
        default:
            target &= (target >>> 24) < 0x43 ? 0xFFFFFF00 : 0xFF000000;
            u += 3;
            break;
        }
        int pathLength = readByte(u);
        context.typeRef = target;
        context.typePath = pathLength == 0 ? null : new TypePath(b, u);
        return u + 1 + 2 * pathLength;
    }

    /**
     * Reads parameter annotations and makes the given visitor visit them.
     * 
     * @param mv
     *            the visitor that must visit the annotations.
     * @param context
     *            information about the class being parsed.
     * @param v
     *            start offset in {@link #b b} of the annotations to be read.
     * @param visible
     *            <tt>true</tt> if the annotations to be read are visible at
     *            runtime.
     */
    private void readParameterAnnotations(final MethodVisitor mv,
            final Context context, int v, final boolean visible) {
        int i;
        int n = b[v++] & 0xFF;
        // workaround for a bug in javac (javac compiler generates a parameter
        // annotation array whose size is equal to the number of parameters in
        // the Java source file, while it should generate an array whose size is
        // equal to the number of parameters in the method descriptor - which
        // includes the synthetic parameters added by the compiler). This work-
        // around supposes that the synthetic parameters are the first ones.
        int synthetics = Type.getArgumentTypes(context.desc).length - n;
        AnnotationVisitor av;
        for (i = 0; i < synthetics; ++i) {
            // virtual annotation to detect synthetic parameters in MethodWriter
            av = mv.visitParameterAnnotation(i, "Ljava/lang/Synthetic;", false);
            if (av != null) {
                av.visitEnd();
            }
        }
        char[] c = context.buffer;
        for (; i < n + synthetics; ++i) {
            int j = readUnsignedShort(v);
            v += 2;
            for (; j > 0; --j) {
                av = mv.visitParameterAnnotation(i, readUTF8(v, c), visible);
                v = readAnnotationValues(v + 2, c, true, av);
            }
        }
    }

    /**
     * Reads the values of an annotation and makes the given visitor visit them.
     * 
     * @param v
     *            the start offset in {@link #b b} of the values to be read
     *            (including the unsigned short that gives the number of
     *            values).
     * @param buf
     *            buffer to be used to call {@link #readUTF8 readUTF8},
     *            {@link #readClass(int,char[]) readClass} or {@link #readConst
     *            readConst}.
     * @param named
     *            if the annotation values are named or not.
     * @param av
     *            the visitor that must visit the values.
     * @return the end offset of the annotation values.
     */
    private int readAnnotationValues(int v, final char[] buf,
            final boolean named, final AnnotationVisitor av) {
        int i = readUnsignedShort(v);
        v += 2;
        if (named) {
            for (; i > 0; --i) {
                v = readAnnotationValue(v + 2, buf, readUTF8(v, buf), av);
            }
        } else {
            for (; i > 0; --i) {
                v = readAnnotationValue(v, buf, null, av);
            }
        }
        if (av != null) {
            av.visitEnd();
        }
        return v;
    }

    /**
     * Reads a value of an annotation and makes the given visitor visit it.
     * 
     * @param v
     *            the start offset in {@link #b b} of the value to be read
     *            (<i>not including the value name constant pool index</i>).
     * @param buf
     *            buffer to be used to call {@link #readUTF8 readUTF8},
     *            {@link #readClass(int,char[]) readClass} or {@link #readConst
     *            readConst}.
     * @param name
     *            the name of the value to be read.
     * @param av
     *            the visitor that must visit the value.
     * @return the end offset of the annotation value.
     */
    private int readAnnotationValue(int v, final char[] buf, final String name,
            final AnnotationVisitor av) {
        int i;
        if (av == null) {
            switch (b[v] & 0xFF) {
            case 'e': // enum_const_value
                return v + 5;
            case '@': // annotation_value
                return readAnnotationValues(v + 3, buf, true, null);
            case '[': // array_value
                return readAnnotationValues(v + 1, buf, false, null);
            default:
                return v + 3;
            }
        }
        switch (b[v++] & 0xFF) {
        case 'I': // pointer to CONSTANT_Integer
        case 'J': // pointer to CONSTANT_Long
        case 'F': // pointer to CONSTANT_Float
        case 'D': // pointer to CONSTANT_Double
            av.visit(name, readConst(readUnsignedShort(v), buf));
            v += 2;
            break;
        case 'B': // pointer to CONSTANT_Byte
            av.visit(name, (byte) readInt(items[readUnsignedShort(v)]));
            v += 2;
            break;
        case 'Z': // pointer to CONSTANT_Boolean
            av.visit(name,
                    readInt(items[readUnsignedShort(v)]) == 0 ? Boolean.FALSE
                            : Boolean.TRUE);
            v += 2;
            break;
        case 'S': // pointer to CONSTANT_Short
            av.visit(name, (short) readInt(items[readUnsignedShort(v)]));
            v += 2;
            break;
        case 'C': // pointer to CONSTANT_Char
            av.visit(name, (char) readInt(items[readUnsignedShort(v)]));
            v += 2;
            break;
        case 's': // pointer to CONSTANT_Utf8
            av.visit(name, readUTF8(v, buf));
            v += 2;
            break;
        case 'e': // enum_const_value
            av.visitEnum(name, readUTF8(v, buf), readUTF8(v + 2, buf));
            v += 4;
            break;
        case 'c': // class_info
            av.visit(name, Type.getType(readUTF8(v, buf)));
            v += 2;
            break;
        case '@': // annotation_value
            v = readAnnotationValues(v + 2, buf, true,
                    av.visitAnnotation(name, readUTF8(v, buf)));
            break;
        case '[': // array_value
            int size = readUnsignedShort(v);
            v += 2;
            if (size == 0) {
                return readAnnotationValues(v - 2, buf, false,
                        av.visitArray(name));
            }
            switch (this.b[v++] & 0xFF) {
            case 'B':
                byte[] bv = new byte[size];
                for (i = 0; i < size; i++) {
                    bv[i] = (byte) readInt(items[readUnsignedShort(v)]);
                    v += 3;
                }
                av.visit(name, bv);
                --v;
                break;
            case 'Z':
                boolean[] zv = new boolean[size];
                for (i = 0; i < size; i++) {
                    zv[i] = readInt(items[readUnsignedShort(v)]) != 0;
                    v += 3;
                }
                av.visit(name, zv);
                --v;
                break;
            case 'S':
                short[] sv = new short[size];
                for (i = 0; i < size; i++) {
                    sv[i] = (short) readInt(items[readUnsignedShort(v)]);
                    v += 3;
                }
                av.visit(name, sv);
                --v;
                break;
            case 'C':
                char[] cv = new char[size];
                for (i = 0; i < size; i++) {
                    cv[i] = (char) readInt(items[readUnsignedShort(v)]);
                    v += 3;
                }
                av.visit(name, cv);
                --v;
                break;
            case 'I':
                int[] iv = new int[size];
                for (i = 0; i < size; i++) {
                    iv[i] = readInt(items[readUnsignedShort(v)]);
                    v += 3;
                }
                av.visit(name, iv);
                --v;
                break;
            case 'J':
                long[] lv = new long[size];
                for (i = 0; i < size; i++) {
                    lv[i] = readLong(items[readUnsignedShort(v)]);
                    v += 3;
                }
                av.visit(name, lv);
                --v;
                break;
            case 'F':
                float[] fv = new float[size];
                for (i = 0; i < size; i++) {
                    fv[i] = Float
                            .intBitsToFloat(readInt(items[readUnsignedShort(v)]));
                    v += 3;
                }
                av.visit(name, fv);
                --v;
                break;
            case 'D':
                double[] dv = new double[size];
                for (i = 0; i < size; i++) {
                    dv[i] = Double
                            .longBitsToDouble(readLong(items[readUnsignedShort(v)]));
                    v += 3;
                }
                av.visit(name, dv);
                --v;
                break;
            default:
                v = readAnnotationValues(v - 3, buf, false, av.visitArray(name));
            }
        }
        return v;
    }

    /**
     * Computes the implicit frame of the method currently being parsed (as
     * defined in the given {@link Context}) and stores it in the given context.
     * 
     * @param frame
     *            information about the class being parsed.
     */
    private void getImplicitFrame(final Context frame) {
        String desc = frame.desc;
        Object[] locals = frame.local;
        int local = 0;
        if ((frame.access & Opcodes.ACC_STATIC) == 0) {
            if ("<init>".equals(frame.name)) {
                locals[local++] = Opcodes.UNINITIALIZED_THIS;
            } else {
                locals[local++] = readClass(header + 2, frame.buffer);
            }
        }
        int i = 1;
        loop: while (true) {
            int j = i;
            switch (desc.charAt(i++)) {
            case 'Z':
            case 'C':
            case 'B':
            case 'S':
            case 'I':
                locals[local++] = Opcodes.INTEGER;
                break;
            case 'F':
                locals[local++] = Opcodes.FLOAT;
                break;
            case 'J':
                locals[local++] = Opcodes.LONG;
                break;
            case 'D':
                locals[local++] = Opcodes.DOUBLE;
                break;
            case '[':
                while (desc.charAt(i) == '[') {
                    ++i;
                }
                if (desc.charAt(i) == 'L') {
                    ++i;
                    while (desc.charAt(i) != ';') {
                        ++i;
                    }
                }
                locals[local++] = desc.substring(j, ++i);
                break;
            case 'L':
                while (desc.charAt(i) != ';') {
                    ++i;
                }
                locals[local++] = desc.substring(j + 1, i++);
                break;
            default:
                break loop;
            }
        }
        frame.localCount = local;
    }

    /**
     * Reads a stack map frame and stores the result in the given
     * {@link Context} object.
     * 
     * @param stackMap
     *            the start offset of a stack map frame in the class file.
     * @param zip
     *            if the stack map frame at stackMap is compressed or not.
     * @param unzip
     *            if the stack map frame must be uncompressed.
     * @param frame
     *            where the parsed stack map frame must be stored.
     * @return the offset of the first byte following the parsed frame.
     */
    private int readFrame(int stackMap, boolean zip, boolean unzip,
            Context frame) {
        char[] c = frame.buffer;
        Label[] labels = frame.labels;
        int tag;
        int delta;
        if (zip) {
            tag = b[stackMap++] & 0xFF;
        } else {
            tag = MethodWriter.FULL_FRAME;
            frame.offset = -1;
        }
        frame.localDiff = 0;
        if (tag < MethodWriter.SAME_LOCALS_1_STACK_ITEM_FRAME) {
            delta = tag;
            frame.mode = Opcodes.F_SAME;
            frame.stackCount = 0;
        } else if (tag < MethodWriter.RESERVED) {
            delta = tag - MethodWriter.SAME_LOCALS_1_STACK_ITEM_FRAME;
            stackMap = readFrameType(frame.stack, 0, stackMap, c, labels);
            frame.mode = Opcodes.F_SAME1;
            frame.stackCount = 1;
        } else {
            delta = readUnsignedShort(stackMap);
            stackMap += 2;
            if (tag == MethodWriter.SAME_LOCALS_1_STACK_ITEM_FRAME_EXTENDED) {
                stackMap = readFrameType(frame.stack, 0, stackMap, c, labels);
                frame.mode = Opcodes.F_SAME1;
                frame.stackCount = 1;
            } else if (tag >= MethodWriter.CHOP_FRAME
                    && tag < MethodWriter.SAME_FRAME_EXTENDED) {
                frame.mode = Opcodes.F_CHOP;
                frame.localDiff = MethodWriter.SAME_FRAME_EXTENDED - tag;
                frame.localCount -= frame.localDiff;
                frame.stackCount = 0;
            } else if (tag == MethodWriter.SAME_FRAME_EXTENDED) {
                frame.mode = Opcodes.F_SAME;
                frame.stackCount = 0;
            } else if (tag < MethodWriter.FULL_FRAME) {
                int local = unzip ? frame.localCount : 0;
                for (int i = tag - MethodWriter.SAME_FRAME_EXTENDED; i > 0; i--) {
                    stackMap = readFrameType(frame.local, local++, stackMap, c,
                            labels);
                }
                frame.mode = Opcodes.F_APPEND;
                frame.localDiff = tag - MethodWriter.SAME_FRAME_EXTENDED;
                frame.localCount += frame.localDiff;
                frame.stackCount = 0;
            } else { // if (tag == FULL_FRAME) {
                frame.mode = Opcodes.F_FULL;
                int n = readUnsignedShort(stackMap);
                stackMap += 2;
                frame.localDiff = n;
                frame.localCount = n;
                for (int local = 0; n > 0; n--) {
                    stackMap = readFrameType(frame.local, local++, stackMap, c,
                            labels);
                }
                n = readUnsignedShort(stackMap);
                stackMap += 2;
                frame.stackCount = n;
                for (int stack = 0; n > 0; n--) {
                    stackMap = readFrameType(frame.stack, stack++, stackMap, c,
                            labels);
                }
            }
        }
        frame.offset += delta + 1;
        readLabel(frame.offset, labels);
        return stackMap;
    }

    /**
     * Reads a stack map frame type and stores it at the given index in the
     * given array.
     * 
     * @param frame
     *            the array where the parsed type must be stored.
     * @param index
     *            the index in 'frame' where the parsed type must be stored.
     * @param v
     *            the start offset of the stack map frame type to read.
     * @param buf
     *            a buffer to read strings.
     * @param labels
     *            the labels of the method currently being parsed, indexed by
     *            their offset. If the parsed type is an Uninitialized type, a
     *            new label for the corresponding NEW instruction is stored in
     *            this array if it does not already exist.
     * @return the offset of the first byte after the parsed type.
     */
    private int readFrameType(final Object[] frame, final int index, int v,
            final char[] buf, final Label[] labels) {
        int type = b[v++] & 0xFF;
        switch (type) {
        case 0:
            frame[index] = Opcodes.TOP;
            break;
        case 1:
            frame[index] = Opcodes.INTEGER;
            break;
        case 2:
            frame[index] = Opcodes.FLOAT;
            break;
        case 3:
            frame[index] = Opcodes.DOUBLE;
            break;
        case 4:
            frame[index] = Opcodes.LONG;
            break;
        case 5:
            frame[index] = Opcodes.NULL;
            break;
        case 6:
            frame[index] = Opcodes.UNINITIALIZED_THIS;
            break;
        case 7: // Object
            frame[index] = readClass(v, buf);
            v += 2;
            break;
        default: // Uninitialized
            frame[index] = readLabel(readUnsignedShort(v), labels);
            v += 2;
        }
        return v;
    }

    /**
     * Returns the label corresponding to the given offset. The default
     * implementation of this method creates a label for the given offset if it
     * has not been already created.
     * 
     * @param offset
     *            a bytecode offset in a method.
     * @param labels
     *            the already created labels, indexed by their offset. If a
     *            label already exists for offset this method must not create a
     *            new one. Otherwise it must store the new label in this array.
     * @return a non null Label, which must be equal to labels[offset].
     */
    protected Label readLabel(int offset, Label[] labels) {
        // SPRING PATCH: leniently handle offset mismatch
        if (offset >= labels.length) {
            return new Label();
        }
        // END OF PATCH
        if (labels[offset] == null) {
            labels[offset] = new Label();
        }
        return labels[offset];
    }

    /**
     * Returns the start index of the attribute_info structure of this class.
     * 
     * @return the start index of the attribute_info structure of this class.
     */
    private int getAttributes() {
        // skips the header
        int u = header + 8 + readUnsignedShort(header + 6) * 2;
        // skips fields and methods
        for (int i = readUnsignedShort(u); i > 0; --i) {
            for (int j = readUnsignedShort(u + 8); j > 0; --j) {
                u += 6 + readInt(u + 12);
            }
            u += 8;
        }
        u += 2;
        for (int i = readUnsignedShort(u); i > 0; --i) {
            for (int j = readUnsignedShort(u + 8); j > 0; --j) {
                u += 6 + readInt(u + 12);
            }
            u += 8;
        }
        // the attribute_info structure starts just after the methods
        return u + 2;
    }

    /**
     * Reads an attribute in {@link #b b}.
     * 
     * @param attrs
     *            prototypes of the attributes that must be parsed during the
     *            visit of the class. Any attribute whose type is not equal to
     *            the type of one the prototypes is ignored (i.e. an empty
     *            {@link Attribute} instance is returned).
     * @param type
     *            the type of the attribute.
     * @param off
     *            index of the first byte of the attribute's content in
     *            {@link #b b}. The 6 attribute header bytes, containing the
     *            type and the length of the attribute, are not taken into
     *            account here (they have already been read).
     * @param len
     *            the length of the attribute's content.
     * @param buf
     *            buffer to be used to call {@link #readUTF8 readUTF8},
     *            {@link #readClass(int,char[]) readClass} or {@link #readConst
     *            readConst}.
     * @param codeOff
     *            index of the first byte of code's attribute content in
     *            {@link #b b}, or -1 if the attribute to be read is not a code
     *            attribute. The 6 attribute header bytes, containing the type
     *            and the length of the attribute, are not taken into account
     *            here.
     * @param labels
     *            the labels of the method's code, or <tt>null</tt> if the
     *            attribute to be read is not a code attribute.
     * @return the attribute that has been read, or <tt>null</tt> to skip this
     *         attribute.
     */
    private Attribute readAttribute(final Attribute[] attrs, final String type,
            final int off, final int len, final char[] buf, final int codeOff,
            final Label[] labels) {
        for (int i = 0; i < attrs.length; ++i) {
            if (attrs[i].type.equals(type)) {
                return attrs[i].read(this, off, len, buf, codeOff, labels);
            }
        }
        return new Attribute(type).read(this, off, len, null, -1, null);
    }

    // ------------------------------------------------------------------------
    // Utility methods: low level parsing
    // ------------------------------------------------------------------------

    /**
     * Returns the number of constant pool items in {@link #b b}.
     * 
     * @return the number of constant pool items in {@link #b b}.
     */
    public int getItemCount() {
        return items.length;
    }

    /**
     * Returns the start index of the constant pool item in {@link #b b}, plus
     * one. <i>This method is intended for {@link Attribute} sub classes, and is
     * normally not needed by class generators or adapters.</i>
     * 
     * @param item
     *            the index a constant pool item.
     * @return the start index of the constant pool item in {@link #b b}, plus
     *         one.
     */
    public int getItem(final int item) {
        return items[item];
    }

    /**
     * Returns the maximum length of the strings contained in the constant pool
     * of the class.
     * 
     * @return the maximum length of the strings contained in the constant pool
     *         of the class.
     */
    public int getMaxStringLength() {
        return maxStringLength;
    }

    /**
     * Reads a byte value in {@link #b b}. <i>This method is intended for
     * {@link Attribute} sub classes, and is normally not needed by class
     * generators or adapters.</i>
     * 
     * @param index
     *            the start index of the value to be read in {@link #b b}.
     * @return the read value.
     */
    public int readByte(final int index) {
        return b[index] & 0xFF;
    }

    /**
     * Reads an unsigned short value in {@link #b b}. <i>This method is intended
     * for {@link Attribute} sub classes, and is normally not needed by class
     * generators or adapters.</i>
     * 
     * @param index
     *            the start index of the value to be read in {@link #b b}.
     * @return the read value.
     */
    public int readUnsignedShort(final int index) {
        byte[] b = this.b;
        return ((b[index] & 0xFF) << 8) | (b[index + 1] & 0xFF);
    }

    /**
     * Reads a signed short value in {@link #b b}. <i>This method is intended
     * for {@link Attribute} sub classes, and is normally not needed by class
     * generators or adapters.</i>
     * 
     * @param index
     *            the start index of the value to be read in {@link #b b}.
     * @return the read value.
     */
    public short readShort(final int index) {
        byte[] b = this.b;
        return (short) (((b[index] & 0xFF) << 8) | (b[index + 1] & 0xFF));
    }

    /**
     * Reads a signed int value in {@link #b b}. <i>This method is intended for
     * {@link Attribute} sub classes, and is normally not needed by class
     * generators or adapters.</i>
     * 
     * @param index
     *            the start index of the value to be read in {@link #b b}.
     * @return the read value.
     */
    public int readInt(final int index) {
        byte[] b = this.b;
        return ((b[index] & 0xFF) << 24) | ((b[index + 1] & 0xFF) << 16)
                | ((b[index + 2] & 0xFF) << 8) | (b[index + 3] & 0xFF);
    }

    /**
     * Reads a signed long value in {@link #b b}. <i>This method is intended for
     * {@link Attribute} sub classes, and is normally not needed by class
     * generators or adapters.</i>
     * 
     * @param index
     *            the start index of the value to be read in {@link #b b}.
     * @return the read value.
     */
    public long readLong(final int index) {
        long l1 = readInt(index);
        long l0 = readInt(index + 4) & 0xFFFFFFFFL;
        return (l1 << 32) | l0;
    }

    /**
     * Reads an UTF8 string constant pool item in {@link #b b}. <i>This method
     * is intended for {@link Attribute} sub classes, and is normally not needed
     * by class generators or adapters.</i>
     * 
     * @param index
     *            the start index of an unsigned short value in {@link #b b},
     *            whose value is the index of an UTF8 constant pool item.
     * @param buf
     *            buffer to be used to read the item. This buffer must be
     *            sufficiently large. It is not automatically resized.
     * @return the String corresponding to the specified UTF8 item.
     */
    public String readUTF8(int index, final char[] buf) {
        int item = readUnsignedShort(index);
        if (index == 0 || item == 0) {
            return null;
        }
        String s = strings[item];
        if (s != null) {
            return s;
        }
        index = items[item];
        return strings[item] = readUTF(index + 2, readUnsignedShort(index), buf);
    }

    /**
     * Reads UTF8 string in {@link #b b}.
     * 
     * @param index
     *            start offset of the UTF8 string to be read.
     * @param utfLen
     *            length of the UTF8 string to be read.
     * @param buf
     *            buffer to be used to read the string. This buffer must be
     *            sufficiently large. It is not automatically resized.
     * @return the String corresponding to the specified UTF8 string.
     */
    private String readUTF(int index, final int utfLen, final char[] buf) {
        int endIndex = index + utfLen;
        byte[] b = this.b;
        int strLen = 0;
        int c;
        int st = 0;
        char cc = 0;
        while (index < endIndex) {
            c = b[index++];
            switch (st) {
            case 0:
                c = c & 0xFF;
                if (c < 0x80) { // 0xxxxxxx
                    buf[strLen++] = (char) c;
                } else if (c < 0xE0 && c > 0xBF) { // 110x xxxx 10xx xxxx
                    cc = (char) (c & 0x1F);
                    st = 1;
                } else { // 1110 xxxx 10xx xxxx 10xx xxxx
                    cc = (char) (c & 0x0F);
                    st = 2;
                }
                break;

            case 1: // byte 2 of 2-byte char or byte 3 of 3-byte char
                buf[strLen++] = (char) ((cc << 6) | (c & 0x3F));
                st = 0;
                break;

            case 2: // byte 2 of 3-byte char
                cc = (char) ((cc << 6) | (c & 0x3F));
                st = 1;
                break;
            }
        }
        return new String(buf, 0, strLen);
    }

    /**
     * Reads a class constant pool item in {@link #b b}. <i>This method is
     * intended for {@link Attribute} sub classes, and is normally not needed by
     * class generators or adapters.</i>
     * 
     * @param index
     *            the start index of an unsigned short value in {@link #b b},
     *            whose value is the index of a class constant pool item.
     * @param buf
     *            buffer to be used to read the item. This buffer must be
     *            sufficiently large. It is not automatically resized.
     * @return the String corresponding to the specified class item.
     */
    public String readClass(final int index, final char[] buf) {
        // computes the start index of the CONSTANT_Class item in b
        // and reads the CONSTANT_Utf8 item designated by
        // the first two bytes of this CONSTANT_Class item
        String name = readUTF8(items[readUnsignedShort(index)], buf);
        return (name != null ? name.intern() : null);
    }

    /**
     * Reads a numeric or string constant pool item in {@link #b b}. <i>This
     * method is intended for {@link Attribute} sub classes, and is normally not
     * needed by class generators or adapters.</i>
     * 
     * @param item
     *            the index of a constant pool item.
     * @param buf
     *            buffer to be used to read the item. This buffer must be
     *            sufficiently large. It is not automatically resized.
     * @return the {@link Integer}, {@link Float}, {@link Long}, {@link Double},
     *         {@link String}, {@link Type} or {@link Handle} corresponding to
     *         the given constant pool item.
     */
    public Object readConst(final int item, final char[] buf) {
        int index = items[item];
        switch (b[index - 1]) {
        case ClassWriter.INT:
            return readInt(index);
        case ClassWriter.FLOAT:
            return Float.intBitsToFloat(readInt(index));
        case ClassWriter.LONG:
            return readLong(index);
        case ClassWriter.DOUBLE:
            return Double.longBitsToDouble(readLong(index));
        case ClassWriter.CLASS:
            return Type.getObjectType(readUTF8(index, buf));
        case ClassWriter.STR:
            return readUTF8(index, buf);
        case ClassWriter.MTYPE:
            return Type.getMethodType(readUTF8(index, buf));
        default: // case ClassWriter.HANDLE_BASE + [1..9]:
            int tag = readByte(index);
            int[] items = this.items;
            int cpIndex = items[readUnsignedShort(index + 1)];
            boolean itf = b[cpIndex - 1] == ClassWriter.IMETH;
            String owner = readClass(cpIndex, buf);
            cpIndex = items[readUnsignedShort(cpIndex + 2)];
            String name = readUTF8(cpIndex, buf);
            String desc = readUTF8(cpIndex + 2, buf);
            return new Handle(tag, owner, name, desc, itf);
        }
    }
}
