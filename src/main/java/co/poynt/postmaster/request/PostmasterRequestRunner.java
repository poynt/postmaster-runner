package co.poynt.postmaster.request;

import co.poynt.postmaster.listener.ApiListener;
import co.poynt.postmaster.listener.KafkaListener;
import co.poynt.postmaster.model.PostmasterRequest;
import co.poynt.postmaster.model.PostmasterRequest.Type;
import co.poynt.postman.PostmanRunResult;
import co.poynt.postman.model.PostmanVariables;

import java.util.HashMap;
import java.util.Map;

/**
 * The main request runner that keeps individual runners and routes
 * the run request
 */
public class PostmasterRequestRunner implements IRequestRunner {

	private Map<Type, IRequestRunner> requestRunners;
	private Map<Type, IListenerRequestRunner> listenerRunners;

	public PostmasterRequestRunner(PostmanVariables var, boolean haltOnError) {
		requestRunners = new HashMap<>();
		requestRunners.put(Type.POSTMAN, new PostmanRequestRunnerWrapper(var, haltOnError));
		requestRunners.put(Type.KAFKA_PUBLISH, new KafkaPublisherRequestRunner(var));
		requestRunners.put(Type.KAFKA_LISTEN, new KafkaListenerRequestRunner(var));
		requestRunners.put(Type.API_LISTEN, new ApiListenerRequestRunner(var));

		listenerRunners = new HashMap<>();
		listenerRunners.put(Type.KAFKA_LISTEN, (IListenerRequestRunner) requestRunners.get(Type.KAFKA_LISTEN));
		listenerRunners.put(Type.API_LISTEN, (IListenerRequestRunner) requestRunners.get(Type.API_LISTEN));
	}

	@Override
	public boolean run(PostmasterRequest request, PostmanRunResult runResult) {
		return requestRunners.get(request.type).run(request, runResult);
	}

	public void addKafkaListener(KafkaListener kafkaListener) {
		listenerRunners.get(Type.KAFKA_LISTEN).addListener(kafkaListener);
	}

	public void addApiListener(ApiListener apiListener) {
		listenerRunners.get(Type.API_LISTEN).addListener(apiListener);
	}

	public void cleanListeners() {
		listenerRunners.forEach((k, v) -> v.clean());
	}
}
