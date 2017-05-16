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

/**
 * A {@link MethodVisitor} that generates methods in bytecode form. Each visit
 * method of this class appends the bytecode corresponding to the visited
 * instruction to a byte vector, in the order these methods are called.
 * 
 * @author Eric Bruneton
 * @author Eugene Kuleshov
 */
class MethodWriter extends MethodVisitor {

    /**
     * Pseudo access flag used to denote constructors.
     */
    static final int ACC_CONSTRUCTOR = 0x80000;

    /**
     * Frame has exactly the same locals as the previous stack map frame and
     * number of stack items is zero.
     */
    static final int SAME_FRAME = 0; // to 63 (0-3f)

    /**
     * Frame has exactly the same locals as the previous stack map frame and
     * number of stack items is 1
     */
    static final int SAME_LOCALS_1_STACK_ITEM_FRAME = 64; // to 127 (40-7f)

    /**
     * Reserved for future use
     */
    static final int RESERVED = 128;

    /**
     * Frame has exactly the same locals as the previous stack map frame and
     * number of stack items is 1. Offset is bigger then 63;
     */
    static final int SAME_LOCALS_1_STACK_ITEM_FRAME_EXTENDED = 247; // f7

    /**
     * Frame where current locals are the same as the locals in the previous
     * frame, except that the k last locals are absent. The value of k is given
     * by the formula 251-frame_type.
     */
    static final int CHOP_FRAME = 248; // to 250 (f8-fA)

    /**
     * Frame has exactly the same locals as the previous stack map frame and
     * number of stack items is zero. Offset is bigger then 63;
     */
    static final int SAME_FRAME_EXTENDED = 251; // fb

    /**
     * Frame where current locals are the same as the locals in the previous
     * frame, except that k additional locals are defined. The value of k is
     * given by the formula frame_type-251.
     */
    static final int APPEND_FRAME = 252; // to 254 // fc-fe

    /**
     * Full frame
     */
    static final int FULL_FRAME = 255; // ff

    /**
     * Indicates that the stack map frames must be recomputed from scratch. In
     * this case the maximum stack size and number of local variables is also
     * recomputed from scratch.
     * 
     * @see #compute
     */
    static final int FRAMES = 0;

    /**
     * Indicates that the stack map frames of type F_INSERT must be computed.
     * The other frames are not (re)computed. They should all be of type F_NEW
     * and should be sufficient to compute the content of the F_INSERT frames,
     * together with the bytecode instructions between a F_NEW and a F_INSERT
     * frame - and without any knowledge of the type hierarchy (by definition of
     * F_INSERT).
     *
     * @see #compute
     */
    static final int INSERTED_FRAMES = 1;

    /**
     * Indicates that the maximum stack size and number of local variables must
     * be automatically computed.
     * 
     * @see #compute
     */
    static final int MAXS = 2;

    /**
     * Indicates that nothing must be automatically computed.
     * 
     * @see #compute
     */
    static final int NOTHING = 3;

    /**
     * The class writer to which this method must be added.
     */
    final ClassWriter cw;

    /**
     * Access flags of this method.
     */
    private int access;

    /**
     * The index of the constant pool item that contains the name of this
     * method.
     */
    private final int name;

    /**
     * The index of the constant pool item that contains the descriptor of this
     * method.
     */
    private final int desc;

    /**
     * The descriptor of this method.
     */
    private final String descriptor;

    /**
     * The signature of this method.
     */
    String signature;

    /**
     * If not zero, indicates that the code of this method must be copied from
     * the ClassReader associated to this writer in <code>cw.cr</code>. More
     * precisely, this field gives the index of the first byte to copied from
     * <code>cw.cr.b</code>.
     */
    int classReaderOffset;

    /**
     * If not zero, indicates that the code of this method must be copied from
     * the ClassReader associated to this writer in <code>cw.cr</code>. More
     * precisely, this field gives the number of bytes to copied from
     * <code>cw.cr.b</code>.
     */
    int classReaderLength;

    /**
     * Number of exceptions that can be thrown by this method.
     */
    int exceptionCount;

    /**
     * The exceptions that can be thrown by this method. More precisely, this
     * array contains the indexes of the constant pool items that contain the
     * internal names of these exception classes.
     */
    int[] exceptions;

    /**
     * The annotation default attribute of this method. May be <tt>null</tt>.
     */
    private ByteVector annd;

    /**
     * The runtime visible annotations of this method. May be <tt>null</tt>.
     */
    private AnnotationWriter anns;

    /**
     * The runtime invisible annotations of this method. May be <tt>null</tt>.
     */
    private AnnotationWriter ianns;

    /**
     * The runtime visible type annotations of this method. May be <tt>null</tt>
     * .
     */
    private AnnotationWriter tanns;

    /**
     * The runtime invisible type annotations of this method. May be
     * <tt>null</tt>.
     */
    private AnnotationWriter itanns;

    /**
     * The runtime visible parameter annotations of this method. May be
     * <tt>null</tt>.
     */
    private AnnotationWriter[] panns;

    /**
     * The runtime invisible parameter annotations of this method. May be
     * <tt>null</tt>.
     */
    private AnnotationWriter[] ipanns;

    /**
     * The number of synthetic parameters of this method.
     */
    private int synthetics;

    /**
     * The non standard attributes of the method.
     */
    private Attribute attrs;

    /**
     * The bytecode of this method.
     */
    private ByteVector code = new ByteVector();

    /**
     * Maximum stack size of this method.
     */
    private int maxStack;

    /**
     * Maximum number of local variables for this method.
     */
    private int maxLocals;

    /**
     * Number of local variables in the current stack map frame.
     */
    private int currentLocals;

    /**
     * Number of stack map frames in the StackMapTable attribute.
     */
    private int frameCount;

    /**
     * The StackMapTable attribute.
     */
    private ByteVector stackMap;

    /**
     * The offset of the last frame that was written in the StackMapTable
     * attribute.
     */
    private int previousFrameOffset;

    /**
     * The last frame that was written in the StackMapTable attribute.
     * 
     * @see #frame
     */
    private int[] previousFrame;

    /**
     * The current stack map frame. The first element contains the offset of the
     * instruction to which the frame corresponds, the second element is the
     * number of locals and the third one is the number of stack elements. The
     * local variables start at index 3 and are followed by the operand stack
     * values. In summary frame[0] = offset, frame[1] = nLocal, frame[2] =
     * nStack, frame[3] = nLocal. All types are encoded as integers, with the
     * same format as the one used in {@link Label}, but limited to BASE types.
     */
    private int[] frame;

    /**
     * Number of elements in the exception handler list.
     */
    private int handlerCount;

    /**
     * The first element in the exception handler list.
     */
    private Handler firstHandler;

    /**
     * The last element in the exception handler list.
     */
    private Handler lastHandler;

    /**
     * Number of entries in the MethodParameters attribute.
     */
    private int methodParametersCount;

    /**
     * The MethodParameters attribute.
     */
    private ByteVector methodParameters;

    /**
     * Number of entries in the LocalVariableTable attribute.
     */
    private int localVarCount;

    /**
     * The LocalVariableTable attribute.
     */
    private ByteVector localVar;

    /**
     * Number of entries in the LocalVariableTypeTable attribute.
     */
    private int localVarTypeCount;

    /**
     * The LocalVariableTypeTable attribute.
     */
    private ByteVector localVarType;

    /**
     * Number of entries in the LineNumberTable attribute.
     */
    private int lineNumberCount;

    /**
     * The LineNumberTable attribute.
     */
    private ByteVector lineNumber;

    /**
     * The start offset of the last visited instruction.
     */
    private int lastCodeOffset;

    /**
     * The runtime visible type annotations of the code. May be <tt>null</tt>.
     */
    private AnnotationWriter ctanns;

    /**
     * The runtime invisible type annotations of the code. May be <tt>null</tt>.
     */
    private AnnotationWriter ictanns;

    /**
     * The non standard attributes of the method's code.
     */
    private Attribute cattrs;

    /**
     * The number of subroutines in this method.
     */
    private int subroutines;

    // ------------------------------------------------------------------------

    /*
     * Fields for the control flow graph analysis algorithm (used to compute the
     * maximum stack size). A control flow graph contains one node per "basic
     * block", and one edge per "jump" from one basic block to another. Each
     * node (i.e., each basic block) is represented by the Label object that
     * corresponds to the first instruction of this basic block. Each node also
     * stores the list of its successors in the graph, as a linked list of Edge
     * objects.
     */

    /**
     * Indicates what must be automatically computed.
     * 
     * @see #FRAMES
     * @see #INSERTED_FRAMES
     * @see #MAXS
     * @see #NOTHING
     */
    private final int compute;

    /**
     * A list of labels. This list is the list of basic blocks in the method,
     * i.e. a list of Label objects linked to each other by their
     * {@link Label#successor} field, in the order they are visited by
     * {@link MethodVisitor#visitLabel}, and starting with the first basic
     * block.
     */
    private Label labels;

    /**
     * The previous basic block.
     */
    private Label previousBlock;

    /**
     * The current basic block.
     */
    private Label currentBlock;

    /**
     * The (relative) stack size after the last visited instruction. This size
     * is relative to the beginning of the current basic block, i.e., the true
     * stack size after the last visited instruction is equal to the
     * {@link Label#inputStackTop beginStackSize} of the current basic block
     * plus <tt>stackSize</tt>.
     */
    private int stackSize;

