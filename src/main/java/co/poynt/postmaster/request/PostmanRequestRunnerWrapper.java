package co.poynt.postmaster.request;

import co.poynt.postmaster.model.PostmasterRequest;
import co.poynt.postman.PostmanRequestRunner;
import co.poynt.postman.PostmanRunResult;
import co.poynt.postman.model.PostmanVariables;

/**
 * Simple wrapper around the original postman request runner
 */
public class PostmanRequestRunnerWrapper implements IRequestRunner {

	private PostmanRequestRunner runner;

	public PostmanRequestRunnerWrapper(PostmanVariables var, boolean haltOnError) {
		runner = new PostmanRequestRunner(var, haltOnError);
	}

	@Override
	public boolean run(PostmasterRequest request, PostmanRunResult runResult) {
		return runner.run(request, runResult);
	}
}
