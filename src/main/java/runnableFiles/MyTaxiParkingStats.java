package runnableFiles;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.taxi.schedule.TaxiTask.TaxiTaskType;
import org.matsim.contrib.zone.Zone;

import org.matsim.contrib.util.*;

public class MyTaxiParkingStats {
	public final String id;
	public final DescriptiveStatistics passengerWaitTime = new DescriptiveStatistics();
	public final DescriptiveStatistics parkingDuration = new DescriptiveStatistics();
	public final Map <Id<Zone>, DescriptiveStatistics> zonalParkingDuration = new HashMap <Id<Zone>, DescriptiveStatistics>();
	public final Map <Id<Zone>, DescriptiveStatistics> zonalInitParkingVehicles = new HashMap <Id<Zone>, DescriptiveStatistics>();
	public final Map <Id<Zone>, DescriptiveStatistics> zonalParkingVehicles = new HashMap <Id<Zone>, DescriptiveStatistics>();
	//public final Map <Id<Zone>, DescriptiveStatistics> zonalParkingCounterVehicles = new HashMap <Id<Zone>, DescriptiveStatistics>();
	//public final Map <Id<Zone>, DescriptiveStatistics> zonalParkingVehicles85 = new HashMap <Id<Zone>, DescriptiveStatistics>();
	//public final Map <Id<Zone>, DescriptiveStatistics> zonalInOutCounter = new HashMap <Id<Zone>, DescriptiveStatistics>();
	//public final Map <Id<Zone>, DescriptiveStatistics> zonalInParkingVehicles = new HashMap <Id<Zone>, DescriptiveStatistics>();
	//public final Map <Id<Zone>, DescriptiveStatistics> zonalOutParkingVehicles = new HashMap <Id<Zone>, DescriptiveStatistics>();
	public final Map <Id<Zone>, DescriptiveStatistics> zonalWaitingTime = new HashMap <Id<Zone>, DescriptiveStatistics>();
	public final Map <Id<Zone>, DescriptiveStatistics> zonalRelocationFrom = new HashMap <Id<Zone>, DescriptiveStatistics>();
	public final Map <Id<Zone>, DescriptiveStatistics> zonalRelocationTo = new HashMap <Id<Zone>, DescriptiveStatistics>();
	public final Map <Id<Zone>, DescriptiveStatistics> zonalRelocationIn = new HashMap <Id<Zone>, DescriptiveStatistics>();
	public final Map <Id<Zone>, DescriptiveStatistics> zonalTaxiRequestServed = new HashMap <Id<Zone>, DescriptiveStatistics>();
	public final EnumAdder<TaxiTaskType, Long> taskTimeSumsByType = new LongEnumAdder<>(TaxiTaskType.class);
	
	public final DescriptiveStatistics vehicleEmptyDriveTT = new DescriptiveStatistics();
	public final DescriptiveStatistics vehicleRelocatingTT = new DescriptiveStatistics();
	
	public final DescriptiveStatistics vehicleEmptyDriveRatio = new DescriptiveStatistics();
	public final DescriptiveStatistics vehicleStayRatio = new DescriptiveStatistics();
	public final DescriptiveStatistics vehicleRelocatingDriveRatio =  new DescriptiveStatistics();
	
	
	/////////////////////////////////////////////////
	
	public MyTaxiParkingStats(String id){
		this.id = id;
	}
	
	public double getFleetEmptyDriveRatio() {
		double empty = taskTimeSumsByType.get(TaxiTaskType.EMPTY_DRIVE).doubleValue();
		double occupied = taskTimeSumsByType.get(TaxiTaskType.OCCUPIED_DRIVE).doubleValue();
		return empty / (empty + occupied);
	}
	
	public double getFleetStayRatio() {
		double stay = taskTimeSumsByType.get(TaxiTaskType.STAY).doubleValue();
		double total = taskTimeSumsByType.getTotal().doubleValue();
		return stay / total;
	}

	public double getOccupiedDriveRatio() {
		double occupied = taskTimeSumsByType.get(TaxiTaskType.OCCUPIED_DRIVE).doubleValue();
		double total = taskTimeSumsByType.getTotal().doubleValue();
		return occupied / total;
	}

	public double getFleetRelocatingDriveRatio() {
		double empty = taskTimeSumsByType.get(TaxiTaskType.EMPTY_DRIVE).doubleValue();
		double relocating = vehicleRelocatingTT.getSum();
		return relocating/empty;
	}
	
	public double getGiniCoefficientWaitingTimes() {
		Double sumList = 0.0;
		Double gini = 0.0;
		List<Double> list = new ArrayList<Double>();
		double[] doublelist = passengerWaitTime.getValues();
		for (Double wt: doublelist) {
			list.add(wt);
			sumList = sumList + wt;
		}
		if (!list.isEmpty()) {
			list.sort(Comparator.naturalOrder());
			Double sumTop = 0.0;
			Integer n = list.size();
			for (int i = 0; i < n; i++) {
				Double y = list.get(i);
				Double x = (n + 1 - (i + 1)) * y;
				sumTop = sumTop + x;
			}
			double aa = sumTop/sumList;
			aa = aa *2;
			double cc = n + 1 - aa;
			double bb = cc / n;
			gini = bb * 100;
		}
		return gini;
	}
	
	
}