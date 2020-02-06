package runnableFiles;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.zone.Zone;

public 	class TaxiActivity {
	private Id<Vehicle> taxiId;
	private String type;
	private Double startTime;
	private Double endTime;
	private Double duration;
	private Id<Zone> fromZone;
	private Id<Zone> toZone;
	private Id<Zone> inZone;
	private Double distance;


	public String toString() {
		return String.format("ACT: taxiId: %s type: %s start: %6.0f end: %6.0f duration: %6.0f fromZone: %s toZone: %s inZone: %s distance: %6.0f\n",
						getId(), getType(), getStartTime(), getEndTime(), getDuration(),
						getFromZone(), getToZone(), getInZone(), getDistance());
	}

	public Id<Vehicle> getId() {
		return taxiId;
	}

	public void setId(Id<Vehicle> taxiId) {
		this.taxiId = taxiId;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
	
	public Double getStartTime() {
		return startTime;
	}

	public void setStartTime(Double startTime) {
		this.startTime = startTime;
	}
	
	public Double getEndTime() {
		return endTime;
	}

	public void setEndTime(Double endTime) {
		this.endTime = endTime;
	}
	
	public Double getDuration() {
		return duration;
	}

	public void setDuration(Double duration) {
		this.duration = duration;
	}
	
	public Id<Zone> getFromZone() {
		return fromZone;
	}

	public void setFromZone(Id<Zone> fromZone) {
		this.fromZone = fromZone;
	}
	
	public Id<Zone> getToZone() {
		return toZone;
	}

	public void setToZone(Id<Zone> toZone) {
		this.toZone = toZone;
	}
	
	public Id<Zone> getInZone() {
		return inZone;
	}

	public void setInZone(Id<Zone> inZone) {
		this.inZone = inZone;
	}
	
	public Double getDistance() {
		return distance;
	}

	public void setDistance(Double distance) {
		this.distance = distance;
	}
}