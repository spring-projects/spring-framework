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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * A Java field or method type. This class can be used to make it easier to manipulate type and
 * method descriptors.
 *
 * @author Eric Bruneton
 * @author Chris Nokleberg
 */
public class Type {

  /** The sort of the <tt>void</tt> type. See {@link #getSort}. */
  public static final int VOID = 0;

  /** The sort of the <tt>boolean</tt> type. See {@link #getSort}. */
  public static final int BOOLEAN = 1;

  /** The sort of the <tt>char</tt> type. See {@link #getSort}. */
  public static final int CHAR = 2;

  /** The sort of the <tt>byte</tt> type. See {@link #getSort}. */
  public static final int BYTE = 3;

  /** The sort of the <tt>short</tt> type. See {@link #getSort}. */
  public static final int SHORT = 4;

  /** The sort of the <tt>int</tt> type. See {@link #getSort}. */
  public static final int INT = 5;

  /** The sort of the <tt>float</tt> type. See {@link #getSort}. */
  public static final int FLOAT = 6;

  /** The sort of the <tt>long</tt> type. See {@link #getSort}. */
  public static final int LONG = 7;

  /** The sort of the <tt>double</tt> type. See {@link #getSort}. */
  public static final int DOUBLE = 8;

  /** The sort of array reference types. See {@link #getSort}. */
  public static final int ARRAY = 9;

  /** The sort of object reference types. See {@link #getSort}. */
  public static final int OBJECT = 10;

  /** The sort of method types. See {@link #getSort}. */
  public static final int METHOD = 11;

  /** The (private) sort of object reference types represented with an internal name. */
  private static final int INTERNAL = 12;

  /** The descriptors of the primitive types. */
  private static final String PRIMITIVE_DESCRIPTORS = "VZCBSIFJD";

  /** The <tt>void</tt> type. */
  public static final Type VOID_TYPE = new Type(VOID, PRIMITIVE_DESCRIPTORS, VOID, VOID + 1);

  /** The <tt>boolean</tt> type. */
  public static final Type BOOLEAN_TYPE =
      new Type(BOOLEAN, PRIMITIVE_DESCRIPTORS, BOOLEAN, BOOLEAN + 1);

  /** The <tt>char</tt> type. */
  public static final Type CHAR_TYPE = new Type(CHAR, PRIMITIVE_DESCRIPTORS, CHAR, CHAR + 1);

  /** The <tt>byte</tt> type. */
  public static final Type BYTE_TYPE = new Type(BYTE, PRIMITIVE_DESCRIPTORS, BYTE, BYTE + 1);

  /** The <tt>short</tt> type. */
  public static final Type SHORT_TYPE = new Type(SHORT, PRIMITIVE_DESCRIPTORS, SHORT, SHORT + 1);

  /** The <tt>int</tt> type. */
  public static final Type INT_TYPE = new Type(INT, PRIMITIVE_DESCRIPTORS, INT, INT + 1);

  /** The <tt>float</tt> type. */
  public static final Type FLOAT_TYPE = new Type(FLOAT, PRIMITIVE_DESCRIPTORS, FLOAT, FLOAT + 1);

  /** The <tt>long</tt> type. */
  public static final Type LONG_TYPE = new Type(LONG, PRIMITIVE_DESCRIPTORS, LONG, LONG + 1);

  /** The <tt>double</tt> type. */
  public static final Type DOUBLE_TYPE =
      new Type(DOUBLE, PRIMITIVE_DESCRIPTORS, DOUBLE, DOUBLE + 1);

  // -----------------------------------------------------------------------------------------------
  // Fields
  // -----------------------------------------------------------------------------------------------

  /**
   * The sort of this type. Either {@link #VOID}, {@link #BOOLEAN}, {@link #CHAR}, {@link #BYTE},
   * {@link #SHORT}, {@link #INT}, {@link #FLOAT}, {@link #LONG}, {@link #DOUBLE}, {@link #ARRAY},
   * {@link #OBJECT}, {@link #METHOD} or {@link #INTERNAL}.
   */
  private final int sort;

