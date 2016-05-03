package co.poynt.postmaster.listener;

import co.poynt.postmaster.exception.ListenerInitializationException;
import co.poynt.postmaster.exception.MessageNotFoundException;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class ApiListener implements IListener {

	protected static final String CONTEXT_INCOMING_MESSAGES = "incomingMessages";
	private static final int MESSAGE_QUEUE_SIZE = 100;
	/**
	 * api path to incoming messages map
	 */
	private Map<String, BlockingQueue<String>> incomingMessages;
	private Server server;

	public ApiListener(List<String> apiUrls) {
		Integer port = null;
		Set<String> apiPaths = new HashSet<>();
		for (String url : apiUrls) {
			try {
				URI uri = new URI(url);
				int p = uri.getPort();
				if (port == null) {
					port = p;
				} else if (port != p) {
					// TODO allow multiple ports per folder
					throw new ListenerInitializationException("Only one port per folder is allowed");
				}
				apiPaths.add(trimPath(uri.getPath()));
			} catch (URISyntaxException e) {
				throw new ListenerInitializationException("Invalid listener url", e);
			}
		}

		incomingMessages = new HashMap<>();
		apiPaths.forEach(t -> incomingMessages.put(t, new ArrayBlockingQueue<>(MESSAGE_QUEUE_SIZE)));
		startServer(port);
	}

	/**
	 * Starts a http server on given port and pass the map of blocking queue
	 * to keep track of the incoming messages
	 *
	 * @param port port for the server to bind to
	 */
	private void startServer(int port) {
		server = new Server(port);
		ServletContextHandler handler = new ServletContextHandler(server, "/");
		handler.setAttribute(CONTEXT_INCOMING_MESSAGES, incomingMessages);
		handler.addServlet(ApiListenerServlet.class, "/");
		try {
			server.start();
		} catch (Exception e) {
			throw new ListenerInitializationException(e);
		}
	}

	@Override
	public String getMessage(String url, String matcher, int timeout) {
		try {
			URI uri = new URI(url);
			MessageSearcher searcher = new MessageSearcher(incomingMessages.get(trimPath(uri.getPath())));
			return searcher.getMessage(matcher, timeout);
		} catch (URISyntaxException e) {
			throw new MessageNotFoundException(e);
		}
	}

	@Override
	public void clean() {
		try {
			server.stop();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected static String trimPath(String path) {
		if (!path.isEmpty() && path.charAt(path.length() - 1) == '/') {
			return path.substring(0, path.length() - 1);
		}
		return path;
	}
}
