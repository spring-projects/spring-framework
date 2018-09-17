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
 * The constant pool entries, the BootstrapMethods attribute entries and the (ASM specific) type
 * table entries of a class.
 *
 * @see <a href="https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.4">JVMS
 *     4.4</a>
 * @see <a href="https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.23">JVMS
 *     4.7.23</a>
 * @author Eric Bruneton
 */
final class SymbolTable {

  /**
   * The ClassWriter to which this SymbolTable belongs. This is only used to get access to {@link
   * ClassWriter#getCommonSuperClass} and to serialize custom attributes with {@link
   * Attribute#write}.
   */
  final ClassWriter classWriter;

  /**
   * The ClassReader from which this SymbolTable was constructed, or {@literal null} if it was
   * constructed from scratch.
   */
  private final ClassReader sourceClassReader;

  /** The major version number of the class to which this symbol table belongs. */
  private int majorVersion;

  /** The internal name of the class to which this symbol table belongs. */
  private String className;

  /**
   * The total number of {@link Entry} instances in {@link #entries}. This includes entries that are
   * accessible (recursively) via {@link Entry#next}.
   */
  private int entryCount;

  /**
   * A hash set of all the entries in this SymbolTable (this includes the constant pool entries, the
   * bootstrap method entries and the type table entries). Each {@link Entry} instance is stored at
   * the array index given by its hash code modulo the array size. If several entries must be stored
   * at the same array index, they are linked together via their {@link Entry#next} field. The
   * factory methods of this class make sure that this table does not contain duplicated entries.
   */
  private Entry[] entries;

  /**
   * The number of constant pool items in {@link #constantPool}, plus 1. The first constant pool
   * item has index 1, and long and double items count for two items.
   */
  private int constantPoolCount;

  /**
   * The content of the ClassFile's constant_pool JVMS structure corresponding to this SymbolTable.
   * The ClassFile's constant_pool_count field is <i>not</i> included.
   */
  private ByteVector constantPool;

  /**
   * The number of bootstrap methods in {@link #bootstrapMethods}. Corresponds to the
   * BootstrapMethods_attribute's num_bootstrap_methods field value.
   */
  private int bootstrapMethodCount;

  /**
   * The content of the BootstrapMethods attribute 'bootstrap_methods' array corresponding to this
   * SymbolTable. Note that the first 6 bytes of the BootstrapMethods_attribute, and its
   * num_bootstrap_methods field, are <i>not</i> included.
   */
  private ByteVector bootstrapMethods;

  /**
   * The actual number of elements in {@link #typeTable}. These elements are stored from index 0 to
   * typeCount (excluded). The other array entries are empty.
   */
  private int typeCount;

  /**
   * An ASM specific type table used to temporarily store internal names that will not necessarily
   * be stored in the constant pool. This type table is used by the control flow and data flow
   * analysis algorithm used to compute stack map frames from scratch. This array stores {@link
   * Symbol#TYPE_TAG} and {@link Symbol#UNINITIALIZED_TYPE_TAG}) Symbol. The type symbol at index
   * {@code i} has its {@link Symbol#index} equal to {@code i} (and vice versa).
   */
  private Entry[] typeTable;

  /**
   * Constructs a new, empty SymbolTable for the given ClassWriter.
   *
   * @param classWriter a ClassWriter.
   */
  SymbolTable(final ClassWriter classWriter) {
    this.classWriter = classWriter;
    this.sourceClassReader = null;
    this.entries = new Entry[256];
    this.constantPoolCount = 1;
    this.constantPool = new ByteVector();
  }

  /**
   * Constructs a new SymbolTable for the given ClassWriter, initialized with the constant pool and
   * bootstrap methods of the given ClassReader.
   *
   * @param classWriter a ClassWriter.
   * @param classReader the ClassReader whose constant pool and bootstrap methods must be copied to
   *     initialize the SymbolTable.
   */
  SymbolTable(final ClassWriter classWriter, final ClassReader classReader) {
    this.classWriter = classWriter;
    this.sourceClassReader = classReader;

    // Copy the constant pool binary content.
    byte[] inputBytes = classReader.b;
    int constantPoolOffset = classReader.getItem(1) - 1;
    int constantPoolLength = classReader.header - constantPoolOffset;
    constantPoolCount = classReader.getItemCount();
    constantPool = new ByteVector(constantPoolLength);
    constantPool.putByteArray(inputBytes, constantPoolOffset, constantPoolLength);

    // Add the constant pool items in the symbol table entries. Reserve enough space in 'entries' to
    // avoid too many hash set collisions (entries is not dynamically resized by the addConstant*
    // method calls below), and to account for bootstrap method entries.
    entries = new Entry[constantPoolCount * 2];
    char[] charBuffer = new char[classReader.getMaxStringLength()];
    boolean hasBootstrapMethods = false;
    int itemIndex = 1;
    while (itemIndex < constantPoolCount) {
      int itemOffset = classReader.getItem(itemIndex);
      int itemTag = inputBytes[itemOffset - 1];
      int nameAndTypeItemOffset;
      switch (itemTag) {
        case Symbol.CONSTANT_FIELDREF_TAG:
        case Symbol.CONSTANT_METHODREF_TAG:
        case Symbol.CONSTANT_INTERFACE_METHODREF_TAG:
          nameAndTypeItemOffset =
              classReader.getItem(classReader.readUnsignedShort(itemOffset + 2));
          addConstantMemberReference(
              itemIndex,
              itemTag,
              classReader.readClass(itemOffset, charBuffer),
              classReader.readUTF8(nameAndTypeItemOffset, charBuffer),
              classReader.readUTF8(nameAndTypeItemOffset + 2, charBuffer));
          break;
        case Symbol.CONSTANT_INTEGER_TAG:
        case Symbol.CONSTANT_FLOAT_TAG:
          addConstantIntegerOrFloat(itemIndex, itemTag, classReader.readInt(itemOffset));
          break;
        case Symbol.CONSTANT_NAME_AND_TYPE_TAG:
          addConstantNameAndType(
              itemIndex,
              classReader.readUTF8(itemOffset, charBuffer),
              classReader.readUTF8(itemOffset + 2, charBuffer));
          break;
        case Symbol.CONSTANT_LONG_TAG:
        case Symbol.CONSTANT_DOUBLE_TAG:
          addConstantLongOrDouble(itemIndex, itemTag, classReader.readLong(itemOffset));
          break;
        case Symbol.CONSTANT_UTF8_TAG:
          addConstantUtf8(itemIndex, classReader.readUtf(itemIndex, charBuffer));
          break;
        case Symbol.CONSTANT_METHOD_HANDLE_TAG:
          int memberRefItemOffset =
              classReader.getItem(classReader.readUnsignedShort(itemOffset + 1));
          nameAndTypeItemOffset =
              classReader.getItem(classReader.readUnsignedShort(memberRefItemOffset + 2));
          addConstantMethodHandle(
              itemIndex,
              classReader.readByte(itemOffset),
              classReader.readClass(memberRefItemOffset, charBuffer),
              classReader.readUTF8(nameAndTypeItemOffset, charBuffer),
              classReader.readUTF8(nameAndTypeItemOffset + 2, charBuffer));
          break;
        case Symbol.CONSTANT_DYNAMIC_TAG:
        case Symbol.CONSTANT_INVOKE_DYNAMIC_TAG:
          hasBootstrapMethods = true;
          nameAndTypeItemOffset =
              classReader.getItem(classReader.readUnsignedShort(itemOffset + 2));
          addConstantDynamicOrInvokeDynamicReference(
              itemTag,
              itemIndex,
              classReader.readUTF8(nameAndTypeItemOffset, charBuffer),
              classReader.readUTF8(nameAndTypeItemOffset + 2, charBuffer),
              classReader.readUnsignedShort(itemOffset));
          break;
        case Symbol.CONSTANT_STRING_TAG:
        case Symbol.CONSTANT_CLASS_TAG:
        case Symbol.CONSTANT_METHOD_TYPE_TAG:
        case Symbol.CONSTANT_MODULE_TAG:
        case Symbol.CONSTANT_PACKAGE_TAG:
          addConstantUtf8Reference(
              itemIndex, itemTag, classReader.readUTF8(itemOffset, charBuffer));
          break;
        default:
          throw new IllegalArgumentException();
      }
      itemIndex +=
          (itemTag == Symbol.CONSTANT_LONG_TAG || itemTag == Symbol.CONSTANT_DOUBLE_TAG) ? 2 : 1;
    }

    // Copy the BootstrapMethods, if any.
    if (hasBootstrapMethods) {
      copyBootstrapMethods(classReader, charBuffer);
    }
  }

