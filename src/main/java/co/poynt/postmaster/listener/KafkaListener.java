package co.poynt.postmaster.listener;

import co.poynt.postmaster.exception.ListenerInitializationException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class KafkaListener implements IListener {

	private static final int MESSAGE_QUEUE_SIZE = 100;

	/**
	 * topic to incoming messages map
	 */
	private Map<String, BlockingQueue<String>> incomingMessages;
	private List<KafkaSimpleConsumer> consumers;
	private ExecutorService threadPool;

	public KafkaListener(String kafka, Set<String> topics) {
		incomingMessages = new HashMap<>();
		topics.forEach(t -> incomingMessages.put(t, new ArrayBlockingQueue<>(MESSAGE_QUEUE_SIZE)));

		consumers = new ArrayList<>();

		threadPool = Executors.newFixedThreadPool(topics.size());
		startKafkaConsumer(kafka, topics);
	}

	/**
	 * Starts the Kafka consumers to keep track of the incoming messages
	 * per topic
	 *
	 * @param kafka kafka hostname for Kafka
	 * @param topics set of topics to consume the messages from
	 */
	private void startKafkaConsumer(String kafka, Set<String> topics) {
		// kafka settings
		String hostname;
		int port;
		try {
			hostname = kafka.split(":")[0];
			port = Integer.valueOf(kafka.split(":")[1]);
		} catch (ArrayIndexOutOfBoundsException | NumberFormatException e) {
			throw new ListenerInitializationException("Invalid listener url", e);
		}

		// TODO only works with single partition topic
		topics.forEach(t -> consumers.add(new KafkaSimpleConsumer(hostname, port, t, 0, incomingMessages.get(t))));
		consumers.forEach(threadPool::submit);
	}

	@Override
	public String getMessage(String topic, String matcher, int timeout) {
		MessageSearcher searcher = new MessageSearcher(incomingMessages.get(topic));
		return searcher.getMessage(matcher, timeout);
	}

	@Override
	public void clean() {
		consumers.forEach(KafkaSimpleConsumer::shutdown);
		threadPool.shutdownNow();
	}
}
