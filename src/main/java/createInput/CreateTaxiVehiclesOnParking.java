/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

/**
 * 
 */
package createInput;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.data.VehicleImpl;
import org.matsim.contrib.dvrp.data.file.VehicleWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacility;

public class CreateTaxiVehiclesOnParking {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		int numberofVehicles = 2750;
		double operationStartTime = 0.0; //t0
		double operationEndTime = 29*3600.;	//t1
		int seats = 5;
		String taxisFile = "Taxis_"+numberofVehicles+"on2750nOUTSIDE.xml";
		
		//final String CONFIG_FILE = "./Amsterdam/Resources/ConfigWithTaxi.xml";
		final String CONFIG_FILE = "ConfigWithTaxi.xml";
		Config config = ConfigUtils.loadConfig(CONFIG_FILE);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		List<Vehicle> vehicles = new ArrayList<>();
		Network network = scenario.getNetwork();
		List<Id<Link>> allLinks = new ArrayList<>();
		allLinks.addAll(scenario.getNetwork().getLinks().keySet());
		
		Map<Id<Link>, ? extends Link> links = network.getLinks();
		String PARKACTIVITYTYPE = "car interaction";
		TreeMap<Id<ActivityFacility>, ActivityFacility> taxiParkingFacilities = scenario.getActivityFacilities().getFacilitiesForActivityType(PARKACTIVITYTYPE);
		Map<Id<Link>, Integer> LinkParking = new TreeMap<Id<Link>, Integer>();	
		int parkingCounterTotal = 0;

		Iterator<ActivityFacility> it = taxiParkingFacilities.values().iterator();
		while (it.hasNext()){
			ActivityFacility act = it.next();
			Id<Link> link = act.getLinkId();
			
			if (links.containsKey(link)){
				Integer parking = (int)(act.getActivityOptions().get("car interaction").getCapacity());	
				//exclude the following parking links
				if (link.toString().equals("m25965010_0") || link.toString().equals("m27080498_0") || link.toString().equals("m324010758_1")|| link.toString().equals("m493217936_2")){
					LinkParking.put(link, 0);
				}else{
					LinkParking.put(link, parking);
					parkingCounterTotal = parkingCounterTotal + parking;
				}
			}	
		}
				
		for (int i = 0; i< numberofVehicles;i++){
			
			if (LinkParking.size() == 0){
				System.out.println("somthing went wrong" );
				return;
			}
			Random generator = new Random();
				
			LinkParking.entrySet().removeIf(entry -> entry.getValue() ==0);
			
			Object[] values = LinkParking.keySet().toArray();
			Id<Link> randomParkingLink = (Id<Link>) values[generator.nextInt(values.length)];
			Link startingLink = links.get(randomParkingLink);
			Vehicle v = new VehicleImpl(Id.create("Taxis"+i, Vehicle.class), startingLink, seats, operationStartTime, operationEndTime);
		    vehicles.add(v); 
		    
		    // remove link capacity/ link
		    int newCap = LinkParking.get(randomParkingLink) - 1;
			LinkParking.put(randomParkingLink, newCap);
			if (newCap == 0){
				LinkParking.remove(randomParkingLink);
			}
		}
		new VehicleWriter(vehicles).write(taxisFile);
		System.out.println("Done. Don't forget to refresh the Resource folder now :)" );
		System.out.println("parkingcoutnertotal " + parkingCounterTotal );
	}
}