  /**
   * Read the BootstrapMethods 'bootstrap_methods' array binary content and add them as entries of
   * the SymbolTable.
   *
   * @param classReader the ClassReader whose bootstrap methods must be copied to initialize the
   *     SymbolTable.
   * @param charBuffer a buffer used to read strings in the constant pool.
   */
  private void copyBootstrapMethods(final ClassReader classReader, final char[] charBuffer) {
    // Find attributOffset of the 'bootstrap_methods' array.
    byte[] inputBytes = classReader.b;
    int currentAttributeOffset = classReader.getFirstAttributeOffset();
    for (int i = classReader.readUnsignedShort(currentAttributeOffset - 2); i > 0; --i) {
      String attributeName = classReader.readUTF8(currentAttributeOffset, charBuffer);
      if (Constants.BOOTSTRAP_METHODS.equals(attributeName)) {
        bootstrapMethodCount = classReader.readUnsignedShort(currentAttributeOffset + 6);
        break;
      }
      currentAttributeOffset += 6 + classReader.readInt(currentAttributeOffset + 2);
    }
    if (bootstrapMethodCount > 0) {
      // Compute the offset and the length of the BootstrapMethods 'bootstrap_methods' array.
      int bootstrapMethodsOffset = currentAttributeOffset + 8;
      int bootstrapMethodsLength = classReader.readInt(currentAttributeOffset + 2) - 2;
      bootstrapMethods = new ByteVector(bootstrapMethodsLength);
      bootstrapMethods.putByteArray(inputBytes, bootstrapMethodsOffset, bootstrapMethodsLength);

      // Add each bootstrap method in the symbol table entries.
      int currentOffset = bootstrapMethodsOffset;
      for (int i = 0; i < bootstrapMethodCount; i++) {
        int offset = currentOffset - bootstrapMethodsOffset;
        int bootstrapMethodRef = classReader.readUnsignedShort(currentOffset);
        currentOffset += 2;
        int numBootstrapArguments = classReader.readUnsignedShort(currentOffset);
        currentOffset += 2;
        int hashCode = classReader.readConst(bootstrapMethodRef, charBuffer).hashCode();
        while (numBootstrapArguments-- > 0) {
          int bootstrapArgument = classReader.readUnsignedShort(currentOffset);
          currentOffset += 2;
          hashCode ^= classReader.readConst(bootstrapArgument, charBuffer).hashCode();
        }
        add(new Entry(i, Symbol.BOOTSTRAP_METHOD_TAG, offset, hashCode & 0x7FFFFFFF));
      }
    }
  }

  /**
   * Returns the ClassReader from which this SymbolTable was constructed.
   *
   * @return the ClassReader from which this SymbolTable was constructed, or {@literal null} if it
   *     was constructed from scratch.
   */
  ClassReader getSource() {
    return sourceClassReader;
  }

  /**
   * Returns the major version of the class to which this symbol table belongs.
   *
   * @return the major version of the class to which this symbol table belongs.
   */
  int getMajorVersion() {
    return majorVersion;
  }

  /**
   * Returns the internal name of the class to which this symbol table belongs.
   *
   * @return the internal name of the class to which this symbol table belongs.
   */
  String getClassName() {
    return className;
  }

  /**
   * Sets the major version and the name of the class to which this symbol table belongs. Also adds
   * the class name to the constant pool.
   *
   * @param majorVersion a major ClassFile version number.
   * @param className an internal class name.
   * @return the constant pool index of a new or already existing Symbol with the given class name.
   */
  int setMajorVersionAndClassName(final int majorVersion, final String className) {
    this.majorVersion = majorVersion;
    this.className = className;
    return addConstantClass(className).index;
  }

  /**
   * Returns the number of items in this symbol table's constant_pool array (plus 1).
   *
   * @return the number of items in this symbol table's constant_pool array (plus 1).
   */
  int getConstantPoolCount() {
    return constantPoolCount;
  }

  /**
   * Returns the length in bytes of this symbol table's constant_pool array.
   *
   * @return the length in bytes of this symbol table's constant_pool array.
   */
  int getConstantPoolLength() {
    return constantPool.length;
  }

  /**
   * Puts this symbol table's constant_pool array in the given ByteVector, preceded by the
   * constant_pool_count value.
   *
   * @param output where the JVMS ClassFile's constant_pool array must be put.
   */
  void putConstantPool(final ByteVector output) {
    output.putShort(constantPoolCount).putByteArray(constantPool.data, 0, constantPool.length);
  }

  /**
   * Returns the size in bytes of this symbol table's BootstrapMethods attribute. Also adds the
   * attribute name in the constant pool.
   *
   * @return the size in bytes of this symbol table's BootstrapMethods attribute.
   */
  int computeBootstrapMethodsSize() {
    if (bootstrapMethods != null) {
      addConstantUtf8(Constants.BOOTSTRAP_METHODS);
      return 8 + bootstrapMethods.length;
    } else {
      return 0;
    }
  }

