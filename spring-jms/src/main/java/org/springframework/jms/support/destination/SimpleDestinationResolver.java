/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jms.support.destination;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.jms.JMSException;
import jakarta.jms.Queue;
import jakarta.jms.Session;
import jakarta.jms.Topic;

/**
 * A simple {@link DestinationResolver} implementation for {@link Session}-based
 * destination resolution, caching {@link Queue} and {@link Topic} instances per
 * queue/topic name. In that sense, the destinations themselves also need to be
 * "simple": not Session-specific and therefore stable across an entire JMS setup.
 *
 * <p>This is the default resolver used by {@link org.springframework.jms.core.JmsClient}
 * and also {@link org.springframework.jms.core.JmsTemplate} and listener containers,
 * as of 7.0. For enforcing fresh resolution on every call, you may explicitly set a
 * {@link DynamicDestinationResolver} instead.
 *
 * @author Juergen Hoeller
 * @since 7.0
 * @see DynamicDestinationResolver
 * @see jakarta.jms.Session#createQueue
 * @see jakarta.jms.Session#createTopic
 */
public class SimpleDestinationResolver extends DynamicDestinationResolver implements CachingDestinationResolver {

	private final Map<String, Topic> topicCache = new ConcurrentHashMap<>(4);

	private final Map<String, Queue> queueCache = new ConcurrentHashMap<>(4);


	@Override
	protected Topic resolveTopic(Session session, String topicName) throws JMSException {
		Topic topic = this.topicCache.get(topicName);
		if (topic != null) {
			return topic;
		}
		topic = super.resolveTopic(session, topicName);
		Topic existing = this.topicCache.putIfAbsent(topicName, topic);
		return (existing != null ? existing : topic);
	}

	@Override
	protected Queue resolveQueue(Session session, String queueName) throws JMSException {
		Queue queue = this.queueCache.get(queueName);
		if (queue != null) {
			return queue;
		}
		queue = super.resolveQueue(session, queueName);
		Queue existing = this.queueCache.putIfAbsent(queueName, queue);
		return (existing != null ? existing : queue);
	}

	@Override
	public void removeFromCache(String destinationName) {
		this.topicCache.remove(destinationName);
		this.queueCache.remove(destinationName);
	}

	@Override
	public void clearCache() {
		this.topicCache.clear();
		this.queueCache.clear();
	}

}
