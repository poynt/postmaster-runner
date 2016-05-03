package co.poynt.postmaster.request;

import co.poynt.postmaster.listener.IListener;

public interface IListenerRequestRunner<T extends IListener> extends IRequestRunner {
	void addListener(T listener);
	void clean();
}