  /**
   * Puts this symbol table's BootstrapMethods attribute in the given ByteVector. This includes the
   * 6 attribute header bytes and the num_bootstrap_methods value.
   *
   * @param output where the JVMS BootstrapMethods attribute must be put.
   */
  void putBootstrapMethods(final ByteVector output) {
    if (bootstrapMethods != null) {
      output
          .putShort(addConstantUtf8(Constants.BOOTSTRAP_METHODS))
          .putInt(bootstrapMethods.length + 2)
          .putShort(bootstrapMethodCount)
          .putByteArray(bootstrapMethods.data, 0, bootstrapMethods.length);
    }
  }

  // -----------------------------------------------------------------------------------------------
  // Generic symbol table entries management.
  // -----------------------------------------------------------------------------------------------

  /**
   * Returns the list of entries which can potentially have the given hash code.
   *
   * @param hashCode a {@link Entry#hashCode} value.
   * @return the list of entries which can potentially have the given hash code. The list is stored
   *     via the {@link Entry#next} field.
   */
  private Entry get(final int hashCode) {
    return entries[hashCode % entries.length];
  }

  /**
   * Puts the given entry in the {@link #entries} hash set. This method does <i>not</i> check
   * whether {@link #entries} already contains a similar entry or not. {@link #entries} is resized
   * if necessary to avoid hash collisions (multiple entries needing to be stored at the same {@link
   * #entries} array index) as much as possible, with reasonable memory usage.
   *
   * @param entry an Entry (which must not already be contained in {@link #entries}).
   * @return the given entry
   */
  private Entry put(final Entry entry) {
    if (entryCount > (entries.length * 3) / 4) {
      int currentCapacity = entries.length;
      int newCapacity = currentCapacity * 2 + 1;
      Entry[] newEntries = new Entry[newCapacity];
      for (int i = currentCapacity - 1; i >= 0; --i) {
        Entry currentEntry = entries[i];
        while (currentEntry != null) {
          int newCurrentEntryIndex = currentEntry.hashCode % newCapacity;
          Entry nextEntry = currentEntry.next;
          currentEntry.next = newEntries[newCurrentEntryIndex];
          newEntries[newCurrentEntryIndex] = currentEntry;
          currentEntry = nextEntry;
        }
      }
      entries = newEntries;
    }
    entryCount++;
    int index = entry.hashCode % entries.length;
    entry.next = entries[index];
    return entries[index] = entry;
  }

  /**
   * Adds the given entry in the {@link #entries} hash set. This method does <i>not</i> check
   * whether {@link #entries} already contains a similar entry or not, and does <i>not</i> resize
   * {@link #entries} if necessary.
   *
   * @param entry an Entry (which must not already be contained in {@link #entries}).
   */
  private void add(final Entry entry) {
    entryCount++;
    int index = entry.hashCode % entries.length;
    entry.next = entries[index];
    entries[index] = entry;
  }

  // -----------------------------------------------------------------------------------------------
  // Constant pool entries management.
  // -----------------------------------------------------------------------------------------------

  /**
   * Adds a number or string constant to the constant pool of this symbol table. Does nothing if the
   * constant pool already contains a similar item.
   *
   * @param value the value of the constant to be added to the constant pool. This parameter must be
   *     an {@link Integer}, {@link Byte}, {@link Character}, {@link Short}, {@link Boolean}, {@link
   *     Float}, {@link Long}, {@link Double}, {@link String}, {@link Type} or {@link Handle}.
   * @return a new or already existing Symbol with the given value.
   */
  Symbol addConstant(final Object value) {
    if (value instanceof Integer) {
      return addConstantInteger(((Integer) value).intValue());
    } else if (value instanceof Byte) {
      return addConstantInteger(((Byte) value).intValue());
    } else if (value instanceof Character) {
      return addConstantInteger(((Character) value).charValue());
    } else if (value instanceof Short) {
      return addConstantInteger(((Short) value).intValue());
    } else if (value instanceof Boolean) {
      return addConstantInteger(((Boolean) value).booleanValue() ? 1 : 0);
    } else if (value instanceof Float) {
      return addConstantFloat(((Float) value).floatValue());
    } else if (value instanceof Long) {
      return addConstantLong(((Long) value).longValue());
    } else if (value instanceof Double) {
      return addConstantDouble(((Double) value).doubleValue());
    } else if (value instanceof String) {
      return addConstantString((String) value);
    } else if (value instanceof Type) {
      Type type = (Type) value;
      int typeSort = type.getSort();
      if (typeSort == Type.OBJECT) {
        return addConstantClass(type.getInternalName());
      } else if (typeSort == Type.METHOD) {
        return addConstantMethodType(type.getDescriptor());
      } else { // type is a primitive or array type.
        return addConstantClass(type.getDescriptor());
      }
    } else if (value instanceof Handle) {
      Handle handle = (Handle) value;
      return addConstantMethodHandle(
          handle.getTag(),
          handle.getOwner(),
          handle.getName(),
          handle.getDesc(),
          handle.isInterface());
    } else if (value instanceof ConstantDynamic) {
      ConstantDynamic constantDynamic = (ConstantDynamic) value;
      return addConstantDynamic(
          constantDynamic.getName(),
          constantDynamic.getDescriptor(),
          constantDynamic.getBootstrapMethod(),
          constantDynamic.getBootstrapMethodArguments());
    } else {
      throw new IllegalArgumentException("value " + value);
    }
  }

  /**
   * Adds a CONSTANT_Class_info to the constant pool of this symbol table. Does nothing if the
   * constant pool already contains a similar item.
   *
   * @param value the internal name of a class.
   * @return a new or already existing Symbol with the given value.
   */
  Symbol addConstantClass(final String value) {
    return addConstantUtf8Reference(Symbol.CONSTANT_CLASS_TAG, value);
  }

  /**
   * Adds a CONSTANT_Fieldref_info to the constant pool of this symbol table. Does nothing if the
   * constant pool already contains a similar item.
   *
   * @param owner the internal name of a class.
   * @param name a field name.
   * @param descriptor a field descriptor.
   * @return a new or already existing Symbol with the given value.
   */
  Symbol addConstantFieldref(final String owner, final String name, final String descriptor) {
    return addConstantMemberReference(Symbol.CONSTANT_FIELDREF_TAG, owner, name, descriptor);
  }

