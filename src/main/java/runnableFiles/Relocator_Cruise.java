package runnableFiles;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.data.Fleet;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.path.VrpPaths;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.taxi.optimizer.rules.IdleTaxiZonalRegistry;
import org.matsim.contrib.taxi.schedule.TaxiEmptyDriveTask;
import org.matsim.contrib.taxi.schedule.TaxiStayTask;
import org.matsim.contrib.taxi.schedule.TaxiTask;
import org.matsim.contrib.taxi.scheduler.TaxiScheduler;
import org.matsim.contrib.zone.ZonalSystem;
import org.matsim.contrib.zone.Zone;
import org.matsim.contrib.zone.util.ZoneFinder;
import org.matsim.contrib.zone.util.ZoneFinderImpl;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.router.DijkstraFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

public class Relocator_Cruise {
	// inner city zones
	public static List<String> innerCityZones = Arrays.asList("zone.16","zone.17","zone.19","zone.20","zone.22",
			"zone.23","zone.24","zone.26","zone.28","zone.29","zone.32","zone.34","zone.35","zone.37","zone.39",
			"zone.40","zone.41","zone.42","zone.43","zone.44","zone.46","zone.47","zone.49","zone.50","zone.55",
			"zone.56","zone.57","zone.58","zone.65","zone.71");
	public static List<String> outerCityZones = Arrays.asList("zone.1","zone.2","zone.3","zone.4","zone.5",
			"zone.6","zone.7","zone.8","zone.9","zone.10","zone.11","zone.12","zone.13","zone.15", 
			"zone.18","zone.21","zone.25","zone.27","zone.30","zone.31","zone.33","zone.36","zone.38","zone.45",
			"zone.48","zone.51","zone.52","zone.53","zone.54","zone.59","zone.60","zone.61","zone.62","zone.63",
			"zone.64","zone.66","zone.67","zone.68","zone.69","zone.70","zone.72","zone.73","zone.74","zone.75",
			"zone.76","zone.77","zone.78","zone.79","zone.80","zone.81","zone.82");	
		
	public static Link relocate(Vehicle veh, MobsimTimer timer, Network network, QSim qsim,
			Map<Id<Zone>, List<Id<Link>>> ZoneLink, Fleet fleet, ZonalSystem zonalSystem,
			IdleTaxiZonalRegistry idleTaxiRegistry, TravelDisutility travelDisutility, TravelTime travelTime,
			TaxiScheduler scheduler) {
				
		// 1. vehicles will only move if they have performed first task and have not just relocated
		Schedule schedule = veh.getSchedule();
		TaxiTask currentTask = (TaxiTask) schedule.getCurrentTask();
		Double timeStep = timer.getTimeOfDay();
	
		// 2. Find Link according to desired strategy: CRUISE through network
		Map<Id<Zone>, Zone> zones = zonalSystem.getZones();
		ZoneFinder zoneFinder = new ZoneFinderImpl(zones, 1.0);
		Link currentLink = ((TaxiStayTask) veh.getSchedule().getCurrentTask()).getLink();
		Zone currentZone = zoneFinder.findZone(currentLink.getCoord());
		Id<Link> fLink = currentLink.getId();
		
		if (Relocator.getZonalCruise() == true) {
			String cZone = currentZone.getId().toString();
			if (cZone.contains("83")){
				fLink = networkZonalCruise(outerCityZones, zones);
			}else{
				fLink = zonalCruise(ZoneLink, currentZone);
			}
		}
		
		if (Relocator.getZonalDemandCruise()==true) {
			fLink = zonalDemandCruise(ZoneLink, currentLink, currentZone, timeStep, zones, network);
		}
		
		if (Relocator.getOuterCityCruise() == true) {
			fLink = networkZonalCruise(outerCityZones, zones);
		}
		
		if (Relocator.getInnerCityCruise() == true) {
			fLink = networkZonalCruise(innerCityZones, zones);
		}
		
		if (Relocator.getNetworkCruise()== true) {
			fLink = networkCruise();
		}
		
		Link futureLink = network.getLinks().get(fLink);
		if (currentLink.getId().equals(futureLink.getId()) || currentLink.getToNode() == futureLink.getFromNode() ){								
			return futureLink;
		} 
		
		// 3. Find path to link
		LeastCostPathCalculator router = new DijkstraFactory().createPathCalculator(network, travelDisutility, travelTime);	
		Path path = router.calcLeastCostPath(currentLink.getToNode(), futureLink.getFromNode(), timeStep + 1, null,null);
		VrpPathWithTravelData pathVRP = VrpPaths.createPath(currentLink, futureLink, timeStep + 1, path, travelTime);
		// 4. Create Task based on path
		TaxiEmptyDriveTask relocationTask = new TaxiEmptyDriveTask(pathVRP);
		double endTimeTask = relocationTask.getEndTime();
		if (endTimeTask < veh.getServiceEndTime()) { // only assign parking tasks that can be finished in time		
		// 5. add driveTask to schedule
			currentTask.setEndTime(timeStep + 1);
			schedule.addTask(relocationTask);			
		// 6. add new stayTask to schedule
			TaxiStayTask stayTask = new TaxiStayTask(endTimeTask, endTimeTask + 1.0, futureLink);
			schedule.addTask(stayTask);				
		} else {
			futureLink = currentLink;
		}
		return futureLink;	
	}
	
	public static Id<Link> zonalCruise(Map<Id<Zone>, List<Id<Link>>> ZoneLink, Zone currentZone) {
		List<Id<Link>> linksInZone = ZoneLink.get(currentZone.getId());
		Id<Link> LinkInZone = null;
		if (linksInZone == null) {
			LinkInZone = networkCruise ();
		}else {
			Random r = new Random();
			LinkInZone = linksInZone.get(r.nextInt(linksInZone.size()));
		}		
		return LinkInZone;
	}
	
