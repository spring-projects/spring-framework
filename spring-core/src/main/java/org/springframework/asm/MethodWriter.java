// ASM: a very small and fast Java bytecode manipulation framework
// Copyright (c) 2000-2011 INRIA, France Telecom
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions
// are met:
// 1. Redistributions of source code must retain the above copyright
//    notice, this list of conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright
//    notice, this list of conditions and the following disclaimer in the
//    documentation and/or other materials provided with the distribution.
// 3. Neither the name of the copyright holders nor the names of its
//    contributors may be used to endorse or promote products derived from
//    this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
// LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
// CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
// SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
// CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
// THE POSSIBILITY OF SUCH DAMAGE.
package org.springframework.asm;

/**
 * A {@link MethodVisitor} that generates a corresponding 'method_info' structure, as defined in the
 * Java Virtual Machine Specification (JVMS).
 *
 * @see <a href="https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.6">JVMS
 *     4.6</a>
 * @author Eric Bruneton
 * @author Eugene Kuleshov
 */
final class MethodWriter extends MethodVisitor {

  /** Indicates that nothing must be computed. */
  static final int COMPUTE_NOTHING = 0;

  /**
   * Indicates that the maximum stack size and the maximum number of local variables must be
   * computed, from scratch.
   */
  static final int COMPUTE_MAX_STACK_AND_LOCAL = 1;

  /**
   * Indicates that the maximum stack size and the maximum number of local variables must be
   * computed, from the existing stack map frames. This can be done more efficiently than with the
   * control flow graph algorithm used for {@link #COMPUTE_MAX_STACK_AND_LOCAL}, by using a linear
   * scan of the bytecode instructions.
   */
  static final int COMPUTE_MAX_STACK_AND_LOCAL_FROM_FRAMES = 2;

  /**
   * Indicates that the stack map frames of type F_INSERT must be computed. The other frames are not
   * computed. They should all be of type F_NEW and should be sufficient to compute the content of
   * the F_INSERT frames, together with the bytecode instructions between a F_NEW and a F_INSERT
   * frame - and without any knowledge of the type hierarchy (by definition of F_INSERT).
   */
  static final int COMPUTE_INSERTED_FRAMES = 3;

  /**
   * Indicates that all the stack map frames must be computed. In this case the maximum stack size
   * and the maximum number of local variables is also computed.
   */
  static final int COMPUTE_ALL_FRAMES = 4;

  /** Indicates that {@link #STACK_SIZE_DELTA} is not applicable (not constant or never used). */
  private static final int NA = 0;

  /**
   * The stack size variation corresponding to each JVM opcode. The stack size variation for opcode
   * 'o' is given by the array element at index 'o'.
   *
   * @see <a href="https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-6.html">JVMS 6</a>
   */
  private static final int[] STACK_SIZE_DELTA = {
    0, // nop = 0 (0x0)
    1, // aconst_null = 1 (0x1)
    1, // iconst_m1 = 2 (0x2)
    1, // iconst_0 = 3 (0x3)
    1, // iconst_1 = 4 (0x4)
    1, // iconst_2 = 5 (0x5)
    1, // iconst_3 = 6 (0x6)
    1, // iconst_4 = 7 (0x7)
    1, // iconst_5 = 8 (0x8)
    2, // lconst_0 = 9 (0x9)
    2, // lconst_1 = 10 (0xa)
    1, // fconst_0 = 11 (0xb)
    1, // fconst_1 = 12 (0xc)
    1, // fconst_2 = 13 (0xd)
    2, // dconst_0 = 14 (0xe)
    2, // dconst_1 = 15 (0xf)
    1, // bipush = 16 (0x10)
    1, // sipush = 17 (0x11)
    1, // ldc = 18 (0x12)
    NA, // ldc_w = 19 (0x13)
    NA, // ldc2_w = 20 (0x14)
    1, // iload = 21 (0x15)
    2, // lload = 22 (0x16)
    1, // fload = 23 (0x17)
    2, // dload = 24 (0x18)
    1, // aload = 25 (0x19)
    NA, // iload_0 = 26 (0x1a)
    NA, // iload_1 = 27 (0x1b)
    NA, // iload_2 = 28 (0x1c)
    NA, // iload_3 = 29 (0x1d)
    NA, // lload_0 = 30 (0x1e)
    NA, // lload_1 = 31 (0x1f)
    NA, // lload_2 = 32 (0x20)
    NA, // lload_3 = 33 (0x21)
    NA, // fload_0 = 34 (0x22)
    NA, // fload_1 = 35 (0x23)
    NA, // fload_2 = 36 (0x24)
    NA, // fload_3 = 37 (0x25)
    NA, // dload_0 = 38 (0x26)
    NA, // dload_1 = 39 (0x27)
    NA, // dload_2 = 40 (0x28)
    NA, // dload_3 = 41 (0x29)
    NA, // aload_0 = 42 (0x2a)
    NA, // aload_1 = 43 (0x2b)
    NA, // aload_2 = 44 (0x2c)
    NA, // aload_3 = 45 (0x2d)
    -1, // iaload = 46 (0x2e)
    0, // laload = 47 (0x2f)
    -1, // faload = 48 (0x30)
    0, // daload = 49 (0x31)
    -1, // aaload = 50 (0x32)
    -1, // baload = 51 (0x33)
    -1, // caload = 52 (0x34)
    -1, // saload = 53 (0x35)
    -1, // istore = 54 (0x36)
    -2, // lstore = 55 (0x37)
    -1, // fstore = 56 (0x38)
    -2, // dstore = 57 (0x39)
    -1, // astore = 58 (0x3a)
    NA, // istore_0 = 59 (0x3b)
    NA, // istore_1 = 60 (0x3c)
    NA, // istore_2 = 61 (0x3d)
    NA, // istore_3 = 62 (0x3e)
    NA, // lstore_0 = 63 (0x3f)
    NA, // lstore_1 = 64 (0x40)
    NA, // lstore_2 = 65 (0x41)
    NA, // lstore_3 = 66 (0x42)
    NA, // fstore_0 = 67 (0x43)
    NA, // fstore_1 = 68 (0x44)
    NA, // fstore_2 = 69 (0x45)
    NA, // fstore_3 = 70 (0x46)
    NA, // dstore_0 = 71 (0x47)
    NA, // dstore_1 = 72 (0x48)
    NA, // dstore_2 = 73 (0x49)
    NA, // dstore_3 = 74 (0x4a)
    NA, // astore_0 = 75 (0x4b)
    NA, // astore_1 = 76 (0x4c)
    NA, // astore_2 = 77 (0x4d)
    NA, // astore_3 = 78 (0x4e)
    -3, // iastore = 79 (0x4f)
    -4, // lastore = 80 (0x50)
    -3, // fastore = 81 (0x51)
    -4, // dastore = 82 (0x52)
    -3, // aastore = 83 (0x53)
    -3, // bastore = 84 (0x54)
    -3, // castore = 85 (0x55)
    -3, // sastore = 86 (0x56)
    -1, // pop = 87 (0x57)
    -2, // pop2 = 88 (0x58)
    1, // dup = 89 (0x59)
    1, // dup_x1 = 90 (0x5a)
    1, // dup_x2 = 91 (0x5b)
    2, // dup2 = 92 (0x5c)
    2, // dup2_x1 = 93 (0x5d)
    2, // dup2_x2 = 94 (0x5e)
    0, // swap = 95 (0x5f)
    -1, // iadd = 96 (0x60)
    -2, // ladd = 97 (0x61)
    -1, // fadd = 98 (0x62)
    -2, // dadd = 99 (0x63)
    -1, // isub = 100 (0x64)
    -2, // lsub = 101 (0x65)
    -1, // fsub = 102 (0x66)
    -2, // dsub = 103 (0x67)
    -1, // imul = 104 (0x68)
    -2, // lmul = 105 (0x69)
    -1, // fmul = 106 (0x6a)
    -2, // dmul = 107 (0x6b)
    -1, // idiv = 108 (0x6c)
    -2, // ldiv = 109 (0x6d)
    -1, // fdiv = 110 (0x6e)
    -2, // ddiv = 111 (0x6f)
    -1, // irem = 112 (0x70)
    -2, // lrem = 113 (0x71)
    -1, // frem = 114 (0x72)
    -2, // drem = 115 (0x73)
    0, // ineg = 116 (0x74)
    0, // lneg = 117 (0x75)
    0, // fneg = 118 (0x76)
    0, // dneg = 119 (0x77)
    -1, // ishl = 120 (0x78)
    -1, // lshl = 121 (0x79)
    -1, // ishr = 122 (0x7a)
    -1, // lshr = 123 (0x7b)
    -1, // iushr = 124 (0x7c)
    -1, // lushr = 125 (0x7d)
    -1, // iand = 126 (0x7e)
    -2, // land = 127 (0x7f)
    -1, // ior = 128 (0x80)
    -2, // lor = 129 (0x81)
    -1, // ixor = 130 (0x82)
    -2, // lxor = 131 (0x83)
    0, // iinc = 132 (0x84)
    1, // i2l = 133 (0x85)
    0, // i2f = 134 (0x86)
    1, // i2d = 135 (0x87)
    -1, // l2i = 136 (0x88)
    -1, // l2f = 137 (0x89)
    0, // l2d = 138 (0x8a)
    0, // f2i = 139 (0x8b)
    1, // f2l = 140 (0x8c)
    1, // f2d = 141 (0x8d)
    -1, // d2i = 142 (0x8e)
    0, // d2l = 143 (0x8f)
    -1, // d2f = 144 (0x90)
    0, // i2b = 145 (0x91)
    0, // i2c = 146 (0x92)
    0, // i2s = 147 (0x93)
    -3, // lcmp = 148 (0x94)
    -1, // fcmpl = 149 (0x95)
    -1, // fcmpg = 150 (0x96)
    -3, // dcmpl = 151 (0x97)
    -3, // dcmpg = 152 (0x98)
    -1, // ifeq = 153 (0x99)
    -1, // ifne = 154 (0x9a)
    -1, // iflt = 155 (0x9b)
    -1, // ifge = 156 (0x9c)
    -1, // ifgt = 157 (0x9d)
    -1, // ifle = 158 (0x9e)
    -2, // if_icmpeq = 159 (0x9f)
    -2, // if_icmpne = 160 (0xa0)
    -2, // if_icmplt = 161 (0xa1)
    -2, // if_icmpge = 162 (0xa2)
    -2, // if_icmpgt = 163 (0xa3)
    -2, // if_icmple = 164 (0xa4)
    -2, // if_acmpeq = 165 (0xa5)
    -2, // if_acmpne = 166 (0xa6)
    0, // goto = 167 (0xa7)
    1, // jsr = 168 (0xa8)
    0, // ret = 169 (0xa9)
    -1, // tableswitch = 170 (0xaa)
    -1, // lookupswitch = 171 (0xab)
    -1, // ireturn = 172 (0xac)
    -2, // lreturn = 173 (0xad)
    -1, // freturn = 174 (0xae)
    -2, // dreturn = 175 (0xaf)
    -1, // areturn = 176 (0xb0)
    0, // return = 177 (0xb1)
    NA, // getstatic = 178 (0xb2)
    NA, // putstatic = 179 (0xb3)
    NA, // getfield = 180 (0xb4)
    NA, // putfield = 181 (0xb5)
    NA, // invokevirtual = 182 (0xb6)
    NA, // invokespecial = 183 (0xb7)
    NA, // invokestatic = 184 (0xb8)
    NA, // invokeinterface = 185 (0xb9)
    NA, // invokedynamic = 186 (0xba)
    1, // new = 187 (0xbb)
    0, // newarray = 188 (0xbc)
    0, // anewarray = 189 (0xbd)
    0, // arraylength = 190 (0xbe)
    NA, // athrow = 191 (0xbf)
    0, // checkcast = 192 (0xc0)
    0, // instanceof = 193 (0xc1)
    -1, // monitorenter = 194 (0xc2)
    -1, // monitorexit = 195 (0xc3)
    NA, // wide = 196 (0xc4)
    NA, // multianewarray = 197 (0xc5)
    -1, // ifnull = 198 (0xc6)
    -1, // ifnonnull = 199 (0xc7)
    NA, // goto_w = 200 (0xc8)
    NA // jsr_w = 201 (0xc9)
  };

  /** Where the constants used in this MethodWriter must be stored. */
  private final SymbolTable symbolTable;

  // Note: fields are ordered as in the method_info structure, and those related to attributes are
  // ordered as in Section 4.7 of the JVMS.

  /**
   * The access_flags field of the method_info JVMS structure. This field can contain ASM specific
   * access flags, such as {@link Opcodes#ACC_DEPRECATED}, which are removed when generating the
   * ClassFile structure.
   */
  private final int accessFlags;

  /** The name_index field of the method_info JVMS structure. */
  private final int nameIndex;

  /** The name of this method. */
  private final String name;

  /** The descriptor_index field of the method_info JVMS structure. */
  private final int descriptorIndex;

