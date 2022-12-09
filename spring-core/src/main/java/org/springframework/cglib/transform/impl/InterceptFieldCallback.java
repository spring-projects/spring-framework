/*
 * Copyright 2003 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.cglib.transform.impl;

/**
 * @author Juozas Baliuka
 */
public interface InterceptFieldCallback {

    int writeInt(Object obj, String name, int oldValue, int newValue);
    char writeChar(Object obj, String name, char oldValue, char newValue);
    byte writeByte(Object obj, String name, byte oldValue, byte newValue);
    boolean writeBoolean(Object obj, String name, boolean oldValue, boolean newValue);
    short writeShort(Object obj, String name, short oldValue, short newValue);
    float writeFloat(Object obj, String name, float oldValue, float newValue);
    double writeDouble(Object obj, String name, double oldValue, double newValue);
    long writeLong(Object obj, String name, long oldValue, long newValue);
    Object writeObject(Object obj, String name, Object oldValue, Object newValue);

    int readInt(Object obj, String name, int oldValue);
    char readChar(Object obj, String name, char oldValue);
    byte readByte(Object obj, String name, byte oldValue);
    boolean readBoolean(Object obj, String name, boolean oldValue);
    short readShort(Object obj, String name, short oldValue);
    float readFloat(Object obj, String name, float oldValue);
    double readDouble(Object obj, String name, double oldValue);
    long readLong(Object obj, String name, long oldValue);
    Object readObject(Object obj, String name, Object oldValue);
}
