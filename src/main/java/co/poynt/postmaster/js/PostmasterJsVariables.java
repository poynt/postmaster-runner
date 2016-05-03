package co.poynt.postmaster.js;

import co.poynt.postman.model.PostmanEnvValue;
import co.poynt.postman.model.PostmanEnvironment;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import java.util.Map;
import java.util.Set;

public class PostmasterJsVariables {
	protected Object responseBody;
	protected Object iteration;
	protected Object postman;
	protected NativeObject environment;
	protected NativeObject tests;

	private Scriptable scope;
	private PostmanEnvironment env;

	public PostmasterJsVariables(Scriptable scope, PostmanEnvironment env) {
		this.scope = scope;
		this.env = env;
	}

	public void prepare(String message) {
		this.prepareJsVariables(message);
		this.injectJsVariablesToScope();
	}

	private void prepareJsVariables(String message) {
		this.responseBody = Context.javaToJS(message, scope);
		this.postman = Context.javaToJS(this.env, scope);
		this.environment = new NativeObject();
		Set<Map.Entry<String, PostmanEnvValue>> map = this.env.lookup
				.entrySet();
		for (Map.Entry<String, PostmanEnvValue> e : map) {
			this.environment.put(e.getKey(), environment, e.getValue().value);
		}
		this.tests = new NativeObject();
	}

	private void injectJsVariablesToScope() {
		ScriptableObject.putProperty(scope, "responseBody", responseBody);
		ScriptableObject.putProperty(scope, "iteration", iteration);
		ScriptableObject.putProperty(scope, "postman", postman);
		ScriptableObject.putProperty(scope, "environment", environment);
		ScriptableObject.putProperty(scope, "tests", tests);
	}
}