  /**
   * A buffer containing the value of this field or method type. This value is an internal name for
   * {@link #OBJECT} and {@link #INTERNAL} types, and a field or method descriptor in the other
   * cases.
   *
   * <p>For {@link #OBJECT} types, this field also contains the descriptor: the characters in
   * [{@link #valueBegin},{@link #valueEnd}) contain the internal name, and those in [{@link
   * #valueBegin} - 1, {@link #valueEnd} + 1) contain the descriptor.
   */
  private final String valueBuffer;

  /**
   * The beginning index, inclusive, of the value of this Java field or method type in {@link
   * #valueBuffer}. This value is an internal name for {@link #OBJECT} and {@link #INTERNAL} types,
   * and a field or method descriptor in the other cases.
   */
  private final int valueBegin;

  /**
   * The end index, exclusive, of the value of this Java field or method type in {@link
   * #valueBuffer}. This value is an internal name for {@link #OBJECT} and {@link #INTERNAL} types,
   * and a field or method descriptor in the other cases.
   */
  private final int valueEnd;

  // -----------------------------------------------------------------------------------------------
  // Constructors
  // -----------------------------------------------------------------------------------------------

  /**
   * Constructs a reference type.
   *
   * @param sort the sort of this type, see {@link #sort}.
   * @param valueBuffer a buffer containing the value of this field or method type.
   * @param valueBegin the beginning index, inclusive, of the value of this field or method type in
   *     valueBuffer.
   * @param valueEnd tne end index, exclusive, of the value of this field or method type in
   *     valueBuffer.
   */
  private Type(final int sort, final String valueBuffer, final int valueBegin, final int valueEnd) {
    this.sort = sort;
    this.valueBuffer = valueBuffer;
    this.valueBegin = valueBegin;
    this.valueEnd = valueEnd;
  }

  /**
   * Returns the {@link Type} corresponding to the given type descriptor.
   *
   * @param typeDescriptor a field or method type descriptor.
   * @return the {@link Type} corresponding to the given type descriptor.
   */
  public static Type getType(final String typeDescriptor) {
    return getType(typeDescriptor, 0, typeDescriptor.length());
  }

  /**
   * Returns the {@link Type} corresponding to the given internal name.
   *
   * @param internalName an internal name.
   * @return the {@link Type} corresponding to the given internal name.
   */
  public static Type getObjectType(final String internalName) {
    return new Type(
        internalName.charAt(0) == '[' ? ARRAY : INTERNAL, internalName, 0, internalName.length());
  }

  /**
   * Returns the {@link Type} corresponding to the given method descriptor. Equivalent to <code>
   * Type.getType(methodDescriptor)</code>.
   *
   * @param methodDescriptor a method descriptor.
   * @return the {@link Type} corresponding to the given method descriptor.
   */
  public static Type getMethodType(final String methodDescriptor) {
    return new Type(METHOD, methodDescriptor, 0, methodDescriptor.length());
  }

  /**
   * Returns the method {@link Type} corresponding to the given argument and return types.
   *
   * @param returnType the return type of the method.
   * @param argumentTypes the argument types of the method.
   * @return the method {@link Type} corresponding to the given argument and return types.
   */
  public static Type getMethodType(final Type returnType, final Type... argumentTypes) {
    return getType(getMethodDescriptor(returnType, argumentTypes));
  }

  /**
   * Returns the {@link Type} corresponding to the given class.
   *
   * @param clazz a class.
   * @return the {@link Type} corresponding to the given class.
   */
  public static Type getType(final Class<?> clazz) {
    if (clazz.isPrimitive()) {
      if (clazz == Integer.TYPE) {
        return INT_TYPE;
      } else if (clazz == Void.TYPE) {
        return VOID_TYPE;
      } else if (clazz == Boolean.TYPE) {
        return BOOLEAN_TYPE;
      } else if (clazz == Byte.TYPE) {
        return BYTE_TYPE;
      } else if (clazz == Character.TYPE) {
        return CHAR_TYPE;
      } else if (clazz == Short.TYPE) {
        return SHORT_TYPE;
      } else if (clazz == Double.TYPE) {
        return DOUBLE_TYPE;
      } else if (clazz == Float.TYPE) {
        return FLOAT_TYPE;
      } else if (clazz == Long.TYPE) {
        return LONG_TYPE;
      } else {
        throw new AssertionError();
      }
    } else {
      return getType(getDescriptor(clazz));
    }
  }

