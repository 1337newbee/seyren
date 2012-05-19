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

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.GetMethod;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.joda.time.DateTime;

import com.seyren.core.domain.Alert;
import com.seyren.core.domain.AlertType;
import com.seyren.core.domain.Check;
import com.seyren.core.util.graphite.GraphiteConfig;

@Named
public class GraphiteTargetChecker implements TargetChecker {
    
    private static final String GRAPHITE_TARGET_PATH_FORMAT = "%s/render?from=-5minutes&amp;until=now&amp;uniq=%s&amp;format=json&amp;target=%s";

	private HttpClient client;
	private final GraphiteConfig graphiteConfig;

	@Inject
	public GraphiteTargetChecker(GraphiteConfig graphiteConfig) {
		client = new HttpClient(new MultiThreadedHttpConnectionManager());
		this.graphiteConfig = graphiteConfig;
	}

	@Override
	public Alert check(Check check) throws Exception {
		GetMethod get = new GetMethod(String.format(GRAPHITE_TARGET_PATH_FORMAT, graphiteConfig.getBaseUrl(), new DateTime().getMillis(), check.getTarget()));

		try {
			client.executeMethod(get);
			JsonNode response = new ObjectMapper().readTree(get.getResponseBodyAsString());
			Double value = getLatestValue(response.get(0));
			return createAlert(check, value);
		} finally {
			get.releaseConnection();
		}
		
	}

	private Alert createAlert(Check check, Double value) {
		AlertType currentState = check.getState();
		AlertType newState = AlertType.OK;

		if (check.isBeyondErrorThreshold(value)) {
			newState = AlertType.ERROR;
		} else if (check.isBeyondWarnThreshold(value)) {
			newState = AlertType.WARN;
		}
		
		return createAlert(check, value, currentState, newState);
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

	private Alert createAlert(Check check, Double value, AlertType from, AlertType to) {
		return new Alert()
				.withValue(value)
				.withTarget(check.getTarget())
				.withWarn(check.getWarn())
				.withError(check.getError())
				.withFromType(from)
				.withToType(to)
				.withTimestamp(new DateTime());
	}

}
