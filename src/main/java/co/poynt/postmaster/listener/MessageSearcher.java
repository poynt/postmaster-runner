package co.poynt.postmaster.listener;

import co.poynt.postmaster.exception.MessageNotFoundException;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class MessageSearcher {

	private BlockingQueue<String> messages;
	private ExecutorService threadPool;

	public MessageSearcher(BlockingQueue<String> messages) {
		this.messages = messages;
		this.threadPool = Executors.newFixedThreadPool(1);
	}

	/**
	 * Gets the message that matches the regex matcher within the given
	 * timeout in milliseconds.
	 *
	 * @param matcher the regex matcher
	 * @param timeout the timeout in milliseconds before giving up
	 * @return found message
	 * @throws MessageNotFoundException if no message found
	 */
	public String getMessage(String matcher, int timeout) {
		Searcher searcher = new Searcher(messages, matcher);
		Future<String> found = threadPool.submit(searcher);
		try {
			String message = found.get(timeout, TimeUnit.MILLISECONDS);
			// stop the thread first
			searcher.stop();
			found.cancel(true);
			if (message == null) {
				throw new MessageNotFoundException("Message not found");
			}
			return message;
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			throw new MessageNotFoundException("Failed while waiting for the message", e);
		}
	}

	/**
	 * Searcher callable class to iterate through the messages blocking
	 * queue until the message is found or stopped
	 */
	private class Searcher implements Callable<String> {
		private BlockingQueue<String> messages;
		private String matcher;
		private volatile boolean stop = false;

		private Searcher(BlockingQueue<String> messages, String matcher) {
			this.messages = messages;
			this.matcher = matcher;
		}

		private void stop() {
			this.stop = true;
		}

		@Override
		public String call() throws Exception {
			while (!stop) {
				try {
					String m = messages.take();
					if (m.matches(matcher)) {
						return m;
					}
				} catch (InterruptedException e) {
					return null;
				}
			}
			return null;
		}
	}
}
