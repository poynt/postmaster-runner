package co.poynt.postmaster;

import co.poynt.postmaster.model.PostmasterCollection;
import co.poynt.postman.model.PostmanEnvironment;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class PostmasterReader {
	private ObjectMapper om;

	public PostmasterReader() {
		om = new ObjectMapper();
		om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}

	public PostmasterCollection readCollectionFile(String filePath) throws Exception {
		if (filePath.startsWith("classpath:")) {
			return readCollectionFileClasspath(filePath);
		}
		InputStream stream = new FileInputStream(new File(filePath));
		PostmasterCollection collection = om.readValue(stream, PostmasterCollection.class);
		stream.close();
		return collection;
	}

	private PostmasterCollection readCollectionFileClasspath(String fileOnClasspath) throws IOException {
		String fileName = fileOnClasspath.substring(fileOnClasspath.indexOf(":") + 1);
		InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(fileName);

		PostmasterCollection collection = om.readValue(stream, PostmasterCollection.class);
		stream.close();
		return collection;
	}

	public PostmanEnvironment readEnvironmentFile(String filePath) throws Exception {
		if (filePath == null) {
			return new PostmanEnvironment();
		}
		if (filePath.startsWith("classpath:")) {
			return readEnvironmentFileClasspath(filePath);
		}
		InputStream stream = new FileInputStream(new File(filePath));
		PostmanEnvironment env = om.readValue(stream, PostmanEnvironment.class);
		stream.close();
		return env;
	}

	private PostmanEnvironment readEnvironmentFileClasspath(String fileOnClasspath) throws IOException {
		String fileName = fileOnClasspath.substring(fileOnClasspath.indexOf(":") + 1);
		InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(fileName);

		PostmanEnvironment env = om.readValue(stream, PostmanEnvironment.class);
		stream.close();
		return env;
	}
}
