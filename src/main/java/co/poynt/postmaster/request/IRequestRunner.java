package co.poynt.postmaster.request;

import co.poynt.postmaster.model.PostmasterRequest;
import co.poynt.postman.PostmanRunResult;

public interface IRequestRunner {
	boolean run(PostmasterRequest request, PostmanRunResult runResult);
}
