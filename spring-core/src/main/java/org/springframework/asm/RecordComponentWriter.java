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

final class RecordComponentWriter extends RecordComponentVisitor {
  /** Where the constants used in this RecordComponentWriter must be stored. */
  private final SymbolTable symbolTable;

  // Note: fields are ordered as in the record_component_info structure, and those related to
  // attributes are ordered as in Section 4.7 of the JVMS.

  /** The name_index field of the Record attribute. */
  private final int nameIndex;

  /** The descriptor_index field of the the Record attribute. */
  private final int descriptorIndex;

  /**
   * The signature_index field of the Signature attribute of this record component, or 0 if there is
   * no Signature attribute.
   */
  private int signatureIndex;

  /**
   * The last runtime visible annotation of this record component. The previous ones can be accessed
   * with the {@link AnnotationWriter#previousAnnotation} field. May be {@literal null}.
   */
  private AnnotationWriter lastRuntimeVisibleAnnotation;

  /**
   * The last runtime invisible annotation of this record component. The previous ones can be
   * accessed with the {@link AnnotationWriter#previousAnnotation} field. May be {@literal null}.
   */
  private AnnotationWriter lastRuntimeInvisibleAnnotation;

  /**
   * The last runtime visible type annotation of this record component. The previous ones can be
   * accessed with the {@link AnnotationWriter#previousAnnotation} field. May be {@literal null}.
   */
  private AnnotationWriter lastRuntimeVisibleTypeAnnotation;

  /**
   * The last runtime invisible type annotation of this record component. The previous ones can be
   * accessed with the {@link AnnotationWriter#previousAnnotation} field. May be {@literal null}.
   */
  private AnnotationWriter lastRuntimeInvisibleTypeAnnotation;

  /**
   * The first non standard attribute of this record component. The next ones can be accessed with
   * the {@link Attribute#nextAttribute} field. May be {@literal null}.
   *
   * <p><b>WARNING</b>: this list stores the attributes in the <i>reverse</i> order of their visit.
   * firstAttribute is actually the last attribute visited in {@link #visitAttribute(Attribute)}.
   * The {@link #putRecordComponentInfo(ByteVector)} method writes the attributes in the order
   * defined by this list, i.e. in the reverse order specified by the user.
   */
  private Attribute firstAttribute;

  /**
   * Constructs a new {@link RecordComponentWriter}.
   *
   * @param symbolTable where the constants used in this RecordComponentWriter must be stored.
   * @param name the record component name.
   * @param descriptor the record component descriptor (see {@link Type}).
   * @param signature the record component signature. May be {@literal null}.
   */
  RecordComponentWriter(
      final SymbolTable symbolTable,
      final String name,
      final String descriptor,
      final String signature) {
    super(/* latest api = */ Opcodes.ASM9);
    this.symbolTable = symbolTable;
    this.nameIndex = symbolTable.addConstantUtf8(name);
    this.descriptorIndex = symbolTable.addConstantUtf8(descriptor);
    if (signature != null) {
      this.signatureIndex = symbolTable.addConstantUtf8(signature);
    }
  }

  // -----------------------------------------------------------------------------------------------
  // Implementation of the FieldVisitor abstract class
  // -----------------------------------------------------------------------------------------------

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
  public void visitAttribute(final Attribute attribute) {
    // Store the attributes in the <i>reverse</i> order of their visit by this method.
    attribute.nextAttribute = firstAttribute;
    firstAttribute = attribute;
  }

  @Override
  public void visitEnd() {
    // Nothing to do.
  }

  // -----------------------------------------------------------------------------------------------
  // Utility methods
  // -----------------------------------------------------------------------------------------------

  /**
   * Returns the size of the record component JVMS structure generated by this
   * RecordComponentWriter. Also adds the names of the attributes of this record component in the
   * constant pool.
   *
   * @return the size in bytes of the record_component_info of the Record attribute.
   */
  int computeRecordComponentInfoSize() {
    // name_index, descriptor_index and attributes_count fields use 6 bytes.
    int size = 6;
    size += Attribute.computeAttributesSize(symbolTable, 0, signatureIndex);
    size +=
        AnnotationWriter.computeAnnotationsSize(
            lastRuntimeVisibleAnnotation,
            lastRuntimeInvisibleAnnotation,
            lastRuntimeVisibleTypeAnnotation,
            lastRuntimeInvisibleTypeAnnotation);
    if (firstAttribute != null) {
      size += firstAttribute.computeAttributesSize(symbolTable);
    }
    return size;
  }

  /**
   * Puts the content of the record component generated by this RecordComponentWriter into the given
   * ByteVector.
   *
   * @param output where the record_component_info structure must be put.
   */
  void putRecordComponentInfo(final ByteVector output) {
    output.putShort(nameIndex).putShort(descriptorIndex);
    // Compute and put the attributes_count field.
    // For ease of reference, we use here the same attribute order as in Section 4.7 of the JVMS.
    int attributesCount = 0;
    if (signatureIndex != 0) {
      ++attributesCount;
    }
    if (lastRuntimeVisibleAnnotation != null) {
      ++attributesCount;
    }
    if (lastRuntimeInvisibleAnnotation != null) {
      ++attributesCount;
    }
    if (lastRuntimeVisibleTypeAnnotation != null) {
      ++attributesCount;
    }
    if (lastRuntimeInvisibleTypeAnnotation != null) {
      ++attributesCount;
    }
    if (firstAttribute != null) {
      attributesCount += firstAttribute.getAttributeCount();
    }
    output.putShort(attributesCount);
    Attribute.putAttributes(symbolTable, 0, signatureIndex, output);
    AnnotationWriter.putAnnotations(
        symbolTable,
        lastRuntimeVisibleAnnotation,
        lastRuntimeInvisibleAnnotation,
        lastRuntimeVisibleTypeAnnotation,
        lastRuntimeInvisibleTypeAnnotation,
        output);
    if (firstAttribute != null) {
      firstAttribute.putAttributes(symbolTable, output);
    }
  }

  /**
   * Collects the attributes of this record component into the given set of attribute prototypes.
   *
   * @param attributePrototypes a set of attribute prototypes.
   */
  final void collectAttributePrototypes(final Attribute.Set attributePrototypes) {
    attributePrototypes.addAttributes(firstAttribute);
  }
}
