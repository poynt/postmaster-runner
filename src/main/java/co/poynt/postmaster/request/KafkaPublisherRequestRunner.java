package co.poynt.postmaster.request;

import co.poynt.postmaster.model.PostmasterRequest;
import co.poynt.postman.PostmanRunResult;
import co.poynt.postman.model.PostmanVariables;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A request runner that maintains Kafka producer to publish Kafka messages
 */
public class KafkaPublisherRequestRunner implements IRequestRunner {

	private static final int REQUEST_TIMEOUT = 2000;
	private PostmanVariables var;
	private String kafkaHost;
	private KafkaProducer<String, String> producer;

	public KafkaPublisherRequestRunner(PostmanVariables var) {
		this.var = var;
		this.kafkaHost = var.getEnv().lookup.get("kafka").value;
		Properties props = new Properties();
		props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");
		props.put("bootstrap.servers", kafkaHost);
		props.put("block.on.buffer.full", false);
		props.put("reconnect.backoff.ms", 5000);
		props.put("retry.backoff.ms", 2000);
		props.put("metadata.fetch.timeout.ms", 10000);
		props.put("acks", "all");
		this.producer = new KafkaProducer<>(props, new StringSerializer(), new StringSerializer());
	}

	@Override
	public boolean run(PostmasterRequest request, PostmanRunResult runResult) {
		String data = request.getData(var);

		ProducerRecord<String, String> record = new ProducerRecord<>(request.topic, data);
		Future<RecordMetadata> metadata = producer.send(record);
		try {
			// wait until the message is sent
			metadata.get(REQUEST_TIMEOUT, TimeUnit.MILLISECONDS);
		} catch (TimeoutException | InterruptedException | ExecutionException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
}