  /**
   * Adds a CONSTANT_Methodref_info or CONSTANT_InterfaceMethodref_info to the constant pool of this
   * symbol table. Does nothing if the constant pool already contains a similar item.
   *
   * @param owner the internal name of a class.
   * @param name a method name.
   * @param descriptor a method descriptor.
   * @param isInterface whether owner is an interface or not.
   * @return a new or already existing Symbol with the given value.
   */
  Symbol addConstantMethodref(
      final String owner, final String name, final String descriptor, final boolean isInterface) {
    int tag = isInterface ? Symbol.CONSTANT_INTERFACE_METHODREF_TAG : Symbol.CONSTANT_METHODREF_TAG;
    return addConstantMemberReference(tag, owner, name, descriptor);
  }

  /**
   * Adds a CONSTANT_Fieldref_info, CONSTANT_Methodref_info or CONSTANT_InterfaceMethodref_info to
   * the constant pool of this symbol table. Does nothing if the constant pool already contains a
   * similar item.
   *
   * @param tag one of {@link Symbol#CONSTANT_FIELDREF_TAG}, {@link Symbol#CONSTANT_METHODREF_TAG}
   *     or {@link Symbol#CONSTANT_INTERFACE_METHODREF_TAG}.
   * @param owner the internal name of a class.
   * @param name a field or method name.
   * @param descriptor a field or method descriptor.
   * @return a new or already existing Symbol with the given value.
   */
  private Entry addConstantMemberReference(
      final int tag, final String owner, final String name, final String descriptor) {
    int hashCode = hash(tag, owner, name, descriptor);
    Entry entry = get(hashCode);
    while (entry != null) {
      if (entry.tag == tag
          && entry.hashCode == hashCode
          && entry.owner.equals(owner)
          && entry.name.equals(name)
          && entry.value.equals(descriptor)) {
        return entry;
      }
      entry = entry.next;
    }
    constantPool.put122(
        tag, addConstantClass(owner).index, addConstantNameAndType(name, descriptor));
    return put(new Entry(constantPoolCount++, tag, owner, name, descriptor, 0, hashCode));
  }

  /**
   * Adds a new CONSTANT_Fieldref_info, CONSTANT_Methodref_info or CONSTANT_InterfaceMethodref_info
   * to the constant pool of this symbol table.
   *
   * @param index the constant pool index of the new Symbol.
   * @param tag one of {@link Symbol#CONSTANT_FIELDREF_TAG}, {@link Symbol#CONSTANT_METHODREF_TAG}
   *     or {@link Symbol#CONSTANT_INTERFACE_METHODREF_TAG}.
   * @param owner the internal name of a class.
   * @param name a field or method name.
   * @param descriptor a field or method descriptor.
   */
  private void addConstantMemberReference(
      final int index,
      final int tag,
      final String owner,
      final String name,
      final String descriptor) {
    add(new Entry(index, tag, owner, name, descriptor, 0, hash(tag, owner, name, descriptor)));
  }

  /**
   * Adds a CONSTANT_String_info to the constant pool of this symbol table. Does nothing if the
   * constant pool already contains a similar item.
   *
   * @param value a string.
   * @return a new or already existing Symbol with the given value.
   */
  Symbol addConstantString(final String value) {
    return addConstantUtf8Reference(Symbol.CONSTANT_STRING_TAG, value);
  }

  /**
   * Adds a CONSTANT_Integer_info to the constant pool of this symbol table. Does nothing if the
   * constant pool already contains a similar item.
   *
   * @param value an int.
   * @return a new or already existing Symbol with the given value.
   */
  Symbol addConstantInteger(final int value) {
    return addConstantIntegerOrFloat(Symbol.CONSTANT_INTEGER_TAG, value);
  }

  /**
   * Adds a CONSTANT_Float_info to the constant pool of this symbol table. Does nothing if the
   * constant pool already contains a similar item.
   *
   * @param value a float.
   * @return a new or already existing Symbol with the given value.
   */
  Symbol addConstantFloat(final float value) {
    return addConstantIntegerOrFloat(Symbol.CONSTANT_FLOAT_TAG, Float.floatToRawIntBits(value));
  }

  /**
   * Adds a CONSTANT_Integer_info or CONSTANT_Float_info to the constant pool of this symbol table.
   * Does nothing if the constant pool already contains a similar item.
   *
   * @param tag one of {@link Symbol#CONSTANT_INTEGER_TAG} or {@link Symbol#CONSTANT_FLOAT_TAG}.
   * @param value an int or float.
   * @return a constant pool constant with the given tag and primitive values.
   */
  private Symbol addConstantIntegerOrFloat(final int tag, final int value) {
    int hashCode = hash(tag, value);
    Entry entry = get(hashCode);
    while (entry != null) {
      if (entry.tag == tag && entry.hashCode == hashCode && entry.data == value) {
        return entry;
      }
      entry = entry.next;
    }
    constantPool.putByte(tag).putInt(value);
    return put(new Entry(constantPoolCount++, tag, value, hashCode));
  }

  /**
   * Adds a new CONSTANT_Integer_info or CONSTANT_Float_info to the constant pool of this symbol
   * table.
   *
   * @param index the constant pool index of the new Symbol.
   * @param tag one of {@link Symbol#CONSTANT_INTEGER_TAG} or {@link Symbol#CONSTANT_FLOAT_TAG}.
   * @param value an int or float.
   */
  private void addConstantIntegerOrFloat(final int index, final int tag, final int value) {
    add(new Entry(index, tag, value, hash(tag, value)));
  }

  /**
   * Adds a CONSTANT_Long_info to the constant pool of this symbol table. Does nothing if the
   * constant pool already contains a similar item.
   *
   * @param value a long.
   * @return a new or already existing Symbol with the given value.
   */
  Symbol addConstantLong(final long value) {
    return addConstantLongOrDouble(Symbol.CONSTANT_LONG_TAG, value);
  }

  /**
   * Adds a CONSTANT_Double_info to the constant pool of this symbol table. Does nothing if the
   * constant pool already contains a similar item.
   *
   * @param value a double.
   * @return a new or already existing Symbol with the given value.
   */
  Symbol addConstantDouble(final double value) {
    return addConstantLongOrDouble(Symbol.CONSTANT_DOUBLE_TAG, Double.doubleToRawLongBits(value));
  }

  /**
   * Adds a CONSTANT_Long_info or CONSTANT_Double_info to the constant pool of this symbol table.
   * Does nothing if the constant pool already contains a similar item.
   *
   * @param tag one of {@link Symbol#CONSTANT_LONG_TAG} or {@link Symbol#CONSTANT_DOUBLE_TAG}.
   * @param value a long or double.
   * @return a constant pool constant with the given tag and primitive values.
   */
  private Symbol addConstantLongOrDouble(final int tag, final long value) {
    int hashCode = hash(tag, value);
    Entry entry = get(hashCode);
    while (entry != null) {
      if (entry.tag == tag && entry.hashCode == hashCode && entry.data == value) {
        return entry;
      }
      entry = entry.next;
    }
    int index = constantPoolCount;
    constantPool.putByte(tag).putLong(value);
    constantPoolCount += 2;
    return put(new Entry(index, tag, value, hashCode));
  }

