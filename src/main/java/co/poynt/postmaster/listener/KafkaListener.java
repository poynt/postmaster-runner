package co.poynt.postmaster.listener;

import kafka.consumer.Consumer;
import kafka.consumer.ConsumerConfig;
import kafka.consumer.ConsumerIterator;
import kafka.consumer.KafkaStream;
import kafka.javaapi.consumer.ConsumerConnector;
import kafka.serializer.StringDecoder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
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
	private ConsumerConnector consumerConnector;
	private ExecutorService threadPool;

	public KafkaListener(String zookeeper, Set<String> topics) {
		incomingMessages = new HashMap<>();
		topics.forEach(t -> incomingMessages.put(t, new ArrayBlockingQueue<>(MESSAGE_QUEUE_SIZE)));

		threadPool = Executors.newFixedThreadPool(topics.size());
		startKafkaConsumer(zookeeper, topics);
	}

	/**
	 * Starts the Kafka consumers to keep track of the incoming messages
	 * per topic
	 *
	 * @param zookeeper zookeeper hostname for Kafka
	 * @param topics set of topics to consume the messages from
	 */
	private void startKafkaConsumer(String zookeeper, Set<String> topics) {
		// kafka settings
		Properties properties = new Properties();
		properties.put("zookeeper.connect", zookeeper);
		properties.put("group.id", "KafkaListenerPostmaster");
		properties.put("autocommit.enable", true);
		properties.put("auto.commit.interval.ms", String.valueOf(5000));
		properties.put("fetch.wait.max.ms", String.valueOf(30000));
		properties.put("refresh.leader.backoff.ms", String.valueOf(5000));
		properties.put("zookeeper.sync.time.ms", String.valueOf(5000));

		ConsumerConfig consumerConfig = new ConsumerConfig(properties);
		consumerConnector = Consumer.createJavaConsumerConnector(consumerConfig);

		// create one stream per topic config
		Map<String, Integer> topicCountMap = new HashMap<>();
		topics.forEach(t -> topicCountMap.put(t, 1));

		// create message streams
		Map<String, List<KafkaStream<String, String>>> consumerMap =
				consumerConnector.createMessageStreams(topicCountMap, new StringDecoder(null), new StringDecoder(null));

		// submit one worker runnable per stream to keep dumping the message
		// onto the blocking queue
		consumerMap.forEach((topic, streams) -> threadPool.submit(() -> {
			ConsumerIterator<String, String> stream = streams.get(0).iterator();
			while (stream.hasNext()) {
				incomingMessages.get(topic).add(stream.next().message());
			}
		}));
	}

	@Override
	public String getMessage(String topic, String matcher, int timeout) {
		MessageSearcher searcher = new MessageSearcher(incomingMessages.get(topic));
		return searcher.getMessage(matcher, timeout);
	}

	@Override
	public void clean() {
		consumerConnector.shutdown();
		threadPool.shutdownNow();
	}
}