    /**
     * The (relative) maximum stack size after the last visited instruction.
     * This size is relative to the beginning of the current basic block, i.e.,
     * the true maximum stack size after the last visited instruction is equal
     * to the {@link Label#inputStackTop beginStackSize} of the current basic
     * block plus <tt>stackSize</tt>.
     */
    private int maxStackSize;

    // ------------------------------------------------------------------------
    // Constructor
    // ------------------------------------------------------------------------

    /**
     * Constructs a new {@link MethodWriter}.
     * 
     * @param cw
     *            the class writer in which the method must be added.
     * @param access
     *            the method's access flags (see {@link Opcodes}).
     * @param name
     *            the method's name.
     * @param desc
     *            the method's descriptor (see {@link Type}).
     * @param signature
     *            the method's signature. May be <tt>null</tt>.
     * @param exceptions
     *            the internal names of the method's exceptions. May be
     *            <tt>null</tt>.
     * @param compute
     *            Indicates what must be automatically computed (see #compute).
     */
    MethodWriter(final ClassWriter cw, final int access, final String name,
            final String desc, final String signature,
            final String[] exceptions, final int compute) {
        super(Opcodes.ASM5);
        if (cw.firstMethod == null) {
            cw.firstMethod = this;
        } else {
            cw.lastMethod.mv = this;
        }
        cw.lastMethod = this;
        this.cw = cw;
        this.access = access;
        if ("<init>".equals(name)) {
            this.access |= ACC_CONSTRUCTOR;
        }
        this.name = cw.newUTF8(name);
        this.desc = cw.newUTF8(desc);
        this.descriptor = desc;
        if (ClassReader.SIGNATURES) {
            this.signature = signature;
        }
        if (exceptions != null && exceptions.length > 0) {
            exceptionCount = exceptions.length;
            this.exceptions = new int[exceptionCount];
            for (int i = 0; i < exceptionCount; ++i) {
                this.exceptions[i] = cw.newClass(exceptions[i]);
            }
        }
        this.compute = compute;
        if (compute != NOTHING) {
            // updates maxLocals
            int size = Type.getArgumentsAndReturnSizes(descriptor) >> 2;
            if ((access & Opcodes.ACC_STATIC) != 0) {
                --size;
            }
            maxLocals = size;
            currentLocals = size;
            // creates and visits the label for the first basic block
            labels = new Label();
            labels.status |= Label.PUSHED;
            visitLabel(labels);
        }
    }

    // ------------------------------------------------------------------------
    // Implementation of the MethodVisitor abstract class
    // ------------------------------------------------------------------------

    @Override
    public void visitParameter(String name, int access) {
        if (methodParameters == null) {
            methodParameters = new ByteVector();
        }
        ++methodParametersCount;
        methodParameters.putShort((name == null) ? 0 : cw.newUTF8(name))
                .putShort(access);
    }

    @Override
    public AnnotationVisitor visitAnnotationDefault() {
        if (!ClassReader.ANNOTATIONS) {
            return null;
        }
        annd = new ByteVector();
        return new AnnotationWriter(cw, false, annd, null, 0);
    }

    @Override
    public AnnotationVisitor visitAnnotation(final String desc,
            final boolean visible) {
        if (!ClassReader.ANNOTATIONS) {
            return null;
        }
        ByteVector bv = new ByteVector();
        // write type, and reserve space for values count
        bv.putShort(cw.newUTF8(desc)).putShort(0);
        AnnotationWriter aw = new AnnotationWriter(cw, true, bv, bv, 2);
        if (visible) {
            aw.next = anns;
            anns = aw;
        } else {
            aw.next = ianns;
            ianns = aw;
        }
        return aw;
    }

    @Override
    public AnnotationVisitor visitTypeAnnotation(final int typeRef,
            final TypePath typePath, final String desc, final boolean visible) {
        if (!ClassReader.ANNOTATIONS) {
            return null;
        }
        ByteVector bv = new ByteVector();
        // write target_type and target_info
        AnnotationWriter.putTarget(typeRef, typePath, bv);
        // write type, and reserve space for values count
        bv.putShort(cw.newUTF8(desc)).putShort(0);
        AnnotationWriter aw = new AnnotationWriter(cw, true, bv, bv,
                bv.length - 2);
        if (visible) {
            aw.next = tanns;
            tanns = aw;
        } else {
            aw.next = itanns;
            itanns = aw;
        }
        return aw;
    }

    @Override
    public AnnotationVisitor visitParameterAnnotation(final int parameter,
            final String desc, final boolean visible) {
        if (!ClassReader.ANNOTATIONS) {
            return null;
        }
        ByteVector bv = new ByteVector();
        if ("Ljava/lang/Synthetic;".equals(desc)) {
            // workaround for a bug in javac with synthetic parameters
            // see ClassReader.readParameterAnnotations
            synthetics = Math.max(synthetics, parameter + 1);
            return new AnnotationWriter(cw, false, bv, null, 0);
        }
        // write type, and reserve space for values count
        bv.putShort(cw.newUTF8(desc)).putShort(0);
        AnnotationWriter aw = new AnnotationWriter(cw, true, bv, bv, 2);
        if (visible) {
            if (panns == null) {
                panns = new AnnotationWriter[Type.getArgumentTypes(descriptor).length];
            }
            aw.next = panns[parameter];
            panns[parameter] = aw;
        } else {
            if (ipanns == null) {
                ipanns = new AnnotationWriter[Type.getArgumentTypes(descriptor).length];
            }
            aw.next = ipanns[parameter];
            ipanns[parameter] = aw;
        }
        return aw;
    }

    @Override
    public void visitAttribute(final Attribute attr) {
        if (attr.isCodeAttribute()) {
            attr.next = cattrs;
            cattrs = attr;
        } else {
            attr.next = attrs;
            attrs = attr;
        }
    }

    @Override
    public void visitCode() {
    }

    @Override
    public void visitFrame(final int type, final int nLocal,
            final Object[] local, final int nStack, final Object[] stack) {
        if (!ClassReader.FRAMES || compute == FRAMES) {
            return;
        }

        if (compute == INSERTED_FRAMES) {
            if (currentBlock.frame == null) {
                // This should happen only once, for the implicit first frame
                // (which is explicitly visited in ClassReader if the
                // EXPAND_ASM_INSNS option is used).
                currentBlock.frame = new CurrentFrame();
                currentBlock.frame.owner = currentBlock;
                currentBlock.frame.initInputFrame(cw, access,
                        Type.getArgumentTypes(descriptor), nLocal);
                visitImplicitFirstFrame();
            } else {
                if (type == Opcodes.F_NEW) {
                    currentBlock.frame.set(cw, nLocal, local, nStack, stack);
                } else {
                    // In this case type is equal to F_INSERT by hypothesis, and
                    // currentBlock.frame contains the stack map frame at the
                    // current instruction, computed from the last F_NEW frame
                    // and the bytecode instructions in between (via calls to
                    // CurrentFrame#execute).
                }
                visitFrame(currentBlock.frame);
            }
        } else if (type == Opcodes.F_NEW) {
            if (previousFrame == null) {
                visitImplicitFirstFrame();
            }
            currentLocals = nLocal;
            int frameIndex = startFrame(code.length, nLocal, nStack);
            for (int i = 0; i < nLocal; ++i) {
                if (local[i] instanceof String) {
                    frame[frameIndex++] = Frame.OBJECT
                            | cw.addType((String) local[i]);
                } else if (local[i] instanceof Integer) {
                    frame[frameIndex++] = ((Integer) local[i]).intValue();
                } else {
                    frame[frameIndex++] = Frame.UNINITIALIZED
                            | cw.addUninitializedType("",
                                    ((Label) local[i]).position);
                }
            }
            for (int i = 0; i < nStack; ++i) {
                if (stack[i] instanceof String) {
                    frame[frameIndex++] = Frame.OBJECT
                            | cw.addType((String) stack[i]);
                } else if (stack[i] instanceof Integer) {
                    frame[frameIndex++] = ((Integer) stack[i]).intValue();
                } else {
                    frame[frameIndex++] = Frame.UNINITIALIZED
                            | cw.addUninitializedType("",
                                    ((Label) stack[i]).position);
                }
            }
            endFrame();
        } else {
            int delta;
            if (stackMap == null) {
                stackMap = new ByteVector();
                delta = code.length;
            } else {
                delta = code.length - previousFrameOffset - 1;
                if (delta < 0) {
                    if (type == Opcodes.F_SAME) {
                        return;
                    } else {
                        throw new IllegalStateException();
                    }
                }
            }

            switch (type) {
            case Opcodes.F_FULL:
                currentLocals = nLocal;
                stackMap.putByte(FULL_FRAME).putShort(delta).putShort(nLocal);
                for (int i = 0; i < nLocal; ++i) {
                    writeFrameType(local[i]);
                }
                stackMap.putShort(nStack);
                for (int i = 0; i < nStack; ++i) {
                    writeFrameType(stack[i]);
                }
                break;
            case Opcodes.F_APPEND:
                currentLocals += nLocal;
                stackMap.putByte(SAME_FRAME_EXTENDED + nLocal).putShort(delta);
                for (int i = 0; i < nLocal; ++i) {
                    writeFrameType(local[i]);
                }
                break;
            case Opcodes.F_CHOP:
                currentLocals -= nLocal;
                stackMap.putByte(SAME_FRAME_EXTENDED - nLocal).putShort(delta);
                break;
            case Opcodes.F_SAME:
                if (delta < 64) {
                    stackMap.putByte(delta);
                } else {
                    stackMap.putByte(SAME_FRAME_EXTENDED).putShort(delta);
                }
                break;
            case Opcodes.F_SAME1:
                if (delta < 64) {
                    stackMap.putByte(SAME_LOCALS_1_STACK_ITEM_FRAME + delta);
                } else {
                    stackMap.putByte(SAME_LOCALS_1_STACK_ITEM_FRAME_EXTENDED)
                            .putShort(delta);
                }
                writeFrameType(stack[0]);
                break;
            }

            previousFrameOffset = code.length;
            ++frameCount;
        }

        maxStack = Math.max(maxStack, nStack);
        maxLocals = Math.max(maxLocals, currentLocals);
    }

