package io.github.vlsergey.springdata.entitysecurity;

import static java.util.Collections.synchronizedList;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import net.ttddyy.dsproxy.ExecutionInfo;
import net.ttddyy.dsproxy.QueryInfo;
import net.ttddyy.dsproxy.listener.QueryExecutionListener;

public class TestQueryListener implements QueryExecutionListener {

	private List<Consumer<String>> queryListeners = synchronizedList(new ArrayList<>());

	@Override
	public void beforeQuery(ExecutionInfo execInfo, List<QueryInfo> queryInfoList) {
	}

	@Override
	public void afterQuery(ExecutionInfo execInfo, List<QueryInfo> queryInfoList) {
		queryInfoList.forEach(qi -> queryListeners.forEach(listener -> listener.accept(qi.getQuery())));
	}

	public List<String> listen(Runnable runnable) {
		final List<String> queries = new ArrayList<>();
		final Consumer<String> listener = queries::add;
		queryListeners.add(listener);
		try {
			runnable.run();
			return queries;
		} finally {
			queryListeners.remove(listener);
		}
	}

}