  /** The descriptor of this method. */
  private final String descriptor;

  // Code attribute fields and sub attributes:

  /** The max_stack field of the Code attribute. */
  private int maxStack;

  /** The max_locals field of the Code attribute. */
  private int maxLocals;

  /** The 'code' field of the Code attribute. */
  private final ByteVector code = new ByteVector();

  /**
   * The first element in the exception handler list (used to generate the exception_table of the
   * Code attribute). The next ones can be accessed with the {@link Handler#nextHandler} field. May
   * be {@literal null}.
   */
  private Handler firstHandler;

  /**
   * The last element in the exception handler list (used to generate the exception_table of the
   * Code attribute). The next ones can be accessed with the {@link Handler#nextHandler} field. May
   * be {@literal null}.
   */
  private Handler lastHandler;

  /** The line_number_table_length field of the LineNumberTable code attribute. */
  private int lineNumberTableLength;

  /** The line_number_table array of the LineNumberTable code attribute, or {@literal null}. */
  private ByteVector lineNumberTable;

  /** The local_variable_table_length field of the LocalVariableTable code attribute. */
  private int localVariableTableLength;

  /**
   * The local_variable_table array of the LocalVariableTable code attribute, or {@literal null}.
   */
  private ByteVector localVariableTable;

  /** The local_variable_type_table_length field of the LocalVariableTypeTable code attribute. */
  private int localVariableTypeTableLength;

  /**
   * The local_variable_type_table array of the LocalVariableTypeTable code attribute, or {@literal
   * null}.
   */
  private ByteVector localVariableTypeTable;

  /** The number_of_entries field of the StackMapTable code attribute. */
  private int stackMapTableNumberOfEntries;

  /** The 'entries' array of the StackMapTable code attribute. */
  private ByteVector stackMapTableEntries;

  /**
   * The last runtime visible type annotation of the Code attribute. The previous ones can be
   * accessed with the {@link AnnotationWriter#previousAnnotation} field. May be {@literal null}.
   */
  private AnnotationWriter lastCodeRuntimeVisibleTypeAnnotation;

  /**
   * The last runtime invisible type annotation of the Code attribute. The previous ones can be
   * accessed with the {@link AnnotationWriter#previousAnnotation} field. May be {@literal null}.
   */
  private AnnotationWriter lastCodeRuntimeInvisibleTypeAnnotation;

  /**
   * The first non standard attribute of the Code attribute. The next ones can be accessed with the
   * {@link Attribute#nextAttribute} field. May be {@literal null}.
   *
   * <p><b>WARNING</b>: this list stores the attributes in the <i>reverse</i> order of their visit.
   * firstAttribute is actually the last attribute visited in {@link #visitAttribute}. The {@link
   * #putMethodInfo} method writes the attributes in the order defined by this list, i.e. in the
   * reverse order specified by the user.
   */
  private Attribute firstCodeAttribute;

  // Other method_info attributes:

  /** The number_of_exceptions field of the Exceptions attribute. */
  private final int numberOfExceptions;

  /** The exception_index_table array of the Exceptions attribute, or {@literal null}. */
  private final int[] exceptionIndexTable;

  /** The signature_index field of the Signature attribute. */
  private final int signatureIndex;

  /**
   * The last runtime visible annotation of this method. The previous ones can be accessed with the
   * {@link AnnotationWriter#previousAnnotation} field. May be {@literal null}.
   */
  private AnnotationWriter lastRuntimeVisibleAnnotation;

  /**
   * The last runtime invisible annotation of this method. The previous ones can be accessed with
   * the {@link AnnotationWriter#previousAnnotation} field. May be {@literal null}.
   */
  private AnnotationWriter lastRuntimeInvisibleAnnotation;

  /** The number of method parameters that can have runtime visible annotations, or 0. */
  private int visibleAnnotableParameterCount;

  /**
   * The runtime visible parameter annotations of this method. Each array element contains the last
   * annotation of a parameter (which can be {@literal null} - the previous ones can be accessed
   * with the {@link AnnotationWriter#previousAnnotation} field). May be {@literal null}.
   */
  private AnnotationWriter[] lastRuntimeVisibleParameterAnnotations;

  /** The number of method parameters that can have runtime visible annotations, or 0. */
  private int invisibleAnnotableParameterCount;

  /**
   * The runtime invisible parameter annotations of this method. Each array element contains the
   * last annotation of a parameter (which can be {@literal null} - the previous ones can be
   * accessed with the {@link AnnotationWriter#previousAnnotation} field). May be {@literal null}.
   */
  private AnnotationWriter[] lastRuntimeInvisibleParameterAnnotations;

  /**
   * The last runtime visible type annotation of this method. The previous ones can be accessed with
   * the {@link AnnotationWriter#previousAnnotation} field. May be {@literal null}.
   */
  private AnnotationWriter lastRuntimeVisibleTypeAnnotation;

  /**
   * The last runtime invisible type annotation of this method. The previous ones can be accessed
   * with the {@link AnnotationWriter#previousAnnotation} field. May be {@literal null}.
   */
  private AnnotationWriter lastRuntimeInvisibleTypeAnnotation;

  /** The default_value field of the AnnotationDefault attribute, or {@literal null}. */
  private ByteVector defaultValue;

  /** The parameters_count field of the MethodParameters attribute. */
  private int parametersCount;

  /** The 'parameters' array of the MethodParameters attribute, or {@literal null}. */
  private ByteVector parameters;

  /**
   * The first non standard attribute of this method. The next ones can be accessed with the {@link
   * Attribute#nextAttribute} field. May be {@literal null}.
   *
   * <p><b>WARNING</b>: this list stores the attributes in the <i>reverse</i> order of their visit.
   * firstAttribute is actually the last attribute visited in {@link #visitAttribute}. The {@link
   * #putMethodInfo} method writes the attributes in the order defined by this list, i.e. in the
   * reverse order specified by the user.
   */
  private Attribute firstAttribute;

  // -----------------------------------------------------------------------------------------------
  // Fields used to compute the maximum stack size and number of locals, and the stack map frames
  // -----------------------------------------------------------------------------------------------

  /**
   * Indicates what must be computed. Must be one of {@link #COMPUTE_ALL_FRAMES}, {@link
   * #COMPUTE_INSERTED_FRAMES}, {@link #COMPUTE_MAX_STACK_AND_LOCAL} or {@link #COMPUTE_NOTHING}.
   */
  private final int compute;

  /**
   * The first basic block of the method. The next ones (in bytecode offset order) can be accessed
   * with the {@link Label#nextBasicBlock} field.
   */
  private Label firstBasicBlock;

  /**
   * The last basic block of the method (in bytecode offset order). This field is updated each time
   * a basic block is encountered, and is used to append it at the end of the basic block list.
   */
  private Label lastBasicBlock;

  /**
   * The current basic block, i.e. the basic block of the last visited instruction. When {@link
   * #compute} is equal to {@link #COMPUTE_MAX_STACK_AND_LOCAL} or {@link #COMPUTE_ALL_FRAMES}, this
   * field is {@literal null} for unreachable code. When {@link #compute} is equal to {@link
   * #COMPUTE_MAX_STACK_AND_LOCAL_FROM_FRAMES} or {@link #COMPUTE_INSERTED_FRAMES}, this field stays
   * unchanged throughout the whole method (i.e. the whole code is seen as a single basic block;
   * indeed, the existing frames are sufficient by hypothesis to compute any intermediate frame -
   * and the maximum stack size as well - without using any control flow graph).
   */
  private Label currentBasicBlock;

  /**
   * The relative stack size after the last visited instruction. This size is relative to the
   * beginning of {@link #currentBasicBlock}, i.e. the true stack size after the last visited
   * instruction is equal to the {@link Label#inputStackSize} of the current basic block plus {@link
   * #relativeStackSize}. When {@link #compute} is equal to {@link
   * #COMPUTE_MAX_STACK_AND_LOCAL_FROM_FRAMES}, {@link #currentBasicBlock} is always the start of
   * the method, so this relative size is also equal to the absolute stack size after the last
   * visited instruction.
   */
  private int relativeStackSize;

  /**
   * The maximum relative stack size after the last visited instruction. This size is relative to
   * the beginning of {@link #currentBasicBlock}, i.e. the true maximum stack size after the last
   * visited instruction is equal to the {@link Label#inputStackSize} of the current basic block
   * plus {@link #maxRelativeStackSize}.When {@link #compute} is equal to {@link
   * #COMPUTE_MAX_STACK_AND_LOCAL_FROM_FRAMES}, {@link #currentBasicBlock} is always the start of
   * the method, so this relative size is also equal to the absolute maximum stack size after the
   * last visited instruction.
   */
  private int maxRelativeStackSize;

  /** The number of local variables in the last visited stack map frame. */
  private int currentLocals;

  /** The bytecode offset of the last frame that was written in {@link #stackMapTableEntries}. */
  private int previousFrameOffset;

  /**
   * The last frame that was written in {@link #stackMapTableEntries}. This field has the same
   * format as {@link #currentFrame}.
   */
  private int[] previousFrame;

  /**
   * The current stack map frame. The first element contains the bytecode offset of the instruction
   * to which the frame corresponds, the second element is the number of locals and the third one is
   * the number of stack elements. The local variables start at index 3 and are followed by the
   * operand stack elements. In summary frame[0] = offset, frame[1] = numLocal, frame[2] = numStack.
   * Local variables and operand stack entries contain abstract types, as defined in {@link Frame},
   * but restricted to {@link Frame#CONSTANT_KIND}, {@link Frame#REFERENCE_KIND} or {@link
   * Frame#UNINITIALIZED_KIND} abstract types. Long and double types use only one array entry.
   */
  private int[] currentFrame;

  /** Whether this method contains subroutines. */
  private boolean hasSubroutines;

  // -----------------------------------------------------------------------------------------------
  // Other miscellaneous status fields
  // -----------------------------------------------------------------------------------------------

  /** Whether the bytecode of this method contains ASM specific instructions. */
  private boolean hasAsmInstructions;

  /**
   * The start offset of the last visited instruction. Used to set the offset field of type
   * annotations of type 'offset_target' (see <a
   * href="https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.20.1">JVMS
   * 4.7.20.1</a>).
   */
  private int lastBytecodeOffset;

  /**
   * The offset in bytes in {@link SymbolTable#getSource} from which the method_info for this method
   * (excluding its first 6 bytes) must be copied, or 0.
   */
  private int sourceOffset;

  /**
   * The length in bytes in {@link SymbolTable#getSource} which must be copied to get the
   * method_info for this method (excluding its first 6 bytes for access_flags, name_index and
   * descriptor_index).
   */
  private int sourceLength;

  // -----------------------------------------------------------------------------------------------
  // Constructor and accessors
  // -----------------------------------------------------------------------------------------------

  /**
   * Constructs a new {@link MethodWriter}.
   *
   * @param symbolTable where the constants used in this AnnotationWriter must be stored.
   * @param access the method's access flags (see {@link Opcodes}).
   * @param name the method's name.
   * @param descriptor the method's descriptor (see {@link Type}).
   * @param signature the method's signature. May be {@literal null}.
   * @param exceptions the internal names of the method's exceptions. May be {@literal null}.
   * @param compute indicates what must be computed (see #compute).
   */
  MethodWriter(
      final SymbolTable symbolTable,
      final int access,
      final String name,
      final String descriptor,
      final String signature,
      final String[] exceptions,
      final int compute) {
    super(/* latest api = */ Opcodes.ASM7);
    this.symbolTable = symbolTable;
    this.accessFlags = "<init>".equals(name) ? access | Constants.ACC_CONSTRUCTOR : access;
    this.nameIndex = symbolTable.addConstantUtf8(name);
    this.name = name;
    this.descriptorIndex = symbolTable.addConstantUtf8(descriptor);
    this.descriptor = descriptor;
    this.signatureIndex = signature == null ? 0 : symbolTable.addConstantUtf8(signature);
    if (exceptions != null && exceptions.length > 0) {
      numberOfExceptions = exceptions.length;
      this.exceptionIndexTable = new int[numberOfExceptions];
      for (int i = 0; i < numberOfExceptions; ++i) {
        this.exceptionIndexTable[i] = symbolTable.addConstantClass(exceptions[i]).index;
      }
    } else {
      numberOfExceptions = 0;
      this.exceptionIndexTable = null;
    }
    this.compute = compute;
    if (compute != COMPUTE_NOTHING) {
      // Update maxLocals and currentLocals.
      int argumentsSize = Type.getArgumentsAndReturnSizes(descriptor) >> 2;
      if ((access & Opcodes.ACC_STATIC) != 0) {
        --argumentsSize;
      }
      maxLocals = argumentsSize;
      currentLocals = argumentsSize;
      // Create and visit the label for the first basic block.
      firstBasicBlock = new Label();
      visitLabel(firstBasicBlock);
    }
  }

  boolean hasFrames() {
    return stackMapTableNumberOfEntries > 0;
  }