  /**
   * Adds a new CONSTANT_Long_info or CONSTANT_Double_info to the constant pool of this symbol
   * table.
   *
   * @param index the constant pool index of the new Symbol.
   * @param tag one of {@link Symbol#CONSTANT_LONG_TAG} or {@link Symbol#CONSTANT_DOUBLE_TAG}.
   * @param value a long or double.
   */
  private void addConstantLongOrDouble(final int index, final int tag, final long value) {
    add(new Entry(index, tag, value, hash(tag, value)));
  }

  /**
   * Adds a CONSTANT_NameAndType_info to the constant pool of this symbol table. Does nothing if the
   * constant pool already contains a similar item.
   *
   * @param name a field or method name.
   * @param descriptor a field or method descriptor.
   * @return a new or already existing Symbol with the given value.
   */
  int addConstantNameAndType(final String name, final String descriptor) {
    final int tag = Symbol.CONSTANT_NAME_AND_TYPE_TAG;
    int hashCode = hash(tag, name, descriptor);
    Entry entry = get(hashCode);
    while (entry != null) {
      if (entry.tag == tag
          && entry.hashCode == hashCode
          && entry.name.equals(name)
          && entry.value.equals(descriptor)) {
        return entry.index;
      }
      entry = entry.next;
    }
    constantPool.put122(tag, addConstantUtf8(name), addConstantUtf8(descriptor));
    return put(new Entry(constantPoolCount++, tag, name, descriptor, hashCode)).index;
  }

  /**
   * Adds a new CONSTANT_NameAndType_info to the constant pool of this symbol table.
   *
   * @param index the constant pool index of the new Symbol.
   * @param name a field or method name.
   * @param descriptor a field or method descriptor.
   */
  private void addConstantNameAndType(final int index, final String name, final String descriptor) {
    final int tag = Symbol.CONSTANT_NAME_AND_TYPE_TAG;
    add(new Entry(index, tag, name, descriptor, hash(tag, name, descriptor)));
  }

  /**
   * Adds a CONSTANT_Utf8_info to the constant pool of this symbol table. Does nothing if the
   * constant pool already contains a similar item.
   *
   * @param value a string.
   * @return a new or already existing Symbol with the given value.
   */
  int addConstantUtf8(final String value) {
    int hashCode = hash(Symbol.CONSTANT_UTF8_TAG, value);
    Entry entry = get(hashCode);
    while (entry != null) {
      if (entry.tag == Symbol.CONSTANT_UTF8_TAG
          && entry.hashCode == hashCode
          && entry.value.equals(value)) {
        return entry.index;
      }
      entry = entry.next;
    }
    constantPool.putByte(Symbol.CONSTANT_UTF8_TAG).putUTF8(value);
    return put(new Entry(constantPoolCount++, Symbol.CONSTANT_UTF8_TAG, value, hashCode)).index;
  }

  /**
   * Adds a new CONSTANT_String_info to the constant pool of this symbol table.
   *
   * @param index the constant pool index of the new Symbol.
   * @param value a string.
   */
  private void addConstantUtf8(final int index, final String value) {
    add(new Entry(index, Symbol.CONSTANT_UTF8_TAG, value, hash(Symbol.CONSTANT_UTF8_TAG, value)));
  }

  /**
   * Adds a CONSTANT_MethodHandle_info to the constant pool of this symbol table. Does nothing if
   * the constant pool already contains a similar item.
   *
   * @param referenceKind one of {@link Opcodes#H_GETFIELD}, {@link Opcodes#H_GETSTATIC}, {@link
   *     Opcodes#H_PUTFIELD}, {@link Opcodes#H_PUTSTATIC}, {@link Opcodes#H_INVOKEVIRTUAL}, {@link
   *     Opcodes#H_INVOKESTATIC}, {@link Opcodes#H_INVOKESPECIAL}, {@link
   *     Opcodes#H_NEWINVOKESPECIAL} or {@link Opcodes#H_INVOKEINTERFACE}.
   * @param owner the internal name of a class of interface.
   * @param name a field or method name.
   * @param descriptor a field or method descriptor.
   * @param isInterface whether owner is an interface or not.
   * @return a new or already existing Symbol with the given value.
   */
  Symbol addConstantMethodHandle(
      final int referenceKind,
      final String owner,
      final String name,
      final String descriptor,
      final boolean isInterface) {
    final int tag = Symbol.CONSTANT_METHOD_HANDLE_TAG;
    // Note that we don't need to include isInterface in the hash computation, because it is
    // redundant with owner (we can't have the same owner with different isInterface values).
    int hashCode = hash(tag, owner, name, descriptor, referenceKind);
    Entry entry = get(hashCode);
    while (entry != null) {
      if (entry.tag == tag
          && entry.hashCode == hashCode
          && entry.data == referenceKind
          && entry.owner.equals(owner)
          && entry.name.equals(name)
          && entry.value.equals(descriptor)) {
        return entry;
      }
      entry = entry.next;
    }
    if (referenceKind <= Opcodes.H_PUTSTATIC) {
      constantPool.put112(tag, referenceKind, addConstantFieldref(owner, name, descriptor).index);
    } else {
      constantPool.put112(
          tag, referenceKind, addConstantMethodref(owner, name, descriptor, isInterface).index);
    }
    return put(
        new Entry(constantPoolCount++, tag, owner, name, descriptor, referenceKind, hashCode));
  }

  /**
   * Adds a new CONSTANT_MethodHandle_info to the constant pool of this symbol table.
   *
   * @param index the constant pool index of the new Symbol.
   * @param referenceKind one of {@link Opcodes#H_GETFIELD}, {@link Opcodes#H_GETSTATIC}, {@link
   *     Opcodes#H_PUTFIELD}, {@link Opcodes#H_PUTSTATIC}, {@link Opcodes#H_INVOKEVIRTUAL}, {@link
   *     Opcodes#H_INVOKESTATIC}, {@link Opcodes#H_INVOKESPECIAL}, {@link
   *     Opcodes#H_NEWINVOKESPECIAL} or {@link Opcodes#H_INVOKEINTERFACE}.
   * @param owner the internal name of a class of interface.
   * @param name a field or method name.
   * @param descriptor a field or method descriptor.
   */
  private void addConstantMethodHandle(
      final int index,
      final int referenceKind,
      final String owner,
      final String name,
      final String descriptor) {
    final int tag = Symbol.CONSTANT_METHOD_HANDLE_TAG;
    int hashCode = hash(tag, owner, name, descriptor, referenceKind);
    add(new Entry(index, tag, owner, name, descriptor, referenceKind, hashCode));
  }

