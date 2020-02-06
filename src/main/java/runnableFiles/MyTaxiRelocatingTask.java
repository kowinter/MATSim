package runnableFiles;

import org.matsim.contrib.taxi.schedule.TaxiTask;

import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.schedule.DriveTaskImpl;

public class MyTaxiRelocatingTask extends DriveTaskImpl implements TaxiTask {
	private VrpPathWithTravelData path;
	public MyTaxiRelocatingTask(VrpPathWithTravelData path) {
		super(path);
		this.path = path;
	}

	@Override
	public TaxiTaskType getTaxiTaskType() {
		return TaxiTaskType.EMPTY_DRIVE;
	}
	public MyTaxiTaskType getMyTaxiTaskType() {
		return MyTaxiTaskType.RELOCATING;
	}

	@Override
	protected String commonToString() {
		return "[" + getMyTaxiTaskType().name() + "]" + super.commonToString();
	}

	static enum MyTaxiTaskType {
		EMPTY_DRIVE, // not directly related to any customer (although may be related to serving a customer; e.g. a
						// pickup drive)
		PICKUP, OCCUPIED_DRIVE, DROPOFF, // serving a customer (TaxiTaskWithRequest)
		STAY,// not directly related to any customer
		RELOCATING, // not directly related to any customer
	}
	
	public VrpPathWithTravelData getMyTaxiRelocationPath(){
		return path;
	}
	
}
