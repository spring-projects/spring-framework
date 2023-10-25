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
 * An entry of the constant pool, of the BootstrapMethods attribute, or of the (ASM specific) type
 * table of a class.
 *
 * @see <a href="https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.4">JVMS
 *     4.4</a>
 * @see <a href="https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.23">JVMS
 *     4.7.23</a>
 * @author Eric Bruneton
 */
abstract class Symbol {

  // Tag values for the constant pool entries (using the same order as in the JVMS).

  /** The tag value of CONSTANT_Class_info JVMS structures. */
  static final int CONSTANT_CLASS_TAG = 7;

  /** The tag value of CONSTANT_Fieldref_info JVMS structures. */
  static final int CONSTANT_FIELDREF_TAG = 9;

  /** The tag value of CONSTANT_Methodref_info JVMS structures. */
  static final int CONSTANT_METHODREF_TAG = 10;

  /** The tag value of CONSTANT_InterfaceMethodref_info JVMS structures. */
  static final int CONSTANT_INTERFACE_METHODREF_TAG = 11;

  /** The tag value of CONSTANT_String_info JVMS structures. */
  static final int CONSTANT_STRING_TAG = 8;

  /** The tag value of CONSTANT_Integer_info JVMS structures. */
  static final int CONSTANT_INTEGER_TAG = 3;

  /** The tag value of CONSTANT_Float_info JVMS structures. */
  static final int CONSTANT_FLOAT_TAG = 4;

  /** The tag value of CONSTANT_Long_info JVMS structures. */
  static final int CONSTANT_LONG_TAG = 5;

  /** The tag value of CONSTANT_Double_info JVMS structures. */
  static final int CONSTANT_DOUBLE_TAG = 6;

  /** The tag value of CONSTANT_NameAndType_info JVMS structures. */
  static final int CONSTANT_NAME_AND_TYPE_TAG = 12;

  /** The tag value of CONSTANT_Utf8_info JVMS structures. */
  static final int CONSTANT_UTF8_TAG = 1;

  /** The tag value of CONSTANT_MethodHandle_info JVMS structures. */
  static final int CONSTANT_METHOD_HANDLE_TAG = 15;

  /** The tag value of CONSTANT_MethodType_info JVMS structures. */
  static final int CONSTANT_METHOD_TYPE_TAG = 16;

  /** The tag value of CONSTANT_Dynamic_info JVMS structures. */
  static final int CONSTANT_DYNAMIC_TAG = 17;

  /** The tag value of CONSTANT_InvokeDynamic_info JVMS structures. */
  static final int CONSTANT_INVOKE_DYNAMIC_TAG = 18;

  /** The tag value of CONSTANT_Module_info JVMS structures. */
  static final int CONSTANT_MODULE_TAG = 19;

  /** The tag value of CONSTANT_Package_info JVMS structures. */
  static final int CONSTANT_PACKAGE_TAG = 20;

  // Tag values for the BootstrapMethods attribute entries (ASM specific tag).

  /** The tag value of the BootstrapMethods attribute entries. */
  static final int BOOTSTRAP_METHOD_TAG = 64;

  // Tag values for the type table entries (ASM specific tags).

  /** The tag value of a normal type entry in the (ASM specific) type table of a class. */
  static final int TYPE_TAG = 128;

  /**
   * The tag value of an uninitialized type entry in the type table of a class. This type is used
   * for the normal case where the NEW instruction is before the &lt;init&gt; constructor call (in
   * bytecode offset order), i.e. when the label of the NEW instruction is resolved when the
   * constructor call is visited. If the NEW instruction is after the constructor call, use the
   * {@link #FORWARD_UNINITIALIZED_TYPE_TAG} tag value instead.
   */
  static final int UNINITIALIZED_TYPE_TAG = 129;

  /**
   * The tag value of an uninitialized type entry in the type table of a class. This type is used
   * for the unusual case where the NEW instruction is after the &lt;init&gt; constructor call (in
   * bytecode offset order), i.e. when the label of the NEW instruction is not resolved when the
   * constructor call is visited. If the NEW instruction is before the constructor call, use the
   * {@link #UNINITIALIZED_TYPE_TAG} tag value instead.
   */
  static final int FORWARD_UNINITIALIZED_TYPE_TAG = 130;

  /** The tag value of a merged type entry in the (ASM specific) type table of a class. */
  static final int MERGED_TYPE_TAG = 131;

  // Instance fields.

  /**
   * The index of this symbol in the constant pool, in the BootstrapMethods attribute, or in the
   * (ASM specific) type table of a class (depending on the {@link #tag} value).
   */
  final int index;

  /**
   * A tag indicating the type of this symbol. Must be one of the static tag values defined in this
   * class.
   */
  final int tag;

  /**
   * The internal name of the owner class of this symbol. Only used for {@link
   * #CONSTANT_FIELDREF_TAG}, {@link #CONSTANT_METHODREF_TAG}, {@link
   * #CONSTANT_INTERFACE_METHODREF_TAG}, and {@link #CONSTANT_METHOD_HANDLE_TAG} symbols.
   */
  final String owner;

  /**
   * The name of the class field or method corresponding to this symbol. Only used for {@link
   * #CONSTANT_FIELDREF_TAG}, {@link #CONSTANT_METHODREF_TAG}, {@link
   * #CONSTANT_INTERFACE_METHODREF_TAG}, {@link #CONSTANT_NAME_AND_TYPE_TAG}, {@link
   * #CONSTANT_METHOD_HANDLE_TAG}, {@link #CONSTANT_DYNAMIC_TAG} and {@link
   * #CONSTANT_INVOKE_DYNAMIC_TAG} symbols.
   */
  final String name;

