package readOutput;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;

import runnableFiles.TravelComponent;

public 	 class Taxi extends TravelComponent {
	Journey journey;
	private String mode;
	private Id vehicle;
	private double distance;
	private double duration;
	private String type;
	private double taxiDistance;
	private boolean taxiJourney = true;

	public String toString() {
		return String.format("\tTRIP: type: %s start: %6.0f end: %6.0f distance: %6.0f duration: %6f \n",
						getType(), getStartTime(), getEndTime(), getDistance(), getDuration());
	}
	
	public void incrementTaxiDistance(double increment) {
		taxiDistance += increment;
	}
	
	public double getInVehDistance() {
		return taxiDistance;
	}
	
	public double getTaxiDistance() {
		return taxiDistance;
	}
	
	public void setTaxiDistance(double taxiDistance) {
		this.taxiDistance = taxiDistance;
	}

	public String getType(){
		return type;
	}
	
	public void setType(String type){
		this.type = type.trim();
	}
	
	public String getMode() {
		return mode;
	}

	public void setMode(String mode) {
		this.mode = mode.trim();
	}
	
	public Id getId() {
		return vehicle;
	}

	public void setid(Id vehicle) {
		this.vehicle = vehicle;
	}


	public double getDistance() {
		return distance;
	}

	public void setDistance(double distance) {
		this.distance = distance;
	}
	
	public double getDuration() {
		return distance;
	}

	public void setDuration(double duration) {
		this.duration = duration;
	}

	public void incrementDistance(double linkLength) {
		this.distance += linkLength;	
	}

	public void incrementTime(double linkTime) {
		this.setEndTime(this.getEndTime()+linkTime);	
	}
}
