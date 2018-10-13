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
 * A reference to a type appearing in a class, field or method declaration, or on an instruction.
 * Such a reference designates the part of the class where the referenced type is appearing (e.g. an
 * 'extends', 'implements' or 'throws' clause, a 'new' instruction, a 'catch' clause, a type cast, a
 * local variable declaration, etc).
 *
 * @author Eric Bruneton
 */
public class TypeReference {

  /**
   * The sort of type references that target a type parameter of a generic class. See {@link
   * #getSort}.
   */
  public static final int CLASS_TYPE_PARAMETER = 0x00;

  /**
   * The sort of type references that target a type parameter of a generic method. See {@link
   * #getSort}.
   */
  public static final int METHOD_TYPE_PARAMETER = 0x01;

  /**
   * The sort of type references that target the super class of a class or one of the interfaces it
   * implements. See {@link #getSort}.
   */
  public static final int CLASS_EXTENDS = 0x10;

  /**
   * The sort of type references that target a bound of a type parameter of a generic class. See
   * {@link #getSort}.
   */
  public static final int CLASS_TYPE_PARAMETER_BOUND = 0x11;

  /**
   * The sort of type references that target a bound of a type parameter of a generic method. See
   * {@link #getSort}.
   */
  public static final int METHOD_TYPE_PARAMETER_BOUND = 0x12;

  /** The sort of type references that target the type of a field. See {@link #getSort}. */
  public static final int FIELD = 0x13;

  /** The sort of type references that target the return type of a method. See {@link #getSort}. */
  public static final int METHOD_RETURN = 0x14;

  /**
   * The sort of type references that target the receiver type of a method. See {@link #getSort}.
   */
  public static final int METHOD_RECEIVER = 0x15;

  /**
   * The sort of type references that target the type of a formal parameter of a method. See {@link
   * #getSort}.
   */
  public static final int METHOD_FORMAL_PARAMETER = 0x16;

  /**
   * The sort of type references that target the type of an exception declared in the throws clause
   * of a method. See {@link #getSort}.
   */
  public static final int THROWS = 0x17;

  /**
   * The sort of type references that target the type of a local variable in a method. See {@link
   * #getSort}.
   */
  public static final int LOCAL_VARIABLE = 0x40;

  /**
   * The sort of type references that target the type of a resource variable in a method. See {@link
   * #getSort}.
   */
  public static final int RESOURCE_VARIABLE = 0x41;

  /**
   * The sort of type references that target the type of the exception of a 'catch' clause in a
   * method. See {@link #getSort}.
   */
  public static final int EXCEPTION_PARAMETER = 0x42;

  /**
   * The sort of type references that target the type declared in an 'instanceof' instruction. See
   * {@link #getSort}.
   */
  public static final int INSTANCEOF = 0x43;

  /**
   * The sort of type references that target the type of the object created by a 'new' instruction.
   * See {@link #getSort}.
   */
  public static final int NEW = 0x44;

  /**
   * The sort of type references that target the receiver type of a constructor reference. See
   * {@link #getSort}.
   */
  public static final int CONSTRUCTOR_REFERENCE = 0x45;

  /**
   * The sort of type references that target the receiver type of a method reference. See {@link
   * #getSort}.
   */
  public static final int METHOD_REFERENCE = 0x46;

  /**
   * The sort of type references that target the type declared in an explicit or implicit cast
   * instruction. See {@link #getSort}.
   */
  public static final int CAST = 0x47;

  /**
   * The sort of type references that target a type parameter of a generic constructor in a
   * constructor call. See {@link #getSort}.
   */
  public static final int CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT = 0x48;

  /**
   * The sort of type references that target a type parameter of a generic method in a method call.
   * See {@link #getSort}.
   */
  public static final int METHOD_INVOCATION_TYPE_ARGUMENT = 0x49;

  /**
   * The sort of type references that target a type parameter of a generic constructor in a
   * constructor reference. See {@link #getSort}.
   */
  public static final int CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT = 0x4A;