  /**
   * Returns the method {@link Type} corresponding to the given constructor.
   *
   * @param constructor a {@link Constructor} object.
   * @return the method {@link Type} corresponding to the given constructor.
   */
  public static Type getType(final Constructor<?> constructor) {
    return getType(getConstructorDescriptor(constructor));
  }

  /**
   * Returns the method {@link Type} corresponding to the given method.
   *
   * @param method a {@link Method} object.
   * @return the method {@link Type} corresponding to the given method.
   */
  public static Type getType(final Method method) {
    return getType(getMethodDescriptor(method));
  }

  /**
   * Returns the {@link Type} values corresponding to the argument types of the given method
   * descriptor.
   *
   * @param methodDescriptor a method descriptor.
   * @return the {@link Type} values corresponding to the argument types of the given method
   *     descriptor.
   */
  public static Type[] getArgumentTypes(final String methodDescriptor) {
    // First step: compute the number of argument types in methodDescriptor.
    int numArgumentTypes = 0;
    // Skip the first character, which is always a '('.
    int currentOffset = 1;
    // Parse the argument types, one at a each loop iteration.
    while (methodDescriptor.charAt(currentOffset) != ')') {
      while (methodDescriptor.charAt(currentOffset) == '[') {
        currentOffset++;
      }
      if (methodDescriptor.charAt(currentOffset++) == 'L') {
        while (methodDescriptor.charAt(currentOffset++) != ';') {
          // Skip the argument descriptor content.
        }
      }
      ++numArgumentTypes;
    }

    // Second step: create a Type instance for each argument type.
    Type[] argumentTypes = new Type[numArgumentTypes];
    // Skip the first character, which is always a '('.
    currentOffset = 1;
    // Parse and create the argument types, one at each loop iteration.
    int currentArgumentTypeIndex = 0;
    while (methodDescriptor.charAt(currentOffset) != ')') {
      final int currentArgumentTypeOffset = currentOffset;
      while (methodDescriptor.charAt(currentOffset) == '[') {
        currentOffset++;
      }
      if (methodDescriptor.charAt(currentOffset++) == 'L') {
        while (methodDescriptor.charAt(currentOffset++) != ';') {
          // Skip the argument descriptor content.
        }
      }
      argumentTypes[currentArgumentTypeIndex++] =
          getType(methodDescriptor, currentArgumentTypeOffset, currentOffset);
    }
    return argumentTypes;
  }

  /**
   * Returns the {@link Type} values corresponding to the argument types of the given method.
   *
   * @param method a method.
   * @return the {@link Type} values corresponding to the argument types of the given method.
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
   * Returns the {@link Type} corresponding to the return type of the given method descriptor.
   *
   * @param methodDescriptor a method descriptor.
   * @return the {@link Type} corresponding to the return type of the given method descriptor.
   */
  public static Type getReturnType(final String methodDescriptor) {
    // Skip the first character, which is always a '('.
    int currentOffset = 1;
    // Skip the argument types, one at a each loop iteration.
    while (methodDescriptor.charAt(currentOffset) != ')') {
      while (methodDescriptor.charAt(currentOffset) == '[') {
        currentOffset++;
      }
      if (methodDescriptor.charAt(currentOffset++) == 'L') {
        while (methodDescriptor.charAt(currentOffset++) != ';') {
          // Skip the argument descriptor content.
        }
      }
    }
    return getType(methodDescriptor, currentOffset + 1, methodDescriptor.length());
  }

  /**
   * Returns the {@link Type} corresponding to the return type of the given method.
   *
   * @param method a method.
   * @return the {@link Type} corresponding to the return type of the given method.
   */
  public static Type getReturnType(final Method method) {
    return getType(method.getReturnType());
  }