  /**
   * Adds a CONSTANT_MethodType_info to the constant pool of this symbol table. Does nothing if the
   * constant pool already contains a similar item.
   *
   * @param methodDescriptor a method descriptor.
   * @return a new or already existing Symbol with the given value.
   */
  Symbol addConstantMethodType(final String methodDescriptor) {
    return addConstantUtf8Reference(Symbol.CONSTANT_METHOD_TYPE_TAG, methodDescriptor);
  }

  /**
   * Adds a CONSTANT_Dynamic_info to the constant pool of this symbol table. Also adds the related
   * bootstrap method to the BootstrapMethods of this symbol table. Does nothing if the constant
   * pool already contains a similar item.
   *
   * @param name a method name.
   * @param descriptor a field descriptor.
   * @param bootstrapMethodHandle a bootstrap method handle.
   * @param bootstrapMethodArguments the bootstrap method arguments.
   * @return a new or already existing Symbol with the given value.
   */
  Symbol addConstantDynamic(
      final String name,
      final String descriptor,
      final Handle bootstrapMethodHandle,
      final Object... bootstrapMethodArguments) {
    Symbol bootstrapMethod = addBootstrapMethod(bootstrapMethodHandle, bootstrapMethodArguments);
    return addConstantDynamicOrInvokeDynamicReference(
        Symbol.CONSTANT_DYNAMIC_TAG, name, descriptor, bootstrapMethod.index);
  }

  /**
   * Adds a CONSTANT_InvokeDynamic_info to the constant pool of this symbol table. Also adds the
   * related bootstrap method to the BootstrapMethods of this symbol table. Does nothing if the
   * constant pool already contains a similar item.
   *
   * @param name a method name.
   * @param descriptor a method descriptor.
   * @param bootstrapMethodHandle a bootstrap method handle.
   * @param bootstrapMethodArguments the bootstrap method arguments.
   * @return a new or already existing Symbol with the given value.
   */
  Symbol addConstantInvokeDynamic(
      final String name,
      final String descriptor,
      final Handle bootstrapMethodHandle,
      final Object... bootstrapMethodArguments) {
    Symbol bootstrapMethod = addBootstrapMethod(bootstrapMethodHandle, bootstrapMethodArguments);
    return addConstantDynamicOrInvokeDynamicReference(
        Symbol.CONSTANT_INVOKE_DYNAMIC_TAG, name, descriptor, bootstrapMethod.index);
  }

  /**
   * Adds a CONSTANT_Dynamic or a CONSTANT_InvokeDynamic_info to the constant pool of this symbol
   * table. Does nothing if the constant pool already contains a similar item.
   *
   * @param tag one of {@link Symbol#CONSTANT_DYNAMIC_TAG} or {@link
   *     Symbol#CONSTANT_INVOKE_DYNAMIC_TAG}.
   * @param name a method name.
   * @param descriptor a field descriptor for CONSTANT_DYNAMIC_TAG) or a method descriptor for
   *     CONSTANT_INVOKE_DYNAMIC_TAG.
   * @param bootstrapMethodIndex the index of a bootstrap method in the BootstrapMethods attribute.
   * @return a new or already existing Symbol with the given value.
   */
  private Symbol addConstantDynamicOrInvokeDynamicReference(
      final int tag, final String name, final String descriptor, final int bootstrapMethodIndex) {
    int hashCode = hash(tag, name, descriptor, bootstrapMethodIndex);
    Entry entry = get(hashCode);
    while (entry != null) {
      if (entry.tag == tag
          && entry.hashCode == hashCode
          && entry.data == bootstrapMethodIndex
          && entry.name.equals(name)
          && entry.value.equals(descriptor)) {
        return entry;
      }
      entry = entry.next;
    }
    constantPool.put122(tag, bootstrapMethodIndex, addConstantNameAndType(name, descriptor));
    return put(
        new Entry(
            constantPoolCount++, tag, null, name, descriptor, bootstrapMethodIndex, hashCode));
  }

  /**
   * Adds a new CONSTANT_Dynamic_info or CONSTANT_InvokeDynamic_info to the constant pool of this
   * symbol table.
   *
   * @param tag one of {@link Symbol#CONSTANT_DYNAMIC_TAG} or {@link
   *     Symbol#CONSTANT_INVOKE_DYNAMIC_TAG}.
   * @param index the constant pool index of the new Symbol.
   * @param name a method name.
   * @param descriptor a field descriptor for CONSTANT_DYNAMIC_TAG or a method descriptor for
   *     CONSTANT_INVOKE_DYNAMIC_TAG.
   * @param bootstrapMethodIndex the index of a bootstrap method in the BootstrapMethods attribute.
   */
  private void addConstantDynamicOrInvokeDynamicReference(
      final int tag,
      final int index,
      final String name,
      final String descriptor,
      final int bootstrapMethodIndex) {
    int hashCode = hash(tag, name, descriptor, bootstrapMethodIndex);
    add(new Entry(index, tag, null, name, descriptor, bootstrapMethodIndex, hashCode));
  }

  /**
   * Adds a CONSTANT_Module_info to the constant pool of this symbol table. Does nothing if the
   * constant pool already contains a similar item.
   *
   * @param moduleName a fully qualified name (using dots) of a module.
   * @return a new or already existing Symbol with the given value.
   */
  Symbol addConstantModule(final String moduleName) {
    return addConstantUtf8Reference(Symbol.CONSTANT_MODULE_TAG, moduleName);
  }

  /**
   * Adds a CONSTANT_Package_info to the constant pool of this symbol table. Does nothing if the
   * constant pool already contains a similar item.
   *
   * @param packageName the internal name of a package.
   * @return a new or already existing Symbol with the given value.
   */
  Symbol addConstantPackage(final String packageName) {
    return addConstantUtf8Reference(Symbol.CONSTANT_PACKAGE_TAG, packageName);
  }