	public static Id<Link> zonalDemandCruise(Map<Id<Zone>, List<Id<Link>>> ZoneLink, Link currentLink, Zone currentZone, Double timeStep, Map<Id<Zone>, Zone> zones, Network network) {
		Double horizonTime = 300.0;
		Integer numberOfDesiredTopZones = 3;
		
		LinkedHashMap<Id<Zone>, Integer> zonalRequestCounter = new LinkedHashMap<Id<Zone>, Integer>();
		for (Id<Zone> key : ZoneLink.keySet()) {
			zonalRequestCounter.put(key, 0);
		}
		
		HashMap<Integer, Map<Id<Zone>, Integer>> horReq = Counter_ParkingVehicles.getHorzionRequest();
		Integer minT = (int) (timeStep /60);
		Integer horT = (int) (horizonTime/60);
				
		for (int t = minT; t<horT + 1; t++) {
			if (horReq.containsKey(minT)){
				Map<Id<Zone>, Integer> minCount = horReq.get(minT);
				for (Id<Zone> key : ZoneLink.keySet()) {
					Integer minCountPerZone = minCount.get(key);
					Integer counter = zonalRequestCounter.get(key);
					zonalRequestCounter.put(key, counter + minCountPerZone);
				}
			}
		}
		
		if (horReq.isEmpty()) {
			// cruise in zone vehicle is currently in
			Id<Link> LinkInZone =  zonalCruise(ZoneLink, currentZone); 
			return LinkInZone;
		}
		
		Set<Id<Zone>> keys = ZoneLink.keySet();
		for (Id<Zone> keyP : keys) {
			if (zonalRequestCounter.get(keyP) == 0 || keyP.toString().contains("83") ) {
				zonalRequestCounter.remove(keyP);
			}				
		}
		
		if (zonalRequestCounter.isEmpty() == true) {
			// cruise in zone vehicle is currently in
			Id<Link> LinkInZone =  zonalCruise(ZoneLink, currentZone); 
			return LinkInZone;
		} else {
			if (zonalRequestCounter.size() < numberOfDesiredTopZones) {
				numberOfDesiredTopZones = zonalRequestCounter.size();
			}
		}

		// #3: get candidate zones
		zonalRequestCounter = Relocator.sortedZones(zonalRequestCounter);
		ArrayList<Id<Zone>> reducedZoneSet = Relocator.candidateTopZones(zonalRequestCounter, numberOfDesiredTopZones);
		
		// #4: get closest of the top zones
		Zone closestZone = Relocator.closestZone(reducedZoneSet, currentLink, zones, network, currentZone);
			
		Id<Link> LinkInZone = zonalCruise(ZoneLink, closestZone); 
		return LinkInZone;
		
	}

	
	public static Id<Link> networkZonalCruise(List<String> greaterZone, Map<Id<Zone>, Zone> zones) {
		Map<Id<Zone>, List<Id<Link>>> mapLinksZone = Counter_ParkingVehicles.getZoneLink();
		List<Id<Zone>> zonesToBeRemove = new ArrayList<Id<Zone>>();
		for (Map.Entry<Id<Zone>, List<Id<Link>>> entry : mapLinksZone.entrySet()) {
		    String key = entry.getKey().toString();
		    if (!greaterZone.contains(key)) {
		    	zonesToBeRemove.add(entry.getKey());
		    }
		    if (mapLinksZone.containsKey(entry.getKey()) && mapLinksZone.get(entry.getKey()).isEmpty()) {
		    	zonesToBeRemove.add(entry.getKey());
		    }
		}
		for (int i = 0; i <zonesToBeRemove.size(); i++) {
			mapLinksZone.remove(zonesToBeRemove.get(i));
		}
		
		Id<Zone> zoneId = getRandomZone (mapLinksZone);
		List<Id<Link>> linksZone = mapLinksZone.get(zoneId);
		Id<Link> linkId = getRandomLink (linksZone);
		return linkId;
	}
	
	 public static Id<Zone> getRandomZone(Map<Id<Zone>, List<Id<Link>>>  mapLinksZone){
		Set<Id<Zone>> zonesToSelectFrom = mapLinksZone.keySet();
		int size = zonesToSelectFrom.size();
		int item = new Random().nextInt(size); 
		int i = 0;
		for(Id<Zone> zoneId : zonesToSelectFrom){
			if (i == item)
			    return zoneId;
			i++;
		}
		return null;	 
	 }
	 
	 public static Id<Link> getRandomLink (List<Id<Link>>  linksInRandomZone){
		Random rand = new Random();
		int ListItem = rand.nextInt(linksInRandomZone.size());
		Id<Link> anticipatedLinkID = linksInRandomZone.get(ListItem);
		return anticipatedLinkID;
	 }

	public static Id<Link> networkCruise() {
		Map<Id<Zone>, List<Id<Link>>> mapLinksZone = Counter_ParkingVehicles.getZoneLink();
		Id<Link> Link = null;
		boolean zeroLinksInZone = true;
		do {
			int r1 = new Random().nextInt(mapLinksZone.size());
			ArrayList<Id<Zone>> keysAsArray = new ArrayList<Id<Zone>>(mapLinksZone.keySet());
			Id<Zone> randZone = keysAsArray.get(r1);
			List<Id<Link>> LinksRandZone = mapLinksZone.get(randZone);
			if (LinksRandZone.size()> 0) {
				int r2 = new Random().nextInt(LinksRandZone.size());
				Link = LinksRandZone.get(r2);
				zeroLinksInZone = false;
			}
			
		}while (zeroLinksInZone == true) ;
		return Link;
	}
}