  /**
   * Computes the size of the arguments and of the return value of a method.
   *
   * @param methodDescriptor a method descriptor.
   * @return the size of the arguments of the method (plus one for the implicit this argument),
   *     argumentsSize, and the size of its return value, returnSize, packed into a single int i =
   *     <tt>(argumentsSize &lt;&lt; 2) | returnSize</tt> (argumentsSize is therefore equal to <tt>i
   *     &gt;&gt; 2</tt>, and returnSize to <tt>i &amp; 0x03</tt>).
   */
  public static int getArgumentsAndReturnSizes(final String methodDescriptor) {
    int argumentsSize = 1;
    // Skip the first character, which is always a '('.
    int currentOffset = 1;
    int currentChar = methodDescriptor.charAt(currentOffset);
    // Parse the argument types and compute their size, one at a each loop iteration.
    while (currentChar != ')') {
      if (currentChar == 'J' || currentChar == 'D') {
        currentOffset++;
        argumentsSize += 2;
      } else {
        while (methodDescriptor.charAt(currentOffset) == '[') {
          currentOffset++;
        }
        if (methodDescriptor.charAt(currentOffset++) == 'L') {
          while (methodDescriptor.charAt(currentOffset++) != ';') {
            // Skip the argument descriptor content.
          }
        }
        argumentsSize += 1;
      }
      currentChar = methodDescriptor.charAt(currentOffset);
    }
    currentChar = methodDescriptor.charAt(currentOffset + 1);
    if (currentChar == 'V') {
      return argumentsSize << 2;
    } else {
      int returnSize = (currentChar == 'J' || currentChar == 'D') ? 2 : 1;
      return argumentsSize << 2 | returnSize;
    }
  }

  /**
   * Returns the {@link Type} corresponding to the given field or method descriptor.
   *
   * @param descriptorBuffer a buffer containing the field or method descriptor.
   * @param descriptorBegin the beginning index, inclusive, of the field or method descriptor in
   *     descriptorBuffer.
   * @param descriptorEnd the end index, exclusive, of the field or method descriptor in
   *     descriptorBuffer.
   * @return the {@link Type} corresponding to the given type descriptor.
   */
  private static Type getType(
      final String descriptorBuffer, final int descriptorBegin, final int descriptorEnd) {
    switch (descriptorBuffer.charAt(descriptorBegin)) {
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
        return new Type(ARRAY, descriptorBuffer, descriptorBegin, descriptorEnd);
      case 'L':
        return new Type(OBJECT, descriptorBuffer, descriptorBegin + 1, descriptorEnd - 1);
      case '(':
        return new Type(METHOD, descriptorBuffer, descriptorBegin, descriptorEnd);
      default:
        throw new IllegalArgumentException();
    }
  }

  // -----------------------------------------------------------------------------------------------
  // Accessors
  // -----------------------------------------------------------------------------------------------

  /**
   * Returns the sort of this type.
   *
   * @return {@link #VOID}, {@link #BOOLEAN}, {@link #CHAR}, {@link #BYTE}, {@link #SHORT}, {@link
   *     #INT}, {@link #FLOAT}, {@link #LONG}, {@link #DOUBLE}, {@link #ARRAY}, {@link #OBJECT} or
   *     {@link #METHOD}.
   */
  public int getSort() {
    return sort == INTERNAL ? OBJECT : sort;
  }

  /**
   * Returns the number of dimensions of this array type. This method should only be used for an
   * array type.
   *
   * @return the number of dimensions of this array type.
   */
  public int getDimensions() {
    int numDimensions = 1;
    while (valueBuffer.charAt(valueBegin + numDimensions) == '[') {
      numDimensions++;
    }
    return numDimensions;
  }

  /**
   * Returns the type of the elements of this array type. This method should only be used for an
   * array type.
   *
   * @return Returns the type of the elements of this array type.
   */
  public Type getElementType() {
    final int numDimensions = getDimensions();
    return getType(valueBuffer, valueBegin + numDimensions, valueEnd);
  }

