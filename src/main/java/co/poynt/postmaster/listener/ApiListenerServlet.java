package co.poynt.postmaster.listener;

import org.eclipse.jetty.http.HttpStatus;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.stream.Collectors;

public class ApiListenerServlet extends HttpServlet {
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		// get path first
		String path = ApiListener.trimPath(req.getServletPath());

		// get incoming messages from the context
		ServletContext ctx = req.getServletContext();
		Map<String, BlockingQueue<String>> incomingMessages = (Map) ctx.getAttribute(ApiListener.CONTEXT_INCOMING_MESSAGES);

		// not tracking this path
		if (!incomingMessages.containsKey(path)) {
			resp.setStatus(HttpStatus.OK_200);
			return;
		}

		try {
			// add message to the incoming message queue
			BlockingQueue<String> messages = incomingMessages.get(path);
			messages.put(req.getReader().lines().collect(Collectors.joining(System.lineSeparator())));
			resp.setStatus(HttpStatus.OK_200);
		} catch (InterruptedException e) {
			e.printStackTrace();
			resp.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
		}
	}
}
