package co.poynt.postmaster.js;

import co.poynt.postman.PostmanRunResult;
import co.poynt.postman.model.PostmanRequest;
import co.poynt.postman.model.PostmanVariables;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import java.util.Map;

/**
 * Provides Javascript evaluator to execute the post tests scripts
 * to verify the messages
 */
public class MessageJsEvaluator {

	private PostmanVariables var;

	public MessageJsEvaluator(PostmanVariables var) {
		this.var = var;
	}

	public boolean evaluateTests(PostmanRequest request, String message, PostmanRunResult runResult) {
		if (request.tests == null || request.tests.isEmpty()) {
			return true;
		}

		Context cx = Context.enter();
		String testName = "---------------------> POSTMASTER test";
		boolean isSuccessful = false;
		try {
			Scriptable scope = cx.initStandardObjects();
			PostmasterJsVariables jsVar = new PostmasterJsVariables(scope, this.var.getEnv());
			jsVar.prepare(message);

			// Evaluate the test script
			cx.evaluateString(scope, request.tests, testName, 1, null);

			isSuccessful = true;
			for (Map.Entry e : jsVar.tests.entrySet()) {
				runResult.totalTest++;

				String strVal = e.getValue().toString();
				if ("false".equalsIgnoreCase(strVal)) {
					runResult.failedTest++;
					runResult.failedTestName.add(request.name + "." + e.getKey().toString());
					isSuccessful = false;
				}

				System.out.println(testName + ": " + e.getKey() + " - " + e.getValue());
			}
		} catch (Throwable t) {
			isSuccessful = false;
			System.out.println("=====FAILED TO EVALUATE TEST AGAINST INCOMING MESSAGE======");
			System.out.println("========TEST========");
			System.out.println(request.tests);
			System.out.println("========TEST========");
			System.out.println("========RESPONSE========");
			System.out.println(message);
			System.out.println("========RESPONSE========");
			System.out.println("=====FAILED TO EVALUATE TEST AGAINST INCOMING MESSAGE======");
		} finally {
			Context.exit();
		}
		return isSuccessful;
	}
}
