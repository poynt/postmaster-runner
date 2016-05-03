package co.poynt.postmaster.request;

import co.poynt.postmaster.exception.MessageNotFoundException;
import co.poynt.postmaster.js.MessageJsEvaluator;
import co.poynt.postmaster.listener.KafkaListener;
import co.poynt.postmaster.model.PostmasterRequest;
import co.poynt.postman.PostmanRunResult;
import co.poynt.postman.model.PostmanVariables;

/**
 * A listener request runner that manages Kafka listener to keep track of
 * incoming Kafka messages and verify the published messages
 */
public class KafkaListenerRequestRunner implements IListenerRequestRunner<KafkaListener> {

	private static final int REQUEST_TIMEOUT = 5000;
	private KafkaListener kafkaListener;
	private MessageJsEvaluator evaluator;

	public KafkaListenerRequestRunner(PostmanVariables var) {
		evaluator = new MessageJsEvaluator(var);
	}

	@Override
	public void addListener(KafkaListener kafkaListener) {
		this.kafkaListener = kafkaListener;
	}

	@Override
	public boolean run(PostmasterRequest request, PostmanRunResult runResult) {
		try {
			String message = kafkaListener.getMessage(request.topic, request.matcher, REQUEST_TIMEOUT);
			System.out.println("Message found: " + message);
			return evaluator.evaluateTests(request, message, runResult);
		} catch (MessageNotFoundException e) {
			return false;
		}
	}

	@Override
	public void clean() {
		if (kafkaListener != null) {
			kafkaListener.clean();
		}
	}
}