    @Override
    public void visitInsn(final int opcode) {
        lastCodeOffset = code.length;
        // adds the instruction to the bytecode of the method
        code.putByte(opcode);
        // update currentBlock
        // Label currentBlock = this.currentBlock;
        if (currentBlock != null) {
            if (compute == FRAMES || compute == INSERTED_FRAMES) {
                currentBlock.frame.execute(opcode, 0, null, null);
            } else {
                // updates current and max stack sizes
                int size = stackSize + Frame.SIZE[opcode];
                if (size > maxStackSize) {
                    maxStackSize = size;
                }
                stackSize = size;
            }
            // if opcode == ATHROW or xRETURN, ends current block (no successor)
            if ((opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN)
                    || opcode == Opcodes.ATHROW) {
                noSuccessor();
            }
        }
    }

    @Override
    public void visitIntInsn(final int opcode, final int operand) {
        lastCodeOffset = code.length;
        // Label currentBlock = this.currentBlock;
        if (currentBlock != null) {
            if (compute == FRAMES || compute == INSERTED_FRAMES) {
                currentBlock.frame.execute(opcode, operand, null, null);
            } else if (opcode != Opcodes.NEWARRAY) {
                // updates current and max stack sizes only for NEWARRAY
                // (stack size variation = 0 for BIPUSH or SIPUSH)
                int size = stackSize + 1;
                if (size > maxStackSize) {
                    maxStackSize = size;
                }
                stackSize = size;
            }
        }
        // adds the instruction to the bytecode of the method
        if (opcode == Opcodes.SIPUSH) {
            code.put12(opcode, operand);
        } else { // BIPUSH or NEWARRAY
            code.put11(opcode, operand);
        }
    }

    @Override
    public void visitVarInsn(final int opcode, final int var) {
        lastCodeOffset = code.length;
        // Label currentBlock = this.currentBlock;
        if (currentBlock != null) {
            if (compute == FRAMES || compute == INSERTED_FRAMES) {
                currentBlock.frame.execute(opcode, var, null, null);
            } else {
                // updates current and max stack sizes
                if (opcode == Opcodes.RET) {
                    // no stack change, but end of current block (no successor)
                    currentBlock.status |= Label.RET;
                    // save 'stackSize' here for future use
                    // (see {@link #findSubroutineSuccessors})
                    currentBlock.inputStackTop = stackSize;
                    noSuccessor();
                } else { // xLOAD or xSTORE
                    int size = stackSize + Frame.SIZE[opcode];
                    if (size > maxStackSize) {
                        maxStackSize = size;
                    }
                    stackSize = size;
                }
            }
        }
        if (compute != NOTHING) {
            // updates max locals
            int n;
            if (opcode == Opcodes.LLOAD || opcode == Opcodes.DLOAD
                    || opcode == Opcodes.LSTORE || opcode == Opcodes.DSTORE) {
                n = var + 2;
            } else {
                n = var + 1;
            }
            if (n > maxLocals) {
                maxLocals = n;
            }
        }
        // adds the instruction to the bytecode of the method
        if (var < 4 && opcode != Opcodes.RET) {
            int opt;
            if (opcode < Opcodes.ISTORE) {
                /* ILOAD_0 */
                opt = 26 + ((opcode - Opcodes.ILOAD) << 2) + var;
            } else {
                /* ISTORE_0 */
                opt = 59 + ((opcode - Opcodes.ISTORE) << 2) + var;
            }
            code.putByte(opt);
        } else if (var >= 256) {
            code.putByte(196 /* WIDE */).put12(opcode, var);
        } else {
            code.put11(opcode, var);
        }
        if (opcode >= Opcodes.ISTORE && compute == FRAMES && handlerCount > 0) {
            visitLabel(new Label());
        }
    }

    @Override
    public void visitTypeInsn(final int opcode, final String type) {
        lastCodeOffset = code.length;
        Item i = cw.newClassItem(type);
        // Label currentBlock = this.currentBlock;
        if (currentBlock != null) {
            if (compute == FRAMES || compute == INSERTED_FRAMES) {
                currentBlock.frame.execute(opcode, code.length, cw, i);
            } else if (opcode == Opcodes.NEW) {
                // updates current and max stack sizes only if opcode == NEW
                // (no stack change for ANEWARRAY, CHECKCAST, INSTANCEOF)
                int size = stackSize + 1;
                if (size > maxStackSize) {
                    maxStackSize = size;
                }
                stackSize = size;
            }
        }
        // adds the instruction to the bytecode of the method
        code.put12(opcode, i.index);
    }

    @Override
    public void visitFieldInsn(final int opcode, final String owner,
            final String name, final String desc) {
        lastCodeOffset = code.length;
        Item i = cw.newFieldItem(owner, name, desc);
        // Label currentBlock = this.currentBlock;
        if (currentBlock != null) {
            if (compute == FRAMES || compute == INSERTED_FRAMES) {
                currentBlock.frame.execute(opcode, 0, cw, i);
            } else {
                int size;
                // computes the stack size variation
                char c = desc.charAt(0);
                switch (opcode) {
                case Opcodes.GETSTATIC:
                    size = stackSize + (c == 'D' || c == 'J' ? 2 : 1);
                    break;
                case Opcodes.PUTSTATIC:
                    size = stackSize + (c == 'D' || c == 'J' ? -2 : -1);
                    break;
                case Opcodes.GETFIELD:
                    size = stackSize + (c == 'D' || c == 'J' ? 1 : 0);
                    break;
                // case Constants.PUTFIELD:
                default:
                    size = stackSize + (c == 'D' || c == 'J' ? -3 : -2);
                    break;
                }
                // updates current and max stack sizes
                if (size > maxStackSize) {
                    maxStackSize = size;
                }
                stackSize = size;
            }
        }
        // adds the instruction to the bytecode of the method
        code.put12(opcode, i.index);
    }

    @Override
    public void visitMethodInsn(final int opcode, final String owner,
            final String name, final String desc, final boolean itf) {
        lastCodeOffset = code.length;
        Item i = cw.newMethodItem(owner, name, desc, itf);
        int argSize = i.intVal;
        // Label currentBlock = this.currentBlock;
        if (currentBlock != null) {
            if (compute == FRAMES || compute == INSERTED_FRAMES) {
                currentBlock.frame.execute(opcode, 0, cw, i);
            } else {
                /*
                 * computes the stack size variation. In order not to recompute
                 * several times this variation for the same Item, we use the
                 * intVal field of this item to store this variation, once it
                 * has been computed. More precisely this intVal field stores
                 * the sizes of the arguments and of the return value
                 * corresponding to desc.
                 */
                if (argSize == 0) {
                    // the above sizes have not been computed yet,
                    // so we compute them...
                    argSize = Type.getArgumentsAndReturnSizes(desc);
                    // ... and we save them in order
                    // not to recompute them in the future
                    i.intVal = argSize;
                }
                int size;
                if (opcode == Opcodes.INVOKESTATIC) {
                    size = stackSize - (argSize >> 2) + (argSize & 0x03) + 1;
                } else {
                    size = stackSize - (argSize >> 2) + (argSize & 0x03);
                }
                // updates current and max stack sizes
                if (size > maxStackSize) {
                    maxStackSize = size;
                }
                stackSize = size;
            }
        }
        // adds the instruction to the bytecode of the method
        if (opcode == Opcodes.INVOKEINTERFACE) {
            if (argSize == 0) {
                argSize = Type.getArgumentsAndReturnSizes(desc);
                i.intVal = argSize;
            }
            code.put12(Opcodes.INVOKEINTERFACE, i.index).put11(argSize >> 2, 0);
        } else {
            code.put12(opcode, i.index);
        }
    }

    @Override
    public void visitInvokeDynamicInsn(final String name, final String desc,
            final Handle bsm, final Object... bsmArgs) {
        lastCodeOffset = code.length;
        Item i = cw.newInvokeDynamicItem(name, desc, bsm, bsmArgs);
        int argSize = i.intVal;
        // Label currentBlock = this.currentBlock;
        if (currentBlock != null) {
            if (compute == FRAMES || compute == INSERTED_FRAMES) {
                currentBlock.frame.execute(Opcodes.INVOKEDYNAMIC, 0, cw, i);
            } else {
                /*
                 * computes the stack size variation. In order not to recompute
                 * several times this variation for the same Item, we use the
                 * intVal field of this item to store this variation, once it
                 * has been computed. More precisely this intVal field stores
                 * the sizes of the arguments and of the return value
                 * corresponding to desc.
                 */
                if (argSize == 0) {
                    // the above sizes have not been computed yet,
                    // so we compute them...
                    argSize = Type.getArgumentsAndReturnSizes(desc);
                    // ... and we save them in order
                    // not to recompute them in the future
                    i.intVal = argSize;
                }
                int size = stackSize - (argSize >> 2) + (argSize & 0x03) + 1;

                // updates current and max stack sizes
                if (size > maxStackSize) {
                    maxStackSize = size;
                }
                stackSize = size;
            }
        }
        // adds the instruction to the bytecode of the method
        code.put12(Opcodes.INVOKEDYNAMIC, i.index);
        code.putShort(0);
    }