  /**
   * Returns the binary name of the class corresponding to this type. This method must not be used
   * on method types.
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
        StringBuilder stringBuilder = new StringBuilder(getElementType().getClassName());
        for (int i = getDimensions(); i > 0; --i) {
          stringBuilder.append("[]");
        }
        return stringBuilder.toString();
      case OBJECT:
      case INTERNAL:
        return valueBuffer.substring(valueBegin, valueEnd).replace('/', '.');
      default:
        throw new AssertionError();
    }
  }

  /**
   * Returns the internal name of the class corresponding to this object or array type. The internal
   * name of a class is its fully qualified name (as returned by Class.getName(), where '.' are
   * replaced by '/'). This method should only be used for an object or array type.
   *
   * @return the internal name of the class corresponding to this object type.
   */
  public String getInternalName() {
    return valueBuffer.substring(valueBegin, valueEnd);
  }

  /**
   * Returns the argument types of methods of this type. This method should only be used for method
   * types.
   *
   * @return the argument types of methods of this type.
   */
  public Type[] getArgumentTypes() {
    return getArgumentTypes(getDescriptor());
  }

  /**
   * Returns the return type of methods of this type. This method should only be used for method
   * types.
   *
   * @return the return type of methods of this type.
   */
  public Type getReturnType() {
    return getReturnType(getDescriptor());
  }

  /**
   * Returns the size of the arguments and of the return value of methods of this type. This method
   * should only be used for method types.
   *
   * @return the size of the arguments of the method (plus one for the implicit this argument),
   *     argumentsSize, and the size of its return value, returnSize, packed into a single int i =
   *     <tt>(argumentsSize &lt;&lt; 2) | returnSize</tt> (argumentsSize is therefore equal to <tt>i
   *     &gt;&gt; 2</tt>, and returnSize to <tt>i &amp; 0x03</tt>).
   */
  public int getArgumentsAndReturnSizes() {
    return getArgumentsAndReturnSizes(getDescriptor());
  }

  // -----------------------------------------------------------------------------------------------
  // Conversion to type descriptors
  // -----------------------------------------------------------------------------------------------

  /**
   * Returns the descriptor corresponding to this type.
   *
   * @return the descriptor corresponding to this type.
   */
  public String getDescriptor() {
    if (sort == OBJECT) {
      return valueBuffer.substring(valueBegin - 1, valueEnd + 1);
    } else if (sort == INTERNAL) {
      StringBuilder stringBuilder = new StringBuilder();
      stringBuilder.append('L');
      stringBuilder.append(valueBuffer, valueBegin, valueEnd);
      stringBuilder.append(';');
      return stringBuilder.toString();
    } else {
      return valueBuffer.substring(valueBegin, valueEnd);
    }
  }

  /**
   * Returns the descriptor corresponding to the given argument and return types.
   *
   * @param returnType the return type of the method.
   * @param argumentTypes the argument types of the method.
   * @return the descriptor corresponding to the given argument and return types.
   */
  public static String getMethodDescriptor(final Type returnType, final Type... argumentTypes) {
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append('(');
    for (int i = 0; i < argumentTypes.length; ++i) {
      argumentTypes[i].appendDescriptor(stringBuilder);
    }
    stringBuilder.append(')');
    returnType.appendDescriptor(stringBuilder);
    return stringBuilder.toString();
  }

  /**
   * Appends the descriptor corresponding to this type to the given string buffer.
   *
   * @param stringBuilder the string builder to which the descriptor must be appended.
   */
  private void appendDescriptor(final StringBuilder stringBuilder) {
    if (sort == OBJECT) {
      stringBuilder.append(valueBuffer, valueBegin - 1, valueEnd + 1);
    } else if (sort == INTERNAL) {
      stringBuilder.append('L');
      stringBuilder.append(valueBuffer, valueBegin, valueEnd);
      stringBuilder.append(';');
    } else {
      stringBuilder.append(valueBuffer, valueBegin, valueEnd);
    }
  }

  // -----------------------------------------------------------------------------------------------
  // Direct conversion from classes to type descriptors,
  // without intermediate Type objects
  // -----------------------------------------------------------------------------------------------

  /**
   * Returns the internal name of the given class. The internal name of a class is its fully
   * qualified name, as returned by Class.getName(), where '.' are replaced by '/'.
   *
   * @param clazz an object or array class.
   * @return the internal name of the given class.
   */
  public static String getInternalName(final Class<?> clazz) {
    return clazz.getName().replace('.', '/');
  }

