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
 * The path to a type argument, wildcard bound, array element type, or static inner type within an
 * enclosing type.
 *
 * @author Eric Bruneton
 */
public class TypePath {

  /** A type path step that steps into the element type of an array type. See {@link #getStep}. */
  public static final int ARRAY_ELEMENT = 0;

  /** A type path step that steps into the nested type of a class type. See {@link #getStep}. */
  public static final int INNER_TYPE = 1;

  /** A type path step that steps into the bound of a wildcard type. See {@link #getStep}. */
  public static final int WILDCARD_BOUND = 2;

  /** A type path step that steps into a type argument of a generic type. See {@link #getStep}. */
  public static final int TYPE_ARGUMENT = 3;

  /**
   * The byte array where the 'type_path' structure - as defined in the Java Virtual Machine
   * Specification (JVMS) - corresponding to this TypePath is stored. The first byte of the
   * structure in this array is given by {@link #typePathOffset}.
   *
   * @see <a
   *     href="https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.20.2">JVMS
   *     4.7.20.2</a>
   */
  private final byte[] typePathContainer;

  /** The offset of the first byte of the type_path JVMS structure in {@link #typePathContainer}. */
  private final int typePathOffset;

  /**
   * Constructs a new TypePath.
   *
   * @param typePathContainer a byte array containing a type_path JVMS structure.
   * @param typePathOffset the offset of the first byte of the type_path structure in
   *     typePathContainer.
   */
  TypePath(final byte[] typePathContainer, final int typePathOffset) {
    this.typePathContainer = typePathContainer;
    this.typePathOffset = typePathOffset;
  }

  /**
   * Returns the length of this path, i.e. its number of steps.
   *
   * @return the length of this path.
   */
  public int getLength() {
    // path_length is stored in the first byte of a type_path.
    return typePathContainer[typePathOffset];
  }

  /**
   * Returns the value of the given step of this path.
   *
   * @param index an index between 0 and {@link #getLength()}, exclusive.
   * @return one of {@link #ARRAY_ELEMENT}, {@link #INNER_TYPE}, {@link #WILDCARD_BOUND}, or {@link
   *     #TYPE_ARGUMENT}.
   */
  public int getStep(final int index) {
    // Returns the type_path_kind of the path element of the given index.
    return typePathContainer[typePathOffset + 2 * index + 1];
  }

  /**
   * Returns the index of the type argument that the given step is stepping into. This method should
   * only be used for steps whose value is {@link #TYPE_ARGUMENT}.
   *
   * @param index an index between 0 and {@link #getLength()}, exclusive.
   * @return the index of the type argument that the given step is stepping into.
   */
  public int getStepArgument(final int index) {
    // Returns the type_argument_index of the path element of the given index.
    return typePathContainer[typePathOffset + 2 * index + 2];
  }

  /**
   * Converts a type path in string form, in the format used by {@link #toString()}, into a TypePath
   * object.
   *
   * @param typePath a type path in string form, in the format used by {@link #toString()}. May be
   *     <tt>null</tt> or empty.
   * @return the corresponding TypePath object, or <tt>null</tt> if the path is empty.
   */
  public static TypePath fromString(final String typePath) {
    if (typePath == null || typePath.length() == 0) {
      return null;
    }
    int typePathLength = typePath.length();
    ByteVector output = new ByteVector(typePathLength);
    output.putByte(0);
    int typePathIndex = 0;
    while (typePathIndex < typePathLength) {
      char c = typePath.charAt(typePathIndex++);
      if (c == '[') {
        output.put11(ARRAY_ELEMENT, 0);
      } else if (c == '.') {
        output.put11(INNER_TYPE, 0);
      } else if (c == '*') {
        output.put11(WILDCARD_BOUND, 0);
      } else if (c >= '0' && c <= '9') {
        int typeArg = c - '0';
        while (typePathIndex < typePathLength) {
          c = typePath.charAt(typePathIndex++);
          if (c >= '0' && c <= '9') {
            typeArg = typeArg * 10 + c - '0';
          } else if (c == ';') {
            break;
          } else {
            throw new IllegalArgumentException();
          }
        }
        output.put11(TYPE_ARGUMENT, typeArg);
      } else {
        throw new IllegalArgumentException();
      }
    }
    output.data[0] = (byte) (output.length / 2);
    return new TypePath(output.data, 0);
  }

  /**
   * Returns a string representation of this type path. {@link #ARRAY_ELEMENT} steps are represented
   * with '[', {@link #INNER_TYPE} steps with '.', {@link #WILDCARD_BOUND} steps with '*' and {@link
   * #TYPE_ARGUMENT} steps with their type argument index in decimal form followed by ';'.
   */
  @Override
  public String toString() {
    int length = getLength();
    StringBuilder result = new StringBuilder(length * 2);
    for (int i = 0; i < length; ++i) {
      switch (getStep(i)) {
        case ARRAY_ELEMENT:
          result.append('[');
          break;
        case INNER_TYPE:
          result.append('.');
          break;
        case WILDCARD_BOUND:
          result.append('*');
          break;
        case TYPE_ARGUMENT:
          result.append(getStepArgument(i)).append(';');
          break;
        default:
          throw new AssertionError();
      }
    }
    return result.toString();
  }

  /**
   * Puts the type_path JVMS structure corresponding to the given TypePath into the given
   * ByteVector.
   *
   * @param typePath a TypePath instance, or <tt>null</tt> for empty paths.
   * @param output where the type path must be put.
   */
  static void put(final TypePath typePath, final ByteVector output) {
    if (typePath == null) {
      output.putByte(0);
    } else {
      int length = typePath.typePathContainer[typePath.typePathOffset] * 2 + 1;
      output.putByteArray(typePath.typePathContainer, typePath.typePathOffset, length);
    }
  }
}
