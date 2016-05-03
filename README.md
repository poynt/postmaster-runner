# postmaster-runner

A module to run a Postmaster collections. This project is an extension from [postman-runner](https://github.com/poynt/postman-runner). It is designed to handle incoming messages to test asynchronous aspects of web components.

The postmaster collection also extends from the original postman collection with additional fields to support other requests, such as publishing Kafka messages, consuming Kafka messages, and receiving API callback.
 
## Postmaster Collection
See `sample_postmaster_collection.json` and `sample_postmaster_environment.json` for detailed examples.

###List of Postmaster Requests
* POSTMAN
	* Set `"type": "POSTMAN"`
	* Regular postman request
	* Maintains the same postman request format
* KAFKA_PUBLISH
	* Set `"type": "KAFKA_PUBLISH"`
	* Set `"topic": "[KAFKA-TOPIC-NAME]"`
	* Requires `kafka` environment variable for kafka host
	* Publishes a Kafka messages to `[KAFKA-TOPIC-NAME]` topic
* KAFKA_LISTEN
	* Set `"type": "KAFKA_LISTEN"`
	* Set `"topic": "[KAFKA-TOPIC-NAME]"`
	* Set `"matcher": "[REGEX-MATCHER]"`
	* Set `"tests": "[JS-TESTS]"`
	* Requires `zookeeper` environment variable for zookeeper host
	* Consumes a Kafka message from `[KAFKA-TOPIC-NAME]` with matching `[REGEX-MATCHER]` and runs post Javascript test from `[JS-TESTS]`
* API_LISTEN
	* Set `"type": "API_LISTEN"`
	* Set `"url": "[API-URL]"`
	* Set `"matcher": "[REGEX-MATCHER]"`
	* Set `"tests": "[JS-TESTS]"`
	* Requires `[API-URL]` to specify the port for the listener to bind to
	* Consumes a API callback message from `[API-URL]` with matching `[REGEX-MATCHER]` and runs post Javascript test from `[JS-TESTS]`

## Running from command line
```
bin/postmaster-runner.sh -c src/test/resources/sample_postmaster_collection.json -e src/tesresources/sample_postmaster_environment.json -haltonerror false
```

Run `bin/postmaster-runner.sh` without any argument to see all the options.

## Running from Java

Add the following maven dependency:

```xml
		<dependency>
			<groupId>co.poynt.postmaster.runner</groupId>
			<artifactId>postmaster-runner</artifactId>
			<version>X.X.X</version>
			<scope>test</scope>
		</dependency>
```
where X.X.X is the latest version of this artifact.

From your test driver class, make the following call:

```java
	public void testRunPostmaster() throws Exception {
		PostmasterCollectionRunner cr = new PostmasterCollectionRunner();

		boolean isSuccessful = cr.runCollection(
				"classpath:sample_postmaster_collection.json",
				"classpath:sample_postmaster_environment.json",
				"My use cases", false);
		
		Assert.assertTrue(isSuccessful);
	}
```