  /**
   * The sort of type references that target a type parameter of a generic method in a method
   * reference. See {@link #getSort}.
   */
  public static final int METHOD_REFERENCE_TYPE_ARGUMENT = 0x4B;

  /**
   * The target_type and target_info structures - as defined in the Java Virtual Machine
   * Specification (JVMS) - corresponding to this type reference. target_type uses one byte, and all
   * the target_info union fields use up to 3 bytes (except localvar_target, handled with the
   * specific method {@link MethodVisitor#visitLocalVariableAnnotation}). Thus, both structures can
   * be stored in an int.
   *
   * <p>This int field stores target_type (called the TypeReference 'sort' in the public API of this
   * class) in its most significant byte, followed by the target_info fields. Depending on
   * target_type, 1, 2 or even 3 least significant bytes of this field are unused. target_info
   * fields which reference bytecode offsets are set to 0 (these offsets are ignored in ClassReader,
   * and recomputed in MethodWriter).
   *
   * @see <a href="https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.20">JVMS
   *     4.7.20</a>
   * @see <a
   *     href="https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.20.1">JVMS
   *     4.7.20.1</a>
   */
  private final int targetTypeAndInfo;

  /**
   * Constructs a new TypeReference.
   *
   * @param typeRef the int encoded value of the type reference, as received in a visit method
   *     related to type annotations, such as {@link ClassVisitor#visitTypeAnnotation}.
   */
  public TypeReference(final int typeRef) {
    this.targetTypeAndInfo = typeRef;
  }

  /**
   * Returns a type reference of the given sort.
   *
   * @param sort one of {@link #FIELD}, {@link #METHOD_RETURN}, {@link #METHOD_RECEIVER}, {@link
   *     #LOCAL_VARIABLE}, {@link #RESOURCE_VARIABLE}, {@link #INSTANCEOF}, {@link #NEW}, {@link
   *     #CONSTRUCTOR_REFERENCE}, or {@link #METHOD_REFERENCE}.
   * @return a type reference of the given sort.
   */
  public static TypeReference newTypeReference(final int sort) {
    return new TypeReference(sort << 24);
  }

  /**
   * Returns a reference to a type parameter of a generic class or method.
   *
   * @param sort one of {@link #CLASS_TYPE_PARAMETER} or {@link #METHOD_TYPE_PARAMETER}.
   * @param paramIndex the type parameter index.
   * @return a reference to the given generic class or method type parameter.
   */
  public static TypeReference newTypeParameterReference(final int sort, final int paramIndex) {
    return new TypeReference((sort << 24) | (paramIndex << 16));
  }

  /**
   * Returns a reference to a type parameter bound of a generic class or method.
   *
   * @param sort one of {@link #CLASS_TYPE_PARAMETER} or {@link #METHOD_TYPE_PARAMETER}.
   * @param paramIndex the type parameter index.
   * @param boundIndex the type bound index within the above type parameters.
   * @return a reference to the given generic class or method type parameter bound.
   */
  public static TypeReference newTypeParameterBoundReference(
      final int sort, final int paramIndex, final int boundIndex) {
    return new TypeReference((sort << 24) | (paramIndex << 16) | (boundIndex << 8));
  }

  /**
   * Returns a reference to the super class or to an interface of the 'implements' clause of a
   * class.
   *
   * @param itfIndex the index of an interface in the 'implements' clause of a class, or -1 to
   *     reference the super class of the class.
   * @return a reference to the given super type of a class.
   */
  public static TypeReference newSuperTypeReference(final int itfIndex) {
    return new TypeReference((CLASS_EXTENDS << 24) | ((itfIndex & 0xFFFF) << 8));
  }

  /**
   * Returns a reference to the type of a formal parameter of a method.
   *
   * @param paramIndex the formal parameter index.
   * @return a reference to the type of the given method formal parameter.
   */
  public static TypeReference newFormalParameterReference(final int paramIndex) {
    return new TypeReference((METHOD_FORMAL_PARAMETER << 24) | (paramIndex << 16));
  }