    @Override
    public void visitJumpInsn(int opcode, final Label label) {
        boolean isWide = opcode >= 200; // GOTO_W
        opcode = isWide ? opcode - 33 : opcode;
        lastCodeOffset = code.length;
        Label nextInsn = null;
        // Label currentBlock = this.currentBlock;
        if (currentBlock != null) {
            if (compute == FRAMES) {
                currentBlock.frame.execute(opcode, 0, null, null);
                // 'label' is the target of a jump instruction
                label.getFirst().status |= Label.TARGET;
                // adds 'label' as a successor of this basic block
                addSuccessor(Edge.NORMAL, label);
                if (opcode != Opcodes.GOTO) {
                    // creates a Label for the next basic block
                    nextInsn = new Label();
                }
            } else if (compute == INSERTED_FRAMES) {
                currentBlock.frame.execute(opcode, 0, null, null);
            } else {
                if (opcode == Opcodes.JSR) {
                    if ((label.status & Label.SUBROUTINE) == 0) {
                        label.status |= Label.SUBROUTINE;
                        ++subroutines;
                    }
                    currentBlock.status |= Label.JSR;
                    addSuccessor(stackSize + 1, label);
                    // creates a Label for the next basic block
                    nextInsn = new Label();
                    /*
                     * note that, by construction in this method, a JSR block
                     * has at least two successors in the control flow graph:
                     * the first one leads the next instruction after the JSR,
                     * while the second one leads to the JSR target.
                     */
                } else {
                    // updates current stack size (max stack size unchanged
                    // because stack size variation always negative in this
                    // case)
                    stackSize += Frame.SIZE[opcode];
                    addSuccessor(stackSize, label);
                }
            }
        }
        // adds the instruction to the bytecode of the method
        if ((label.status & Label.RESOLVED) != 0
                && label.position - code.length < Short.MIN_VALUE) {
            /*
             * case of a backward jump with an offset < -32768. In this case we
             * automatically replace GOTO with GOTO_W, JSR with JSR_W and IFxxx
             * <l> with IFNOTxxx <l'> GOTO_W <l>, where IFNOTxxx is the
             * "opposite" opcode of IFxxx (i.e., IFNE for IFEQ) and where <l'>
             * designates the instruction just after the GOTO_W.
             */
            if (opcode == Opcodes.GOTO) {
                code.putByte(200); // GOTO_W
            } else if (opcode == Opcodes.JSR) {
                code.putByte(201); // JSR_W
            } else {
                // if the IF instruction is transformed into IFNOT GOTO_W the
                // next instruction becomes the target of the IFNOT instruction
                if (nextInsn != null) {
                    nextInsn.status |= Label.TARGET;
                }
                code.putByte(opcode <= 166 ? ((opcode + 1) ^ 1) - 1
                        : opcode ^ 1);
                code.putShort(8); // jump offset
                code.putByte(200); // GOTO_W
            }
            label.put(this, code, code.length - 1, true);
        } else if (isWide) {
            /*
             * case of a GOTO_W or JSR_W specified by the user (normally
             * ClassReader when used to resize instructions). In this case we
             * keep the original instruction.
             */
            code.putByte(opcode + 33);
            label.put(this, code, code.length - 1, true);
        } else {
            /*
             * case of a backward jump with an offset >= -32768, or of a forward
             * jump with, of course, an unknown offset. In these cases we store
             * the offset in 2 bytes (which will be increased in
             * resizeInstructions, if needed).
             */
            code.putByte(opcode);
            label.put(this, code, code.length - 1, false);
        }
        if (currentBlock != null) {
            if (nextInsn != null) {
                // if the jump instruction is not a GOTO, the next instruction
                // is also a successor of this instruction. Calling visitLabel
                // adds the label of this next instruction as a successor of the
                // current block, and starts a new basic block
                visitLabel(nextInsn);
            }
            if (opcode == Opcodes.GOTO) {
                noSuccessor();
            }
        }
    }

    @Override
    public void visitLabel(final Label label) {
        // resolves previous forward references to label, if any
        cw.hasAsmInsns |= label.resolve(this, code.length, code.data);
        // updates currentBlock
        if ((label.status & Label.DEBUG) != 0) {
            return;
        }
        if (compute == FRAMES) {
            if (currentBlock != null) {
                if (label.position == currentBlock.position) {
                    // successive labels, do not start a new basic block
                    currentBlock.status |= (label.status & Label.TARGET);
                    label.frame = currentBlock.frame;
                    return;
                }
                // ends current block (with one new successor)
                addSuccessor(Edge.NORMAL, label);
            }
            // begins a new current block
            currentBlock = label;
            if (label.frame == null) {
                label.frame = new Frame();
                label.frame.owner = label;
            }
            // updates the basic block list
            if (previousBlock != null) {
                if (label.position == previousBlock.position) {
                    previousBlock.status |= (label.status & Label.TARGET);
                    label.frame = previousBlock.frame;
                    currentBlock = previousBlock;
                    return;
                }
                previousBlock.successor = label;
            }
            previousBlock = label;
        } else if (compute == INSERTED_FRAMES) {
            if (currentBlock == null) {
                // This case should happen only once, for the visitLabel call in
                // the constructor. Indeed, if compute is equal to
                // INSERTED_FRAMES currentBlock can not be set back to null (see
                // #noSuccessor).
                currentBlock = label;
            } else {
                // Updates the frame owner so that a correct frame offset is
                // computed in visitFrame(Frame).
                currentBlock.frame.owner = label;
            }
        } else if (compute == MAXS) {
            if (currentBlock != null) {
                // ends current block (with one new successor)
                currentBlock.outputStackMax = maxStackSize;
                addSuccessor(stackSize, label);
            }
            // begins a new current block
            currentBlock = label;
            // resets the relative current and max stack sizes
            stackSize = 0;
            maxStackSize = 0;
            // updates the basic block list
            if (previousBlock != null) {
                previousBlock.successor = label;
            }
            previousBlock = label;
        }
    }

    @Override
    public void visitLdcInsn(final Object cst) {
        lastCodeOffset = code.length;
        Item i = cw.newConstItem(cst);
        // Label currentBlock = this.currentBlock;
        if (currentBlock != null) {
            if (compute == FRAMES || compute == INSERTED_FRAMES) {
                currentBlock.frame.execute(Opcodes.LDC, 0, cw, i);
            } else {
                int size;
                // computes the stack size variation
                if (i.type == ClassWriter.LONG || i.type == ClassWriter.DOUBLE) {
                    size = stackSize + 2;
                } else {
                    size = stackSize + 1;
                }
                // updates current and max stack sizes
                if (size > maxStackSize) {
                    maxStackSize = size;
                }
                stackSize = size;
            }
        }
        // adds the instruction to the bytecode of the method
        int index = i.index;
        if (i.type == ClassWriter.LONG || i.type == ClassWriter.DOUBLE) {
            code.put12(20 /* LDC2_W */, index);
        } else if (index >= 256) {
            code.put12(19 /* LDC_W */, index);
        } else {
            code.put11(Opcodes.LDC, index);
        }
    }

    @Override
    public void visitIincInsn(final int var, final int increment) {
        lastCodeOffset = code.length;
        if (currentBlock != null) {
            if (compute == FRAMES || compute == INSERTED_FRAMES) {
                currentBlock.frame.execute(Opcodes.IINC, var, null, null);
            }
        }
        if (compute != NOTHING) {
            // updates max locals
            int n = var + 1;
            if (n > maxLocals) {
                maxLocals = n;
            }
        }
        // adds the instruction to the bytecode of the method
        if ((var > 255) || (increment > 127) || (increment < -128)) {
            code.putByte(196 /* WIDE */).put12(Opcodes.IINC, var)
                    .putShort(increment);
        } else {
            code.putByte(Opcodes.IINC).put11(var, increment);
        }
    }

    @Override
    public void visitTableSwitchInsn(final int min, final int max,
            final Label dflt, final Label... labels) {
        lastCodeOffset = code.length;
        // adds the instruction to the bytecode of the method
        int source = code.length;
        code.putByte(Opcodes.TABLESWITCH);
        code.putByteArray(null, 0, (4 - code.length % 4) % 4);
        dflt.put(this, code, source, true);
        code.putInt(min).putInt(max);
        for (int i = 0; i < labels.length; ++i) {
            labels[i].put(this, code, source, true);
        }
        // updates currentBlock
        visitSwitchInsn(dflt, labels);
    }

    @Override
    public void visitLookupSwitchInsn(final Label dflt, final int[] keys,
            final Label[] labels) {
        lastCodeOffset = code.length;
        // adds the instruction to the bytecode of the method
        int source = code.length;
        code.putByte(Opcodes.LOOKUPSWITCH);
        code.putByteArray(null, 0, (4 - code.length % 4) % 4);
        dflt.put(this, code, source, true);
        code.putInt(labels.length);
        for (int i = 0; i < labels.length; ++i) {
            code.putInt(keys[i]);
            labels[i].put(this, code, source, true);
        }
        // updates currentBlock
        visitSwitchInsn(dflt, labels);
    }

