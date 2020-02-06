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
import java.util.List;
import java.util.Random;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.data.VehicleImpl;
import org.matsim.contrib.dvrp.data.file.VehicleWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;


public class CreateTaxiVehicles {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		int numberofVehicles = 2500;
		double operationStartTime = 0.; //t0
		double operationEndTime = 29*3600.;	//t1
		int seats = 5;
		String networkfile = "Network.xml ";
		String taxisFile = "Taxis_"+numberofVehicles+".xml";
		List<Vehicle> vehicles = new ArrayList<>();
		Random random = MatsimRandom.getLocalInstance();
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		//new MatsimNetworkReader(scenario.getNetwork()).readFile("./Amsterdam/Resources/Network.xml ");
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkfile);
		List<Id<Link>> allLinks = new ArrayList<>();
		allLinks.addAll(scenario.getNetwork().getLinks().keySet());
		
			
		
		for (int i = 0; i< numberofVehicles;i++){
			Link startLink;
			do {
			Id<Link> linkId = allLinks.get(random.nextInt(allLinks.size()));
			startLink =  scenario.getNetwork().getLinks().get(linkId);
			} 
			//for multi-modal networks: Only links where cars can ride should be used.
			//while (!startLink.getAllowedModes().contains(TransportMode.car));
			while (startLink.getFreespeed() > 10); // park on links with parking facilities
				Vehicle v = new VehicleImpl(Id.create("Taxis"+i, Vehicle.class), startLink, seats, operationStartTime, operationEndTime);
		    vehicles.add(v);    
			
		}
		new VehicleWriter(vehicles).write(taxisFile);
	}

}
