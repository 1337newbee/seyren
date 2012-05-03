package com.seyren.core.domain;

import com.seyren.core.service.NotificationService;

/**
 * This class represents something wanting to be notified of an alert
 * 
 * @author mark
 *
 */
public class Subscription {

	private String id;
	private String target;
	private SubscriptionType type;
	private boolean su, mo, tu, we, th, fr, sa;
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
	
	public Subscription withId(String id) {
		setId(id);
		return this;
	}
	
	public String getTarget() {
		return target;
	}
	
	public void setTarget(String target) {
		this.target = target;
	}
	
	public Subscription withTarget(String target) {
		setTarget(target);
		return this;
	}
	
	public SubscriptionType getType() {
		return type;
	}
	
	public void setType(SubscriptionType type) {
		this.type = type;
	}
	
	public Subscription withType(SubscriptionType type) {
		setType(type);
		return this;
	}
	
	public boolean isSu() {
		return su;
	}

	public void setSu(boolean su) {
		this.su = su;
	}
	
	public Subscription withSu(boolean su) {
		setSu(su);
		return this;
	}

	public boolean isMo() {
		return mo;
	}

	public void setMo(boolean mo) {
		this.mo = mo;
	}
	
	public Subscription withMo(boolean mo) {
		setMo(mo);
		return this;
	}

	public boolean isTu() {
		return tu;
	}

	public void setTu(boolean tu) {
		this.tu = tu;
	}
	
	public Subscription withTu(boolean tu) {
		setTu(tu);
		return this;
	}

	public boolean isWe() {
		return we;
	}

	public void setWe(boolean we) {
		this.we = we;
	}

	public Subscription withWe(boolean we) {
		setWe(we);
		return this;
	}
	
	public boolean isTh() {
		return th;
	}

	public void setTh(boolean th) {
		this.th = th;
	}
	
	public Subscription withTh(boolean th) {
		setTh(th);
		return this;
	}

	public boolean isFr() {
		return fr;
	}

	public void setFr(boolean fr) {
		this.fr = fr;
	}
	
	public Subscription withFr(boolean fr) {
		setFr(fr);
		return this;
	}

	public boolean isSa() {
		return sa;
	}

	public void setSa(boolean sa) {
		this.sa = sa;
	}
	
	public Subscription withSa(boolean sa) {
		setSa(sa);
		return this;
	}

    /**
     * Report on this alert
     * @param alert
     * @param notificationService
     */
    public void report(Alert alert, NotificationService notificationService) {

    }
}