  /**
   * Returns a reference to the type of an exception, in a 'throws' clause of a method.
   *
   * @param exceptionIndex the index of an exception in a 'throws' clause of a method.
   * @return a reference to the type of the given exception.
   */
  public static TypeReference newExceptionReference(final int exceptionIndex) {
    return new TypeReference((THROWS << 24) | (exceptionIndex << 8));
  }

  /**
   * Returns a reference to the type of the exception declared in a 'catch' clause of a method.
   *
   * @param tryCatchBlockIndex the index of a try catch block (using the order in which they are
   *     visited with visitTryCatchBlock).
   * @return a reference to the type of the given exception.
   */
  public static TypeReference newTryCatchReference(final int tryCatchBlockIndex) {
    return new TypeReference((EXCEPTION_PARAMETER << 24) | (tryCatchBlockIndex << 8));
  }

  /**
   * Returns a reference to the type of a type argument in a constructor or method call or
   * reference.
   *
   * @param sort one of {@link #CAST}, {@link #CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT}, {@link
   *     #METHOD_INVOCATION_TYPE_ARGUMENT}, {@link #CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT}, or {@link
   *     #METHOD_REFERENCE_TYPE_ARGUMENT}.
   * @param argIndex the type argument index.
   * @return a reference to the type of the given type argument.
   */
  public static TypeReference newTypeArgumentReference(final int sort, final int argIndex) {
    return new TypeReference((sort << 24) | argIndex);
  }

  /**
   * Returns the sort of this type reference.
   *
   * @return one of {@link #CLASS_TYPE_PARAMETER}, {@link #METHOD_TYPE_PARAMETER}, {@link
   *     #CLASS_EXTENDS}, {@link #CLASS_TYPE_PARAMETER_BOUND}, {@link #METHOD_TYPE_PARAMETER_BOUND},
   *     {@link #FIELD}, {@link #METHOD_RETURN}, {@link #METHOD_RECEIVER}, {@link
   *     #METHOD_FORMAL_PARAMETER}, {@link #THROWS}, {@link #LOCAL_VARIABLE}, {@link
   *     #RESOURCE_VARIABLE}, {@link #EXCEPTION_PARAMETER}, {@link #INSTANCEOF}, {@link #NEW},
   *     {@link #CONSTRUCTOR_REFERENCE}, {@link #METHOD_REFERENCE}, {@link #CAST}, {@link
   *     #CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT}, {@link #METHOD_INVOCATION_TYPE_ARGUMENT}, {@link
   *     #CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT}, or {@link #METHOD_REFERENCE_TYPE_ARGUMENT}.
   */
  public int getSort() {
    return targetTypeAndInfo >>> 24;
  }

  /**
   * Returns the index of the type parameter referenced by this type reference. This method must
   * only be used for type references whose sort is {@link #CLASS_TYPE_PARAMETER}, {@link
   * #METHOD_TYPE_PARAMETER}, {@link #CLASS_TYPE_PARAMETER_BOUND} or {@link
   * #METHOD_TYPE_PARAMETER_BOUND}.
   *
   * @return a type parameter index.
   */
  public int getTypeParameterIndex() {
    return (targetTypeAndInfo & 0x00FF0000) >> 16;
  }

  /**
   * Returns the index of the type parameter bound, within the type parameter {@link
   * #getTypeParameterIndex}, referenced by this type reference. This method must only be used for
   * type references whose sort is {@link #CLASS_TYPE_PARAMETER_BOUND} or {@link
   * #METHOD_TYPE_PARAMETER_BOUND}.
   *
   * @return a type parameter bound index.
   */
  public int getTypeParameterBoundIndex() {
    return (targetTypeAndInfo & 0x0000FF00) >> 8;
  }