  boolean hasAsmInstructions() {
    return hasAsmInstructions;
  }

  // -----------------------------------------------------------------------------------------------
  // Implementation of the MethodVisitor abstract class
  // -----------------------------------------------------------------------------------------------

  @Override
  public void visitParameter(final String name, final int access) {
    if (parameters == null) {
      parameters = new ByteVector();
    }
    ++parametersCount;
    parameters.putShort((name == null) ? 0 : symbolTable.addConstantUtf8(name)).putShort(access);
  }

  @Override
  public AnnotationVisitor visitAnnotationDefault() {
    defaultValue = new ByteVector();
    return new AnnotationWriter(symbolTable, /* useNamedValues = */ false, defaultValue, null);
  }

  @Override
  public AnnotationVisitor visitAnnotation(final String descriptor, final boolean visible) {
    if (visible) {
      return lastRuntimeVisibleAnnotation =
          AnnotationWriter.create(symbolTable, descriptor, lastRuntimeVisibleAnnotation);
    } else {
      return lastRuntimeInvisibleAnnotation =
          AnnotationWriter.create(symbolTable, descriptor, lastRuntimeInvisibleAnnotation);
    }
  }

  @Override
  public AnnotationVisitor visitTypeAnnotation(
      final int typeRef, final TypePath typePath, final String descriptor, final boolean visible) {
    if (visible) {
      return lastRuntimeVisibleTypeAnnotation =
          AnnotationWriter.create(
              symbolTable, typeRef, typePath, descriptor, lastRuntimeVisibleTypeAnnotation);
    } else {
      return lastRuntimeInvisibleTypeAnnotation =
          AnnotationWriter.create(
              symbolTable, typeRef, typePath, descriptor, lastRuntimeInvisibleTypeAnnotation);
    }
  }

  @Override
  public void visitAnnotableParameterCount(final int parameterCount, final boolean visible) {
    if (visible) {
      visibleAnnotableParameterCount = parameterCount;
    } else {
      invisibleAnnotableParameterCount = parameterCount;
    }
  }

  @Override
  public AnnotationVisitor visitParameterAnnotation(
      final int parameter, final String annotationDescriptor, final boolean visible) {
    if (visible) {
      if (lastRuntimeVisibleParameterAnnotations == null) {
        lastRuntimeVisibleParameterAnnotations =
            new AnnotationWriter[Type.getArgumentTypes(descriptor).length];
      }
      return lastRuntimeVisibleParameterAnnotations[parameter] =
          AnnotationWriter.create(
              symbolTable, annotationDescriptor, lastRuntimeVisibleParameterAnnotations[parameter]);
    } else {
      if (lastRuntimeInvisibleParameterAnnotations == null) {
        lastRuntimeInvisibleParameterAnnotations =
            new AnnotationWriter[Type.getArgumentTypes(descriptor).length];
      }
      return lastRuntimeInvisibleParameterAnnotations[parameter] =
          AnnotationWriter.create(
              symbolTable,
              annotationDescriptor,
              lastRuntimeInvisibleParameterAnnotations[parameter]);
    }
  }

  @Override
  public void visitAttribute(final Attribute attribute) {
    // Store the attributes in the <i>reverse</i> order of their visit by this method.
    if (attribute.isCodeAttribute()) {
      attribute.nextAttribute = firstCodeAttribute;
      firstCodeAttribute = attribute;
    } else {
      attribute.nextAttribute = firstAttribute;
      firstAttribute = attribute;
    }
  }

  @Override
  public void visitCode() {
    // Nothing to do.
  }

  @Override
  public void visitFrame(
      final int type,
      final int numLocal,
      final Object[] local,
      final int numStack,
      final Object[] stack) {
    if (compute == COMPUTE_ALL_FRAMES) {
      return;
    }

    if (compute == COMPUTE_INSERTED_FRAMES) {
      if (currentBasicBlock.frame == null) {
        // This should happen only once, for the implicit first frame (which is explicitly visited
        // in ClassReader if the EXPAND_ASM_INSNS option is used - and COMPUTE_INSERTED_FRAMES
        // can't be set if EXPAND_ASM_INSNS is not used).
        currentBasicBlock.frame = new CurrentFrame(currentBasicBlock);
        currentBasicBlock.frame.setInputFrameFromDescriptor(
            symbolTable, accessFlags, descriptor, numLocal);
        currentBasicBlock.frame.accept(this);
      } else {
        if (type == Opcodes.F_NEW) {
          currentBasicBlock.frame.setInputFrameFromApiFormat(
              symbolTable, numLocal, local, numStack, stack);
        }
        // If type is not F_NEW then it is F_INSERT by hypothesis, and currentBlock.frame contains
        // the stack map frame at the current instruction, computed from the last F_NEW frame and
        // the bytecode instructions in between (via calls to CurrentFrame#execute).
        currentBasicBlock.frame.accept(this);
      }
    } else if (type == Opcodes.F_NEW) {
      if (previousFrame == null) {
        int argumentsSize = Type.getArgumentsAndReturnSizes(descriptor) >> 2;
        Frame implicitFirstFrame = new Frame(new Label());
        implicitFirstFrame.setInputFrameFromDescriptor(
            symbolTable, accessFlags, descriptor, argumentsSize);
        implicitFirstFrame.accept(this);
      }
      currentLocals = numLocal;
      int frameIndex = visitFrameStart(code.length, numLocal, numStack);
      for (int i = 0; i < numLocal; ++i) {
        currentFrame[frameIndex++] = Frame.getAbstractTypeFromApiFormat(symbolTable, local[i]);
      }
      for (int i = 0; i < numStack; ++i) {
        currentFrame[frameIndex++] = Frame.getAbstractTypeFromApiFormat(symbolTable, stack[i]);
      }
      visitFrameEnd();
    } else {
      if (symbolTable.getMajorVersion() < Opcodes.V1_6) {
        throw new IllegalArgumentException("Class versions V1_5 or less must use F_NEW frames.");
      }
      int offsetDelta;
      if (stackMapTableEntries == null) {
        stackMapTableEntries = new ByteVector();
        offsetDelta = code.length;
      } else {
        offsetDelta = code.length - previousFrameOffset - 1;
        if (offsetDelta < 0) {
          if (type == Opcodes.F_SAME) {
            return;
          } else {
            throw new IllegalStateException();
          }
        }
      }

      switch (type) {
        case Opcodes.F_FULL:
          currentLocals = numLocal;
          stackMapTableEntries.putByte(Frame.FULL_FRAME).putShort(offsetDelta).putShort(numLocal);
          for (int i = 0; i < numLocal; ++i) {
            putFrameType(local[i]);
          }
          stackMapTableEntries.putShort(numStack);
          for (int i = 0; i < numStack; ++i) {
            putFrameType(stack[i]);
          }
          break;
        case Opcodes.F_APPEND:
          currentLocals += numLocal;
          stackMapTableEntries.putByte(Frame.SAME_FRAME_EXTENDED + numLocal).putShort(offsetDelta);
          for (int i = 0; i < numLocal; ++i) {
            putFrameType(local[i]);
          }
          break;
        case Opcodes.F_CHOP:
          currentLocals -= numLocal;
          stackMapTableEntries.putByte(Frame.SAME_FRAME_EXTENDED - numLocal).putShort(offsetDelta);
          break;
        case Opcodes.F_SAME:
          if (offsetDelta < 64) {
            stackMapTableEntries.putByte(offsetDelta);
          } else {
            stackMapTableEntries.putByte(Frame.SAME_FRAME_EXTENDED).putShort(offsetDelta);
          }
          break;
        case Opcodes.F_SAME1:
          if (offsetDelta < 64) {
            stackMapTableEntries.putByte(Frame.SAME_LOCALS_1_STACK_ITEM_FRAME + offsetDelta);
          } else {
            stackMapTableEntries
                .putByte(Frame.SAME_LOCALS_1_STACK_ITEM_FRAME_EXTENDED)
                .putShort(offsetDelta);
          }
          putFrameType(stack[0]);
          break;
        default:
          throw new IllegalArgumentException();
      }

      previousFrameOffset = code.length;
      ++stackMapTableNumberOfEntries;
    }

    if (compute == COMPUTE_MAX_STACK_AND_LOCAL_FROM_FRAMES) {
      relativeStackSize = numStack;
      for (int i = 0; i < numStack; ++i) {
        if (stack[i] == Opcodes.LONG || stack[i] == Opcodes.DOUBLE) {
          relativeStackSize++;
        }
      }
      if (relativeStackSize > maxRelativeStackSize) {
        maxRelativeStackSize = relativeStackSize;
      }
    }

    maxStack = Math.max(maxStack, numStack);
    maxLocals = Math.max(maxLocals, currentLocals);
  }

  @Override
  public void visitInsn(final int opcode) {
    lastBytecodeOffset = code.length;
    // Add the instruction to the bytecode of the method.
    code.putByte(opcode);
    // If needed, update the maximum stack size and number of locals, and stack map frames.
    if (currentBasicBlock != null) {
      if (compute == COMPUTE_ALL_FRAMES || compute == COMPUTE_INSERTED_FRAMES) {
        currentBasicBlock.frame.execute(opcode, 0, null, null);
      } else {
        int size = relativeStackSize + STACK_SIZE_DELTA[opcode];
        if (size > maxRelativeStackSize) {
          maxRelativeStackSize = size;
        }
        relativeStackSize = size;
      }
      if ((opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN) || opcode == Opcodes.ATHROW) {
        endCurrentBasicBlockWithNoSuccessor();
      }
    }
  }

  @Override
  public void visitIntInsn(final int opcode, final int operand) {
    lastBytecodeOffset = code.length;
    // Add the instruction to the bytecode of the method.
    if (opcode == Opcodes.SIPUSH) {
      code.put12(opcode, operand);
    } else { // BIPUSH or NEWARRAY
      code.put11(opcode, operand);
    }
    // If needed, update the maximum stack size and number of locals, and stack map frames.
    if (currentBasicBlock != null) {
      if (compute == COMPUTE_ALL_FRAMES || compute == COMPUTE_INSERTED_FRAMES) {
        currentBasicBlock.frame.execute(opcode, operand, null, null);
      } else if (opcode != Opcodes.NEWARRAY) {
        // The stack size delta is 1 for BIPUSH or SIPUSH, and 0 for NEWARRAY.
        int size = relativeStackSize + 1;
        if (size > maxRelativeStackSize) {
          maxRelativeStackSize = size;
        }
        relativeStackSize = size;
      }
    }
  }

  @Override
  public void visitVarInsn(final int opcode, final int var) {
    lastBytecodeOffset = code.length;
    // Add the instruction to the bytecode of the method.
    if (var < 4 && opcode != Opcodes.RET) {
      int optimizedOpcode;
      if (opcode < Opcodes.ISTORE) {
        optimizedOpcode = Constants.ILOAD_0 + ((opcode - Opcodes.ILOAD) << 2) + var;
      } else {
        optimizedOpcode = Constants.ISTORE_0 + ((opcode - Opcodes.ISTORE) << 2) + var;
      }
      code.putByte(optimizedOpcode);
    } else if (var >= 256) {
      code.putByte(Constants.WIDE).put12(opcode, var);
    } else {
      code.put11(opcode, var);
    }
    // If needed, update the maximum stack size and number of locals, and stack map frames.
    if (currentBasicBlock != null) {
      if (compute == COMPUTE_ALL_FRAMES || compute == COMPUTE_INSERTED_FRAMES) {
        currentBasicBlock.frame.execute(opcode, var, null, null);
      } else {
        if (opcode == Opcodes.RET) {
          // No stack size delta.
          currentBasicBlock.flags |= Label.FLAG_SUBROUTINE_END;
          currentBasicBlock.outputStackSize = (short) relativeStackSize;
          endCurrentBasicBlockWithNoSuccessor();
        } else { // xLOAD or xSTORE
          int size = relativeStackSize + STACK_SIZE_DELTA[opcode];
          if (size > maxRelativeStackSize) {
            maxRelativeStackSize = size;
          }
          relativeStackSize = size;
        }
      }
    }
    if (compute != COMPUTE_NOTHING) {
      int currentMaxLocals;
      if (opcode == Opcodes.LLOAD
          || opcode == Opcodes.DLOAD
          || opcode == Opcodes.LSTORE
          || opcode == Opcodes.DSTORE) {
        currentMaxLocals = var + 2;
      } else {
        currentMaxLocals = var + 1;
      }
      if (currentMaxLocals > maxLocals) {
        maxLocals = currentMaxLocals;
      }
    }
    if (opcode >= Opcodes.ISTORE && compute == COMPUTE_ALL_FRAMES && firstHandler != null) {
      // If there are exception handler blocks, each instruction within a handler range is, in
      // theory, a basic block (since execution can jump from this instruction to the exception
      // handler). As a consequence, the local variable types at the beginning of the handler
      // block should be the merge of the local variable types at all the instructions within the
      // handler range. However, instead of creating a basic block for each instruction, we can
      // get the same result in a more efficient way. Namely, by starting a new basic block after
      // each xSTORE instruction, which is what we do here.
      visitLabel(new Label());
    }
  }