  /**
   * Adds a CONSTANT_Class_info, CONSTANT_String_info, CONSTANT_MethodType_info,
   * CONSTANT_Module_info or CONSTANT_Package_info to the constant pool of this symbol table. Does
   * nothing if the constant pool already contains a similar item.
   *
   * @param tag one of {@link Symbol#CONSTANT_CLASS_TAG}, {@link Symbol#CONSTANT_STRING_TAG}, {@link
   *     Symbol#CONSTANT_METHOD_TYPE_TAG}, {@link Symbol#CONSTANT_MODULE_TAG} or {@link
   *     Symbol#CONSTANT_PACKAGE_TAG}.
   * @param value an internal class name, an arbitrary string, a method descriptor, a module or a
   *     package name, depending on tag.
   * @return a new or already existing Symbol with the given value.
   */
  private Symbol addConstantUtf8Reference(final int tag, final String value) {
    int hashCode = hash(tag, value);
    Entry entry = get(hashCode);
    while (entry != null) {
      if (entry.tag == tag && entry.hashCode == hashCode && entry.value.equals(value)) {
        return entry;
      }
      entry = entry.next;
    }
    constantPool.put12(tag, addConstantUtf8(value));
    return put(new Entry(constantPoolCount++, tag, value, hashCode));
  }

  /**
   * Adds a new CONSTANT_Class_info, CONSTANT_String_info, CONSTANT_MethodType_info,
   * CONSTANT_Module_info or CONSTANT_Package_info to the constant pool of this symbol table.
   *
   * @param index the constant pool index of the new Symbol.
   * @param tag one of {@link Symbol#CONSTANT_CLASS_TAG}, {@link Symbol#CONSTANT_STRING_TAG}, {@link
   *     Symbol#CONSTANT_METHOD_TYPE_TAG}, {@link Symbol#CONSTANT_MODULE_TAG} or {@link
   *     Symbol#CONSTANT_PACKAGE_TAG}.
   * @param value an internal class name, an arbitrary string, a method descriptor, a module or a
   *     package name, depending on tag.
   */
  private void addConstantUtf8Reference(final int index, final int tag, final String value) {
    add(new Entry(index, tag, value, hash(tag, value)));
  }

  // -----------------------------------------------------------------------------------------------
  // Bootstrap method entries management.
  // -----------------------------------------------------------------------------------------------

  /**
   * Adds a bootstrap method to the BootstrapMethods attribute of this symbol table. Does nothing if
   * the BootstrapMethods already contains a similar bootstrap method.
   *
   * @param bootstrapMethodHandle a bootstrap method handle.
   * @param bootstrapMethodArguments the bootstrap method arguments.
   * @return a new or already existing Symbol with the given value.
   */
  Symbol addBootstrapMethod(
      final Handle bootstrapMethodHandle, final Object... bootstrapMethodArguments) {
    ByteVector bootstrapMethodsAttribute = bootstrapMethods;
    if (bootstrapMethodsAttribute == null) {
      bootstrapMethodsAttribute = bootstrapMethods = new ByteVector();
    }

    // The bootstrap method arguments can be Constant_Dynamic values, which reference other
    // bootstrap methods. We must therefore add the bootstrap method arguments to the constant pool
    // and BootstrapMethods attribute first, so that the BootstrapMethods attribute is not modified
    // while adding the given bootstrap method to it, in the rest of this method.
    for (Object bootstrapMethodArgument : bootstrapMethodArguments) {
      addConstant(bootstrapMethodArgument);
    }

    // Write the bootstrap method in the BootstrapMethods table. This is necessary to be able to
    // compare it with existing ones, and will be reverted below if there is already a similar
    // bootstrap method.
    int bootstrapMethodOffset = bootstrapMethodsAttribute.length;
    bootstrapMethodsAttribute.putShort(
        addConstantMethodHandle(
                bootstrapMethodHandle.getTag(),
                bootstrapMethodHandle.getOwner(),
                bootstrapMethodHandle.getName(),
                bootstrapMethodHandle.getDesc(),
                bootstrapMethodHandle.isInterface())
            .index);
    int numBootstrapArguments = bootstrapMethodArguments.length;
    bootstrapMethodsAttribute.putShort(numBootstrapArguments);
    for (Object bootstrapMethodArgument : bootstrapMethodArguments) {
      bootstrapMethodsAttribute.putShort(addConstant(bootstrapMethodArgument).index);
    }

    // Compute the length and the hash code of the bootstrap method.
    int bootstrapMethodlength = bootstrapMethodsAttribute.length - bootstrapMethodOffset;
    int hashCode = bootstrapMethodHandle.hashCode();
    for (Object bootstrapMethodArgument : bootstrapMethodArguments) {
      hashCode ^= bootstrapMethodArgument.hashCode();
    }
    hashCode &= 0x7FFFFFFF;

    // Add the bootstrap method to the symbol table or revert the above changes.
    return addBootstrapMethod(bootstrapMethodOffset, bootstrapMethodlength, hashCode);
  }

  /**
   * Adds a bootstrap method to the BootstrapMethods attribute of this symbol table. Does nothing if
   * the BootstrapMethods already contains a similar bootstrap method (more precisely, reverts the
   * content of {@link #bootstrapMethods} to remove the last, duplicate bootstrap method).
   *
   * @param offset the offset of the last bootstrap method in {@link #bootstrapMethods}, in bytes.
   * @param length the length of this bootstrap method in {@link #bootstrapMethods}, in bytes.
   * @param hashCode the hash code of this bootstrap method.
   * @return a new or already existing Symbol with the given value.
   */
  private Symbol addBootstrapMethod(final int offset, final int length, final int hashCode) {
    final byte[] bootstrapMethodsData = bootstrapMethods.data;
    Entry entry = get(hashCode);
    while (entry != null) {
      if (entry.tag == Symbol.BOOTSTRAP_METHOD_TAG && entry.hashCode == hashCode) {
        int otherOffset = (int) entry.data;
        boolean isSameBootstrapMethod = true;
        for (int i = 0; i < length; ++i) {
          if (bootstrapMethodsData[offset + i] != bootstrapMethodsData[otherOffset + i]) {
            isSameBootstrapMethod = false;
            break;
          }
        }
        if (isSameBootstrapMethod) {
          bootstrapMethods.length = offset; // Revert to old position.
          return entry;
        }
      }
      entry = entry.next;
    }
    return put(new Entry(bootstrapMethodCount++, Symbol.BOOTSTRAP_METHOD_TAG, offset, hashCode));
  }

  // -----------------------------------------------------------------------------------------------
  // Type table entries management.
  // -----------------------------------------------------------------------------------------------

  /**
   * Returns the type table element whose index is given.
   *
   * @param typeIndex a type table index.
   * @return the type table element whose index is given.
   */
  Symbol getType(final int typeIndex) {
    return typeTable[typeIndex];
  }

  /**
   * Adds a type in the type table of this symbol table. Does nothing if the type table already
   * contains a similar type.
   *
   * @param value an internal class name.
   * @return the index of a new or already existing type Symbol with the given value.
   */
  int addType(final String value) {
    int hashCode = hash(Symbol.TYPE_TAG, value);
    Entry entry = get(hashCode);
    while (entry != null) {
      if (entry.tag == Symbol.TYPE_TAG && entry.hashCode == hashCode && entry.value.equals(value)) {
        return entry.index;
      }
      entry = entry.next;
    }
    return addTypeInternal(new Entry(typeCount, Symbol.TYPE_TAG, value, hashCode));
  }

