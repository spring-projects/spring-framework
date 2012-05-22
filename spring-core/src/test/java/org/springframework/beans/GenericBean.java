/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.beans;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.core.io.Resource;

/**
 * @author Juergen Hoeller
 */
public class GenericBean<T> {

	private Set<Integer> integerSet;

	private List<Resource> resourceList;

	private List<List<Integer>> listOfLists;

	private ArrayList<String[]> listOfArrays;

	private List<Map<Integer, Long>> listOfMaps;

	@SuppressWarnings("rawtypes")
	private Map plainMap;

	private Map<Short, Integer> shortMap;

	private HashMap<Long, ?> longMap;

	private Map<Number, Collection<? extends Object>> collectionMap;

	private Map<String, Map<Integer, Long>> mapOfMaps;

	private Map<Integer, List<Integer>> mapOfLists;

	private CustomEnum customEnum;

	private T genericProperty;

	private List<T> genericListProperty;


	public GenericBean() {
	}

	public GenericBean(Set<Integer> integerSet) {
		this.integerSet = integerSet;
	}

	public GenericBean(Set<Integer> integerSet, List<Resource> resourceList) {
		this.integerSet = integerSet;
		this.resourceList = resourceList;
	}

	public GenericBean(HashSet<Integer> integerSet, Map<Short, Integer> shortMap) {
		this.integerSet = integerSet;
		this.shortMap = shortMap;
	}

	public GenericBean(Map<Short, Integer> shortMap, Resource resource) {
		this.shortMap = shortMap;
		this.resourceList = Collections.singletonList(resource);
	}

	@SuppressWarnings("rawtypes")
	public GenericBean(Map plainMap, Map<Short, Integer> shortMap) {
		this.plainMap = plainMap;
		this.shortMap = shortMap;
	}

	public GenericBean(HashMap<Long, ?> longMap) {
		this.longMap = longMap;
	}

	public GenericBean(boolean someFlag, Map<Number, Collection<? extends Object>> collectionMap) {
		this.collectionMap = collectionMap;
	}


	public Set<Integer> getIntegerSet() {
		return integerSet;
	}

	public void setIntegerSet(Set<Integer> integerSet) {
		this.integerSet = integerSet;
	}

	public List<Resource> getResourceList() {
		return resourceList;
	}

	public void setResourceList(List<Resource> resourceList) {
		this.resourceList = resourceList;
	}

	public List<List<Integer>> getListOfLists() {
		return listOfLists;
	}

	public ArrayList<String[]> getListOfArrays() {
		return listOfArrays;
	}

	public void setListOfArrays(ArrayList<String[]> listOfArrays) {
		this.listOfArrays = listOfArrays;
	}

	public void setListOfLists(List<List<Integer>> listOfLists) {
		this.listOfLists = listOfLists;
	}

	public List<Map<Integer, Long>> getListOfMaps() {
		return listOfMaps;
	}

	public void setListOfMaps(List<Map<Integer, Long>> listOfMaps) {
		this.listOfMaps = listOfMaps;
	}

	@SuppressWarnings("rawtypes")
	public Map getPlainMap() {
		return plainMap;
	}

	public Map<Short, Integer> getShortMap() {
		return shortMap;
	}

	public void setShortMap(Map<Short, Integer> shortMap) {
		this.shortMap = shortMap;
	}

	public HashMap<Long, ?> getLongMap() {
		return longMap;
	}

	public void setLongMap(HashMap<Long, ?> longMap) {
		this.longMap = longMap;
	}

	public Map<Number, Collection<? extends Object>> getCollectionMap() {
		return collectionMap;
	}

	public void setCollectionMap(Map<Number, Collection<? extends Object>> collectionMap) {
		this.collectionMap = collectionMap;
	}

	public Map<String, Map<Integer, Long>> getMapOfMaps() {
		return mapOfMaps;
	}

	public void setMapOfMaps(Map<String, Map<Integer, Long>> mapOfMaps) {
		this.mapOfMaps = mapOfMaps;
	}

	public Map<Integer, List<Integer>> getMapOfLists() {
		return mapOfLists;
	}

	public void setMapOfLists(Map<Integer, List<Integer>> mapOfLists) {
		this.mapOfLists = mapOfLists;
	}

	public T getGenericProperty() {
		return genericProperty;
	}

	public void setGenericProperty(T genericProperty) {
		this.genericProperty = genericProperty;
	}

	public List<T> getGenericListProperty() {
		return genericListProperty;
	}

	public void setGenericListProperty(List<T> genericListProperty) {
		this.genericListProperty = genericListProperty;
	}

	public CustomEnum getCustomEnum() {
		return customEnum;
	}

	public void setCustomEnum(CustomEnum customEnum) {
		this.customEnum = customEnum;
	}


	public static GenericBean<Set<Integer>> createInstance(Set<Integer> integerSet) {
		return new GenericBean<Set<Integer>>(integerSet);
	}

	public static GenericBean<Set<Integer>> createInstance(Set<Integer> integerSet, List<Resource> resourceList) {
		return new GenericBean<Set<Integer>>(integerSet, resourceList);
	}

	public static GenericBean<HashSet<Integer>> createInstance(HashSet<Integer> integerSet, Map<Short, Integer> shortMap) {
		return new GenericBean<HashSet<Integer>>(integerSet, shortMap);
	}

	public static GenericBean<Map<String, Integer>> createInstance(Map<Short, Integer> shortMap, Resource resource) {
		return new GenericBean<Map<String, Integer>>(shortMap, resource);
	}

	@SuppressWarnings("rawtypes")
	public static GenericBean<Map> createInstance(Map map, Map<Short, Integer> shortMap) {
		return new GenericBean<Map>(map, shortMap);
	}

	public static GenericBean<Set<Integer>> createInstance(HashMap<Long, ?> longMap) {
		return new GenericBean<Set<Integer>>(longMap);
	}

	public static GenericBean<Set<Integer>> createInstance(boolean someFlag, Map<Number, Collection<? extends Object>> collectionMap) {
		return new GenericBean<Set<Integer>>(someFlag, collectionMap);
	}

}