  /**
   * Returns the descriptor corresponding to the given class.
   *
   * @param clazz an object class, a primitive class or an array class.
   * @return the descriptor corresponding to the given class.
   */
  public static String getDescriptor(final Class<?> clazz) {
    StringBuilder stringBuilder = new StringBuilder();
    appendDescriptor(stringBuilder, clazz);
    return stringBuilder.toString();
  }

  /**
   * Returns the descriptor corresponding to the given constructor.
   *
   * @param constructor a {@link Constructor} object.
   * @return the descriptor of the given constructor.
   */
  public static String getConstructorDescriptor(final Constructor<?> constructor) {
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append('(');
    Class<?>[] parameters = constructor.getParameterTypes();
    for (int i = 0; i < parameters.length; ++i) {
      appendDescriptor(stringBuilder, parameters[i]);
    }
    return stringBuilder.append(")V").toString();
  }

  /**
   * Returns the descriptor corresponding to the given method.
   *
   * @param method a {@link Method} object.
   * @return the descriptor of the given method.
   */
  public static String getMethodDescriptor(final Method method) {
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append('(');
    Class<?>[] parameters = method.getParameterTypes();
    for (int i = 0; i < parameters.length; ++i) {
      appendDescriptor(stringBuilder, parameters[i]);
    }
    stringBuilder.append(')');
    appendDescriptor(stringBuilder, method.getReturnType());
    return stringBuilder.toString();
  }

  /**
   * Appends the descriptor of the given class to the given string builder.
   *
   * @param stringBuilder the string builder to which the descriptor must be appended.
   * @param clazz the class whose descriptor must be computed.
   */
  private static void appendDescriptor(final StringBuilder stringBuilder, final Class<?> clazz) {
    Class<?> currentClass = clazz;
    while (currentClass.isArray()) {
      stringBuilder.append('[');
      currentClass = currentClass.getComponentType();
    }
    if (currentClass.isPrimitive()) {
      char descriptor;
      if (currentClass == Integer.TYPE) {
        descriptor = 'I';
      } else if (currentClass == Void.TYPE) {
        descriptor = 'V';
      } else if (currentClass == Boolean.TYPE) {
        descriptor = 'Z';
      } else if (currentClass == Byte.TYPE) {
        descriptor = 'B';
      } else if (currentClass == Character.TYPE) {
        descriptor = 'C';
      } else if (currentClass == Short.TYPE) {
        descriptor = 'S';
      } else if (currentClass == Double.TYPE) {
        descriptor = 'D';
      } else if (currentClass == Float.TYPE) {
        descriptor = 'F';
      } else if (currentClass == Long.TYPE) {
        descriptor = 'J';
      } else {
        throw new AssertionError();
      }
      stringBuilder.append(descriptor);
    } else {
      stringBuilder.append('L');
      String name = currentClass.getName();
      int nameLength = name.length();
      for (int i = 0; i < nameLength; ++i) {
        char car = name.charAt(i);
        stringBuilder.append(car == '.' ? '/' : car);
      }
      stringBuilder.append(';');
    }
  }

  // -----------------------------------------------------------------------------------------------
  // Corresponding size and opcodes
  // -----------------------------------------------------------------------------------------------

  /**
   * Returns the size of values of this type. This method must not be used for method types.
   *
   * @return the size of values of this type, i.e., 2 for <tt>long</tt> and <tt>double</tt>, 0 for
   *     <tt>void</tt> and 1 otherwise.
   */
  public int getSize() {
    switch (sort) {
      case VOID:
        return 0;
      case BOOLEAN:
      case CHAR:
      case BYTE:
      case SHORT:
      case INT:
      case FLOAT:
      case ARRAY:
      case OBJECT:
      case INTERNAL:
        return 1;
      case LONG:
      case DOUBLE:
        return 2;
      default:
        throw new AssertionError();
    }
  }

