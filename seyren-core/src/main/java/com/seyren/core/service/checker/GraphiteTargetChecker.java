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
import com.seyren.core.domain.Subscription;
import com.seyren.core.service.notification.NotificationService;
import com.seyren.core.util.graphite.GraphiteConfig;

@Named
public class GraphiteTargetChecker implements TargetChecker {

	private HttpClient client;
	private NotificationService notificationService;
	private final GraphiteConfig graphiteConfig;

	@Inject
	public GraphiteTargetChecker(NotificationService notificationService, GraphiteConfig graphiteConfig) {
		client = new HttpClient(new MultiThreadedHttpConnectionManager());
		this.notificationService = notificationService;
		this.graphiteConfig = graphiteConfig;
	}

	@Override
	public Alert check(Check check) throws Exception {

		GetMethod get = new GetMethod(String.format(graphiteConfig.getUri(), new DateTime().getMillis(), check.getTarget()));

		try {

			client.executeMethod(get);
			Double value = getValue(get);

			// Always create an alert
			Alert alert = createAlert(check, value);

			// Is the alert all OK
			if (alert.getFromType() == AlertType.OK && alert.getToType() == AlertType.OK) {
				return null;
			}

			// Only notify if the alert has changed state
			if (alert.getFromType() != alert.getToType()) {
		        for (Subscription subscription : check.getSubscriptions()) {
		        	if (subscription.shouldNotify(alert)) {
		        		notificationService.sendNotification(check, alert);
		        	} 
		        }
			}

			return alert;

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

	private Double getValue(GetMethod get) throws Exception {
		JsonNode tree = new ObjectMapper().readTree(get.getResponseBodyAsString());
		JsonNode points = tree.get(0).get("datapoints");

		// Loop through the datapoints in reverse order until we find the latest non-null value
		for (int i = points.size() - 1; i >= 0; i--) {
			String value = points.get(i).get(0).asText();
			if (!value.equals("null")) {
				return Double.valueOf(value);
			}
		}

		throw new Exception("Could not find a valid datapoint for uri: " + get);
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
