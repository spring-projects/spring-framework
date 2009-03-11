/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.jdbc.object;

/**
 * Concrete implementation making it possible to define the RDBMS stored procedures
 * in an application context without writing a custom Java implementation class.
 * <p>
 * This implementation does not provide a typed method for invocation so executions
 * must use one of the generic {@link StoredProcedure#execute(java.util.Map)} or
 * {@link StoredProcedure#execute(org.springframework.jdbc.core.ParameterMapper)} methods.
 *
 * @author Thomas Risberg
 * @see org.springframework.jdbc.object.StoredProcedure
 */
public class GenericStoredProcedure extends StoredProcedure {

}
