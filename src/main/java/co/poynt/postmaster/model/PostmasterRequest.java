package co.poynt.postmaster.model;

import co.poynt.postman.model.PostmanRequest;

public class PostmasterRequest extends PostmanRequest {

	public enum Type {
		POSTMAN, KAFKA_PUBLISH, KAFKA_LISTEN, API_LISTEN
	}

	public Type type;
	public String topic;
	public String matcher;
}