    private void visitSwitchInsn(final Label dflt, final Label[] labels) {
        // Label currentBlock = this.currentBlock;
        if (currentBlock != null) {
            if (compute == FRAMES) {
                currentBlock.frame.execute(Opcodes.LOOKUPSWITCH, 0, null, null);
                // adds current block successors
                addSuccessor(Edge.NORMAL, dflt);
                dflt.getFirst().status |= Label.TARGET;
                for (int i = 0; i < labels.length; ++i) {
                    addSuccessor(Edge.NORMAL, labels[i]);
                    labels[i].getFirst().status |= Label.TARGET;
                }
            } else {
                // updates current stack size (max stack size unchanged)
                --stackSize;
                // adds current block successors
                addSuccessor(stackSize, dflt);
                for (int i = 0; i < labels.length; ++i) {
                    addSuccessor(stackSize, labels[i]);
                }
            }
            // ends current block
            noSuccessor();
        }
    }

    @Override
    public void visitMultiANewArrayInsn(final String desc, final int dims) {
        lastCodeOffset = code.length;
        Item i = cw.newClassItem(desc);
        // Label currentBlock = this.currentBlock;
        if (currentBlock != null) {
            if (compute == FRAMES || compute == INSERTED_FRAMES) {
                currentBlock.frame.execute(Opcodes.MULTIANEWARRAY, dims, cw, i);
            } else {
                // updates current stack size (max stack size unchanged because
                // stack size variation always negative or null)
                stackSize += 1 - dims;
            }
        }
        // adds the instruction to the bytecode of the method
        code.put12(Opcodes.MULTIANEWARRAY, i.index).putByte(dims);
    }

    @Override
    public AnnotationVisitor visitInsnAnnotation(int typeRef,
            TypePath typePath, String desc, boolean visible) {
        if (!ClassReader.ANNOTATIONS) {
            return null;
        }
        ByteVector bv = new ByteVector();
        // write target_type and target_info
        typeRef = (typeRef & 0xFF0000FF) | (lastCodeOffset << 8);
        AnnotationWriter.putTarget(typeRef, typePath, bv);
        // write type, and reserve space for values count
        bv.putShort(cw.newUTF8(desc)).putShort(0);
        AnnotationWriter aw = new AnnotationWriter(cw, true, bv, bv,
                bv.length - 2);
        if (visible) {
            aw.next = ctanns;
            ctanns = aw;
        } else {
            aw.next = ictanns;
            ictanns = aw;
        }
        return aw;
    }

    @Override
    public void visitTryCatchBlock(final Label start, final Label end,
            final Label handler, final String type) {
        ++handlerCount;
        Handler h = new Handler();
        h.start = start;
        h.end = end;
        h.handler = handler;
        h.desc = type;
        h.type = type != null ? cw.newClass(type) : 0;
        if (lastHandler == null) {
            firstHandler = h;
        } else {
            lastHandler.next = h;
        }
        lastHandler = h;
    }

    @Override
    public AnnotationVisitor visitTryCatchAnnotation(int typeRef,
            TypePath typePath, String desc, boolean visible) {
        if (!ClassReader.ANNOTATIONS) {
            return null;
        }
        ByteVector bv = new ByteVector();
        // write target_type and target_info
        AnnotationWriter.putTarget(typeRef, typePath, bv);
        // write type, and reserve space for values count
        bv.putShort(cw.newUTF8(desc)).putShort(0);
        AnnotationWriter aw = new AnnotationWriter(cw, true, bv, bv,
                bv.length - 2);
        if (visible) {
            aw.next = ctanns;
            ctanns = aw;
        } else {
            aw.next = ictanns;
            ictanns = aw;
        }
        return aw;
    }

    @Override
    public void visitLocalVariable(final String name, final String desc,
            final String signature, final Label start, final Label end,
            final int index) {
        if (signature != null) {
            if (localVarType == null) {
                localVarType = new ByteVector();
            }
            ++localVarTypeCount;
            localVarType.putShort(start.position)
                    .putShort(end.position - start.position)
                    .putShort(cw.newUTF8(name)).putShort(cw.newUTF8(signature))
                    .putShort(index);
        }
        if (localVar == null) {
            localVar = new ByteVector();
        }
        ++localVarCount;
        localVar.putShort(start.position)
                .putShort(end.position - start.position)
                .putShort(cw.newUTF8(name)).putShort(cw.newUTF8(desc))
                .putShort(index);
        if (compute != NOTHING) {
            // updates max locals
            char c = desc.charAt(0);
            int n = index + (c == 'J' || c == 'D' ? 2 : 1);
            if (n > maxLocals) {
                maxLocals = n;
            }
        }
    }

    @Override
    public AnnotationVisitor visitLocalVariableAnnotation(int typeRef,
            TypePath typePath, Label[] start, Label[] end, int[] index,
            String desc, boolean visible) {
        if (!ClassReader.ANNOTATIONS) {
            return null;
        }
        ByteVector bv = new ByteVector();
        // write target_type and target_info
        bv.putByte(typeRef >>> 24).putShort(start.length);
        for (int i = 0; i < start.length; ++i) {
            bv.putShort(start[i].position)
                    .putShort(end[i].position - start[i].position)
                    .putShort(index[i]);
        }
        if (typePath == null) {
            bv.putByte(0);
        } else {
            int length = typePath.b[typePath.offset] * 2 + 1;
            bv.putByteArray(typePath.b, typePath.offset, length);
        }
        // write type, and reserve space for values count
        bv.putShort(cw.newUTF8(desc)).putShort(0);
        AnnotationWriter aw = new AnnotationWriter(cw, true, bv, bv,
                bv.length - 2);
        if (visible) {
            aw.next = ctanns;
            ctanns = aw;
        } else {
            aw.next = ictanns;
            ictanns = aw;
        }
        return aw;
    }

    @Override
    public void visitLineNumber(final int line, final Label start) {
        if (lineNumber == null) {
            lineNumber = new ByteVector();
        }
        ++lineNumberCount;
        lineNumber.putShort(start.position);
        lineNumber.putShort(line);
    }

