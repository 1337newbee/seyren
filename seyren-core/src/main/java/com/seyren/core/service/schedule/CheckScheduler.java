package com.seyren.core.service.schedule;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

import com.seyren.core.domain.Alert;
import com.seyren.core.domain.Check;
import com.seyren.core.domain.Subscription;
import com.seyren.core.service.checker.TargetChecker;
import com.seyren.core.service.notification.NotificationService;
import com.seyren.core.store.AlertsStore;
import com.seyren.core.store.ChecksStore;

@Named
public class CheckScheduler {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(CheckScheduler.class);

	private final ChecksStore checksStore;
	private final AlertsStore alertsStore;
	private final TargetChecker checker;
	private final NotificationService notificationService;
	private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(8);

	@Inject
	public CheckScheduler(ChecksStore checksStore, AlertsStore alertsStore, NotificationService notificationService, TargetChecker checker) {
		this.checksStore = checksStore;
		this.alertsStore = alertsStore;
        this.notificationService = notificationService;
		this.checker = checker;
	}
	
	@Scheduled(fixedRate = 60000)
	public void performChecks() {
	    List<Check> checks = checksStore.getChecks();
		for (final Check check : checks) {
		    executor.execute(new CheckRunner(check));
		}
	}
	
	private class CheckRunner implements Runnable {
	    
	    private final Check check;
	    
	    public CheckRunner(Check check) {
	        this.check = check;
	    }
	    
	    @Override
	    public final void run() {
	        if (check.isEnabled()) {
                try {
                    Alert alert = checker.check(check);
                    
                    if (alert.isStillOk()) {
                        return;
                    }
                    
                    alertsStore.createAlert(check.getId(), alert);
                    check.setState(alert.getToType());
                    checksStore.saveCheck(check);
                    
                    // Only notify if the alert has changed state
                    if (!alert.hasStateChanged()) {
                        return;
                    }
                    
                    for (Subscription subscription : check.getSubscriptions()) {
                        if (!subscription.shouldNotify(alert)) {
                            continue;
                        }
                        
                        try {
                            notificationService.sendNotification(check, subscription, alert);
                        } catch (Exception e) {
                            LOGGER.warn(subscription.getTarget() + " failed", e);
                        }
                    }
                } catch (Exception e) {
                    LOGGER.warn(check.getName() + " failed", e);
                }
            }
	    }
	    
	}
	
}