  /**
   * Returns a JVM instruction opcode adapted to this {@link Type}. This method must not be used for
   * method types.
   *
   * @param opcode a JVM instruction opcode. This opcode must be one of ILOAD, ISTORE, IALOAD,
   *     IASTORE, IADD, ISUB, IMUL, IDIV, IREM, INEG, ISHL, ISHR, IUSHR, IAND, IOR, IXOR and
   *     IRETURN.
   * @return an opcode that is similar to the given opcode, but adapted to this {@link Type}. For
   *     example, if this type is <tt>float</tt> and <tt>opcode</tt> is IRETURN, this method returns
   *     FRETURN.
   */
  public int getOpcode(final int opcode) {
    if (opcode == Opcodes.IALOAD || opcode == Opcodes.IASTORE) {
      switch (sort) {
        case BOOLEAN:
        case BYTE:
          return opcode + (Opcodes.BALOAD - Opcodes.IALOAD);
        case CHAR:
          return opcode + (Opcodes.CALOAD - Opcodes.IALOAD);
        case SHORT:
          return opcode + (Opcodes.SALOAD - Opcodes.IALOAD);
        case INT:
          return opcode;
        case FLOAT:
          return opcode + (Opcodes.FALOAD - Opcodes.IALOAD);
        case LONG:
          return opcode + (Opcodes.LALOAD - Opcodes.IALOAD);
        case DOUBLE:
          return opcode + (Opcodes.DALOAD - Opcodes.IALOAD);
        case ARRAY:
        case OBJECT:
        case INTERNAL:
          return opcode + (Opcodes.AALOAD - Opcodes.IALOAD);
        case METHOD:
        case VOID:
          throw new UnsupportedOperationException();
        default:
          throw new AssertionError();
      }
    } else {
      switch (sort) {
        case VOID:
          if (opcode != Opcodes.IRETURN) {
            throw new UnsupportedOperationException();
          }
          return Opcodes.RETURN;
        case BOOLEAN:
        case BYTE:
        case CHAR:
        case SHORT:
        case INT:
          return opcode;
        case FLOAT:
          return opcode + (Opcodes.FRETURN - Opcodes.IRETURN);
        case LONG:
          return opcode + (Opcodes.LRETURN - Opcodes.IRETURN);
        case DOUBLE:
          return opcode + (Opcodes.DRETURN - Opcodes.IRETURN);
        case ARRAY:
        case OBJECT:
        case INTERNAL:
          if (opcode != Opcodes.ILOAD && opcode != Opcodes.ISTORE && opcode != Opcodes.IRETURN) {
            throw new UnsupportedOperationException();
          }
          return opcode + (Opcodes.ARETURN - Opcodes.IRETURN);
        case METHOD:
          throw new UnsupportedOperationException();
        default:
          throw new AssertionError();
      }
    }
  }

  // -----------------------------------------------------------------------------------------------
  // Equals, hashCode and toString
  // -----------------------------------------------------------------------------------------------

  /**
   * Tests if the given object is equal to this type.
   *
   * @param object the object to be compared to this type.
   * @return <tt>true</tt> if the given object is equal to this type.
   */
  @Override
  public boolean equals(final Object object) {
    if (this == object) {
      return true;
    }
    if (!(object instanceof Type)) {
      return false;
    }
    Type other = (Type) object;
    if ((sort == INTERNAL ? OBJECT : sort) != (other.sort == INTERNAL ? OBJECT : other.sort)) {
      return false;
    }
    int begin = valueBegin;
    int end = valueEnd;
    int otherBegin = other.valueBegin;
    int otherEnd = other.valueEnd;
    // Compare the values.
    if (end - begin != otherEnd - otherBegin) {
      return false;
    }
    for (int i = begin, j = otherBegin; i < end; i++, j++) {
      if (valueBuffer.charAt(i) != other.valueBuffer.charAt(j)) {
        return false;
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
    int hashCode = 13 * (sort == INTERNAL ? OBJECT : sort);
    if (sort >= ARRAY) {
      for (int i = valueBegin, end = valueEnd; i < end; i++) {
        hashCode = 17 * (hashCode + valueBuffer.charAt(i));
      }
    }
    return hashCode;
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
