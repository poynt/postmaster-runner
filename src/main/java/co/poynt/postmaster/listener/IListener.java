package co.poynt.postmaster.listener;

import co.poynt.postmaster.exception.MessageNotFoundException;

public interface IListener {
	/**
	 * Gets the first message that matches the regex matcher in the
	 * namespace, waiting if no messages match yet. Throws
	 * {@link MessageNotFoundException} if no match found given the
	 * timeout in milliseconds.
	 *
	 * @param namespace the namespace of the message
	 * @param matcher the regex matcher
	 * @param timeout the timeout in milliseconds before giving up
	 * @return found message
	 * @throws MessageNotFoundException if no message found
	 */
	String getMessage(String namespace, String matcher, int timeout);

	/**
	 * Clean all resources before shutdown
	 */
	void clean();
}
