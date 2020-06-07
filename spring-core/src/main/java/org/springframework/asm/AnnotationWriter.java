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
 * An {@link AnnotationVisitor} that generates a corresponding 'annotation' or 'type_annotation'
 * structure, as defined in the Java Virtual Machine Specification (JVMS). AnnotationWriter
 * instances can be chained in a doubly linked list, from which Runtime[In]Visible[Type]Annotations
 * attributes can be generated with the {@link #putAnnotations} method. Similarly, arrays of such
 * lists can be used to generate Runtime[In]VisibleParameterAnnotations attributes.
 *
 * @see <a href="https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.16">JVMS
 *     4.7.16</a>
 * @see <a href="https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.20">JVMS
 *     4.7.20</a>
 * @author Eric Bruneton
 * @author Eugene Kuleshov
 */
final class AnnotationWriter extends AnnotationVisitor {

  /** Where the constants used in this AnnotationWriter must be stored. */
  private final SymbolTable symbolTable;

  /**
   * Whether values are named or not. AnnotationWriter instances used for annotation default and
   * annotation arrays use unnamed values (i.e. they generate an 'element_value' structure for each
   * value, instead of an element_name_index followed by an element_value).
   */
  private final boolean useNamedValues;

  /**
   * The 'annotation' or 'type_annotation' JVMS structure corresponding to the annotation values
   * visited so far. All the fields of these structures, except the last one - the
   * element_value_pairs array, must be set before this ByteVector is passed to the constructor
   * (num_element_value_pairs can be set to 0, it is reset to the correct value in {@link
   * #visitEnd()}). The element_value_pairs array is filled incrementally in the various visit()
   * methods.
   *
   * <p>Note: as an exception to the above rules, for AnnotationDefault attributes (which contain a
   * single element_value by definition), this ByteVector is initially empty when passed to the
   * constructor, and {@link #numElementValuePairsOffset} is set to -1.
   */
  private final ByteVector annotation;

  /**
   * The offset in {@link #annotation} where {@link #numElementValuePairs} must be stored (or -1 for
   * the case of AnnotationDefault attributes).
   */
  private final int numElementValuePairsOffset;

  /** The number of element value pairs visited so far. */
  private int numElementValuePairs;

  /**
   * The previous AnnotationWriter. This field is used to store the list of annotations of a
   * Runtime[In]Visible[Type]Annotations attribute. It is unused for nested or array annotations
   * (annotation values of annotation type), or for AnnotationDefault attributes.
   */
  private final AnnotationWriter previousAnnotation;

  /**
   * The next AnnotationWriter. This field is used to store the list of annotations of a
   * Runtime[In]Visible[Type]Annotations attribute. It is unused for nested or array annotations
   * (annotation values of annotation type), or for AnnotationDefault attributes.
   */
  private AnnotationWriter nextAnnotation;

  // -----------------------------------------------------------------------------------------------
  // Constructors and factories
  // -----------------------------------------------------------------------------------------------

  /**
   * Constructs a new {@link AnnotationWriter}.
   *
   * @param symbolTable where the constants used in this AnnotationWriter must be stored.
   * @param useNamedValues whether values are named or not. AnnotationDefault and annotation arrays
   *     use unnamed values.
   * @param annotation where the 'annotation' or 'type_annotation' JVMS structure corresponding to
   *     the visited content must be stored. This ByteVector must already contain all the fields of
   *     the structure except the last one (the element_value_pairs array).
   * @param previousAnnotation the previously visited annotation of the
   *     Runtime[In]Visible[Type]Annotations attribute to which this annotation belongs, or
   *     {@literal null} in other cases (e.g. nested or array annotations).
   */
  AnnotationWriter(
      final SymbolTable symbolTable,
      final boolean useNamedValues,
      final ByteVector annotation,
      final AnnotationWriter previousAnnotation) {
    super(/* latest api = */ Opcodes.ASM7);
    this.symbolTable = symbolTable;
    this.useNamedValues = useNamedValues;
    this.annotation = annotation;
    // By hypothesis, num_element_value_pairs is stored in the last unsigned short of 'annotation'.
    this.numElementValuePairsOffset = annotation.length == 0 ? -1 : annotation.length - 2;
    this.previousAnnotation = previousAnnotation;
    if (previousAnnotation != null) {
      previousAnnotation.nextAnnotation = this;
    }
  }

  /**
   * Creates a new {@link AnnotationWriter} using named values.
   *
   * @param symbolTable where the constants used in this AnnotationWriter must be stored.
   * @param descriptor the class descriptor of the annotation class.
   * @param previousAnnotation the previously visited annotation of the
   *     Runtime[In]Visible[Type]Annotations attribute to which this annotation belongs, or
   *     {@literal null} in other cases (e.g. nested or array annotations).
   * @return a new {@link AnnotationWriter} for the given annotation descriptor.
   */
  static AnnotationWriter create(
      final SymbolTable symbolTable,
      final String descriptor,
      final AnnotationWriter previousAnnotation) {
    // Create a ByteVector to hold an 'annotation' JVMS structure.
    // See https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.16.
    ByteVector annotation = new ByteVector();
    // Write type_index and reserve space for num_element_value_pairs.
    annotation.putShort(symbolTable.addConstantUtf8(descriptor)).putShort(0);
    return new AnnotationWriter(
        symbolTable, /* useNamedValues = */ true, annotation, previousAnnotation);
  }

  /**
   * Creates a new {@link AnnotationWriter} using named values.
   *
   * @param symbolTable where the constants used in this AnnotationWriter must be stored.
   * @param typeRef a reference to the annotated type. The sort of this type reference must be
   *     {@link TypeReference#CLASS_TYPE_PARAMETER}, {@link
   *     TypeReference#CLASS_TYPE_PARAMETER_BOUND} or {@link TypeReference#CLASS_EXTENDS}. See
   *     {@link TypeReference}.
   * @param typePath the path to the annotated type argument, wildcard bound, array element type, or
   *     static inner type within 'typeRef'. May be {@literal null} if the annotation targets
   *     'typeRef' as a whole.
   * @param descriptor the class descriptor of the annotation class.
   * @param previousAnnotation the previously visited annotation of the
   *     Runtime[In]Visible[Type]Annotations attribute to which this annotation belongs, or
   *     {@literal null} in other cases (e.g. nested or array annotations).
   * @return a new {@link AnnotationWriter} for the given type annotation reference and descriptor.
   */
  static AnnotationWriter create(
      final SymbolTable symbolTable,
      final int typeRef,
      final TypePath typePath,
      final String descriptor,
      final AnnotationWriter previousAnnotation) {
    // Create a ByteVector to hold a 'type_annotation' JVMS structure.
    // See https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.20.
    ByteVector typeAnnotation = new ByteVector();
    // Write target_type, target_info, and target_path.
    TypeReference.putTarget(typeRef, typeAnnotation);
    TypePath.put(typePath, typeAnnotation);
    // Write type_index and reserve space for num_element_value_pairs.
    typeAnnotation.putShort(symbolTable.addConstantUtf8(descriptor)).putShort(0);
    return new AnnotationWriter(
        symbolTable, /* useNamedValues = */ true, typeAnnotation, previousAnnotation);
  }

  // -----------------------------------------------------------------------------------------------
  // Implementation of the AnnotationVisitor abstract class
  // -----------------------------------------------------------------------------------------------

  @Override
  public void visit(final String name, final Object value) {
    // Case of an element_value with a const_value_index, class_info_index or array_index field.
    // See https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.16.1.
    ++numElementValuePairs;
    if (useNamedValues) {
      annotation.putShort(symbolTable.addConstantUtf8(name));
    }
    if (value instanceof String) {
      annotation.put12('s', symbolTable.addConstantUtf8((String) value));
    } else if (value instanceof Byte) {
      annotation.put12('B', symbolTable.addConstantInteger(((Byte) value).byteValue()).index);
    } else if (value instanceof Boolean) {
      int booleanValue = ((Boolean) value).booleanValue() ? 1 : 0;
      annotation.put12('Z', symbolTable.addConstantInteger(booleanValue).index);
    } else if (value instanceof Character) {
      annotation.put12('C', symbolTable.addConstantInteger(((Character) value).charValue()).index);
    } else if (value instanceof Short) {
      annotation.put12('S', symbolTable.addConstantInteger(((Short) value).shortValue()).index);
    } else if (value instanceof Type) {
      annotation.put12('c', symbolTable.addConstantUtf8(((Type) value).getDescriptor()));
    } else if (value instanceof byte[]) {
      byte[] byteArray = (byte[]) value;
      annotation.put12('[', byteArray.length);
      for (byte byteValue : byteArray) {
        annotation.put12('B', symbolTable.addConstantInteger(byteValue).index);
      }
    } else if (value instanceof boolean[]) {
      boolean[] booleanArray = (boolean[]) value;
      annotation.put12('[', booleanArray.length);
      for (boolean booleanValue : booleanArray) {
        annotation.put12('Z', symbolTable.addConstantInteger(booleanValue ? 1 : 0).index);
      }
    } else if (value instanceof short[]) {
      short[] shortArray = (short[]) value;
      annotation.put12('[', shortArray.length);
      for (short shortValue : shortArray) {
        annotation.put12('S', symbolTable.addConstantInteger(shortValue).index);
      }
    } else if (value instanceof char[]) {
      char[] charArray = (char[]) value;
      annotation.put12('[', charArray.length);
      for (char charValue : charArray) {
        annotation.put12('C', symbolTable.addConstantInteger(charValue).index);
      }
    } else if (value instanceof int[]) {
      int[] intArray = (int[]) value;
      annotation.put12('[', intArray.length);
      for (int intValue : intArray) {
        annotation.put12('I', symbolTable.addConstantInteger(intValue).index);
      }
    } else if (value instanceof long[]) {
      long[] longArray = (long[]) value;
      annotation.put12('[', longArray.length);
      for (long longValue : longArray) {
        annotation.put12('J', symbolTable.addConstantLong(longValue).index);
      }
    } else if (value instanceof float[]) {
      float[] floatArray = (float[]) value;
      annotation.put12('[', floatArray.length);
      for (float floatValue : floatArray) {
        annotation.put12('F', symbolTable.addConstantFloat(floatValue).index);
      }
    } else if (value instanceof double[]) {
      double[] doubleArray = (double[]) value;
      annotation.put12('[', doubleArray.length);
      for (double doubleValue : doubleArray) {
        annotation.put12('D', symbolTable.addConstantDouble(doubleValue).index);
      }
    } else {
      Symbol symbol = symbolTable.addConstant(value);
      annotation.put12(".s.IFJDCS".charAt(symbol.tag), symbol.index);
    }
  }

  @Override
  public void visitEnum(final String name, final String descriptor, final String value) {
    // Case of an element_value with an enum_const_value field.
    // See https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.16.1.
    ++numElementValuePairs;
    if (useNamedValues) {
      annotation.putShort(symbolTable.addConstantUtf8(name));
    }
    annotation
        .put12('e', symbolTable.addConstantUtf8(descriptor))
        .putShort(symbolTable.addConstantUtf8(value));
  }

  @Override
  public AnnotationVisitor visitAnnotation(final String name, final String descriptor) {
    // Case of an element_value with an annotation_value field.
    // See https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.16.1.
    ++numElementValuePairs;
    if (useNamedValues) {
      annotation.putShort(symbolTable.addConstantUtf8(name));
    }
    // Write tag and type_index, and reserve 2 bytes for num_element_value_pairs.
    annotation.put12('@', symbolTable.addConstantUtf8(descriptor)).putShort(0);
    return new AnnotationWriter(symbolTable, /* useNamedValues = */ true, annotation, null);
  }

  @Override
  public AnnotationVisitor visitArray(final String name) {
    // Case of an element_value with an array_value field.
    // https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.16.1
    ++numElementValuePairs;
    if (useNamedValues) {
      annotation.putShort(symbolTable.addConstantUtf8(name));
    }
    // Write tag, and reserve 2 bytes for num_values. Here we take advantage of the fact that the
    // end of an element_value of array type is similar to the end of an 'annotation' structure: an
    // unsigned short num_values followed by num_values element_value, versus an unsigned short
    // num_element_value_pairs, followed by num_element_value_pairs { element_name_index,
    // element_value } tuples. This allows us to use an AnnotationWriter with unnamed values to
    // visit the array elements. Its num_element_value_pairs will correspond to the number of array
    // elements and will be stored in what is in fact num_values.
    annotation.put12('[', 0);
    return new AnnotationWriter(symbolTable, /* useNamedValues = */ false, annotation, null);
  }

  @Override
  public void visitEnd() {
    if (numElementValuePairsOffset != -1) {
      byte[] data = annotation.data;
      data[numElementValuePairsOffset] = (byte) (numElementValuePairs >>> 8);
      data[numElementValuePairsOffset + 1] = (byte) numElementValuePairs;
    }
  }

  // -----------------------------------------------------------------------------------------------
  // Utility methods
  // -----------------------------------------------------------------------------------------------

  /**
   * Returns the size of a Runtime[In]Visible[Type]Annotations attribute containing this annotation
   * and all its <i>predecessors</i> (see {@link #previousAnnotation}. Also adds the attribute name
   * to the constant pool of the class (if not null).
   *
   * @param attributeName one of "Runtime[In]Visible[Type]Annotations", or {@literal null}.
   * @return the size in bytes of a Runtime[In]Visible[Type]Annotations attribute containing this
   *     annotation and all its predecessors. This includes the size of the attribute_name_index and
   *     attribute_length fields.
   */
  int computeAnnotationsSize(final String attributeName) {
    if (attributeName != null) {
      symbolTable.addConstantUtf8(attributeName);
    }
    // The attribute_name_index, attribute_length and num_annotations fields use 8 bytes.
    int attributeSize = 8;
    AnnotationWriter annotationWriter = this;
    while (annotationWriter != null) {
      attributeSize += annotationWriter.annotation.length;
      annotationWriter = annotationWriter.previousAnnotation;
    }
    return attributeSize;
  }

  /**
   * Returns the size of the Runtime[In]Visible[Type]Annotations attributes containing the given
   * annotations and all their <i>predecessors</i> (see {@link #previousAnnotation}. Also adds the
   * attribute names to the constant pool of the class (if not null).
   *
   * @param lastRuntimeVisibleAnnotation The last runtime visible annotation of a field, method or
   *     class. The previous ones can be accessed with the {@link #previousAnnotation} field. May be
   *     {@literal null}.
   * @param lastRuntimeInvisibleAnnotation The last runtime invisible annotation of this a field,
   *     method or class. The previous ones can be accessed with the {@link #previousAnnotation}
   *     field. May be {@literal null}.
   * @param lastRuntimeVisibleTypeAnnotation The last runtime visible type annotation of this a
   *     field, method or class. The previous ones can be accessed with the {@link
   *     #previousAnnotation} field. May be {@literal null}.
   * @param lastRuntimeInvisibleTypeAnnotation The last runtime invisible type annotation of a
   *     field, method or class field. The previous ones can be accessed with the {@link
   *     #previousAnnotation} field. May be {@literal null}.
   * @return the size in bytes of a Runtime[In]Visible[Type]Annotations attribute containing the
   *     given annotations and all their predecessors. This includes the size of the
   *     attribute_name_index and attribute_length fields.
   */
  static int computeAnnotationsSize(
      final AnnotationWriter lastRuntimeVisibleAnnotation,
      final AnnotationWriter lastRuntimeInvisibleAnnotation,
      final AnnotationWriter lastRuntimeVisibleTypeAnnotation,
      final AnnotationWriter lastRuntimeInvisibleTypeAnnotation) {
    int size = 0;
    if (lastRuntimeVisibleAnnotation != null) {
      size +=
          lastRuntimeVisibleAnnotation.computeAnnotationsSize(
              Constants.RUNTIME_VISIBLE_ANNOTATIONS);
    }
    if (lastRuntimeInvisibleAnnotation != null) {
      size +=
          lastRuntimeInvisibleAnnotation.computeAnnotationsSize(
              Constants.RUNTIME_INVISIBLE_ANNOTATIONS);
    }
    if (lastRuntimeVisibleTypeAnnotation != null) {
      size +=
          lastRuntimeVisibleTypeAnnotation.computeAnnotationsSize(
              Constants.RUNTIME_VISIBLE_TYPE_ANNOTATIONS);
    }
    if (lastRuntimeInvisibleTypeAnnotation != null) {
      size +=
          lastRuntimeInvisibleTypeAnnotation.computeAnnotationsSize(
              Constants.RUNTIME_INVISIBLE_TYPE_ANNOTATIONS);
    }
    return size;
  }

  /**
   * Puts a Runtime[In]Visible[Type]Annotations attribute containing this annotations and all its
   * <i>predecessors</i> (see {@link #previousAnnotation} in the given ByteVector. Annotations are
   * put in the same order they have been visited.
   *
   * @param attributeNameIndex the constant pool index of the attribute name (one of
   *     "Runtime[In]Visible[Type]Annotations").
   * @param output where the attribute must be put.
   */
  void putAnnotations(final int attributeNameIndex, final ByteVector output) {
    int attributeLength = 2; // For num_annotations.
    int numAnnotations = 0;
    AnnotationWriter annotationWriter = this;
    AnnotationWriter firstAnnotation = null;
    while (annotationWriter != null) {
      // In case the user forgot to call visitEnd().
      annotationWriter.visitEnd();
      attributeLength += annotationWriter.annotation.length;
      numAnnotations++;
      firstAnnotation = annotationWriter;
      annotationWriter = annotationWriter.previousAnnotation;
    }
    output.putShort(attributeNameIndex);
    output.putInt(attributeLength);
    output.putShort(numAnnotations);
    annotationWriter = firstAnnotation;
    while (annotationWriter != null) {
      output.putByteArray(annotationWriter.annotation.data, 0, annotationWriter.annotation.length);
      annotationWriter = annotationWriter.nextAnnotation;
    }
  }

  /**
   * Puts the Runtime[In]Visible[Type]Annotations attributes containing the given annotations and
   * all their <i>predecessors</i> (see {@link #previousAnnotation} in the given ByteVector.
   * Annotations are put in the same order they have been visited.
   *
   * @param symbolTable where the constants used in the AnnotationWriter instances are stored.
   * @param lastRuntimeVisibleAnnotation The last runtime visible annotation of a field, method or
   *     class. The previous ones can be accessed with the {@link #previousAnnotation} field. May be
   *     {@literal null}.
   * @param lastRuntimeInvisibleAnnotation The last runtime invisible annotation of this a field,
   *     method or class. The previous ones can be accessed with the {@link #previousAnnotation}
   *     field. May be {@literal null}.
   * @param lastRuntimeVisibleTypeAnnotation The last runtime visible type annotation of this a
   *     field, method or class. The previous ones can be accessed with the {@link
   *     #previousAnnotation} field. May be {@literal null}.
   * @param lastRuntimeInvisibleTypeAnnotation The last runtime invisible type annotation of a
   *     field, method or class field. The previous ones can be accessed with the {@link
   *     #previousAnnotation} field. May be {@literal null}.
   * @param output where the attributes must be put.
   */
  static void putAnnotations(
      final SymbolTable symbolTable,
      final AnnotationWriter lastRuntimeVisibleAnnotation,
      final AnnotationWriter lastRuntimeInvisibleAnnotation,
      final AnnotationWriter lastRuntimeVisibleTypeAnnotation,
      final AnnotationWriter lastRuntimeInvisibleTypeAnnotation,
      final ByteVector output) {
    if (lastRuntimeVisibleAnnotation != null) {
      lastRuntimeVisibleAnnotation.putAnnotations(
          symbolTable.addConstantUtf8(Constants.RUNTIME_VISIBLE_ANNOTATIONS), output);
    }
    if (lastRuntimeInvisibleAnnotation != null) {
      lastRuntimeInvisibleAnnotation.putAnnotations(
          symbolTable.addConstantUtf8(Constants.RUNTIME_INVISIBLE_ANNOTATIONS), output);
    }
    if (lastRuntimeVisibleTypeAnnotation != null) {
      lastRuntimeVisibleTypeAnnotation.putAnnotations(
          symbolTable.addConstantUtf8(Constants.RUNTIME_VISIBLE_TYPE_ANNOTATIONS), output);
    }
    if (lastRuntimeInvisibleTypeAnnotation != null) {
      lastRuntimeInvisibleTypeAnnotation.putAnnotations(
          symbolTable.addConstantUtf8(Constants.RUNTIME_INVISIBLE_TYPE_ANNOTATIONS), output);
    }
  }

  /**
   * Returns the size of a Runtime[In]VisibleParameterAnnotations attribute containing all the
   * annotation lists from the given AnnotationWriter sub-array. Also adds the attribute name to the
   * constant pool of the class.
   *
   * @param attributeName one of "Runtime[In]VisibleParameterAnnotations".
   * @param annotationWriters an array of AnnotationWriter lists (designated by their <i>last</i>
   *     element).
   * @param annotableParameterCount the number of elements in annotationWriters to take into account
   *     (elements [0..annotableParameterCount[ are taken into account).
   * @return the size in bytes of a Runtime[In]VisibleParameterAnnotations attribute corresponding
   *     to the given sub-array of AnnotationWriter lists. This includes the size of the
   *     attribute_name_index and attribute_length fields.
   */
  static int computeParameterAnnotationsSize(
      final String attributeName,
      final AnnotationWriter[] annotationWriters,
      final int annotableParameterCount) {
    // Note: attributeName is added to the constant pool by the call to computeAnnotationsSize
    // below. This assumes that there is at least one non-null element in the annotationWriters
    // sub-array (which is ensured by the lazy instantiation of this array in MethodWriter).
    // The attribute_name_index, attribute_length and num_parameters fields use 7 bytes, and each
    // element of the parameter_annotations array uses 2 bytes for its num_annotations field.
    int attributeSize = 7 + 2 * annotableParameterCount;
    for (int i = 0; i < annotableParameterCount; ++i) {
      AnnotationWriter annotationWriter = annotationWriters[i];
      attributeSize +=
          annotationWriter == null ? 0 : annotationWriter.computeAnnotationsSize(attributeName) - 8;
    }
    return attributeSize;
  }

  /**
   * Puts a Runtime[In]VisibleParameterAnnotations attribute containing all the annotation lists
   * from the given AnnotationWriter sub-array in the given ByteVector.
   *
   * @param attributeNameIndex constant pool index of the attribute name (one of
   *     Runtime[In]VisibleParameterAnnotations).
   * @param annotationWriters an array of AnnotationWriter lists (designated by their <i>last</i>
   *     element).
   * @param annotableParameterCount the number of elements in annotationWriters to put (elements
   *     [0..annotableParameterCount[ are put).
   * @param output where the attribute must be put.
   */
  static void putParameterAnnotations(
      final int attributeNameIndex,
      final AnnotationWriter[] annotationWriters,
      final int annotableParameterCount,
      final ByteVector output) {
    // The num_parameters field uses 1 byte, and each element of the parameter_annotations array
    // uses 2 bytes for its num_annotations field.
    int attributeLength = 1 + 2 * annotableParameterCount;
    for (int i = 0; i < annotableParameterCount; ++i) {
      AnnotationWriter annotationWriter = annotationWriters[i];
      attributeLength +=
          annotationWriter == null ? 0 : annotationWriter.computeAnnotationsSize(null) - 8;
    }
    output.putShort(attributeNameIndex);
    output.putInt(attributeLength);
    output.putByte(annotableParameterCount);
    for (int i = 0; i < annotableParameterCount; ++i) {
      AnnotationWriter annotationWriter = annotationWriters[i];
      AnnotationWriter firstAnnotation = null;
      int numAnnotations = 0;
      while (annotationWriter != null) {
        // In case user the forgot to call visitEnd().
        annotationWriter.visitEnd();
        numAnnotations++;
        firstAnnotation = annotationWriter;
        annotationWriter = annotationWriter.previousAnnotation;
      }
      output.putShort(numAnnotations);
      annotationWriter = firstAnnotation;
      while (annotationWriter != null) {
        output.putByteArray(
            annotationWriter.annotation.data, 0, annotationWriter.annotation.length);
        annotationWriter = annotationWriter.nextAnnotation;
      }
    }
  }
}