  /**
   * Adds an {@link Frame#ITEM_UNINITIALIZED} type in the type table of this symbol table. Does
   * nothing if the type table already contains a similar type.
   *
   * @param value an internal class name.
   * @param bytecodeOffset the bytecode offset of the NEW instruction that created this {@link
   *     Frame#ITEM_UNINITIALIZED} type value.
   * @return the index of a new or already existing type Symbol with the given value.
   */
  int addUninitializedType(final String value, final int bytecodeOffset) {
    int hashCode = hash(Symbol.UNINITIALIZED_TYPE_TAG, value, bytecodeOffset);
    Entry entry = get(hashCode);
    while (entry != null) {
      if (entry.tag == Symbol.UNINITIALIZED_TYPE_TAG
          && entry.hashCode == hashCode
          && entry.data == bytecodeOffset
          && entry.value.equals(value)) {
        return entry.index;
      }
      entry = entry.next;
    }
    return addTypeInternal(
        new Entry(typeCount, Symbol.UNINITIALIZED_TYPE_TAG, value, bytecodeOffset, hashCode));
  }

  /**
   * Adds a merged type in the type table of this symbol table. Does nothing if the type table
   * already contains a similar type.
   *
   * @param typeTableIndex1 a {@link Symbol#TYPE_TAG} type, specified by its index in the type
   *     table.
   * @param typeTableIndex2 another {@link Symbol#TYPE_TAG} type, specified by its index in the type
   *     table.
   * @return the index of a new or already existing {@link Symbol#TYPE_TAG} type Symbol,
   *     corresponding to the common super class of the given types.
   */
  int addMergedType(final int typeTableIndex1, final int typeTableIndex2) {
    // TODO sort the arguments? The merge result should be independent of their order.
    long data = typeTableIndex1 | (((long) typeTableIndex2) << 32);
    int hashCode = hash(Symbol.MERGED_TYPE_TAG, typeTableIndex1 + typeTableIndex2);
    Entry entry = get(hashCode);
    while (entry != null) {
      if (entry.tag == Symbol.MERGED_TYPE_TAG && entry.hashCode == hashCode && entry.data == data) {
        return entry.info;
      }
      entry = entry.next;
    }
    String type1 = typeTable[typeTableIndex1].value;
    String type2 = typeTable[typeTableIndex2].value;
    int commonSuperTypeIndex = addType(classWriter.getCommonSuperClass(type1, type2));
    put(new Entry(typeCount, Symbol.MERGED_TYPE_TAG, data, hashCode)).info = commonSuperTypeIndex;
    return commonSuperTypeIndex;
  }

  /**
   * Adds the given type Symbol to {@link #typeTable}.
   *
   * @param entry a {@link Symbol#TYPE_TAG} or {@link Symbol#UNINITIALIZED_TYPE_TAG} type symbol.
   *     The index of this Symbol must be equal to the current value of {@link #typeCount}.
   * @return the index in {@link #typeTable} where the given type was added, which is also equal to
   *     entry's index by hypothesis.
   */
  private int addTypeInternal(final Entry entry) {
    if (typeTable == null) {
      typeTable = new Entry[16];
    }
    if (typeCount == typeTable.length) {
      Entry[] newTypeTable = new Entry[2 * typeTable.length];
      System.arraycopy(typeTable, 0, newTypeTable, 0, typeTable.length);
      typeTable = newTypeTable;
    }
    typeTable[typeCount++] = entry;
    return put(entry).index;
  }

  // -----------------------------------------------------------------------------------------------
  // Static helper methods to compute hash codes.
  // -----------------------------------------------------------------------------------------------

  private static int hash(final int tag, final int value) {
    return 0x7FFFFFFF & (tag + value);
  }

  private static int hash(final int tag, final long value) {
    return 0x7FFFFFFF & (tag + (int) value + (int) (value >>> 32));
  }

  private static int hash(final int tag, final String value) {
    return 0x7FFFFFFF & (tag + value.hashCode());
  }

  private static int hash(final int tag, final String value1, final int value2) {
    return 0x7FFFFFFF & (tag + value1.hashCode() + value2);
  }

  private static int hash(final int tag, final String value1, final String value2) {
    return 0x7FFFFFFF & (tag + value1.hashCode() * value2.hashCode());
  }

  private static int hash(
      final int tag, final String value1, final String value2, final int value3) {
    return 0x7FFFFFFF & (tag + value1.hashCode() * value2.hashCode() * (value3 + 1));
  }

  private static int hash(
      final int tag, final String value1, final String value2, final String value3) {
    return 0x7FFFFFFF & (tag + value1.hashCode() * value2.hashCode() * value3.hashCode());
  }

  private static int hash(
      final int tag,
      final String value1,
      final String value2,
      final String value3,
      final int value4) {
    return 0x7FFFFFFF & (tag + value1.hashCode() * value2.hashCode() * value3.hashCode() * value4);
  }

  /**
   * An entry of a SymbolTable. This concrete and private subclass of {@link Symbol} adds two fields
   * which are only used inside SymbolTable, to implement hash sets of symbols (in order to avoid
   * duplicate symbols). See {@link #entries}.
   *
   * @author Eric Bruneton
   */
  private static class Entry extends Symbol {

    /** The hash code of this entry. */
    final int hashCode;

    /**
     * Another entry (and so on recursively) having the same hash code (modulo the size of {@link
     * #entries}) as this one.
     */
    Entry next;

    Entry(
        final int index,
        final int tag,
        final String owner,
        final String name,
        final String value,
        final long data,
        final int hashCode) {
      super(index, tag, owner, name, value, data);
      this.hashCode = hashCode;
    }

    Entry(final int index, final int tag, final String value, final int hashCode) {
      super(index, tag, /* owner = */ null, /* name = */ null, value, /* data = */ 0);
      this.hashCode = hashCode;
    }

    Entry(final int index, final int tag, final String value, final long data, final int hashCode) {
      super(index, tag, /* owner = */ null, /* name = */ null, value, data);
      this.hashCode = hashCode;
    }

    Entry(
        final int index, final int tag, final String name, final String value, final int hashCode) {
      super(index, tag, /* owner = */ null, name, value, /* data = */ 0);
      this.hashCode = hashCode;
    }

    Entry(final int index, final int tag, final long data, final int hashCode) {
      super(index, tag, /* owner = */ null, /* name = */ null, /* value = */ null, data);
      this.hashCode = hashCode;
    }
  }
}