  /**
   * The string value of this symbol. This is:
   *
   * <ul>
   *   <li>a field or method descriptor for {@link #CONSTANT_FIELDREF_TAG}, {@link
   *       #CONSTANT_METHODREF_TAG}, {@link #CONSTANT_INTERFACE_METHODREF_TAG}, {@link
   *       #CONSTANT_NAME_AND_TYPE_TAG}, {@link #CONSTANT_METHOD_HANDLE_TAG}, {@link
   *       #CONSTANT_METHOD_TYPE_TAG}, {@link #CONSTANT_DYNAMIC_TAG} and {@link
   *       #CONSTANT_INVOKE_DYNAMIC_TAG} symbols,
   *   <li>an arbitrary string for {@link #CONSTANT_UTF8_TAG} and {@link #CONSTANT_STRING_TAG}
   *       symbols,
   *   <li>an internal class name for {@link #CONSTANT_CLASS_TAG}, {@link #TYPE_TAG}, {@link
   *       #UNINITIALIZED_TYPE_TAG} and {@link #FORWARD_UNINITIALIZED_TYPE_TAG} symbols,
   *   <li>{@literal null} for the other types of symbol.
   * </ul>
   */
  final String value;

  /**
   * The numeric value of this symbol. This is:
   *
   * <ul>
   *   <li>the symbol's value for {@link #CONSTANT_INTEGER_TAG},{@link #CONSTANT_FLOAT_TAG}, {@link
   *       #CONSTANT_LONG_TAG}, {@link #CONSTANT_DOUBLE_TAG},
   *   <li>the CONSTANT_MethodHandle_info reference_kind field value for {@link
   *       #CONSTANT_METHOD_HANDLE_TAG} symbols,
   *   <li>the CONSTANT_InvokeDynamic_info bootstrap_method_attr_index field value for {@link
   *       #CONSTANT_INVOKE_DYNAMIC_TAG} symbols,
   *   <li>the offset of a bootstrap method in the BootstrapMethods boostrap_methods array, for
   *       {@link #CONSTANT_DYNAMIC_TAG} or {@link #BOOTSTRAP_METHOD_TAG} symbols,
   *   <li>the bytecode offset of the NEW instruction that created an {@link
   *       Frame#ITEM_UNINITIALIZED} type for {@link #UNINITIALIZED_TYPE_TAG} symbols,
   *   <li>the index of the {@link Label} (in the {@link SymbolTable#labelTable} table) of the NEW
   *       instruction that created an {@link Frame#ITEM_UNINITIALIZED} type for {@link
   *       #FORWARD_UNINITIALIZED_TYPE_TAG} symbols,
   *   <li>the indices (in the class' type table) of two {@link #TYPE_TAG} source types for {@link
   *       #MERGED_TYPE_TAG} symbols,
   *   <li>0 for the other types of symbol.
   * </ul>
   */
  final long data;

  /**
   * Additional information about this symbol, generally computed lazily. <i>Warning: the value of
   * this field is ignored when comparing Symbol instances</i> (to avoid duplicate entries in a
   * SymbolTable). Therefore, this field should only contain data that can be computed from the
   * other fields of this class. It contains:
   *
   * <ul>
   *   <li>the {@link Type#getArgumentsAndReturnSizes} of the symbol's method descriptor for {@link
   *       #CONSTANT_METHODREF_TAG}, {@link #CONSTANT_INTERFACE_METHODREF_TAG} and {@link
   *       #CONSTANT_INVOKE_DYNAMIC_TAG} symbols,
   *   <li>the index in the InnerClasses_attribute 'classes' array (plus one) corresponding to this
   *       class, for {@link #CONSTANT_CLASS_TAG} symbols,
   *   <li>the index (in the class' type table) of the merged type of the two source types for
   *       {@link #MERGED_TYPE_TAG} symbols,
   *   <li>0 for the other types of symbol, or if this field has not been computed yet.
   * </ul>
   */
  int info;

  /**
   * Constructs a new Symbol. This constructor can't be used directly because the Symbol class is
   * abstract. Instead, use the factory methods of the {@link SymbolTable} class.
   *
   * @param index the symbol index in the constant pool, in the BootstrapMethods attribute, or in
   *     the (ASM specific) type table of a class (depending on 'tag').
   * @param tag the symbol type. Must be one of the static tag values defined in this class.
   * @param owner The internal name of the symbol's owner class. Maybe {@literal null}.
   * @param name The name of the symbol's corresponding class field or method. Maybe {@literal
   *     null}.
   * @param value The string value of this symbol. Maybe {@literal null}.
   * @param data The numeric value of this symbol.
   */
  Symbol(
      final int index,
      final int tag,
      final String owner,
      final String name,
      final String value,
      final long data) {
    this.index = index;
    this.tag = tag;
    this.owner = owner;
    this.name = name;
    this.value = value;
    this.data = data;
  }

  /**
   * Returns the result {@link Type#getArgumentsAndReturnSizes} on {@link #value}.
   *
   * @return the result {@link Type#getArgumentsAndReturnSizes} on {@link #value} (memoized in
   *     {@link #info} for efficiency). This should only be used for {@link
   *     #CONSTANT_METHODREF_TAG}, {@link #CONSTANT_INTERFACE_METHODREF_TAG} and {@link
   *     #CONSTANT_INVOKE_DYNAMIC_TAG} symbols.
   */
  int getArgumentsAndReturnSizes() {
    if (info == 0) {
      info = Type.getArgumentsAndReturnSizes(value);
    }
    return info;
  }
}
