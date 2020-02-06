package readOutput;

import java.util.LinkedList;

import runnableFiles.TravelComponent;


public 	 class TravellerChain {
	// use linked lists so I can use the getlast method
	private LinkedList<Activity> acts = new LinkedList<Activity>();
	private LinkedList<Journey> journeys = new LinkedList<Journey>();
	private LinkedList<Taxi> taxis = new LinkedList<Taxi>();
	LinkedList<TravelComponent> planElements = new LinkedList<TravelComponent>();

	public Journey addJourney() {
		Journey journey = new Journey();
		getJourneys().add(journey);
		planElements.add(journey);
		return journey;
	}

	public Activity addActivity() {
		Activity activity = new Activity();
		getActs().add(activity);
		planElements.add(activity);
		return activity;
	}
	
	public Taxi addTaxi(){
		Taxi taxi = new Taxi();
		getTaxis().add(taxi);		
		planElements.add(taxi);
		return taxi;
	}

	public LinkedList<Taxi> getTaxis() {
		return taxis;
	}
	
	public void setTaxis(LinkedList<Taxi> taxis) {
		this.taxis = taxis;
	}
	
	public LinkedList<Journey> getJourneys() {
		return journeys;
	}

	public void setJourneys(LinkedList<Journey> journeys) {
		this.journeys = journeys;
	}

	public LinkedList<Activity> getActs() {
		return acts;
	}

	public void setActs(LinkedList<Activity> acts) {
		this.acts = acts;
	}

	public boolean isInPT() {
		return inPT;
	}

	public void setInPT(boolean inPT) {
		this.inPT = inPT;
	}

	private boolean inPT = false;
	public boolean inCongestedMode;
	public boolean inCongestedModeTaxi;
	public boolean traveledVehicle;
	public boolean traveling=false;
	private double linkEnterTime;

	public double getLinkEnterTime() {
		return linkEnterTime;
	}

	public void setLinkEnterTime(double linkEnterTime) {
		this.linkEnterTime = linkEnterTime;
	}

}
