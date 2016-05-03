package co.poynt.postmaster.request;

import co.poynt.postmaster.exception.MessageNotFoundException;
import co.poynt.postmaster.js.MessageJsEvaluator;
import co.poynt.postmaster.listener.ApiListener;
import co.poynt.postmaster.model.PostmasterRequest;
import co.poynt.postman.PostmanRunResult;
import co.poynt.postman.model.PostmanVariables;

/**
 * A listener request runner that manages API listener to keep track of
 * incoming API calls and verify the API callbacks
 */
public class ApiListenerRequestRunner implements IListenerRequestRunner<ApiListener> {

	private static final int REQUEST_TIMEOUT = 5000;
	private ApiListener apiListener;
	private MessageJsEvaluator evaluator;

	public ApiListenerRequestRunner(PostmanVariables var) {
		evaluator = new MessageJsEvaluator(var);
	}

	@Override
	public void addListener(ApiListener apiListener) {
		this.apiListener = apiListener;
	}

	@Override
	public boolean run(PostmasterRequest request, PostmanRunResult runResult) {
		try {
			String message = apiListener.getMessage(request.url, request.matcher, REQUEST_TIMEOUT);
			System.out.println("Message found: " + message);
			return evaluator.evaluateTests(request, message, runResult);
		} catch (MessageNotFoundException e) {
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public void clean() {
		if (apiListener != null) {
			apiListener.clean();
		}
	}
}