    @Override
    public void visitMaxs(final int maxStack, final int maxLocals) {
        if (ClassReader.FRAMES && compute == FRAMES) {
            // completes the control flow graph with exception handler blocks
            Handler handler = firstHandler;
            while (handler != null) {
                Label l = handler.start.getFirst();
                Label h = handler.handler.getFirst();
                Label e = handler.end.getFirst();
                // computes the kind of the edges to 'h'
                String t = handler.desc == null ? "java/lang/Throwable"
                        : handler.desc;
                int kind = Frame.OBJECT | cw.addType(t);
                // h is an exception handler
                h.status |= Label.TARGET;
                // adds 'h' as a successor of labels between 'start' and 'end'
                while (l != e) {
                    // creates an edge to 'h'
                    Edge b = new Edge();
                    b.info = kind;
                    b.successor = h;
                    // adds it to the successors of 'l'
                    b.next = l.successors;
                    l.successors = b;
                    // goes to the next label
                    l = l.successor;
                }
                handler = handler.next;
            }

            // creates and visits the first (implicit) frame
            Frame f = labels.frame;
            f.initInputFrame(cw, access, Type.getArgumentTypes(descriptor),
                    this.maxLocals);
            visitFrame(f);

            /*
             * fix point algorithm: mark the first basic block as 'changed'
             * (i.e. put it in the 'changed' list) and, while there are changed
             * basic blocks, choose one, mark it as unchanged, and update its
             * successors (which can be changed in the process).
             */
            int max = 0;
            Label changed = labels;
            while (changed != null) {
                // removes a basic block from the list of changed basic blocks
                Label l = changed;
                changed = changed.next;
                l.next = null;
                f = l.frame;
                // a reachable jump target must be stored in the stack map
                if ((l.status & Label.TARGET) != 0) {
                    l.status |= Label.STORE;
                }
                // all visited labels are reachable, by definition
                l.status |= Label.REACHABLE;
                // updates the (absolute) maximum stack size
                int blockMax = f.inputStack.length + l.outputStackMax;
                if (blockMax > max) {
                    max = blockMax;
                }
                // updates the successors of the current basic block
                Edge e = l.successors;
                while (e != null) {
                    Label n = e.successor.getFirst();
                    boolean change = f.merge(cw, n.frame, e.info);
                    if (change && n.next == null) {
                        // if n has changed and is not already in the 'changed'
                        // list, adds it to this list
                        n.next = changed;
                        changed = n;
                    }
                    e = e.next;
                }
            }

            // visits all the frames that must be stored in the stack map
            Label l = labels;
            while (l != null) {
                f = l.frame;
                if ((l.status & Label.STORE) != 0) {
                    visitFrame(f);
                }
                if ((l.status & Label.REACHABLE) == 0) {
                    // finds start and end of dead basic block
                    Label k = l.successor;
                    int start = l.position;
                    int end = (k == null ? code.length : k.position) - 1;
                    // if non empty basic block
                    if (end >= start) {
                        max = Math.max(max, 1);
                        // replaces instructions with NOP ... NOP ATHROW
                        for (int i = start; i < end; ++i) {
                            code.data[i] = Opcodes.NOP;
                        }
                        code.data[end] = (byte) Opcodes.ATHROW;
                        // emits a frame for this unreachable block
                        int frameIndex = startFrame(start, 0, 1);
                        frame[frameIndex] = Frame.OBJECT
                                | cw.addType("java/lang/Throwable");
                        endFrame();
                        // removes the start-end range from the exception
                        // handlers
                        firstHandler = Handler.remove(firstHandler, l, k);
                    }
                }
                l = l.successor;
            }

            handler = firstHandler;
            handlerCount = 0;
            while (handler != null) {
                handlerCount += 1;
                handler = handler.next;
            }

            this.maxStack = max;
        } else if (compute == MAXS) {
            // completes the control flow graph with exception handler blocks
            Handler handler = firstHandler;
            while (handler != null) {
                Label l = handler.start;
                Label h = handler.handler;
                Label e = handler.end;
                // adds 'h' as a successor of labels between 'start' and 'end'
                while (l != e) {
                    // creates an edge to 'h'
                    Edge b = new Edge();
                    b.info = Edge.EXCEPTION;
                    b.successor = h;
                    // adds it to the successors of 'l'
                    if ((l.status & Label.JSR) == 0) {
                        b.next = l.successors;
                        l.successors = b;
                    } else {
                        // if l is a JSR block, adds b after the first two edges
                        // to preserve the hypothesis about JSR block successors
                        // order (see {@link #visitJumpInsn})
                        b.next = l.successors.next.next;
                        l.successors.next.next = b;
                    }
                    // goes to the next label
                    l = l.successor;
                }
                handler = handler.next;
            }

            if (subroutines > 0) {
                // completes the control flow graph with the RET successors
                /*
                 * first step: finds the subroutines. This step determines, for
                 * each basic block, to which subroutine(s) it belongs.
                 */
                // finds the basic blocks that belong to the "main" subroutine
                int id = 0;
                labels.visitSubroutine(null, 1, subroutines);
                // finds the basic blocks that belong to the real subroutines
                Label l = labels;
                while (l != null) {
                    if ((l.status & Label.JSR) != 0) {
                        // the subroutine is defined by l's TARGET, not by l
                        Label subroutine = l.successors.next.successor;
                        // if this subroutine has not been visited yet...
                        if ((subroutine.status & Label.VISITED) == 0) {
                            // ...assigns it a new id and finds its basic blocks
                            id += 1;
                            subroutine.visitSubroutine(null, (id / 32L) << 32
                                    | (1L << (id % 32)), subroutines);
                        }
                    }
                    l = l.successor;
                }
                // second step: finds the successors of RET blocks
                l = labels;
                while (l != null) {
                    if ((l.status & Label.JSR) != 0) {
                        Label L = labels;
                        while (L != null) {
                            L.status &= ~Label.VISITED2;
                            L = L.successor;
                        }
                        // the subroutine is defined by l's TARGET, not by l
                        Label subroutine = l.successors.next.successor;
                        subroutine.visitSubroutine(l, 0, subroutines);
                    }
                    l = l.successor;
                }
            }

            /*
             * control flow analysis algorithm: while the block stack is not
             * empty, pop a block from this stack, update the max stack size,
             * compute the true (non relative) begin stack size of the
             * successors of this block, and push these successors onto the
             * stack (unless they have already been pushed onto the stack).
             * Note: by hypothesis, the {@link Label#inputStackTop} of the
             * blocks in the block stack are the true (non relative) beginning
             * stack sizes of these blocks.
             */
            int max = 0;
            Label stack = labels;
            while (stack != null) {
                // pops a block from the stack
                Label l = stack;
                stack = stack.next;
                // computes the true (non relative) max stack size of this block
                int start = l.inputStackTop;
                int blockMax = start + l.outputStackMax;
                // updates the global max stack size
                if (blockMax > max) {
                    max = blockMax;
                }
                // analyzes the successors of the block
                Edge b = l.successors;
                if ((l.status & Label.JSR) != 0) {
                    // ignores the first edge of JSR blocks (virtual successor)
                    b = b.next;
                }
                while (b != null) {
                    l = b.successor;
                    // if this successor has not already been pushed...
                    if ((l.status & Label.PUSHED) == 0) {
                        // computes its true beginning stack size...
                        l.inputStackTop = b.info == Edge.EXCEPTION ? 1 : start
                                + b.info;
                        // ...and pushes it onto the stack
                        l.status |= Label.PUSHED;
                        l.next = stack;
                        stack = l;
                    }
                    b = b.next;
                }
            }
            this.maxStack = Math.max(maxStack, max);
        } else {
            this.maxStack = maxStack;
            this.maxLocals = maxLocals;
        }
    }

    @Override
    public void visitEnd() {
    }

    // ------------------------------------------------------------------------
    // Utility methods: control flow analysis algorithm
    // ------------------------------------------------------------------------

    /**
     * Adds a successor to the {@link #currentBlock currentBlock} block.
     * 
     * @param info
     *            information about the control flow edge to be added.
     * @param successor
     *            the successor block to be added to the current block.
     */
    private void addSuccessor(final int info, final Label successor) {
        // creates and initializes an Edge object...
        Edge b = new Edge();
        b.info = info;
        b.successor = successor;
        // ...and adds it to the successor list of the currentBlock block
        b.next = currentBlock.successors;
        currentBlock.successors = b;
    }

    /**
     * Ends the current basic block. This method must be used in the case where
     * the current basic block does not have any successor.
     */
    private void noSuccessor() {
        if (compute == FRAMES) {
            Label l = new Label();
            l.frame = new Frame();
            l.frame.owner = l;
            l.resolve(this, code.length, code.data);
            previousBlock.successor = l;
            previousBlock = l;
        } else {
            currentBlock.outputStackMax = maxStackSize;
        }
        if (compute != INSERTED_FRAMES) {
            currentBlock = null;
        }
    }

    // ------------------------------------------------------------------------
    // Utility methods: stack map frames
    // ------------------------------------------------------------------------

    /**
     * Visits a frame that has been computed from scratch.
     * 
     * @param f
     *            the frame that must be visited.
     */
    private void visitFrame(final Frame f) {
        int i, t;
        int nTop = 0;
        int nLocal = 0;
        int nStack = 0;
        int[] locals = f.inputLocals;
        int[] stacks = f.inputStack;
        // computes the number of locals (ignores TOP types that are just after
        // a LONG or a DOUBLE, and all trailing TOP types)
        for (i = 0; i < locals.length; ++i) {
            t = locals[i];
            if (t == Frame.TOP) {
                ++nTop;
            } else {
                nLocal += nTop + 1;
                nTop = 0;
            }
            if (t == Frame.LONG || t == Frame.DOUBLE) {
                ++i;
            }
        }
        // computes the stack size (ignores TOP types that are just after
        // a LONG or a DOUBLE)
        for (i = 0; i < stacks.length; ++i) {
            t = stacks[i];
            ++nStack;
            if (t == Frame.LONG || t == Frame.DOUBLE) {
                ++i;
            }
        }
        // visits the frame and its content
        int frameIndex = startFrame(f.owner.position, nLocal, nStack);
        for (i = 0; nLocal > 0; ++i, --nLocal) {
            t = locals[i];
            frame[frameIndex++] = t;
            if (t == Frame.LONG || t == Frame.DOUBLE) {
                ++i;
            }
        }
        for (i = 0; i < stacks.length; ++i) {
            t = stacks[i];
            frame[frameIndex++] = t;
            if (t == Frame.LONG || t == Frame.DOUBLE) {
                ++i;
            }
        }
        endFrame();
    }

    /**
     * Visit the implicit first frame of this method.
     */
    private void visitImplicitFirstFrame() {
        // There can be at most descriptor.length() + 1 locals
        int frameIndex = startFrame(0, descriptor.length() + 1, 0);
        if ((access & Opcodes.ACC_STATIC) == 0) {
            if ((access & ACC_CONSTRUCTOR) == 0) {
                frame[frameIndex++] = Frame.OBJECT | cw.addType(cw.thisName);
            } else {
                frame[frameIndex++] = 6; // Opcodes.UNINITIALIZED_THIS;
            }
        }
        int i = 1;
        loop: while (true) {
            int j = i;
            switch (descriptor.charAt(i++)) {
            case 'Z':
            case 'C':
            case 'B':
            case 'S':
            case 'I':
                frame[frameIndex++] = 1; // Opcodes.INTEGER;
                break;
            case 'F':
                frame[frameIndex++] = 2; // Opcodes.FLOAT;
                break;
            case 'J':
                frame[frameIndex++] = 4; // Opcodes.LONG;
                break;
            case 'D':
                frame[frameIndex++] = 3; // Opcodes.DOUBLE;
                break;
            case '[':
                while (descriptor.charAt(i) == '[') {
                    ++i;
                }
                if (descriptor.charAt(i) == 'L') {
                    ++i;
                    while (descriptor.charAt(i) != ';') {
                        ++i;
                    }
                }
                frame[frameIndex++] = Frame.OBJECT
                        | cw.addType(descriptor.substring(j, ++i));
                break;
            case 'L':
                while (descriptor.charAt(i) != ';') {
                    ++i;
                }
                frame[frameIndex++] = Frame.OBJECT
                        | cw.addType(descriptor.substring(j + 1, i++));
                break;
            default:
                break loop;
            }
        }
        frame[1] = frameIndex - 3;
        endFrame();
    }

    /**
     * Starts the visit of a stack map frame.
     * 
     * @param offset
     *            the offset of the instruction to which the frame corresponds.
     * @param nLocal
     *            the number of local variables in the frame.
     * @param nStack
     *            the number of stack elements in the frame.
     * @return the index of the next element to be written in this frame.
     */
    private int startFrame(final int offset, final int nLocal, final int nStack) {
        int n = 3 + nLocal + nStack;
        if (frame == null || frame.length < n) {
            frame = new int[n];
        }
        frame[0] = offset;
        frame[1] = nLocal;
        frame[2] = nStack;
        return 3;
    }

