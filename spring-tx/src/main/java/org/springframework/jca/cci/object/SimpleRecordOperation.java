/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.jca.cci.object;

import javax.resource.cci.ConnectionFactory;
import javax.resource.cci.InteractionSpec;
import javax.resource.cci.Record;

import org.springframework.dao.DataAccessException;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * EIS operation object that accepts a passed-in CCI input Record
 * and returns a corresponding CCI output Record.
 *
 * @author Juergen Hoeller
 * @since 1.2
 * @deprecated as of 5.3, in favor of specific data access APIs
 * (or native CCI usage if there is no alternative)
 */
@Deprecated
public class SimpleRecordOperation extends EisOperation {

	/**
	 * Constructor that allows use as a JavaBean.
	 */
	public SimpleRecordOperation() {
	}

	/**
	 * Convenient constructor with ConnectionFactory and specifications
	 * (connection and interaction).
	 * @param connectionFactory the ConnectionFactory to use to obtain connections
	 */
	public SimpleRecordOperation(ConnectionFactory connectionFactory, InteractionSpec interactionSpec) {
		getCciTemplate().setConnectionFactory(connectionFactory);
		setInteractionSpec(interactionSpec);
	}


	/**
	 * Execute the CCI interaction encapsulated by this operation object.
	 * <p>This method will call CCI's {@code Interaction.execute} variant
	 * that returns an output Record.
	 * @param inputRecord the input record
	 * @return the output record
	 * @throws DataAccessException if there is any problem
	 * @see javax.resource.cci.Interaction#execute(javax.resource.cci.InteractionSpec, Record)
	 */
	@Nullable
	public Record execute(Record inputRecord) throws DataAccessException {
		InteractionSpec interactionSpec = getInteractionSpec();
		Assert.state(interactionSpec != null, "No InteractionSpec set");
		return getCciTemplate().execute(interactionSpec, inputRecord);
	}

	/**
	 * Execute the CCI interaction encapsulated by this operation object.
	 * <p>This method will call CCI's {@code Interaction.execute} variant
	 * with a passed-in output Record.
	 * @param inputRecord the input record
	 * @param outputRecord the output record
	 * @throws DataAccessException if there is any problem
	 * @see javax.resource.cci.Interaction#execute(javax.resource.cci.InteractionSpec, Record, Record)
	 */
	public void execute(Record inputRecord, Record outputRecord) throws DataAccessException {
		InteractionSpec interactionSpec = getInteractionSpec();
		Assert.state(interactionSpec != null, "No InteractionSpec set");
		getCciTemplate().execute(interactionSpec, inputRecord, outputRecord);
	}

}
