/*
 * Copyright 2004 The Apache Software Foundation
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
 * @author Chris Nokleberg
 */
public class AbstractInterceptFieldCallback implements InterceptFieldCallback {
    
    public int writeInt(Object obj, String name, int oldValue, int newValue) { return newValue; }
    public char writeChar(Object obj, String name, char oldValue, char newValue) { return newValue; }
    public byte writeByte(Object obj, String name, byte oldValue, byte newValue) { return newValue; }
    public boolean writeBoolean(Object obj, String name, boolean oldValue, boolean newValue) { return newValue; }
    public short writeShort(Object obj, String name, short oldValue, short newValue) { return newValue; }
    public float writeFloat(Object obj, String name, float oldValue, float newValue) { return newValue; }
    public double writeDouble(Object obj, String name, double oldValue, double newValue) { return newValue; }
    public long writeLong(Object obj, String name, long oldValue, long newValue) { return newValue; }
    public Object writeObject(Object obj, String name, Object oldValue, Object newValue) { return newValue; }

    public int readInt(Object obj, String name, int oldValue) { return oldValue; }
    public char readChar(Object obj, String name, char oldValue) { return oldValue; }
    public byte readByte(Object obj, String name, byte oldValue) { return oldValue; }
    public boolean readBoolean(Object obj, String name, boolean oldValue) { return oldValue; }
    public short readShort(Object obj, String name, short oldValue) { return oldValue; }
    public float readFloat(Object obj, String name, float oldValue) { return oldValue; }
    public double readDouble(Object obj, String name, double oldValue) { return oldValue; }
    public long readLong(Object obj, String name, long oldValue) { return oldValue; }
    public Object readObject(Object obj, String name, Object oldValue) { return oldValue; }
}