    /**
     * Checks if the visit of the current frame {@link #frame} is finished, and
     * if yes, write it in the StackMapTable attribute.
     */
    private void endFrame() {
        if (previousFrame != null) { // do not write the first frame
            if (stackMap == null) {
                stackMap = new ByteVector();
            }
            writeFrame();
            ++frameCount;
        }
        previousFrame = frame;
        frame = null;
    }

    /**
     * Compress and writes the current frame {@link #frame} in the StackMapTable
     * attribute.
     */
    private void writeFrame() {
        int clocalsSize = frame[1];
        int cstackSize = frame[2];
        if ((cw.version & 0xFFFF) < Opcodes.V1_6) {
            stackMap.putShort(frame[0]).putShort(clocalsSize);
            writeFrameTypes(3, 3 + clocalsSize);
            stackMap.putShort(cstackSize);
            writeFrameTypes(3 + clocalsSize, 3 + clocalsSize + cstackSize);
            return;
        }
        int localsSize = previousFrame[1];
        int type = FULL_FRAME;
        int k = 0;
        int delta;
        if (frameCount == 0) {
            delta = frame[0];
        } else {
            delta = frame[0] - previousFrame[0] - 1;
        }
        if (cstackSize == 0) {
            k = clocalsSize - localsSize;
            switch (k) {
            case -3:
            case -2:
            case -1:
                type = CHOP_FRAME;
                localsSize = clocalsSize;
                break;
            case 0:
                type = delta < 64 ? SAME_FRAME : SAME_FRAME_EXTENDED;
                break;
            case 1:
            case 2:
            case 3:
                type = APPEND_FRAME;
                break;
            }
        } else if (clocalsSize == localsSize && cstackSize == 1) {
            type = delta < 63 ? SAME_LOCALS_1_STACK_ITEM_FRAME
                    : SAME_LOCALS_1_STACK_ITEM_FRAME_EXTENDED;
        }
        if (type != FULL_FRAME) {
            // verify if locals are the same
            int l = 3;
            for (int j = 0; j < localsSize; j++) {
                if (frame[l] != previousFrame[l]) {
                    type = FULL_FRAME;
                    break;
                }
                l++;
            }
        }
        switch (type) {
        case SAME_FRAME:
            stackMap.putByte(delta);
            break;
        case SAME_LOCALS_1_STACK_ITEM_FRAME:
            stackMap.putByte(SAME_LOCALS_1_STACK_ITEM_FRAME + delta);
            writeFrameTypes(3 + clocalsSize, 4 + clocalsSize);
            break;
        case SAME_LOCALS_1_STACK_ITEM_FRAME_EXTENDED:
            stackMap.putByte(SAME_LOCALS_1_STACK_ITEM_FRAME_EXTENDED).putShort(
                    delta);
            writeFrameTypes(3 + clocalsSize, 4 + clocalsSize);
            break;
        case SAME_FRAME_EXTENDED:
            stackMap.putByte(SAME_FRAME_EXTENDED).putShort(delta);
            break;
        case CHOP_FRAME:
            stackMap.putByte(SAME_FRAME_EXTENDED + k).putShort(delta);
            break;
        case APPEND_FRAME:
            stackMap.putByte(SAME_FRAME_EXTENDED + k).putShort(delta);
            writeFrameTypes(3 + localsSize, 3 + clocalsSize);
            break;
        // case FULL_FRAME:
        default:
            stackMap.putByte(FULL_FRAME).putShort(delta).putShort(clocalsSize);
            writeFrameTypes(3, 3 + clocalsSize);
            stackMap.putShort(cstackSize);
            writeFrameTypes(3 + clocalsSize, 3 + clocalsSize + cstackSize);
        }
    }

    /**
     * Writes some types of the current frame {@link #frame} into the
     * StackMapTableAttribute. This method converts types from the format used
     * in {@link Label} to the format used in StackMapTable attributes. In
     * particular, it converts type table indexes to constant pool indexes.
     * 
     * @param start
     *            index of the first type in {@link #frame} to write.
     * @param end
     *            index of last type in {@link #frame} to write (exclusive).
     */
    private void writeFrameTypes(final int start, final int end) {
        for (int i = start; i < end; ++i) {
            int t = frame[i];
            int d = t & Frame.DIM;
            if (d == 0) {
                int v = t & Frame.BASE_VALUE;
                switch (t & Frame.BASE_KIND) {
                case Frame.OBJECT:
                    stackMap.putByte(7).putShort(
                            cw.newClass(cw.typeTable[v].strVal1));
                    break;
                case Frame.UNINITIALIZED:
                    stackMap.putByte(8).putShort(cw.typeTable[v].intVal);
                    break;
                default:
                    stackMap.putByte(v);
                }
            } else {
                StringBuilder sb = new StringBuilder();
                d >>= 28;
                while (d-- > 0) {
                    sb.append('[');
                }
                if ((t & Frame.BASE_KIND) == Frame.OBJECT) {
                    sb.append('L');
                    sb.append(cw.typeTable[t & Frame.BASE_VALUE].strVal1);
                    sb.append(';');
                } else {
                    switch (t & 0xF) {
                    case 1:
                        sb.append('I');
                        break;
                    case 2:
                        sb.append('F');
                        break;
                    case 3:
                        sb.append('D');
                        break;
                    case 9:
                        sb.append('Z');
                        break;
                    case 10:
                        sb.append('B');
                        break;
                    case 11:
                        sb.append('C');
                        break;
                    case 12:
                        sb.append('S');
                        break;
                    default:
                        sb.append('J');
                    }
                }
                stackMap.putByte(7).putShort(cw.newClass(sb.toString()));
            }
        }
    }

    private void writeFrameType(final Object type) {
        if (type instanceof String) {
            stackMap.putByte(7).putShort(cw.newClass((String) type));
        } else if (type instanceof Integer) {
            stackMap.putByte(((Integer) type).intValue());
        } else {
            stackMap.putByte(8).putShort(((Label) type).position);
        }
    }

    // ------------------------------------------------------------------------
    // Utility methods: dump bytecode array
    // ------------------------------------------------------------------------

    /**
     * Returns the size of the bytecode of this method.
     * 
     * @return the size of the bytecode of this method.
     */
    final int getSize() {
        if (classReaderOffset != 0) {
            return 6 + classReaderLength;
        }
        int size = 8;
        if (code.length > 0) {
            if (code.length > 65535) {
                throw new RuntimeException("Method code too large!");
            }
            cw.newUTF8("Code");
            size += 18 + code.length + 8 * handlerCount;
            if (localVar != null) {
                cw.newUTF8("LocalVariableTable");
                size += 8 + localVar.length;
            }
            if (localVarType != null) {
                cw.newUTF8("LocalVariableTypeTable");
                size += 8 + localVarType.length;
            }
            if (lineNumber != null) {
                cw.newUTF8("LineNumberTable");
                size += 8 + lineNumber.length;
            }
            if (stackMap != null) {
                boolean zip = (cw.version & 0xFFFF) >= Opcodes.V1_6;
                cw.newUTF8(zip ? "StackMapTable" : "StackMap");
                size += 8 + stackMap.length;
            }
            if (ClassReader.ANNOTATIONS && ctanns != null) {
                cw.newUTF8("RuntimeVisibleTypeAnnotations");
                size += 8 + ctanns.getSize();
            }
            if (ClassReader.ANNOTATIONS && ictanns != null) {
                cw.newUTF8("RuntimeInvisibleTypeAnnotations");
                size += 8 + ictanns.getSize();
            }
            if (cattrs != null) {
                size += cattrs.getSize(cw, code.data, code.length, maxStack,
                        maxLocals);
            }
        }
        if (exceptionCount > 0) {
            cw.newUTF8("Exceptions");
            size += 8 + 2 * exceptionCount;
        }
        if ((access & Opcodes.ACC_SYNTHETIC) != 0) {
            if ((cw.version & 0xFFFF) < Opcodes.V1_5
                    || (access & ClassWriter.ACC_SYNTHETIC_ATTRIBUTE) != 0) {
                cw.newUTF8("Synthetic");
                size += 6;
            }
        }
        if ((access & Opcodes.ACC_DEPRECATED) != 0) {
            cw.newUTF8("Deprecated");
            size += 6;
        }
        if (ClassReader.SIGNATURES && signature != null) {
            cw.newUTF8("Signature");
            cw.newUTF8(signature);
            size += 8;
        }
        if (methodParameters != null) {
            cw.newUTF8("MethodParameters");
            size += 7 + methodParameters.length;
        }
        if (ClassReader.ANNOTATIONS && annd != null) {
            cw.newUTF8("AnnotationDefault");
            size += 6 + annd.length;
        }
        if (ClassReader.ANNOTATIONS && anns != null) {
            cw.newUTF8("RuntimeVisibleAnnotations");
            size += 8 + anns.getSize();
        }
        if (ClassReader.ANNOTATIONS && ianns != null) {
            cw.newUTF8("RuntimeInvisibleAnnotations");
            size += 8 + ianns.getSize();
        }
        if (ClassReader.ANNOTATIONS && tanns != null) {
            cw.newUTF8("RuntimeVisibleTypeAnnotations");
            size += 8 + tanns.getSize();
        }
        if (ClassReader.ANNOTATIONS && itanns != null) {
            cw.newUTF8("RuntimeInvisibleTypeAnnotations");
            size += 8 + itanns.getSize();
        }
        if (ClassReader.ANNOTATIONS && panns != null) {
            cw.newUTF8("RuntimeVisibleParameterAnnotations");
            size += 7 + 2 * (panns.length - synthetics);
            for (int i = panns.length - 1; i >= synthetics; --i) {
                size += panns[i] == null ? 0 : panns[i].getSize();
            }
        }
        if (ClassReader.ANNOTATIONS && ipanns != null) {
            cw.newUTF8("RuntimeInvisibleParameterAnnotations");
            size += 7 + 2 * (ipanns.length - synthetics);
            for (int i = ipanns.length - 1; i >= synthetics; --i) {
                size += ipanns[i] == null ? 0 : ipanns[i].getSize();
            }
        }
        if (attrs != null) {
            size += attrs.getSize(cw, null, 0, -1, -1);
        }
        return size;
    }