  /**
   * Returns the index of the "super type" of a class that is referenced by this type reference.
   * This method must only be used for type references whose sort is {@link #CLASS_EXTENDS}.
   *
   * @return the index of an interface in the 'implements' clause of a class, or -1 if this type
   *     reference references the type of the super class.
   */
  public int getSuperTypeIndex() {
    return (short) ((targetTypeAndInfo & 0x00FFFF00) >> 8);
  }

  /**
   * Returns the index of the formal parameter whose type is referenced by this type reference. This
   * method must only be used for type references whose sort is {@link #METHOD_FORMAL_PARAMETER}.
   *
   * @return a formal parameter index.
   */
  public int getFormalParameterIndex() {
    return (targetTypeAndInfo & 0x00FF0000) >> 16;
  }

  /**
   * Returns the index of the exception, in a 'throws' clause of a method, whose type is referenced
   * by this type reference. This method must only be used for type references whose sort is {@link
   * #THROWS}.
   *
   * @return the index of an exception in the 'throws' clause of a method.
   */
  public int getExceptionIndex() {
    return (targetTypeAndInfo & 0x00FFFF00) >> 8;
  }

  /**
   * Returns the index of the try catch block (using the order in which they are visited with
   * visitTryCatchBlock), whose 'catch' type is referenced by this type reference. This method must
   * only be used for type references whose sort is {@link #EXCEPTION_PARAMETER} .
   *
   * @return the index of an exception in the 'throws' clause of a method.
   */
  public int getTryCatchBlockIndex() {
    return (targetTypeAndInfo & 0x00FFFF00) >> 8;
  }

  /**
   * Returns the index of the type argument referenced by this type reference. This method must only
   * be used for type references whose sort is {@link #CAST}, {@link
   * #CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT}, {@link #METHOD_INVOCATION_TYPE_ARGUMENT}, {@link
   * #CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT}, or {@link #METHOD_REFERENCE_TYPE_ARGUMENT}.
   *
   * @return a type parameter index.
   */
  public int getTypeArgumentIndex() {
    return targetTypeAndInfo & 0xFF;
  }

  /**
   * Returns the int encoded value of this type reference, suitable for use in visit methods related
   * to type annotations, like visitTypeAnnotation.
   *
   * @return the int encoded value of this type reference.
   */
  public int getValue() {
    return targetTypeAndInfo;
  }

  /**
   * Puts the given target_type and target_info JVMS structures into the given ByteVector.
   *
   * @param targetTypeAndInfo a target_type and a target_info structures encoded as in {@link
   *     #targetTypeAndInfo}. LOCAL_VARIABLE and RESOURCE_VARIABLE target types are not supported.
   * @param output where the type reference must be put.
   */
  static void putTarget(final int targetTypeAndInfo, final ByteVector output) {
    switch (targetTypeAndInfo >>> 24) {
      case CLASS_TYPE_PARAMETER:
      case METHOD_TYPE_PARAMETER:
      case METHOD_FORMAL_PARAMETER:
        output.putShort(targetTypeAndInfo >>> 16);
        break;
      case FIELD:
      case METHOD_RETURN:
      case METHOD_RECEIVER:
        output.putByte(targetTypeAndInfo >>> 24);
        break;
      case CAST:
      case CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT:
      case METHOD_INVOCATION_TYPE_ARGUMENT:
      case CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT:
      case METHOD_REFERENCE_TYPE_ARGUMENT:
        output.putInt(targetTypeAndInfo);
        break;
      case CLASS_EXTENDS:
      case CLASS_TYPE_PARAMETER_BOUND:
      case METHOD_TYPE_PARAMETER_BOUND:
      case THROWS:
      case EXCEPTION_PARAMETER:
      case INSTANCEOF:
      case NEW:
      case CONSTRUCTOR_REFERENCE:
      case METHOD_REFERENCE:
        output.put12(targetTypeAndInfo >>> 24, (targetTypeAndInfo & 0xFFFF00) >> 8);
        break;
      default:
        throw new IllegalArgumentException();
    }
  }
}