  @Override
  public void visitTypeInsn(final int opcode, final String type) {
    lastBytecodeOffset = code.length;
    // Add the instruction to the bytecode of the method.
    Symbol typeSymbol = symbolTable.addConstantClass(type);
    code.put12(opcode, typeSymbol.index);
    // If needed, update the maximum stack size and number of locals, and stack map frames.
    if (currentBasicBlock != null) {
      if (compute == COMPUTE_ALL_FRAMES || compute == COMPUTE_INSERTED_FRAMES) {
        currentBasicBlock.frame.execute(opcode, lastBytecodeOffset, typeSymbol, symbolTable);
      } else if (opcode == Opcodes.NEW) {
        // The stack size delta is 1 for NEW, and 0 for ANEWARRAY, CHECKCAST, or INSTANCEOF.
        int size = relativeStackSize + 1;
        if (size > maxRelativeStackSize) {
          maxRelativeStackSize = size;
        }
        relativeStackSize = size;
      }
    }
  }

  @Override
  public void visitFieldInsn(
      final int opcode, final String owner, final String name, final String descriptor) {
    lastBytecodeOffset = code.length;
    // Add the instruction to the bytecode of the method.
    Symbol fieldrefSymbol = symbolTable.addConstantFieldref(owner, name, descriptor);
    code.put12(opcode, fieldrefSymbol.index);
    // If needed, update the maximum stack size and number of locals, and stack map frames.
    if (currentBasicBlock != null) {
      if (compute == COMPUTE_ALL_FRAMES || compute == COMPUTE_INSERTED_FRAMES) {
        currentBasicBlock.frame.execute(opcode, 0, fieldrefSymbol, symbolTable);
      } else {
        int size;
        char firstDescChar = descriptor.charAt(0);
        switch (opcode) {
          case Opcodes.GETSTATIC:
            size = relativeStackSize + (firstDescChar == 'D' || firstDescChar == 'J' ? 2 : 1);
            break;
          case Opcodes.PUTSTATIC:
            size = relativeStackSize + (firstDescChar == 'D' || firstDescChar == 'J' ? -2 : -1);
            break;
          case Opcodes.GETFIELD:
            size = relativeStackSize + (firstDescChar == 'D' || firstDescChar == 'J' ? 1 : 0);
            break;
          case Opcodes.PUTFIELD:
          default:
            size = relativeStackSize + (firstDescChar == 'D' || firstDescChar == 'J' ? -3 : -2);
            break;
        }
        if (size > maxRelativeStackSize) {
          maxRelativeStackSize = size;
        }
        relativeStackSize = size;
      }
    }
  }

  @Override
  public void visitMethodInsn(
      final int opcode,
      final String owner,
      final String name,
      final String descriptor,
      final boolean isInterface) {
    lastBytecodeOffset = code.length;
    // Add the instruction to the bytecode of the method.
    Symbol methodrefSymbol = symbolTable.addConstantMethodref(owner, name, descriptor, isInterface);
    if (opcode == Opcodes.INVOKEINTERFACE) {
      code.put12(Opcodes.INVOKEINTERFACE, methodrefSymbol.index)
          .put11(methodrefSymbol.getArgumentsAndReturnSizes() >> 2, 0);
    } else {
      code.put12(opcode, methodrefSymbol.index);
    }
    // If needed, update the maximum stack size and number of locals, and stack map frames.
    if (currentBasicBlock != null) {
      if (compute == COMPUTE_ALL_FRAMES || compute == COMPUTE_INSERTED_FRAMES) {
        currentBasicBlock.frame.execute(opcode, 0, methodrefSymbol, symbolTable);
      } else {
        int argumentsAndReturnSize = methodrefSymbol.getArgumentsAndReturnSizes();
        int stackSizeDelta = (argumentsAndReturnSize & 3) - (argumentsAndReturnSize >> 2);
        int size;
        if (opcode == Opcodes.INVOKESTATIC) {
          size = relativeStackSize + stackSizeDelta + 1;
        } else {
          size = relativeStackSize + stackSizeDelta;
        }
        if (size > maxRelativeStackSize) {
          maxRelativeStackSize = size;
        }
        relativeStackSize = size;
      }
    }
  }