    /**
     * Puts the bytecode of this method in the given byte vector.
     * 
     * @param out
     *            the byte vector into which the bytecode of this method must be
     *            copied.
     */
    final void put(final ByteVector out) {
        final int FACTOR = ClassWriter.TO_ACC_SYNTHETIC;
        int mask = ACC_CONSTRUCTOR | Opcodes.ACC_DEPRECATED
                | ClassWriter.ACC_SYNTHETIC_ATTRIBUTE
                | ((access & ClassWriter.ACC_SYNTHETIC_ATTRIBUTE) / FACTOR);
        out.putShort(access & ~mask).putShort(name).putShort(desc);
        if (classReaderOffset != 0) {
            out.putByteArray(cw.cr.b, classReaderOffset, classReaderLength);
            return;
        }
        int attributeCount = 0;
        if (code.length > 0) {
            ++attributeCount;
        }
        if (exceptionCount > 0) {
            ++attributeCount;
        }
        if ((access & Opcodes.ACC_SYNTHETIC) != 0) {
            if ((cw.version & 0xFFFF) < Opcodes.V1_5
                    || (access & ClassWriter.ACC_SYNTHETIC_ATTRIBUTE) != 0) {
                ++attributeCount;
            }
        }
        if ((access & Opcodes.ACC_DEPRECATED) != 0) {
            ++attributeCount;
        }
        if (ClassReader.SIGNATURES && signature != null) {
            ++attributeCount;
        }
        if (methodParameters != null) {
            ++attributeCount;
        }
        if (ClassReader.ANNOTATIONS && annd != null) {
            ++attributeCount;
        }
        if (ClassReader.ANNOTATIONS && anns != null) {
            ++attributeCount;
        }
        if (ClassReader.ANNOTATIONS && ianns != null) {
            ++attributeCount;
        }
        if (ClassReader.ANNOTATIONS && tanns != null) {
            ++attributeCount;
        }
        if (ClassReader.ANNOTATIONS && itanns != null) {
            ++attributeCount;
        }
        if (ClassReader.ANNOTATIONS && panns != null) {
            ++attributeCount;
        }
        if (ClassReader.ANNOTATIONS && ipanns != null) {
            ++attributeCount;
        }
        if (attrs != null) {
            attributeCount += attrs.getCount();
        }
        out.putShort(attributeCount);
        if (code.length > 0) {
            int size = 12 + code.length + 8 * handlerCount;
            if (localVar != null) {
                size += 8 + localVar.length;
            }
            if (localVarType != null) {
                size += 8 + localVarType.length;
            }
            if (lineNumber != null) {
                size += 8 + lineNumber.length;
            }
            if (stackMap != null) {
                size += 8 + stackMap.length;
            }
            if (ClassReader.ANNOTATIONS && ctanns != null) {
                size += 8 + ctanns.getSize();
            }
            if (ClassReader.ANNOTATIONS && ictanns != null) {
                size += 8 + ictanns.getSize();
            }
            if (cattrs != null) {
                size += cattrs.getSize(cw, code.data, code.length, maxStack,
                        maxLocals);
            }
            out.putShort(cw.newUTF8("Code")).putInt(size);
            out.putShort(maxStack).putShort(maxLocals);
            out.putInt(code.length).putByteArray(code.data, 0, code.length);
            out.putShort(handlerCount);
            if (handlerCount > 0) {
                Handler h = firstHandler;
                while (h != null) {
                    out.putShort(h.start.position).putShort(h.end.position)
                            .putShort(h.handler.position).putShort(h.type);
                    h = h.next;
                }
            }
            attributeCount = 0;
            if (localVar != null) {
                ++attributeCount;
            }
            if (localVarType != null) {
                ++attributeCount;
            }
            if (lineNumber != null) {
                ++attributeCount;
            }
            if (stackMap != null) {
                ++attributeCount;
            }
            if (ClassReader.ANNOTATIONS && ctanns != null) {
                ++attributeCount;
            }
            if (ClassReader.ANNOTATIONS && ictanns != null) {
                ++attributeCount;
            }
            if (cattrs != null) {
                attributeCount += cattrs.getCount();
            }
            out.putShort(attributeCount);
            if (localVar != null) {
                out.putShort(cw.newUTF8("LocalVariableTable"));
                out.putInt(localVar.length + 2).putShort(localVarCount);
                out.putByteArray(localVar.data, 0, localVar.length);
            }
            if (localVarType != null) {
                out.putShort(cw.newUTF8("LocalVariableTypeTable"));
                out.putInt(localVarType.length + 2).putShort(localVarTypeCount);
                out.putByteArray(localVarType.data, 0, localVarType.length);
            }
            if (lineNumber != null) {
                out.putShort(cw.newUTF8("LineNumberTable"));
                out.putInt(lineNumber.length + 2).putShort(lineNumberCount);
                out.putByteArray(lineNumber.data, 0, lineNumber.length);
            }
            if (stackMap != null) {
                boolean zip = (cw.version & 0xFFFF) >= Opcodes.V1_6;
                out.putShort(cw.newUTF8(zip ? "StackMapTable" : "StackMap"));
                out.putInt(stackMap.length + 2).putShort(frameCount);
                out.putByteArray(stackMap.data, 0, stackMap.length);
            }
            if (ClassReader.ANNOTATIONS && ctanns != null) {
                out.putShort(cw.newUTF8("RuntimeVisibleTypeAnnotations"));
                ctanns.put(out);
            }
            if (ClassReader.ANNOTATIONS && ictanns != null) {
                out.putShort(cw.newUTF8("RuntimeInvisibleTypeAnnotations"));
                ictanns.put(out);
            }
            if (cattrs != null) {
                cattrs.put(cw, code.data, code.length, maxLocals, maxStack, out);
            }
        }
        if (exceptionCount > 0) {
            out.putShort(cw.newUTF8("Exceptions")).putInt(
                    2 * exceptionCount + 2);
            out.putShort(exceptionCount);
            for (int i = 0; i < exceptionCount; ++i) {
                out.putShort(exceptions[i]);
            }
        }
        if ((access & Opcodes.ACC_SYNTHETIC) != 0) {
            if ((cw.version & 0xFFFF) < Opcodes.V1_5
                    || (access & ClassWriter.ACC_SYNTHETIC_ATTRIBUTE) != 0) {
                out.putShort(cw.newUTF8("Synthetic")).putInt(0);
            }
        }
        if ((access & Opcodes.ACC_DEPRECATED) != 0) {
            out.putShort(cw.newUTF8("Deprecated")).putInt(0);
        }
        if (ClassReader.SIGNATURES && signature != null) {
            out.putShort(cw.newUTF8("Signature")).putInt(2)
                    .putShort(cw.newUTF8(signature));
        }
        if (methodParameters != null) {
            out.putShort(cw.newUTF8("MethodParameters"));
            out.putInt(methodParameters.length + 1).putByte(
                    methodParametersCount);
            out.putByteArray(methodParameters.data, 0, methodParameters.length);
        }
        if (ClassReader.ANNOTATIONS && annd != null) {
            out.putShort(cw.newUTF8("AnnotationDefault"));
            out.putInt(annd.length);
            out.putByteArray(annd.data, 0, annd.length);
        }
        if (ClassReader.ANNOTATIONS && anns != null) {
            out.putShort(cw.newUTF8("RuntimeVisibleAnnotations"));
            anns.put(out);
        }
        if (ClassReader.ANNOTATIONS && ianns != null) {
            out.putShort(cw.newUTF8("RuntimeInvisibleAnnotations"));
            ianns.put(out);
        }
        if (ClassReader.ANNOTATIONS && tanns != null) {
            out.putShort(cw.newUTF8("RuntimeVisibleTypeAnnotations"));
            tanns.put(out);
        }
        if (ClassReader.ANNOTATIONS && itanns != null) {
            out.putShort(cw.newUTF8("RuntimeInvisibleTypeAnnotations"));
            itanns.put(out);
        }
        if (ClassReader.ANNOTATIONS && panns != null) {
            out.putShort(cw.newUTF8("RuntimeVisibleParameterAnnotations"));
            AnnotationWriter.put(panns, synthetics, out);
        }
        if (ClassReader.ANNOTATIONS && ipanns != null) {
            out.putShort(cw.newUTF8("RuntimeInvisibleParameterAnnotations"));
            AnnotationWriter.put(ipanns, synthetics, out);
        }
        if (attrs != null) {
            attrs.put(cw, null, 0, -1, -1, out);
        }
    }
}
