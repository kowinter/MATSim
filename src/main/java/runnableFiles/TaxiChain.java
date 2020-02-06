package runnableFiles;

import java.util.LinkedList;

public 	 class TaxiChain {
	LinkedList<TravelComponent> planElements = new LinkedList<TravelComponent>();
	private LinkedList<TaxiActivity> acts = new LinkedList<TaxiActivity>();
	
	public TaxiActivity addActivity() {
		TaxiActivity activity = new TaxiActivity();
		getActs().add(activity);
		return activity;
	}
	
	public LinkedList<TaxiActivity> getActs() {
	return acts;
	}
	
	public void setActs(LinkedList<TaxiActivity> acts) {
	this.acts = acts;
	}	
	
	private double linkEnterTime;

	public double getLinkEnterTime() {
		return linkEnterTime;
	}

	public void setLinkEnterTime(double linkEnterTime) {
		this.linkEnterTime = linkEnterTime;
	}

}