  @Override
  public void visitInvokeDynamicInsn(
      final String name,
      final String descriptor,
      final Handle bootstrapMethodHandle,
      final Object... bootstrapMethodArguments) {
    lastBytecodeOffset = code.length;
    // Add the instruction to the bytecode of the method.
    Symbol invokeDynamicSymbol =
        symbolTable.addConstantInvokeDynamic(
            name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
    code.put12(Opcodes.INVOKEDYNAMIC, invokeDynamicSymbol.index);
    code.putShort(0);
    // If needed, update the maximum stack size and number of locals, and stack map frames.
    if (currentBasicBlock != null) {
      if (compute == COMPUTE_ALL_FRAMES || compute == COMPUTE_INSERTED_FRAMES) {
        currentBasicBlock.frame.execute(Opcodes.INVOKEDYNAMIC, 0, invokeDynamicSymbol, symbolTable);
      } else {
        int argumentsAndReturnSize = invokeDynamicSymbol.getArgumentsAndReturnSizes();
        int stackSizeDelta = (argumentsAndReturnSize & 3) - (argumentsAndReturnSize >> 2) + 1;
        int size = relativeStackSize + stackSizeDelta;
        if (size > maxRelativeStackSize) {
          maxRelativeStackSize = size;
        }
        relativeStackSize = size;
      }
    }
  }

  @Override
  public void visitJumpInsn(final int opcode, final Label label) {
    lastBytecodeOffset = code.length;
    // Add the instruction to the bytecode of the method.
    // Compute the 'base' opcode, i.e. GOTO or JSR if opcode is GOTO_W or JSR_W, otherwise opcode.
    int baseOpcode =
        opcode >= Constants.GOTO_W ? opcode - Constants.WIDE_JUMP_OPCODE_DELTA : opcode;
    boolean nextInsnIsJumpTarget = false;
    if ((label.flags & Label.FLAG_RESOLVED) != 0
        && label.bytecodeOffset - code.length < Short.MIN_VALUE) {
      // Case of a backward jump with an offset < -32768. In this case we automatically replace GOTO
      // with GOTO_W, JSR with JSR_W and IFxxx <l> with IFNOTxxx <L> GOTO_W <l> L:..., where
      // IFNOTxxx is the "opposite" opcode of IFxxx (e.g. IFNE for IFEQ) and where <L> designates
      // the instruction just after the GOTO_W.
      if (baseOpcode == Opcodes.GOTO) {
        code.putByte(Constants.GOTO_W);
      } else if (baseOpcode == Opcodes.JSR) {
        code.putByte(Constants.JSR_W);
      } else {
        // Put the "opposite" opcode of baseOpcode. This can be done by flipping the least
        // significant bit for IFNULL and IFNONNULL, and similarly for IFEQ ... IF_ACMPEQ (with a
        // pre and post offset by 1). The jump offset is 8 bytes (3 for IFNOTxxx, 5 for GOTO_W).
        code.putByte(baseOpcode >= Opcodes.IFNULL ? baseOpcode ^ 1 : ((baseOpcode + 1) ^ 1) - 1);
        code.putShort(8);
        // Here we could put a GOTO_W in theory, but if ASM specific instructions are used in this
        // method or another one, and if the class has frames, we will need to insert a frame after
        // this GOTO_W during the additional ClassReader -> ClassWriter round trip to remove the ASM
        // specific instructions. To not miss this additional frame, we need to use an ASM_GOTO_W
        // here, which has the unfortunate effect of forcing this additional round trip (which in
        // some case would not have been really necessary, but we can't know this at this point).
        code.putByte(Constants.ASM_GOTO_W);
        hasAsmInstructions = true;
        // The instruction after the GOTO_W becomes the target of the IFNOT instruction.
        nextInsnIsJumpTarget = true;
      }
      label.put(code, code.length - 1, true);
    } else if (baseOpcode != opcode) {
      // Case of a GOTO_W or JSR_W specified by the user (normally ClassReader when used to remove
      // ASM specific instructions). In this case we keep the original instruction.
      code.putByte(opcode);
      label.put(code, code.length - 1, true);
    } else {
      // Case of a jump with an offset >= -32768, or of a jump with an unknown offset. In these
      // cases we store the offset in 2 bytes (which will be increased via a ClassReader ->
      // ClassWriter round trip if it turns out that 2 bytes are not sufficient).
      code.putByte(baseOpcode);
      label.put(code, code.length - 1, false);
    }

    // If needed, update the maximum stack size and number of locals, and stack map frames.
    if (currentBasicBlock != null) {
      Label nextBasicBlock = null;
      if (compute == COMPUTE_ALL_FRAMES) {
        currentBasicBlock.frame.execute(baseOpcode, 0, null, null);
        // Record the fact that 'label' is the target of a jump instruction.
        label.getCanonicalInstance().flags |= Label.FLAG_JUMP_TARGET;
        // Add 'label' as a successor of the current basic block.
        addSuccessorToCurrentBasicBlock(Edge.JUMP, label);
        if (baseOpcode != Opcodes.GOTO) {
          // The next instruction starts a new basic block (except for GOTO: by default the code
          // following a goto is unreachable - unless there is an explicit label for it - and we
          // should not compute stack frame types for its instructions).
          nextBasicBlock = new Label();
        }
      } else if (compute == COMPUTE_INSERTED_FRAMES) {
        currentBasicBlock.frame.execute(baseOpcode, 0, null, null);
      } else if (compute == COMPUTE_MAX_STACK_AND_LOCAL_FROM_FRAMES) {
        // No need to update maxRelativeStackSize (the stack size delta is always negative).
        relativeStackSize += STACK_SIZE_DELTA[baseOpcode];
      } else {
        if (baseOpcode == Opcodes.JSR) {
          // Record the fact that 'label' designates a subroutine, if not already done.
          if ((label.flags & Label.FLAG_SUBROUTINE_START) == 0) {
            label.flags |= Label.FLAG_SUBROUTINE_START;
            hasSubroutines = true;
          }
          currentBasicBlock.flags |= Label.FLAG_SUBROUTINE_CALLER;
          // Note that, by construction in this method, a block which calls a subroutine has at
          // least two successors in the control flow graph: the first one (added below) leads to
          // the instruction after the JSR, while the second one (added here) leads to the JSR
          // target. Note that the first successor is virtual (it does not correspond to a possible
          // execution path): it is only used to compute the successors of the basic blocks ending
          // with a ret, in {@link Label#addSubroutineRetSuccessors}.
          addSuccessorToCurrentBasicBlock(relativeStackSize + 1, label);
          // The instruction after the JSR starts a new basic block.
          nextBasicBlock = new Label();
        } else {
          // No need to update maxRelativeStackSize (the stack size delta is always negative).
          relativeStackSize += STACK_SIZE_DELTA[baseOpcode];
          addSuccessorToCurrentBasicBlock(relativeStackSize, label);
        }
      }
      // If the next instruction starts a new basic block, call visitLabel to add the label of this
      // instruction as a successor of the current block, and to start a new basic block.
      if (nextBasicBlock != null) {
        if (nextInsnIsJumpTarget) {
          nextBasicBlock.flags |= Label.FLAG_JUMP_TARGET;
        }
        visitLabel(nextBasicBlock);
      }
      if (baseOpcode == Opcodes.GOTO) {
        endCurrentBasicBlockWithNoSuccessor();
      }
    }
  }

  @Override
  public void visitLabel(final Label label) {
    // Resolve the forward references to this label, if any.
    hasAsmInstructions |= label.resolve(code.data, code.length);
    // visitLabel starts a new basic block (except for debug only labels), so we need to update the
    // previous and current block references and list of successors.
    if ((label.flags & Label.FLAG_DEBUG_ONLY) != 0) {
      return;
    }
    if (compute == COMPUTE_ALL_FRAMES) {
      if (currentBasicBlock != null) {
        if (label.bytecodeOffset == currentBasicBlock.bytecodeOffset) {
          // We use {@link Label#getCanonicalInstance} to store the state of a basic block in only
          // one place, but this does not work for labels which have not been visited yet.
          // Therefore, when we detect here two labels having the same bytecode offset, we need to
          // - consolidate the state scattered in these two instances into the canonical instance:
          currentBasicBlock.flags |= (label.flags & Label.FLAG_JUMP_TARGET);
          // - make sure the two instances share the same Frame instance (the implementation of
          // {@link Label#getCanonicalInstance} relies on this property; here label.frame should be
          // null):
          label.frame = currentBasicBlock.frame;
          // - and make sure to NOT assign 'label' into 'currentBasicBlock' or 'lastBasicBlock', so
          // that they still refer to the canonical instance for this bytecode offset.
          return;
        }
        // End the current basic block (with one new successor).
        addSuccessorToCurrentBasicBlock(Edge.JUMP, label);
      }
      // Append 'label' at the end of the basic block list.
      if (lastBasicBlock != null) {
        if (label.bytecodeOffset == lastBasicBlock.bytecodeOffset) {
          // Same comment as above.
          lastBasicBlock.flags |= (label.flags & Label.FLAG_JUMP_TARGET);
          // Here label.frame should be null.
          label.frame = lastBasicBlock.frame;
          currentBasicBlock = lastBasicBlock;
          return;
        }
        lastBasicBlock.nextBasicBlock = label;
      }
      lastBasicBlock = label;
      // Make it the new current basic block.
      currentBasicBlock = label;
      // Here label.frame should be null.
      label.frame = new Frame(label);
    } else if (compute == COMPUTE_INSERTED_FRAMES) {
      if (currentBasicBlock == null) {
        // This case should happen only once, for the visitLabel call in the constructor. Indeed, if
        // compute is equal to COMPUTE_INSERTED_FRAMES, currentBasicBlock stays unchanged.
        currentBasicBlock = label;
      } else {
        // Update the frame owner so that a correct frame offset is computed in Frame.accept().
        currentBasicBlock.frame.owner = label;
      }
    } else if (compute == COMPUTE_MAX_STACK_AND_LOCAL) {
      if (currentBasicBlock != null) {
        // End the current basic block (with one new successor).
        currentBasicBlock.outputStackMax = (short) maxRelativeStackSize;
        addSuccessorToCurrentBasicBlock(relativeStackSize, label);
      }
      // Start a new current basic block, and reset the current and maximum relative stack sizes.
      currentBasicBlock = label;
      relativeStackSize = 0;
      maxRelativeStackSize = 0;
      // Append the new basic block at the end of the basic block list.
      if (lastBasicBlock != null) {
        lastBasicBlock.nextBasicBlock = label;
      }
      lastBasicBlock = label;
    } else if (compute == COMPUTE_MAX_STACK_AND_LOCAL_FROM_FRAMES && currentBasicBlock == null) {
      // This case should happen only once, for the visitLabel call in the constructor. Indeed, if
      // compute is equal to COMPUTE_MAX_STACK_AND_LOCAL_FROM_FRAMES, currentBasicBlock stays
      // unchanged.
      currentBasicBlock = label;
    }
  }

  @Override
  public void visitLdcInsn(final Object value) {
    lastBytecodeOffset = code.length;
    // Add the instruction to the bytecode of the method.
    Symbol constantSymbol = symbolTable.addConstant(value);
    int constantIndex = constantSymbol.index;
    char firstDescriptorChar;
    boolean isLongOrDouble =
        constantSymbol.tag == Symbol.CONSTANT_LONG_TAG
            || constantSymbol.tag == Symbol.CONSTANT_DOUBLE_TAG
            || (constantSymbol.tag == Symbol.CONSTANT_DYNAMIC_TAG
                && ((firstDescriptorChar = constantSymbol.value.charAt(0)) == 'J'
                    || firstDescriptorChar == 'D'));
    if (isLongOrDouble) {
      code.put12(Constants.LDC2_W, constantIndex);
    } else if (constantIndex >= 256) {
      code.put12(Constants.LDC_W, constantIndex);
    } else {
      code.put11(Opcodes.LDC, constantIndex);
    }
    // If needed, update the maximum stack size and number of locals, and stack map frames.
    if (currentBasicBlock != null) {
      if (compute == COMPUTE_ALL_FRAMES || compute == COMPUTE_INSERTED_FRAMES) {
        currentBasicBlock.frame.execute(Opcodes.LDC, 0, constantSymbol, symbolTable);
      } else {
        int size = relativeStackSize + (isLongOrDouble ? 2 : 1);
        if (size > maxRelativeStackSize) {
          maxRelativeStackSize = size;
        }
        relativeStackSize = size;
      }
    }
  }

  @Override
  public void visitIincInsn(final int var, final int increment) {
    lastBytecodeOffset = code.length;
    // Add the instruction to the bytecode of the method.
    if ((var > 255) || (increment > 127) || (increment < -128)) {
      code.putByte(Constants.WIDE).put12(Opcodes.IINC, var).putShort(increment);
    } else {
      code.putByte(Opcodes.IINC).put11(var, increment);
    }
    // If needed, update the maximum stack size and number of locals, and stack map frames.
    if (currentBasicBlock != null
        && (compute == COMPUTE_ALL_FRAMES || compute == COMPUTE_INSERTED_FRAMES)) {
      currentBasicBlock.frame.execute(Opcodes.IINC, var, null, null);
    }
    if (compute != COMPUTE_NOTHING) {
      int currentMaxLocals = var + 1;
      if (currentMaxLocals > maxLocals) {
        maxLocals = currentMaxLocals;
      }
    }
  }

  @Override
  public void visitTableSwitchInsn(
      final int min, final int max, final Label dflt, final Label... labels) {
    lastBytecodeOffset = code.length;
    // Add the instruction to the bytecode of the method.
    code.putByte(Opcodes.TABLESWITCH).putByteArray(null, 0, (4 - code.length % 4) % 4);
    dflt.put(code, lastBytecodeOffset, true);
    code.putInt(min).putInt(max);
    for (Label label : labels) {
      label.put(code, lastBytecodeOffset, true);
    }
    // If needed, update the maximum stack size and number of locals, and stack map frames.
    visitSwitchInsn(dflt, labels);
  }

  @Override
  public void visitLookupSwitchInsn(final Label dflt, final int[] keys, final Label[] labels) {
    lastBytecodeOffset = code.length;
    // Add the instruction to the bytecode of the method.
    code.putByte(Opcodes.LOOKUPSWITCH).putByteArray(null, 0, (4 - code.length % 4) % 4);
    dflt.put(code, lastBytecodeOffset, true);
    code.putInt(labels.length);
    for (int i = 0; i < labels.length; ++i) {
      code.putInt(keys[i]);
      labels[i].put(code, lastBytecodeOffset, true);
    }
    // If needed, update the maximum stack size and number of locals, and stack map frames.
    visitSwitchInsn(dflt, labels);
  }

  private void visitSwitchInsn(final Label dflt, final Label[] labels) {
    if (currentBasicBlock != null) {
      if (compute == COMPUTE_ALL_FRAMES) {
        currentBasicBlock.frame.execute(Opcodes.LOOKUPSWITCH, 0, null, null);
        // Add all the labels as successors of the current basic block.
        addSuccessorToCurrentBasicBlock(Edge.JUMP, dflt);
        dflt.getCanonicalInstance().flags |= Label.FLAG_JUMP_TARGET;
        for (Label label : labels) {
          addSuccessorToCurrentBasicBlock(Edge.JUMP, label);
          label.getCanonicalInstance().flags |= Label.FLAG_JUMP_TARGET;
        }
      } else if (compute == COMPUTE_MAX_STACK_AND_LOCAL) {
        // No need to update maxRelativeStackSize (the stack size delta is always negative).
        --relativeStackSize;
        // Add all the labels as successors of the current basic block.
        addSuccessorToCurrentBasicBlock(relativeStackSize, dflt);
        for (Label label : labels) {
          addSuccessorToCurrentBasicBlock(relativeStackSize, label);
        }
      }
      // End the current basic block.
      endCurrentBasicBlockWithNoSuccessor();
    }
  }

  @Override
  public void visitMultiANewArrayInsn(final String descriptor, final int numDimensions) {
    lastBytecodeOffset = code.length;
    // Add the instruction to the bytecode of the method.
    Symbol descSymbol = symbolTable.addConstantClass(descriptor);
    code.put12(Opcodes.MULTIANEWARRAY, descSymbol.index).putByte(numDimensions);
    // If needed, update the maximum stack size and number of locals, and stack map frames.
    if (currentBasicBlock != null) {
      if (compute == COMPUTE_ALL_FRAMES || compute == COMPUTE_INSERTED_FRAMES) {
        currentBasicBlock.frame.execute(
            Opcodes.MULTIANEWARRAY, numDimensions, descSymbol, symbolTable);
      } else {
        // No need to update maxRelativeStackSize (the stack size delta is always negative).
        relativeStackSize += 1 - numDimensions;
      }
    }
  }

  @Override
  public AnnotationVisitor visitInsnAnnotation(
      final int typeRef, final TypePath typePath, final String descriptor, final boolean visible) {
    if (visible) {
      return lastCodeRuntimeVisibleTypeAnnotation =
          AnnotationWriter.create(
              symbolTable,
              (typeRef & 0xFF0000FF) | (lastBytecodeOffset << 8),
              typePath,
              descriptor,
              lastCodeRuntimeVisibleTypeAnnotation);
    } else {
      return lastCodeRuntimeInvisibleTypeAnnotation =
          AnnotationWriter.create(
              symbolTable,
              (typeRef & 0xFF0000FF) | (lastBytecodeOffset << 8),
              typePath,
              descriptor,
              lastCodeRuntimeInvisibleTypeAnnotation);
    }
  }

  @Override
  public void visitTryCatchBlock(
      final Label start, final Label end, final Label handler, final String type) {
    Handler newHandler =
        new Handler(
            start, end, handler, type != null ? symbolTable.addConstantClass(type).index : 0, type);
    if (firstHandler == null) {
      firstHandler = newHandler;
    } else {
      lastHandler.nextHandler = newHandler;
    }
    lastHandler = newHandler;
  }

  @Override
  public AnnotationVisitor visitTryCatchAnnotation(
      final int typeRef, final TypePath typePath, final String descriptor, final boolean visible) {
    if (visible) {
      return lastCodeRuntimeVisibleTypeAnnotation =
          AnnotationWriter.create(
              symbolTable, typeRef, typePath, descriptor, lastCodeRuntimeVisibleTypeAnnotation);
    } else {
      return lastCodeRuntimeInvisibleTypeAnnotation =
          AnnotationWriter.create(
              symbolTable, typeRef, typePath, descriptor, lastCodeRuntimeInvisibleTypeAnnotation);
    }
  }

  @Override
  public void visitLocalVariable(
      final String name,
      final String descriptor,
      final String signature,
      final Label start,
      final Label end,
      final int index) {
    if (signature != null) {
      if (localVariableTypeTable == null) {
        localVariableTypeTable = new ByteVector();
      }
      ++localVariableTypeTableLength;
      localVariableTypeTable
          .putShort(start.bytecodeOffset)
          .putShort(end.bytecodeOffset - start.bytecodeOffset)
          .putShort(symbolTable.addConstantUtf8(name))
          .putShort(symbolTable.addConstantUtf8(signature))
          .putShort(index);
    }
    if (localVariableTable == null) {
      localVariableTable = new ByteVector();
    }
    ++localVariableTableLength;
    localVariableTable
        .putShort(start.bytecodeOffset)
        .putShort(end.bytecodeOffset - start.bytecodeOffset)
        .putShort(symbolTable.addConstantUtf8(name))
        .putShort(symbolTable.addConstantUtf8(descriptor))
        .putShort(index);
    if (compute != COMPUTE_NOTHING) {
      char firstDescChar = descriptor.charAt(0);
      int currentMaxLocals = index + (firstDescChar == 'J' || firstDescChar == 'D' ? 2 : 1);
      if (currentMaxLocals > maxLocals) {
        maxLocals = currentMaxLocals;
      }
    }
  }

  @Override
  public AnnotationVisitor visitLocalVariableAnnotation(
      final int typeRef,
      final TypePath typePath,
      final Label[] start,
      final Label[] end,
      final int[] index,
      final String descriptor,
      final boolean visible) {
    // Create a ByteVector to hold a 'type_annotation' JVMS structure.
    // See https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.20.
    ByteVector typeAnnotation = new ByteVector();
    // Write target_type, target_info, and target_path.
    typeAnnotation.putByte(typeRef >>> 24).putShort(start.length);
    for (int i = 0; i < start.length; ++i) {
      typeAnnotation
          .putShort(start[i].bytecodeOffset)
          .putShort(end[i].bytecodeOffset - start[i].bytecodeOffset)
          .putShort(index[i]);
    }
    TypePath.put(typePath, typeAnnotation);
    // Write type_index and reserve space for num_element_value_pairs.
    typeAnnotation.putShort(symbolTable.addConstantUtf8(descriptor)).putShort(0);
    if (visible) {
      return lastCodeRuntimeVisibleTypeAnnotation =
          new AnnotationWriter(
              symbolTable,
              /* useNamedValues = */ true,
              typeAnnotation,
              lastCodeRuntimeVisibleTypeAnnotation);
    } else {
      return lastCodeRuntimeInvisibleTypeAnnotation =
          new AnnotationWriter(
              symbolTable,
              /* useNamedValues = */ true,
              typeAnnotation,
              lastCodeRuntimeInvisibleTypeAnnotation);
    }
  }

  @Override
  public void visitLineNumber(final int line, final Label start) {
    if (lineNumberTable == null) {
      lineNumberTable = new ByteVector();
    }
    ++lineNumberTableLength;
    lineNumberTable.putShort(start.bytecodeOffset);
    lineNumberTable.putShort(line);
  }

  @Override
  public void visitMaxs(final int maxStack, final int maxLocals) {
    if (compute == COMPUTE_ALL_FRAMES) {
      computeAllFrames();
    } else if (compute == COMPUTE_MAX_STACK_AND_LOCAL) {
      computeMaxStackAndLocal();
    } else if (compute == COMPUTE_MAX_STACK_AND_LOCAL_FROM_FRAMES) {
      this.maxStack = maxRelativeStackSize;
    } else {
      this.maxStack = maxStack;
      this.maxLocals = maxLocals;
    }
  }

  /** Computes all the stack map frames of the method, from scratch. */
  private void computeAllFrames() {
    // Complete the control flow graph with exception handler blocks.
    Handler handler = firstHandler;
    while (handler != null) {
      String catchTypeDescriptor =
          handler.catchTypeDescriptor == null ? "java/lang/Throwable" : handler.catchTypeDescriptor;
      int catchType = Frame.getAbstractTypeFromInternalName(symbolTable, catchTypeDescriptor);
      // Mark handlerBlock as an exception handler.
      Label handlerBlock = handler.handlerPc.getCanonicalInstance();
      handlerBlock.flags |= Label.FLAG_JUMP_TARGET;
      // Add handlerBlock as a successor of all the basic blocks in the exception handler range.
      Label handlerRangeBlock = handler.startPc.getCanonicalInstance();
      Label handlerRangeEnd = handler.endPc.getCanonicalInstance();
      while (handlerRangeBlock != handlerRangeEnd) {
        handlerRangeBlock.outgoingEdges =
            new Edge(catchType, handlerBlock, handlerRangeBlock.outgoingEdges);
        handlerRangeBlock = handlerRangeBlock.nextBasicBlock;
      }
      handler = handler.nextHandler;
    }

    // Create and visit the first (implicit) frame.
    Frame firstFrame = firstBasicBlock.frame;
    firstFrame.setInputFrameFromDescriptor(symbolTable, accessFlags, descriptor, this.maxLocals);
    firstFrame.accept(this);

    // Fix point algorithm: add the first basic block to a list of blocks to process (i.e. blocks
    // whose stack map frame has changed) and, while there are blocks to process, remove one from
    // the list and update the stack map frames of its successor blocks in the control flow graph
    // (which might change them, in which case these blocks must be processed too, and are thus
    // added to the list of blocks to process). Also compute the maximum stack size of the method,
    // as a by-product.
    Label listOfBlocksToProcess = firstBasicBlock;
    listOfBlocksToProcess.nextListElement = Label.EMPTY_LIST;
    int maxStackSize = 0;
    while (listOfBlocksToProcess != Label.EMPTY_LIST) {
      // Remove a basic block from the list of blocks to process.
      Label basicBlock = listOfBlocksToProcess;
      listOfBlocksToProcess = listOfBlocksToProcess.nextListElement;
      basicBlock.nextListElement = null;
      // By definition, basicBlock is reachable.
      basicBlock.flags |= Label.FLAG_REACHABLE;
      // Update the (absolute) maximum stack size.
      int maxBlockStackSize = basicBlock.frame.getInputStackSize() + basicBlock.outputStackMax;
      if (maxBlockStackSize > maxStackSize) {
        maxStackSize = maxBlockStackSize;
      }
      // Update the successor blocks of basicBlock in the control flow graph.
      Edge outgoingEdge = basicBlock.outgoingEdges;
      while (outgoingEdge != null) {
        Label successorBlock = outgoingEdge.successor.getCanonicalInstance();
        boolean successorBlockChanged =
            basicBlock.frame.merge(symbolTable, successorBlock.frame, outgoingEdge.info);
        if (successorBlockChanged && successorBlock.nextListElement == null) {
          // If successorBlock has changed it must be processed. Thus, if it is not already in the
          // list of blocks to process, add it to this list.
          successorBlock.nextListElement = listOfBlocksToProcess;
          listOfBlocksToProcess = successorBlock;
        }
        outgoingEdge = outgoingEdge.nextEdge;
      }
    }

    // Loop over all the basic blocks and visit the stack map frames that must be stored in the
    // StackMapTable attribute. Also replace unreachable code with NOP* ATHROW, and remove it from
    // exception handler ranges.
    Label basicBlock = firstBasicBlock;
    while (basicBlock != null) {
      if ((basicBlock.flags & (Label.FLAG_JUMP_TARGET | Label.FLAG_REACHABLE))
          == (Label.FLAG_JUMP_TARGET | Label.FLAG_REACHABLE)) {
        basicBlock.frame.accept(this);
      }
      if ((basicBlock.flags & Label.FLAG_REACHABLE) == 0) {
        // Find the start and end bytecode offsets of this unreachable block.
        Label nextBasicBlock = basicBlock.nextBasicBlock;
        int startOffset = basicBlock.bytecodeOffset;
        int endOffset = (nextBasicBlock == null ? code.length : nextBasicBlock.bytecodeOffset) - 1;
        if (endOffset >= startOffset) {
          // Replace its instructions with NOP ... NOP ATHROW.
          for (int i = startOffset; i < endOffset; ++i) {
            code.data[i] = Opcodes.NOP;
          }
          code.data[endOffset] = (byte) Opcodes.ATHROW;
          // Emit a frame for this unreachable block, with no local and a Throwable on the stack
          // (so that the ATHROW could consume this Throwable if it were reachable).
          int frameIndex = visitFrameStart(startOffset, /* numLocal = */ 0, /* numStack = */ 1);
          currentFrame[frameIndex] =
              Frame.getAbstractTypeFromInternalName(symbolTable, "java/lang/Throwable");
          visitFrameEnd();
          // Remove this unreachable basic block from the exception handler ranges.
          firstHandler = Handler.removeRange(firstHandler, basicBlock, nextBasicBlock);
          // The maximum stack size is now at least one, because of the Throwable declared above.
          maxStackSize = Math.max(maxStackSize, 1);
        }
      }
      basicBlock = basicBlock.nextBasicBlock;
    }

    this.maxStack = maxStackSize;
  }

  /** Computes the maximum stack size of the method. */
  private void computeMaxStackAndLocal() {
    // Complete the control flow graph with exception handler blocks.
    Handler handler = firstHandler;
    while (handler != null) {
      Label handlerBlock = handler.handlerPc;
      Label handlerRangeBlock = handler.startPc;
      Label handlerRangeEnd = handler.endPc;
      // Add handlerBlock as a successor of all the basic blocks in the exception handler range.
      while (handlerRangeBlock != handlerRangeEnd) {
        if ((handlerRangeBlock.flags & Label.FLAG_SUBROUTINE_CALLER) == 0) {
          handlerRangeBlock.outgoingEdges =
              new Edge(Edge.EXCEPTION, handlerBlock, handlerRangeBlock.outgoingEdges);
        } else {
          // If handlerRangeBlock is a JSR block, add handlerBlock after the first two outgoing
          // edges to preserve the hypothesis about JSR block successors order (see
          // {@link #visitJumpInsn}).
          handlerRangeBlock.outgoingEdges.nextEdge.nextEdge =
              new Edge(
                  Edge.EXCEPTION, handlerBlock, handlerRangeBlock.outgoingEdges.nextEdge.nextEdge);
        }
        handlerRangeBlock = handlerRangeBlock.nextBasicBlock;
      }
      handler = handler.nextHandler;
    }

    // Complete the control flow graph with the successor blocks of subroutines, if needed.
    if (hasSubroutines) {
      // First step: find the subroutines. This step determines, for each basic block, to which
      // subroutine(s) it belongs. Start with the main "subroutine":
      short numSubroutines = 1;
      firstBasicBlock.markSubroutine(numSubroutines);
      // Then, mark the subroutines called by the main subroutine, then the subroutines called by
      // those called by the main subroutine, etc.
      for (short currentSubroutine = 1; currentSubroutine <= numSubroutines; ++currentSubroutine) {
        Label basicBlock = firstBasicBlock;
        while (basicBlock != null) {
          if ((basicBlock.flags & Label.FLAG_SUBROUTINE_CALLER) != 0
              && basicBlock.subroutineId == currentSubroutine) {
            Label jsrTarget = basicBlock.outgoingEdges.nextEdge.successor;
            if (jsrTarget.subroutineId == 0) {
              // If this subroutine has not been marked yet, find its basic blocks.
              jsrTarget.markSubroutine(++numSubroutines);
            }
          }
          basicBlock = basicBlock.nextBasicBlock;
        }
      }
      // Second step: find the successors in the control flow graph of each subroutine basic block
      // 'r' ending with a RET instruction. These successors are the virtual successors of the basic
      // blocks ending with JSR instructions (see {@link #visitJumpInsn)} that can reach 'r'.
      Label basicBlock = firstBasicBlock;
      while (basicBlock != null) {
        if ((basicBlock.flags & Label.FLAG_SUBROUTINE_CALLER) != 0) {
          // By construction, jsr targets are stored in the second outgoing edge of basic blocks
          // that ends with a jsr instruction (see {@link #FLAG_SUBROUTINE_CALLER}).
          Label subroutine = basicBlock.outgoingEdges.nextEdge.successor;
          subroutine.addSubroutineRetSuccessors(basicBlock);
        }
        basicBlock = basicBlock.nextBasicBlock;
      }
    }

    // Data flow algorithm: put the first basic block in a list of blocks to process (i.e. blocks
    // whose input stack size has changed) and, while there are blocks to process, remove one
    // from the list, update the input stack size of its successor blocks in the control flow
    // graph, and add these blocks to the list of blocks to process (if not already done).
    Label listOfBlocksToProcess = firstBasicBlock;
    listOfBlocksToProcess.nextListElement = Label.EMPTY_LIST;
    int maxStackSize = maxStack;
    while (listOfBlocksToProcess != Label.EMPTY_LIST) {
      // Remove a basic block from the list of blocks to process. Note that we don't reset
      // basicBlock.nextListElement to null on purpose, to make sure we don't reprocess already
      // processed basic blocks.
      Label basicBlock = listOfBlocksToProcess;
      listOfBlocksToProcess = listOfBlocksToProcess.nextListElement;
      // Compute the (absolute) input stack size and maximum stack size of this block.
      int inputStackTop = basicBlock.inputStackSize;
      int maxBlockStackSize = inputStackTop + basicBlock.outputStackMax;
      // Update the absolute maximum stack size of the method.
      if (maxBlockStackSize > maxStackSize) {
        maxStackSize = maxBlockStackSize;
      }
      // Update the input stack size of the successor blocks of basicBlock in the control flow
      // graph, and add these blocks to the list of blocks to process, if not already done.
      Edge outgoingEdge = basicBlock.outgoingEdges;
      if ((basicBlock.flags & Label.FLAG_SUBROUTINE_CALLER) != 0) {
        // Ignore the first outgoing edge of the basic blocks ending with a jsr: these are virtual
        // edges which lead to the instruction just after the jsr, and do not correspond to a
        // possible execution path (see {@link #visitJumpInsn} and
        // {@link Label#FLAG_SUBROUTINE_CALLER}).
        outgoingEdge = outgoingEdge.nextEdge;
      }
      while (outgoingEdge != null) {
        Label successorBlock = outgoingEdge.successor;
        if (successorBlock.nextListElement == null) {
          successorBlock.inputStackSize =
              (short) (outgoingEdge.info == Edge.EXCEPTION ? 1 : inputStackTop + outgoingEdge.info);
          successorBlock.nextListElement = listOfBlocksToProcess;
          listOfBlocksToProcess = successorBlock;
        }
        outgoingEdge = outgoingEdge.nextEdge;
      }
    }
    this.maxStack = maxStackSize;
  }

  @Override
  public void visitEnd() {
    // Nothing to do.
  }

  // -----------------------------------------------------------------------------------------------
  // Utility methods: control flow analysis algorithm
  // -----------------------------------------------------------------------------------------------

  /**
   * Adds a successor to {@link #currentBasicBlock} in the control flow graph.
   *
   * @param info information about the control flow edge to be added.
   * @param successor the successor block to be added to the current basic block.
   */
  private void addSuccessorToCurrentBasicBlock(final int info, final Label successor) {
    currentBasicBlock.outgoingEdges = new Edge(info, successor, currentBasicBlock.outgoingEdges);
  }

  /**
   * Ends the current basic block. This method must be used in the case where the current basic
   * block does not have any successor.
   *
   * <p>WARNING: this method must be called after the currently visited instruction has been put in
   * {@link #code} (if frames are computed, this method inserts a new Label to start a new basic
   * block after the current instruction).
   */
  private void endCurrentBasicBlockWithNoSuccessor() {
    if (compute == COMPUTE_ALL_FRAMES) {
      Label nextBasicBlock = new Label();
      nextBasicBlock.frame = new Frame(nextBasicBlock);
      nextBasicBlock.resolve(code.data, code.length);
      lastBasicBlock.nextBasicBlock = nextBasicBlock;
      lastBasicBlock = nextBasicBlock;
      currentBasicBlock = null;
    } else if (compute == COMPUTE_MAX_STACK_AND_LOCAL) {
      currentBasicBlock.outputStackMax = (short) maxRelativeStackSize;
      currentBasicBlock = null;
    }
  }

  // -----------------------------------------------------------------------------------------------
  // Utility methods: stack map frames
  // -----------------------------------------------------------------------------------------------

  /**
   * Starts the visit of a new stack map frame, stored in {@link #currentFrame}.
   *
   * @param offset the bytecode offset of the instruction to which the frame corresponds.
   * @param numLocal the number of local variables in the frame.
   * @param numStack the number of stack elements in the frame.
   * @return the index of the next element to be written in this frame.
   */
  int visitFrameStart(final int offset, final int numLocal, final int numStack) {
    int frameLength = 3 + numLocal + numStack;
    if (currentFrame == null || currentFrame.length < frameLength) {
      currentFrame = new int[frameLength];
    }
    currentFrame[0] = offset;
    currentFrame[1] = numLocal;
    currentFrame[2] = numStack;
    return 3;
  }

  /**
   * Sets an abstract type in {@link #currentFrame}.
   *
   * @param frameIndex the index of the element to be set in {@link #currentFrame}.
   * @param abstractType an abstract type.
   */
  void visitAbstractType(final int frameIndex, final int abstractType) {
    currentFrame[frameIndex] = abstractType;
  }

  /**
   * Ends the visit of {@link #currentFrame} by writing it in the StackMapTable entries and by
   * updating the StackMapTable number_of_entries (except if the current frame is the first one,
   * which is implicit in StackMapTable). Then resets {@link #currentFrame} to {@literal null}.
   */
  void visitFrameEnd() {
    if (previousFrame != null) {
      if (stackMapTableEntries == null) {
        stackMapTableEntries = new ByteVector();
      }
      putFrame();
      ++stackMapTableNumberOfEntries;
    }
    previousFrame = currentFrame;
    currentFrame = null;
  }

  /** Compresses and writes {@link #currentFrame} in a new StackMapTable entry. */
  private void putFrame() {
    final int numLocal = currentFrame[1];
    final int numStack = currentFrame[2];
    if (symbolTable.getMajorVersion() < Opcodes.V1_6) {
      // Generate a StackMap attribute entry, which are always uncompressed.
      stackMapTableEntries.putShort(currentFrame[0]).putShort(numLocal);
      putAbstractTypes(3, 3 + numLocal);
      stackMapTableEntries.putShort(numStack);
      putAbstractTypes(3 + numLocal, 3 + numLocal + numStack);
      return;
    }
    final int offsetDelta =
        stackMapTableNumberOfEntries == 0
            ? currentFrame[0]
            : currentFrame[0] - previousFrame[0] - 1;
    final int previousNumlocal = previousFrame[1];
    final int numLocalDelta = numLocal - previousNumlocal;
    int type = Frame.FULL_FRAME;
    if (numStack == 0) {
      switch (numLocalDelta) {
        case -3:
        case -2:
        case -1:
          type = Frame.CHOP_FRAME;
          break;
        case 0:
          type = offsetDelta < 64 ? Frame.SAME_FRAME : Frame.SAME_FRAME_EXTENDED;
          break;
        case 1:
        case 2:
        case 3:
          type = Frame.APPEND_FRAME;
          break;
        default:
          // Keep the FULL_FRAME type.
          break;
      }
    } else if (numLocalDelta == 0 && numStack == 1) {
      type =
          offsetDelta < 63
              ? Frame.SAME_LOCALS_1_STACK_ITEM_FRAME
              : Frame.SAME_LOCALS_1_STACK_ITEM_FRAME_EXTENDED;
    }
    if (type != Frame.FULL_FRAME) {
      // Verify if locals are the same as in the previous frame.
      int frameIndex = 3;
      for (int i = 0; i < previousNumlocal && i < numLocal; i++) {
        if (currentFrame[frameIndex] != previousFrame[frameIndex]) {
          type = Frame.FULL_FRAME;
          break;
        }
        frameIndex++;
      }
    }
    switch (type) {
      case Frame.SAME_FRAME:
        stackMapTableEntries.putByte(offsetDelta);
        break;
      case Frame.SAME_LOCALS_1_STACK_ITEM_FRAME:
        stackMapTableEntries.putByte(Frame.SAME_LOCALS_1_STACK_ITEM_FRAME + offsetDelta);
        putAbstractTypes(3 + numLocal, 4 + numLocal);
        break;
      case Frame.SAME_LOCALS_1_STACK_ITEM_FRAME_EXTENDED:
        stackMapTableEntries
            .putByte(Frame.SAME_LOCALS_1_STACK_ITEM_FRAME_EXTENDED)
            .putShort(offsetDelta);
        putAbstractTypes(3 + numLocal, 4 + numLocal);
        break;
      case Frame.SAME_FRAME_EXTENDED:
        stackMapTableEntries.putByte(Frame.SAME_FRAME_EXTENDED).putShort(offsetDelta);
        break;
      case Frame.CHOP_FRAME:
        stackMapTableEntries
            .putByte(Frame.SAME_FRAME_EXTENDED + numLocalDelta)
            .putShort(offsetDelta);
        break;
      case Frame.APPEND_FRAME:
        stackMapTableEntries
            .putByte(Frame.SAME_FRAME_EXTENDED + numLocalDelta)
            .putShort(offsetDelta);
        putAbstractTypes(3 + previousNumlocal, 3 + numLocal);
        break;
      case Frame.FULL_FRAME:
      default:
        stackMapTableEntries.putByte(Frame.FULL_FRAME).putShort(offsetDelta).putShort(numLocal);
        putAbstractTypes(3, 3 + numLocal);
        stackMapTableEntries.putShort(numStack);
        putAbstractTypes(3 + numLocal, 3 + numLocal + numStack);
        break;
    }
  }

  /**
   * Puts some abstract types of {@link #currentFrame} in {@link #stackMapTableEntries} , using the
   * JVMS verification_type_info format used in StackMapTable attributes.
   *
   * @param start index of the first type in {@link #currentFrame} to write.
   * @param end index of last type in {@link #currentFrame} to write (exclusive).
   */
  private void putAbstractTypes(final int start, final int end) {
    for (int i = start; i < end; ++i) {
      Frame.putAbstractType(symbolTable, currentFrame[i], stackMapTableEntries);
    }
  }

  /**
   * Puts the given public API frame element type in {@link #stackMapTableEntries} , using the JVMS
   * verification_type_info format used in StackMapTable attributes.
   *
   * @param type a frame element type described using the same format as in {@link
   *     MethodVisitor#visitFrame}, i.e. either {@link Opcodes#TOP}, {@link Opcodes#INTEGER}, {@link
   *     Opcodes#FLOAT}, {@link Opcodes#LONG}, {@link Opcodes#DOUBLE}, {@link Opcodes#NULL}, or
   *     {@link Opcodes#UNINITIALIZED_THIS}, or the internal name of a class, or a Label designating
   *     a NEW instruction (for uninitialized types).
   */
  private void putFrameType(final Object type) {
    if (type instanceof Integer) {
      stackMapTableEntries.putByte(((Integer) type).intValue());
    } else if (type instanceof String) {
      stackMapTableEntries
          .putByte(Frame.ITEM_OBJECT)
          .putShort(symbolTable.addConstantClass((String) type).index);
    } else {
      stackMapTableEntries
          .putByte(Frame.ITEM_UNINITIALIZED)
          .putShort(((Label) type).bytecodeOffset);
    }
  }

  // -----------------------------------------------------------------------------------------------
  // Utility methods
  // -----------------------------------------------------------------------------------------------

  /**
   * Returns whether the attributes of this method can be copied from the attributes of the given
   * method (assuming there is no method visitor between the given ClassReader and this
   * MethodWriter). This method should only be called just after this MethodWriter has been created,
   * and before any content is visited. It returns true if the attributes corresponding to the
   * constructor arguments (at most a Signature, an Exception, a Deprecated and a Synthetic
   * attribute) are the same as the corresponding attributes in the given method.
   *
   * @param source the source ClassReader from which the attributes of this method might be copied.
   * @param hasSyntheticAttribute whether the method_info JVMS structure from which the attributes
   *     of this method might be copied contains a Synthetic attribute.
   * @param hasDeprecatedAttribute whether the method_info JVMS structure from which the attributes
   *     of this method might be copied contains a Deprecated attribute.
   * @param descriptorIndex the descriptor_index field of the method_info JVMS structure from which
   *     the attributes of this method might be copied.
   * @param signatureIndex the constant pool index contained in the Signature attribute of the
   *     method_info JVMS structure from which the attributes of this method might be copied, or 0.
   * @param exceptionsOffset the offset in 'source.b' of the Exceptions attribute of the method_info
   *     JVMS structure from which the attributes of this method might be copied, or 0.
   * @return whether the attributes of this method can be copied from the attributes of the
   *     method_info JVMS structure in 'source.b', between 'methodInfoOffset' and 'methodInfoOffset'
   *     + 'methodInfoLength'.
   */
  boolean canCopyMethodAttributes(
      final ClassReader source,
      final boolean hasSyntheticAttribute,
      final boolean hasDeprecatedAttribute,
      final int descriptorIndex,
      final int signatureIndex,
      final int exceptionsOffset) {
    // If the method descriptor has changed, with more locals than the max_locals field of the
    // original Code attribute, if any, then the original method attributes can't be copied. A
    // conservative check on the descriptor changes alone ensures this (being more precise is not
    // worth the additional complexity, because these cases should be rare -- if a transform changes
    // a method descriptor, most of the time it needs to change the method's code too).
    if (source != symbolTable.getSource()
        || descriptorIndex != this.descriptorIndex
        || signatureIndex != this.signatureIndex
        || hasDeprecatedAttribute != ((accessFlags & Opcodes.ACC_DEPRECATED) != 0)) {
      return false;
    }
    boolean needSyntheticAttribute =
        symbolTable.getMajorVersion() < Opcodes.V1_5 && (accessFlags & Opcodes.ACC_SYNTHETIC) != 0;
    if (hasSyntheticAttribute != needSyntheticAttribute) {
      return false;
    }
    if (exceptionsOffset == 0) {
      if (numberOfExceptions != 0) {
        return false;
      }
    } else if (source.readUnsignedShort(exceptionsOffset) == numberOfExceptions) {
      int currentExceptionOffset = exceptionsOffset + 2;
      for (int i = 0; i < numberOfExceptions; ++i) {
        if (source.readUnsignedShort(currentExceptionOffset) != exceptionIndexTable[i]) {
          return false;
        }
        currentExceptionOffset += 2;
      }
    }
    return true;
  }

  /**
   * Sets the source from which the attributes of this method will be copied.
   *
   * @param methodInfoOffset the offset in 'symbolTable.getSource()' of the method_info JVMS
   *     structure from which the attributes of this method will be copied.
   * @param methodInfoLength the length in 'symbolTable.getSource()' of the method_info JVMS
   *     structure from which the attributes of this method will be copied.
   */
  void setMethodAttributesSource(final int methodInfoOffset, final int methodInfoLength) {
    // Don't copy the attributes yet, instead store their location in the source class reader so
    // they can be copied later, in {@link #putMethodInfo}. Note that we skip the 6 header bytes
    // of the method_info JVMS structure.
    this.sourceOffset = methodInfoOffset + 6;
    this.sourceLength = methodInfoLength - 6;
  }

  /**
   * Returns the size of the method_info JVMS structure generated by this MethodWriter. Also add the
   * names of the attributes of this method in the constant pool.
   *
   * @return the size in bytes of the method_info JVMS structure.
   */
  int computeMethodInfoSize() {
    // If this method_info must be copied from an existing one, the size computation is trivial.
    if (sourceOffset != 0) {
      // sourceLength excludes the first 6 bytes for access_flags, name_index and descriptor_index.
      return 6 + sourceLength;
    }
    // 2 bytes each for access_flags, name_index, descriptor_index and attributes_count.
    int size = 8;
    // For ease of reference, we use here the same attribute order as in Section 4.7 of the JVMS.
    if (code.length > 0) {
      if (code.length > 65535) {
        throw new MethodTooLargeException(
            symbolTable.getClassName(), name, descriptor, code.length);
      }
      symbolTable.addConstantUtf8(Constants.CODE);
      // The Code attribute has 6 header bytes, plus 2, 2, 4 and 2 bytes respectively for max_stack,
      // max_locals, code_length and attributes_count, plus the bytecode and the exception table.
      size += 16 + code.length + Handler.getExceptionTableSize(firstHandler);
      if (stackMapTableEntries != null) {
        boolean useStackMapTable = symbolTable.getMajorVersion() >= Opcodes.V1_6;
        symbolTable.addConstantUtf8(useStackMapTable ? Constants.STACK_MAP_TABLE : "StackMap");
        // 6 header bytes and 2 bytes for number_of_entries.
        size += 8 + stackMapTableEntries.length;
      }
      if (lineNumberTable != null) {
        symbolTable.addConstantUtf8(Constants.LINE_NUMBER_TABLE);
        // 6 header bytes and 2 bytes for line_number_table_length.
        size += 8 + lineNumberTable.length;
      }
      if (localVariableTable != null) {
        symbolTable.addConstantUtf8(Constants.LOCAL_VARIABLE_TABLE);
        // 6 header bytes and 2 bytes for local_variable_table_length.
        size += 8 + localVariableTable.length;
      }
      if (localVariableTypeTable != null) {
        symbolTable.addConstantUtf8(Constants.LOCAL_VARIABLE_TYPE_TABLE);
        // 6 header bytes and 2 bytes for local_variable_type_table_length.
        size += 8 + localVariableTypeTable.length;
      }
      if (lastCodeRuntimeVisibleTypeAnnotation != null) {
        size +=
            lastCodeRuntimeVisibleTypeAnnotation.computeAnnotationsSize(
                Constants.RUNTIME_VISIBLE_TYPE_ANNOTATIONS);
      }
      if (lastCodeRuntimeInvisibleTypeAnnotation != null) {
        size +=
            lastCodeRuntimeInvisibleTypeAnnotation.computeAnnotationsSize(
                Constants.RUNTIME_INVISIBLE_TYPE_ANNOTATIONS);
      }
      if (firstCodeAttribute != null) {
        size +=
            firstCodeAttribute.computeAttributesSize(
                symbolTable, code.data, code.length, maxStack, maxLocals);
      }
    }
    if (numberOfExceptions > 0) {
      symbolTable.addConstantUtf8(Constants.EXCEPTIONS);
      size += 8 + 2 * numberOfExceptions;
    }
    size += Attribute.computeAttributesSize(symbolTable, accessFlags, signatureIndex);
    size +=
        AnnotationWriter.computeAnnotationsSize(
            lastRuntimeVisibleAnnotation,
            lastRuntimeInvisibleAnnotation,
            lastRuntimeVisibleTypeAnnotation,
            lastRuntimeInvisibleTypeAnnotation);
    if (lastRuntimeVisibleParameterAnnotations != null) {
      size +=
          AnnotationWriter.computeParameterAnnotationsSize(
              Constants.RUNTIME_VISIBLE_PARAMETER_ANNOTATIONS,
              lastRuntimeVisibleParameterAnnotations,
              visibleAnnotableParameterCount == 0
                  ? lastRuntimeVisibleParameterAnnotations.length
                  : visibleAnnotableParameterCount);
    }
    if (lastRuntimeInvisibleParameterAnnotations != null) {
      size +=
          AnnotationWriter.computeParameterAnnotationsSize(
              Constants.RUNTIME_INVISIBLE_PARAMETER_ANNOTATIONS,
              lastRuntimeInvisibleParameterAnnotations,
              invisibleAnnotableParameterCount == 0
                  ? lastRuntimeInvisibleParameterAnnotations.length
                  : invisibleAnnotableParameterCount);
    }
    if (defaultValue != null) {
      symbolTable.addConstantUtf8(Constants.ANNOTATION_DEFAULT);
      size += 6 + defaultValue.length;
    }
    if (parameters != null) {
      symbolTable.addConstantUtf8(Constants.METHOD_PARAMETERS);
      // 6 header bytes and 1 byte for parameters_count.
      size += 7 + parameters.length;
    }
    if (firstAttribute != null) {
      size += firstAttribute.computeAttributesSize(symbolTable);
    }
    return size;
  }

  /**
   * Puts the content of the method_info JVMS structure generated by this MethodWriter into the
   * given ByteVector.
   *
   * @param output where the method_info structure must be put.
   */
  void putMethodInfo(final ByteVector output) {
    boolean useSyntheticAttribute = symbolTable.getMajorVersion() < Opcodes.V1_5;
    int mask = useSyntheticAttribute ? Opcodes.ACC_SYNTHETIC : 0;
    output.putShort(accessFlags & ~mask).putShort(nameIndex).putShort(descriptorIndex);
    // If this method_info must be copied from an existing one, copy it now and return early.
    if (sourceOffset != 0) {
      output.putByteArray(symbolTable.getSource().classFileBuffer, sourceOffset, sourceLength);
      return;
    }
    // For ease of reference, we use here the same attribute order as in Section 4.7 of the JVMS.
    int attributeCount = 0;
    if (code.length > 0) {
      ++attributeCount;
    }
    if (numberOfExceptions > 0) {
      ++attributeCount;
    }
    if ((accessFlags & Opcodes.ACC_SYNTHETIC) != 0 && useSyntheticAttribute) {
      ++attributeCount;
    }
    if (signatureIndex != 0) {
      ++attributeCount;
    }
    if ((accessFlags & Opcodes.ACC_DEPRECATED) != 0) {
      ++attributeCount;
    }
    if (lastRuntimeVisibleAnnotation != null) {
      ++attributeCount;
    }
    if (lastRuntimeInvisibleAnnotation != null) {
      ++attributeCount;
    }
    if (lastRuntimeVisibleParameterAnnotations != null) {
      ++attributeCount;
    }
    if (lastRuntimeInvisibleParameterAnnotations != null) {
      ++attributeCount;
    }
    if (lastRuntimeVisibleTypeAnnotation != null) {
      ++attributeCount;
    }
    if (lastRuntimeInvisibleTypeAnnotation != null) {
      ++attributeCount;
    }
    if (defaultValue != null) {
      ++attributeCount;
    }
    if (parameters != null) {
      ++attributeCount;
    }
    if (firstAttribute != null) {
      attributeCount += firstAttribute.getAttributeCount();
    }
    // For ease of reference, we use here the same attribute order as in Section 4.7 of the JVMS.
    output.putShort(attributeCount);
    if (code.length > 0) {
      // 2, 2, 4 and 2 bytes respectively for max_stack, max_locals, code_length and
      // attributes_count, plus the bytecode and the exception table.
      int size = 10 + code.length + Handler.getExceptionTableSize(firstHandler);
      int codeAttributeCount = 0;
      if (stackMapTableEntries != null) {
        // 6 header bytes and 2 bytes for number_of_entries.
        size += 8 + stackMapTableEntries.length;
        ++codeAttributeCount;
      }
      if (lineNumberTable != null) {
        // 6 header bytes and 2 bytes for line_number_table_length.
        size += 8 + lineNumberTable.length;
        ++codeAttributeCount;
      }
      if (localVariableTable != null) {
        // 6 header bytes and 2 bytes for local_variable_table_length.
        size += 8 + localVariableTable.length;
        ++codeAttributeCount;
      }
      if (localVariableTypeTable != null) {
        // 6 header bytes and 2 bytes for local_variable_type_table_length.
        size += 8 + localVariableTypeTable.length;
        ++codeAttributeCount;
      }
      if (lastCodeRuntimeVisibleTypeAnnotation != null) {
        size +=
            lastCodeRuntimeVisibleTypeAnnotation.computeAnnotationsSize(
                Constants.RUNTIME_VISIBLE_TYPE_ANNOTATIONS);
        ++codeAttributeCount;
      }
      if (lastCodeRuntimeInvisibleTypeAnnotation != null) {
        size +=
            lastCodeRuntimeInvisibleTypeAnnotation.computeAnnotationsSize(
                Constants.RUNTIME_INVISIBLE_TYPE_ANNOTATIONS);
        ++codeAttributeCount;
      }
      if (firstCodeAttribute != null) {
        size +=
            firstCodeAttribute.computeAttributesSize(
                symbolTable, code.data, code.length, maxStack, maxLocals);
        codeAttributeCount += firstCodeAttribute.getAttributeCount();
      }
      output
          .putShort(symbolTable.addConstantUtf8(Constants.CODE))
          .putInt(size)
          .putShort(maxStack)
          .putShort(maxLocals)
          .putInt(code.length)
          .putByteArray(code.data, 0, code.length);
      Handler.putExceptionTable(firstHandler, output);
      output.putShort(codeAttributeCount);
      if (stackMapTableEntries != null) {
        boolean useStackMapTable = symbolTable.getMajorVersion() >= Opcodes.V1_6;
        output
            .putShort(
                symbolTable.addConstantUtf8(
                    useStackMapTable ? Constants.STACK_MAP_TABLE : "StackMap"))
            .putInt(2 + stackMapTableEntries.length)
            .putShort(stackMapTableNumberOfEntries)
            .putByteArray(stackMapTableEntries.data, 0, stackMapTableEntries.length);
      }
      if (lineNumberTable != null) {
        output
            .putShort(symbolTable.addConstantUtf8(Constants.LINE_NUMBER_TABLE))
            .putInt(2 + lineNumberTable.length)
            .putShort(lineNumberTableLength)
            .putByteArray(lineNumberTable.data, 0, lineNumberTable.length);
      }
      if (localVariableTable != null) {
        output
            .putShort(symbolTable.addConstantUtf8(Constants.LOCAL_VARIABLE_TABLE))
            .putInt(2 + localVariableTable.length)
            .putShort(localVariableTableLength)
            .putByteArray(localVariableTable.data, 0, localVariableTable.length);
      }
      if (localVariableTypeTable != null) {
        output
            .putShort(symbolTable.addConstantUtf8(Constants.LOCAL_VARIABLE_TYPE_TABLE))
            .putInt(2 + localVariableTypeTable.length)
            .putShort(localVariableTypeTableLength)
            .putByteArray(localVariableTypeTable.data, 0, localVariableTypeTable.length);
      }
      if (lastCodeRuntimeVisibleTypeAnnotation != null) {
        lastCodeRuntimeVisibleTypeAnnotation.putAnnotations(
            symbolTable.addConstantUtf8(Constants.RUNTIME_VISIBLE_TYPE_ANNOTATIONS), output);
      }
      if (lastCodeRuntimeInvisibleTypeAnnotation != null) {
        lastCodeRuntimeInvisibleTypeAnnotation.putAnnotations(
            symbolTable.addConstantUtf8(Constants.RUNTIME_INVISIBLE_TYPE_ANNOTATIONS), output);
      }
      if (firstCodeAttribute != null) {
        firstCodeAttribute.putAttributes(
            symbolTable, code.data, code.length, maxStack, maxLocals, output);
      }
    }
    if (numberOfExceptions > 0) {
      output
          .putShort(symbolTable.addConstantUtf8(Constants.EXCEPTIONS))
          .putInt(2 + 2 * numberOfExceptions)
          .putShort(numberOfExceptions);
      for (int exceptionIndex : exceptionIndexTable) {
        output.putShort(exceptionIndex);
      }
    }
    Attribute.putAttributes(symbolTable, accessFlags, signatureIndex, output);
    AnnotationWriter.putAnnotations(
        symbolTable,
        lastRuntimeVisibleAnnotation,
        lastRuntimeInvisibleAnnotation,
        lastRuntimeVisibleTypeAnnotation,
        lastRuntimeInvisibleTypeAnnotation,
        output);
    if (lastRuntimeVisibleParameterAnnotations != null) {
      AnnotationWriter.putParameterAnnotations(
          symbolTable.addConstantUtf8(Constants.RUNTIME_VISIBLE_PARAMETER_ANNOTATIONS),
          lastRuntimeVisibleParameterAnnotations,
          visibleAnnotableParameterCount == 0
              ? lastRuntimeVisibleParameterAnnotations.length
              : visibleAnnotableParameterCount,
          output);
    }
    if (lastRuntimeInvisibleParameterAnnotations != null) {
      AnnotationWriter.putParameterAnnotations(
          symbolTable.addConstantUtf8(Constants.RUNTIME_INVISIBLE_PARAMETER_ANNOTATIONS),
          lastRuntimeInvisibleParameterAnnotations,
          invisibleAnnotableParameterCount == 0
              ? lastRuntimeInvisibleParameterAnnotations.length
              : invisibleAnnotableParameterCount,
          output);
    }
    if (defaultValue != null) {
      output
          .putShort(symbolTable.addConstantUtf8(Constants.ANNOTATION_DEFAULT))
          .putInt(defaultValue.length)
          .putByteArray(defaultValue.data, 0, defaultValue.length);
    }
    if (parameters != null) {
      output
          .putShort(symbolTable.addConstantUtf8(Constants.METHOD_PARAMETERS))
          .putInt(1 + parameters.length)
          .putByte(parametersCount)
          .putByteArray(parameters.data, 0, parameters.length);
    }
    if (firstAttribute != null) {
      firstAttribute.putAttributes(symbolTable, output);
    }
  }

  /**
   * Collects the attributes of this method into the given set of attribute prototypes.
   *
   * @param attributePrototypes a set of attribute prototypes.
   */
  final void collectAttributePrototypes(final Attribute.Set attributePrototypes) {
    attributePrototypes.addAttributes(firstAttribute);
    attributePrototypes.addAttributes(firstCodeAttribute);
  }
}
