package runnableFiles;

import org.matsim.contrib.taxi.schedule.TaxiTask;

public interface MyTaxiTask extends TaxiTask {
	
	static enum MyTaxiTaskType {
		EMPTY_DRIVE, // not directly related to any customer (although may be related to serving a customer; e.g. a pickup drive)
		PICKUP, OCCUPIED_DRIVE, DROPOFF, // serving a customer (TaxiTaskWithRequest)
		STAY,// not directly related to any customer
		RELOCATING, // not directly related to any customer
	}

	MyTaxiTaskType getMyTaxiTaskType();
}