package co.poynt.postmaster.listener;

import kafka.api.FetchRequest;
import kafka.api.FetchRequestBuilder;
import kafka.api.PartitionOffsetRequestInfo;
import kafka.common.TopicAndPartition;
import kafka.javaapi.FetchResponse;
import kafka.javaapi.OffsetResponse;
import kafka.javaapi.PartitionMetadata;
import kafka.javaapi.TopicMetadata;
import kafka.javaapi.TopicMetadataRequest;
import kafka.javaapi.TopicMetadataResponse;
import kafka.javaapi.consumer.SimpleConsumer;
import kafka.message.MessageAndOffset;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class KafkaSimpleConsumer implements Runnable {

	private static final int TIMEOUT_MS = 100000;
	private static final int CONNECTION_TIMEOUT_MS = 10000;
	private static final int BUFFER_SIZE = 64 * 1024;

	private String hostname;
	private int port;
	private String topic;
	private int partition;
	private BlockingQueue<String> messages;
	private CountDownLatch connected;
	private volatile boolean stop = false;

	public KafkaSimpleConsumer(String hostname, int port, String topic, int partition, BlockingQueue<String> messages) {
		this.hostname = hostname;
		this.port = port;
		this.topic = topic;
		this.partition = partition;
		this.messages = messages;
		this.connected = new CountDownLatch(1);
	}

	@Override
	public void run() {
		try {
			consume();
		} catch (Exception e) {
			System.out.println("Failed to run consumer");
		}
	}

	public void waitForConnection() throws InterruptedException {
		connected.await(CONNECTION_TIMEOUT_MS, TimeUnit.MILLISECONDS);
	}

	public void shutdown() {
		stop = true;
	}

	/**
	 * Low level consumer example from
	 * ttps://cwiki.apache.org/confluence/display/KAFKA/0.8.0+SimpleConsumer+Example
	 */
	private void consume() throws UnsupportedEncodingException {

		PartitionMetadata metadata = findLeader(hostname, port, topic, partition);
		if (metadata == null) {
			System.out.println("Can't find metadata for Topic and Partition. Exiting");
			return;
		}
		if (metadata.leader() == null) {
			System.out.println("Can't find Leader for Topic and Partition. Exiting");
			return;
		}
		String leadBroker = metadata.leader().host();
		String clientName = "Client_" + topic + "_" + partition;

		SimpleConsumer consumer = new SimpleConsumer(leadBroker, port, TIMEOUT_MS, BUFFER_SIZE, clientName);
		long readOffset = getLastOffset(consumer, topic, partition, kafka.api.OffsetRequest.LatestTime(), clientName);

		while (!stop) {
			if (consumer == null) {
				consumer = new SimpleConsumer(leadBroker, port, TIMEOUT_MS, BUFFER_SIZE, clientName);
			}
			FetchRequest req = new FetchRequestBuilder().clientId(clientName)
					.addFetch(topic, partition, readOffset, TIMEOUT_MS).build();
			FetchResponse fetchResponse = consumer.fetch(req);

			if (fetchResponse.hasError()) {
				short code = fetchResponse.errorCode(topic, partition);
				System.out.println("Error fetching data from the Broker:" + leadBroker + " Reason: " + code);
				break;
			}

			long numRead = 0;
			for (MessageAndOffset messageAndOffset : fetchResponse.messageSet(topic, partition)) {
				long currentOffset = messageAndOffset.offset();
				if (currentOffset < readOffset) {
					System.out.println("Found an old offset: " + currentOffset + " Expecting: " + readOffset);
					continue;
				}
				readOffset = messageAndOffset.nextOffset();
				ByteBuffer payload = messageAndOffset.message().payload();

				byte[] bytes = new byte[payload.limit()];
				payload.get(bytes);
				messages.offer(new String(bytes, "UTF-8"));
				numRead++;
			}

			if (numRead == 0) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException ie) {
					// do nothing
				}
			}

			if (connected.getCount() > 0) {
				connected.countDown();
			}
		}

		if (consumer != null) {
			consumer.close();
		}
	}

	private PartitionMetadata findLeader(String broker, int port, String topic, int partition) {
		PartitionMetadata returnMetaData = null;
		SimpleConsumer consumer = null;

		try {
			consumer = new SimpleConsumer(broker, port, TIMEOUT_MS, BUFFER_SIZE, "leaderLookup");
			List<String> topics = Collections.singletonList(topic);
			TopicMetadataRequest req = new TopicMetadataRequest(topics);
			TopicMetadataResponse resp = consumer.send(req);

			List<TopicMetadata> metaData = resp.topicsMetadata();
			loop:
			for (TopicMetadata item : metaData) {
				for (PartitionMetadata part : item.partitionsMetadata()) {
					if (part.partitionId() == partition) {
						returnMetaData = part;
						break loop;
					}
				}
			}
		} catch (Exception e) {
			System.out.println("Error communicating with Broker [" + broker + "] to find Leader for [" + topic
					+ ", " + partition + "] Reason: " + e);
		} finally {
			if (consumer != null) {
				consumer.close();
			}
		}
		return returnMetaData;
	}

	private long getLastOffset(SimpleConsumer consumer, String topic, int partition, long whichTime, String clientName) {
		TopicAndPartition topicAndPartition = new TopicAndPartition(topic, partition);
		Map<TopicAndPartition, PartitionOffsetRequestInfo> requestInfo = new HashMap<>();
		requestInfo.put(topicAndPartition, new PartitionOffsetRequestInfo(whichTime, 1));
		kafka.javaapi.OffsetRequest request = new kafka.javaapi.OffsetRequest(requestInfo,
				kafka.api.OffsetRequest.CurrentVersion(), clientName);
		OffsetResponse response = consumer.getOffsetsBefore(request);

		if (response.hasError()) {
			System.out.println("Error fetching data Offset Data the Broker. Reason: "
					+ response.errorCode(topic, partition));
			return 0;
		}
		long[] offsets = response.offsets(topic, partition);
		return offsets[0];
	}
}
