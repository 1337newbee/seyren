/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.seyren.core.service.checker;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.codehaus.jackson.JsonNode;
import org.joda.time.DateTime;

import com.seyren.core.domain.Alert;
import com.seyren.core.domain.AlertType;
import com.seyren.core.domain.Check;
import com.seyren.core.util.config.GraphiteConfig;

@Named
public class GraphiteTargetChecker implements TargetChecker {
    
    private static final String QUERY_STRING = "from=-5minutes&until=now&uniq=%s&format=json&target=%s";
    private static final int MAX_CONNECTIONS_PER_ROUTE = 20;

	private final HttpClient client;
	private final String graphiteScheme;
	private final String graphiteHost;
	private final JsonNodeResponseHandler handler = new JsonNodeResponseHandler();
	
	@Inject
	public GraphiteTargetChecker(GraphiteConfig graphiteConfig) {
	    this.graphiteScheme = graphiteConfig.getScheme();
	    this.graphiteHost = graphiteConfig.getHost();
	    this.client = new DefaultHttpClient(createConnectionManager());
	}
	
	@Override
	public List<Alert> check(Check check) throws Exception {
	    String formattedQuery = String.format(QUERY_STRING, new DateTime().getMillis(), check.getTarget());
	    URI uri = new URI(graphiteScheme, graphiteHost, "/render", formattedQuery, null);
		HttpGet get = new HttpGet(uri);

		try {
		    JsonNode response = client.execute(get, handler);
			List<Alert> alerts = new ArrayList<Alert>();
			for (JsonNode metric : response) {
    			String target = metric.path("target").asText();
    			Double value = getLatestValue(metric);
    			alerts.add(createAlert(check, target, value));
			}
			return alerts;
		} finally {
			get.releaseConnection();
		}
		
	}
	
	private Alert createAlert(Check check, String target, Double value) {
		AlertType currentState = check.getState();
		AlertType newState = AlertType.OK;

		if (check.isBeyondErrorThreshold(value)) {
			newState = AlertType.ERROR;
		} else if (check.isBeyondWarnThreshold(value)) {
			newState = AlertType.WARN;
		}
		
		return createAlert(check, target, value, currentState, newState);
	}

	/**
	 * Loop through the datapoints in reverse order until we find the latest non-null value
	 */
	private Double getLatestValue(JsonNode node) throws Exception {
		JsonNode datapoints = node.get("datapoints");
		
		for (int i = datapoints.size() - 1; i >= 0; i--) {
			String value = datapoints.get(i).get(0).asText();
			if (!value.equals("null")) {
				return Double.valueOf(value);
			}
		}

		throw new Exception("Could not find a valid datapoint for target: " + node.get("target"));
	}

	private Alert createAlert(Check check, String target, Double value, AlertType from, AlertType to) {
		return new Alert()
				.withValue(value)
				.withTarget(target)
				.withWarn(check.getWarn())
				.withError(check.getError())
				.withFromType(from)
				.withToType(to)
				.withTimestamp(new DateTime());
	}

    private ClientConnectionManager createConnectionManager() {
        PoolingClientConnectionManager manager = new PoolingClientConnectionManager();
        manager.setDefaultMaxPerRoute(MAX_CONNECTIONS_PER_ROUTE);
        return manager;
    }

}
