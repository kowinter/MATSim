package runnableFiles;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Set;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.data.Fleet;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.path.VrpPaths;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.StayTask;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.dvrp.util.LinkTimePair;
import org.matsim.contrib.taxi.schedule.TaxiEmptyDriveTask;
import org.matsim.contrib.taxi.schedule.TaxiStayTask;
import org.matsim.contrib.taxi.schedule.TaxiTask;
import org.matsim.contrib.taxi.scheduler.TaxiScheduler;
import org.matsim.contrib.util.distance.DistanceCalculator;
import org.matsim.contrib.util.distance.DistanceCalculators;
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

public class Relocator {
	////////////////////////////////////////////////////////
	// Select Strategy here
	private static Double horizonTime = 300.0; // 5 min
	private static int numberOfDesiredTopZones = 3;
	/////////////////////////////////////////////////////
	private static boolean noRelocation = true;
	////////////////////////////////////////////////////
	private static boolean demand = false;
	private static boolean supply = false;
	private static boolean demandSupply = false;
	private static boolean zonal = false;
	/////////////////////////////////////////////////////
	private static boolean cruise = false;
	public static boolean zoneCruise = false;
	public static boolean zoneDemandCruise = false;
	public static boolean innerCityCruise = false;
	public static boolean outerCityCruise = false;
	public static boolean networkCruise = false;
	public static boolean initialCruise = false;
	/////////////////////////////////////////////////////
	public static boolean noInnerCityParking = false;
	public static boolean noInnerCityParkingDayTime = false;
	public static Double beginnDayTime = 36000.0; // 10 a.m.
	public static Double endDayTime = 64800.0; // 6 p.m.
	public static boolean timeLimitedInnerCityParking = false;
	public static Double timeLimitInnerCityParking = (60.0 * 60.0 - 1.0); // 60 min
	/////////////////////////////////////////////////////////
	
	// inner city zones
	public static List<String> innerCityZones = Arrays.asList("zone.16", "zone.17", "zone.19", "zone.20", "zone.22",
			"zone.23", "zone.24", "zone.26", "zone.28", "zone.29", "zone.32", "zone.34", "zone.35", "zone.37",
			"zone.39", "zone.40", "zone.41", "zone.42", "zone.43", "zone.44", "zone.46", "zone.47", "zone.49",
			"zone.50", "zone.55", "zone.56", "zone.57", "zone.58", "zone.65", "zone.71");
	public static List<String> outerCityZones = Arrays.asList("zone.1", "zone.2", "zone.3", "zone.4", "zone.5",
			"zone.6", "zone.7", "zone.8", "zone.9", "zone.10", "zone.11", "zone.12", "zone.13", "zone.14", "zone.15",
			"zone.18", "zone.21", "zone.25", "zone.27", "zone.30", "zone.31", "zone.33", "zone.36", "zone.38",
			"zone.45", "zone.48", "zone.51", "zone.52", "zone.53", "zone.54", "zone.59", "zone.60", "zone.61",
			"zone.62", "zone.63", "zone.64", "zone.66", "zone.67", "zone.68", "zone.69", "zone.70", "zone.72",
			"zone.73", "zone.74", "zone.75", "zone.76", "zone.77", "zone.78", "zone.79", "zone.80", "zone.81",
			"zone.82");

	public static Link relocate(Vehicle veh, MobsimTimer timer, Network network, QSim qsim,
			Map<Id<Zone>, List<Id<Link>>> ZoneLink, Fleet fleet, ZonalSystem zonalSystem,
			TravelDisutility travelDisutility, TravelTime travelTime, TaxiScheduler scheduler,
			boolean hasToMoveFromInnerCity) {

		// 1. vehicles will only move if they have not just relocated
		Double timeStep = timer.getTimeOfDay();
		Double horTime = timeStep + horizonTime; // 5 min
		Schedule schedule = veh.getSchedule();
		TaxiTask currentTask = (TaxiTask) schedule.getCurrentTask();
		Map<Id<Zone>, Zone> zones = zonalSystem.getZones();
		ZoneFinder zoneFinder = new ZoneFinderImpl(zones, 1.0);
		Link currentLink = ((TaxiStayTask) veh.getSchedule().getCurrentTask()).getLink();
		Zone currentZone = zoneFinder.findZone(currentLink.getCoord());
		Map<Id<Zone>, Integer> freeParkingPerZone = Counter_ParkingVehicles.getZoneParking();

		if (noRelocation == true) {
			return currentLink;
		}

		int counter = 0;

		boolean isDayTime = false;
		if (beginnDayTime < timeStep && timeStep < endDayTime && noInnerCityParkingDayTime == true) {
			isDayTime = true;
		}
		if (hasToMoveFromInnerCity == true) {
			noInnerCityParking = true;
		}

		for (Id<Zone> keyP : zones.keySet()) {
			counter = counter + freeParkingPerZone.get(keyP);
			if (freeParkingPerZone.get(keyP) < 1 || keyP.toString().contains("83")) {
				freeParkingPerZone.remove(keyP);
			} else {
				if (innerCityZones.contains(keyP.toString())) {
					if (noInnerCityParking == true || isDayTime == true) {
						if (freeParkingPerZone.containsKey(keyP)) {
							freeParkingPerZone.remove(keyP);
						}
					}
				}
			}
		}

		if (timer.getTimeOfDay() > 19600) {
			// System.out.println("Debug from here");
		}

		// 2. Find Link according to desired strategy
		Id<Link> newLink = currentLink.getId();
		Link futureLink = null;

		if (cruise == true) {
			futureLink = networkCruise(network);
		}
		if (demand == true) {
			newLink = getDemandAnticipationLink(freeParkingPerZone, timeStep, horTime, network, qsim, ZoneLink, zones,zoneFinder, currentLink, currentZone, numberOfDesiredTopZones);
			futureLink = network.getLinks().get(newLink);
		}
		if (supply == true) {
			newLink = getSupplyAnticipationLink(freeParkingPerZone, timeStep, currentLink, veh, fleet, network, horTime,ZoneLink, zones, zoneFinder, scheduler, numberOfDesiredTopZones, currentZone);
			futureLink = network.getLinks().get(newLink);
		}
		if (demandSupply == true) {
			newLink = getDemandSupplyBalancingLink(freeParkingPerZone, currentZone, timeStep, currentLink, veh, fleet,horTime, network, qsim, ZoneLink, zones, zoneFinder, scheduler, numberOfDesiredTopZones);
			futureLink = network.getLinks().get(newLink);
		}
		if (zonal == true) {
			newLink = randomLinkInZone (currentZone, network);
			futureLink = network.getLinks().get(newLink);
		}

		// in case no link could be found according to the selected strategy:
		// remain in zone or move to a random other zone
		if (futureLink == null) {
			// step1: try parking in zone
			Id<Link> randLinkId = null;
			if (zoneIsInnerCity(currentZone.getId()) == false) {
				randLinkId = randomLinkInZone(currentZone, network);
			}

			if (randLinkId == null) {
				// step2: park at closest zone
				ArrayList<Id<Zone>> zonesWithParking = new ArrayList<Id<Zone>>();
				for (Id<Zone> key : freeParkingPerZone.keySet()) {
					if (!zonesWithParking.contains(key)) {
						zonesWithParking.add(key);
					}
				}
				Zone closestZone = closestZone(zonesWithParking, currentLink, zones, network, currentZone);
				randLinkId = randomLinkInZone(closestZone, network);
				futureLink = network.getLinks().get(randLinkId);
				Counter_ParkingVehicles.countRelocationRandomAlternative();
			} else {
				futureLink = network.getLinks().get(randLinkId);
				Counter_ParkingVehicles.countRelocationZoneAlternative();
			}
		}

		if (currentLink.getId().equals(futureLink.getId()) || currentLink.getToNode() == futureLink.getFromNode()) {
			Map<Vehicle, Link> taxisWithParkingReservation = Counter_ParkingVehicles.getTaxisWithParkingReservation();
			if (taxisWithParkingReservation.containsKey(veh)) {
				Link linkWithReservation = taxisWithParkingReservation.get(veh);
				Counter_ParkingVehicles.unpark(linkWithReservation, veh);
			}
			Counter_ParkingVehicles.park(futureLink, veh, timer);
			return futureLink;
		}

		// 3. Find path to link
		LeastCostPathCalculator router = new DijkstraFactory().createPathCalculator(network, travelDisutility,
				travelTime);
		Path path = router.calcLeastCostPath(currentLink.getToNode(), futureLink.getFromNode(), timeStep + 1, null,
				null);
		VrpPathWithTravelData pathVRP = VrpPaths.createPath(currentLink, futureLink, timeStep + 1, path, travelTime);

		// 4. Create Task based on path
		TaxiEmptyDriveTask relocationTask = new TaxiEmptyDriveTask(pathVRP);
		double endTimeTask = relocationTask.getEndTime();
		if (endTimeTask < veh.getServiceEndTime()) { // only assign parking
														// tasks that can be
														// finished in time
			// 5. add driveTask to schedule
			currentTask.setEndTime(timeStep + 1);
			schedule.addTask(relocationTask);
			// 6. add new stayTask to schedule
			TaxiStayTask stayTask = new TaxiStayTask(endTimeTask, endTimeTask + 1.0, futureLink);
			schedule.addTask(stayTask);
		} else {
			futureLink = currentLink;
		}

		Counter_ParkingVehicles.park(futureLink, veh, timer);
		Counter_ParkingVehicles.countRelocation();
		// Counter_ParkingVehicles.updateTaxisEarliestIdleness(veh,
		// zoneFinder.findZone(futureLink.getCoord()), endTimeTask);
		return futureLink;
	}

	public static Id<Link> getDemandAnticipationLink(Map<Id<Zone>, Integer> freeParkingPerZone, double timeStep,
			double horizonTime, Network network, QSim qsim, Map<Id<Zone>, List<Id<Link>>> ZoneLink,
			Map<Id<Zone>, Zone> zones, ZoneFinder zoneFinder, Link currentLink, Zone currentZone,
			Integer numberOfDesiredTopZones) {
		
		// #D1: get all open requests per zone
		LinkedHashMap<Id<Zone>, Integer> zonalRequestCounter = new LinkedHashMap<Id<Zone>, Integer>();
		for (Id<Zone> key : zones.keySet()) {
			zonalRequestCounter.put(key, 0);
		}
		HashMap<Integer, Map<Id<Zone>, Integer>> horReq = Counter_ParkingVehicles.getHorzionRequest();
		Integer minT = (int) (timeStep / 60);
		Integer horT = (int) (horizonTime / 60);

		for (int t = minT; t < horT + 1; t++) {
			if (horReq.containsKey(minT)) {
				Map<Id<Zone>, Integer> minCount = horReq.get(minT);
				for (Id<Zone> key : zones.keySet()) {
					Integer minCountPerZone = minCount.get(key);
					Integer counter = zonalRequestCounter.get(key);
					zonalRequestCounter.put(key, counter + minCountPerZone);
				}
			}
		}
		if (horReq.isEmpty()) {
			return null;
		}

		// #D2: remove zones without free parking
		Set<Id<Zone>> keys = zones.keySet();
		for (Id<Zone> keyP : keys) {
			Integer freeParkingInZone = Counter_ParkingVehicles.getParkingInZone(zones.get(keyP));
			Integer freeParking = freeParkingPerZone.get(keyP);
			if (freeParking == null || freeParkingInZone == null) {
				zonalRequestCounter.remove(keyP);
			} else if (!zonalRequestCounter.containsKey(keyP) || zonalRequestCounter.get(keyP) == 0 || freeParking == 0
					|| freeParkingInZone == 0) {
				zonalRequestCounter.remove(keyP);
			}
		}
		if (zonalRequestCounter.isEmpty() == true) {
			return null;
		} else {
			if (zonalRequestCounter.size() < numberOfDesiredTopZones) {
				numberOfDesiredTopZones = zonalRequestCounter.size();
			}
		}

		// #D3: get candidate zones
		zonalRequestCounter = sortedZones(zonalRequestCounter);
		// ArrayList<Id<Zone>> reducedZoneSet =
		// candidateTopZones(zonalRequestCounter, numberOfDesiredTopZones);
		ArrayList<Id<Zone>> reducedZoneSet = candidateBottomZones(zonalRequestCounter, numberOfDesiredTopZones);

		// #D4: get closest of the top zones
		Zone closestZone = closestZone(reducedZoneSet, currentLink, zones, network, currentZone);

		// #D5: get link in zones
		Id<Link> smartLink = randomLinkInZone(closestZone, network);

		return smartLink;
	}

	public static Id<Link> getSupplyAnticipationLink(Map<Id<Zone>, Integer> freeParkingPerZone, Double timeStep,
			Link currentLink, Vehicle vehicle, Fleet fleet, Network network, Double horizonTime,
			Map<Id<Zone>, List<Id<Link>>> ZoneLink, Map<Id<Zone>, Zone> zones, ZoneFinder zoneFinder,
			TaxiScheduler scheduler, Integer numberOfDesiredTopZones, Zone currentZone) {

		// #S1: initialize vehicle counter: only zones with free parking
		LinkedHashMap<Id<Zone>, Integer> zonalVecCounter = new LinkedHashMap<Id<Zone>, Integer>();
		for (Id<Zone> key : zones.keySet()) {
			zonalVecCounter.put(key, 0);
		}

		Set<Id<Zone>> keys = zones.keySet();
		for (Id<Zone> keyP : keys) {
			Integer freeParkingInZone = Counter_ParkingVehicles.getParkingInZone(zones.get(keyP));
			Integer freeParking = freeParkingPerZone.get(keyP);
			if (freeParking == null || freeParkingInZone == null) {
				zonalVecCounter.remove(keyP);
			} else if (!zonalVecCounter.containsKey(keyP) || freeParking == 0 || freeParkingInZone == 0) {
				zonalVecCounter.remove(keyP);
			}
		}

		// #S2: get all expected free vehicles within horizon time per zone
		Map<Id<Vehicle>, ? extends Vehicle> vehFleet = fleet.getVehicles();
		for (Map.Entry<Id<Vehicle>, ? extends Vehicle> entry : vehFleet.entrySet()) {
			List<? extends Task> TaskList = entry.getValue().getSchedule().getTasks();
			StayTask lastTask = (StayTask) TaskList.get(TaskList.size() - 1);
			if (lastTask.getBeginTime() <= (horizonTime)) {
				Id<Zone> lastTaskZoneId = zoneFinder.findZone(lastTask.getLink().getCoord()).getId();
				if (zonalVecCounter.containsKey(lastTaskZoneId)) {
					int vehInLastTaskZone = zonalVecCounter.get(lastTaskZoneId);
					zonalVecCounter.put(lastTaskZoneId, vehInLastTaskZone + 1);
				}
			}
		}

		LinkedHashMap<Id<Zone>, Integer> zonalVehicleCounter = zonalVecCounter;

		if (zonalVehicleCounter.isEmpty()) {
			return null;
		} else if (zonalVehicleCounter.size() < numberOfDesiredTopZones) {
			numberOfDesiredTopZones = zonalVehicleCounter.size();
		}

		// #S3: sort the zones in order && get zones with lowest number of free vehicles
		zonalVehicleCounter = sortedZones(zonalVehicleCounter);
		// ArrayList<Id<Zone>> reducedZoneSet =candidateBottomZones(zonalVehicleCounter, numberOfDesiredTopZones);
		ArrayList<Id<Zone>> reducedZoneSet = candidateTopZones(zonalVehicleCounter, numberOfDesiredTopZones);

		// #S4: get closest of the top zones
		Zone closestZone = closestZone(reducedZoneSet, currentLink, zones, network, currentZone);

		// #S5: get link in zones with the largest number of free parking spots
		Id<Link> smartLink = randomLinkInZone(closestZone, network);

		return smartLink;
	}

	public static Id<Link> getDemandSupplyBalancingLink(Map<Id<Zone>, Integer> freeParkingPerZone, Zone currentZone,
			Double timeStep, Link currentLink, Vehicle vehicle, Fleet fleet, Double horizonTime, Network network,
			QSim qsim, Map<Id<Zone>, List<Id<Link>>> ZoneLink, Map<Id<Zone>, Zone> zones, ZoneFinder zoneFinder,
			TaxiScheduler scheduler, Integer numberOfDesiredTopZones) {

		// #B1: get all open requests per zone
		LinkedHashMap<Id<Zone>, Integer> zonalRequestCounter = new LinkedHashMap<Id<Zone>, Integer>();
		for (Id<Zone> key : zones.keySet()) {
			zonalRequestCounter.put(key, 0);
		}
		HashMap<Integer, Map<Id<Zone>, Integer>> horReq = Counter_ParkingVehicles.getHorzionRequest();
		Integer minT = (int) (timeStep / 60);
		Integer horT = (int) (horizonTime / 60);

		for (int t = minT; t < horT + 1; t++) {
			if (horReq.containsKey(minT)) {
				Map<Id<Zone>, Integer> minCount = horReq.get(minT);
				for (Id<Zone> key : zones.keySet()) {
					Integer minCountPerZone = minCount.get(key);
					Integer counter = zonalRequestCounter.get(key);
					zonalRequestCounter.put(key, counter + minCountPerZone);
				}
			}
		}
		if (horReq.isEmpty()) {
			return null;
		}

		// #B2: remove zones without free parking 
		Set<Id<Zone>> keys = zones.keySet();
		for (Id<Zone> keyP : keys) {
			Integer freeParkingInZone = Counter_ParkingVehicles.getParkingInZone(zones.get(keyP));
			Integer freeParking = freeParkingPerZone.get(keyP);
			if (freeParking == null || freeParkingInZone == null) {
				zonalRequestCounter.remove(keyP);
			} else if (!zonalRequestCounter.containsKey(keyP) || freeParking == 0|| freeParkingInZone == 0) {
				zonalRequestCounter.remove(keyP);
			}
		}
		if (zonalRequestCounter.isEmpty() == true) {
			return null;
		} 

		// #B3: Match requests with idle vehicles
		// LinkedHashMap<Id<Zone>, Integer> zonalMatchCounter =horizonRequestMatcher(fleet, zonalRequestCounter, network,timeStep, horizonTime, qsim, zoneFinder, zones);
		Map<Id<Vehicle>, ? extends Vehicle> vehFleet = fleet.getVehicles();
		for (Map.Entry<Id<Vehicle>, ? extends Vehicle> entry : vehFleet.entrySet()) {
			List<? extends Task> TaskList = entry.getValue().getSchedule().getTasks();
			StayTask lastTask = (StayTask) TaskList.get(TaskList.size() - 1);
			if (lastTask.getBeginTime() <= (horizonTime)) {
				Id<Zone> lastTaskZoneId = zoneFinder.findZone(lastTask.getLink().getCoord()).getId();
				if (zonalRequestCounter.containsKey(lastTaskZoneId)) {
					int openReqInLastTaskZone = zonalRequestCounter.get(lastTaskZoneId);
					//if (openReqInLastTaskZone <= 0) {
						//zonalRequestCounter.remove(lastTaskZoneId);
					//} else {
						zonalRequestCounter.put(lastTaskZoneId, openReqInLastTaskZone - 1);
					//}
				}
			}
		}

		LinkedHashMap<Id<Zone>, Integer> zonalMatchCounter = zonalRequestCounter;

		// #B4: get Zones with the highest request-vehicle deficit
		if (zonalRequestCounter.isEmpty() == true) {
			return null;
		} else {
			if (zonalRequestCounter.size() < numberOfDesiredTopZones) {
				numberOfDesiredTopZones = zonalRequestCounter.size();
			}
		}
		zonalMatchCounter = sortedZones(zonalMatchCounter);
		// ArrayList<Id<Zone>> reducedZoneSet = candidateTopZones(zonalMatchCounter, numberOfDesiredTopZones);
		ArrayList<Id<Zone>> reducedZoneSet = candidateBottomZones(zonalMatchCounter, numberOfDesiredTopZones);

		// #B5: get closest of the top zones
		Zone closestZone = closestZone(reducedZoneSet, currentLink, zones, network, currentZone);

		// #B6: get link in zones with the largest number of free parking spots
		if (closestZone == null) {
			System.out.println("Problem (Relocator l.293)");
		}
		if (closestZone.getId().toString().contains("zone.83")) {
			System.out.println("Problem (Relocator l.296)");
		}

		Id<Link> smartLink = randomLinkInZone(closestZone, network);

		return smartLink;
	}

	public static LinkedHashMap<Id<Zone>, Integer> horizonVehicles(Map<Id<Zone>, Integer> freeParkingPerZone,
			Network network, Fleet fleet, Double timeStep, Double horizonTime, Map<Id<Zone>, Zone> zones,
			ZoneFinder zoneFinder, TaxiScheduler scheduler) {
		// initialize vehicle counter: only zones with free parking
		LinkedHashMap<Id<Zone>, Integer> vehCounter = new LinkedHashMap<Id<Zone>, Integer>();
		for (Id<Zone> key : freeParkingPerZone.keySet()) {
			vehCounter.put(key, 0);
		}
		// get all expected free vehicles within horizon time per zone
		Map<Id<Vehicle>, ? extends Vehicle> vehFleet = fleet.getVehicles();
		for (Map.Entry<Id<Vehicle>, ? extends Vehicle> entry : vehFleet.entrySet()) {
			LinkTimePair linkTimeFree = scheduler.getEarliestIdleness(entry.getValue());
			if (linkTimeFree != null) {
				if (linkTimeFree.time <= horizonTime) {
					if (vehCounter.containsKey(zoneFinder.findZone(linkTimeFree.link.getCoord()).getId())) {
						int vehInLastTaskZone = vehCounter
								.get(zoneFinder.findZone(linkTimeFree.link.getCoord()).getId());
						vehCounter.put(zoneFinder.findZone(linkTimeFree.link.getCoord()).getId(),
								vehInLastTaskZone + 1);
					}
				}
			}
		}
		return vehCounter;
	}

	public static Zone closestZone(ArrayList<Id<Zone>> reducedZoneSet, Link currentLink, Map<Id<Zone>, Zone> zones,
			Network network, Zone currentZone) {
		Coord currentCoord = currentLink.getCoord();
		Zone closestZone = currentZone;
		double OldDistance = 9999999999999.99;
		for (int i = 0; i < reducedZoneSet.size(); i++) {
			Id<Zone> zoneId = reducedZoneSet.get(i);
			Zone zone = zones.get(zoneId);
			Coord zoneCoord = zone.getCoord();
			DistanceCalculator diCa = DistanceCalculators.crateFreespeedDistanceCalculator(network);
			double distance = diCa.calcDistance(currentCoord, zoneCoord);
			if (OldDistance > distance) {
				closestZone = zone;
			}
		}
		return closestZone;
	}

	public static ArrayList<Id<Zone>> candidateTopZones(LinkedHashMap<Id<Zone>, Integer> mapToBeReduced,
			Integer numberOfDesiredTopZones) {
		ArrayList<Id<Zone>> candidateZones = new ArrayList<Id<Zone>>();
		int i = 0;
		for (Id<Zone> keyP : mapToBeReduced.keySet()) {
			if (i < numberOfDesiredTopZones) {
				candidateZones.add(keyP);
				i = i + 1;
			} else {
				return candidateZones;
			}
		}
		return candidateZones;
	}

	public static ArrayList<Id<Zone>> candidateBottomZones(LinkedHashMap<Id<Zone>, Integer> mapToBeReduced,
			Integer numberOfDesiredTopZones) {
		ArrayList<Id<Zone>> candidateZones = new ArrayList<Id<Zone>>();
		int i = 0;
		ArrayList<Id<Zone>> alKeys = new ArrayList<Id<Zone>>(mapToBeReduced.keySet());
		Collections.reverse(alKeys);
		for (Id<Zone> keyP : alKeys) {
			if (i < numberOfDesiredTopZones) {
				candidateZones.add(keyP);
				i = i + 1;
			} else {
				return candidateZones;
			}
		}
		return candidateZones;
	}

	public static Link networkCruise(Network network) {
		Map<Id<Link>, ? extends Link> links = network.getLinks();
		Set<Id<Link>> keySet = links.keySet();
		List<Id<Link>> keyList = new ArrayList<Id<Link>>(keySet);
		int r = new Random().nextInt(links.size());
		Link Link = links.get(keyList.get(r));
		return Link;
	}

	public static Map<Id<Link>, Zone> zonePerLink(ZonalSystem zonalSystem, Network network) {
		ZoneFinder zoneFinder = new ZoneFinderImpl(zonalSystem.getZones(), 1.0);
		Map<Id<Link>, Zone> zonePerLink = new HashMap<Id<Link>, Zone>();
		Map<Id<Link>, ? extends Link> links = network.getLinks();
		for (Id<Link> keyL : links.keySet()) {
			Coord coord = links.get(keyL).getCoord();
			Zone zone = zoneFinder.findZone(coord);
			zonePerLink.put(keyL, zone);
		}
		return zonePerLink;
	}

	public static LinkedHashMap<Id<Zone>, Integer> horizonRequestMatcher(Fleet fleet,
			LinkedHashMap<Id<Zone>, Integer> zonalRequestCounter, Network network, Double timeStep, Double horizonTime,
			QSim qsim, ZoneFinder zoneFinder, Map<Id<Zone>, Zone> zones) {
		// get all expected free vehicles within horizon time per zone
		Map<Id<Vehicle>, ? extends Vehicle> vehFleet = fleet.getVehicles();
		for (Map.Entry<Id<Vehicle>, ? extends Vehicle> entry : vehFleet.entrySet()) {
			List<? extends Task> TaskList = entry.getValue().getSchedule().getTasks();
			StayTask lastTask = (StayTask) TaskList.get(TaskList.size() - 1);
			if (lastTask.getBeginTime() <= (horizonTime)) {
				Id<Zone> lastTaskZoneId = zoneFinder.findZone(lastTask.getLink().getCoord()).getId();
				if (zonalRequestCounter.containsKey(lastTaskZoneId)) {
					int vehInLastTaskZone = zonalRequestCounter.get(lastTaskZoneId);
					zonalRequestCounter.put(lastTaskZoneId, vehInLastTaskZone - 1);
				}
			}
		}
		return zonalRequestCounter;
	}

	public static Zone getMaxZone(ZonalSystem zonalSystem, LinkedHashMap<Id<Zone>, Integer> zonalMatchCounter,
			Zone currentZone) {
		Id<Zone> zoneMaxReqId = currentZone.getId();
		int maxValueInMap = (Collections.max(zonalMatchCounter.values()));
		for (Entry<Id<Zone>, Integer> entry : zonalMatchCounter.entrySet()) {
			if (entry.getValue() == maxValueInMap) {
				zoneMaxReqId = entry.getKey(); // this is the key which has the max value
			}
		}
		Zone zoneMax = (Zone) zonalSystem.getZones().get(zoneMaxReqId);

		return zoneMax;
	}

	public static LinkedHashMap<Id<Zone>, Integer> sortedZones(LinkedHashMap<Id<Zone>, Integer> mapToBeSorted) {
		List<Entry<Id<Zone>, Integer>> list = new LinkedList<>(mapToBeSorted.entrySet());
		Collections.sort(list, new Comparator<Object>() {
			@SuppressWarnings("unchecked")
			public int compare(Object o1, Object o2) {
				return ((Comparable<Integer>) ((Map.Entry<Id<Zone>, Integer>) (o1)).getValue())
						.compareTo(((Map.Entry<Id<Zone>, Integer>) (o2)).getValue());
			}
		});

		LinkedHashMap<Id<Zone>, Integer> resultingMap = new LinkedHashMap<>();
		for (Iterator<Entry<Id<Zone>, Integer>> it = list.iterator(); it.hasNext();) {
			Map.Entry<Id<Zone>, Integer> entry = (Map.Entry<Id<Zone>, Integer>) it.next();
			resultingMap.put(entry.getKey(), entry.getValue());
		}
		return resultingMap;
	}

	public static PriorityQueue<Id<Zone>> topZonesWithParking(LinkedHashMap<Id<Zone>, Integer> zonalMatchCounter,
			Integer numberOfDesiredTopZones) {
		int n = numberOfDesiredTopZones;
		PriorityQueue<Id<Zone>> topN = null;
		if (zonalMatchCounter.size() > 1) {
			topN = new PriorityQueue<Id<Zone>>(n, new Comparator<Id<Zone>>() {
				public int compare(Id<Zone> s1, Id<Zone> s2) {
					return Double.compare(zonalMatchCounter.get(s1), zonalMatchCounter.get(s2));
				}
			});
			for (Id<Zone> key : zonalMatchCounter.keySet()) {
				if (topN.size() < n)
					topN.add(key);
				else if (zonalMatchCounter.get(topN.peek()) < zonalMatchCounter.get(key)) {
					topN.poll();
					topN.add(key);
				}
			}
		}
		return topN;

	}

	public static Zone closestTopZone(Integer n, Network network, LinkedHashMap<Id<Zone>, Integer> zonalMatchCounter,
			Zone currentZone, Link currentLink, Map<Id<Zone>, Zone> zones) {
		Coord currentCoord = currentLink.getCoord();
		Map<Id<Zone>, Integer> distancesToVehicle = new HashMap<Id<Zone>, Integer>();

		if (n > zonalMatchCounter.size()) {
			n = zonalMatchCounter.size();
		}

		int counter = 1;
		for (Id<Zone> z : zonalMatchCounter.keySet()) {
			if (counter <= n) {
				Zone zoneMin = zones.get(z);
				Coord zoneCoord = zoneMin.getCoord();
				DistanceCalculator diCa = DistanceCalculators.crateFreespeedDistanceCalculator(network);
				double distance = diCa.calcDistance(currentCoord, zoneCoord);
				distancesToVehicle.put(z, (int) distance);
				counter = counter + 1;
			}
		}

		Id<Zone> minDistance = distancesToVehicle.entrySet().stream()
				.min((entry1, entry2) -> entry1.getValue() > entry2.getValue() ? 1 : -1).get().getKey();
		Zone closestZone = zones.get(minDistance);

		return closestZone;
	}

	public static Zone zoneWithMinVeh(LinkedHashMap<Id<Zone>, Integer> vehCounter, Link currentLink,
			Map<Id<Zone>, Zone> zones, ZoneFinder zoneFinder) {
		Map<Id<Zone>, Integer> freeParkingPerZone = Counter_ParkingVehicles.getZoneParking();
		// get zone with the least amount of vehicles (that has free parking space)
		Entry<Id<Zone>, Integer> min = null;
		for (Entry<Id<Zone>, Integer> entry : vehCounter.entrySet()) {
			if (min == null || min.getValue() > entry.getValue()) {
				if (freeParkingPerZone.containsKey(entry.getKey())) {
					if (freeParkingPerZone.get(entry.getKey()) > 0) {
						min = entry;
					}
				}
			}
		}
		return zones.get(min.getKey());
	}

	public static Link LinkInZone(ZonalSystem zonalSystem, Zone maxZone, Link currentLink, Network network) {
		Link smartLink = currentLink;
		List<Id<Link>> linkIdsInZoneMax = new ArrayList<Id<Link>>();
		Map<Id<Link>, ? extends Link> links = network.getLinks();

		for (Id<Link> key : links.keySet()) {
			Id<Link> thisId = key;
			ZoneFinder zoneFinder = new ZoneFinderImpl(zonalSystem.getZones(), 1.0);
			Coord coord = links.get(thisId).getCoord();
			Zone zone = zoneFinder.findZone(coord);
			if (zone != maxZone) {
				linkIdsInZoneMax.add(thisId);
			}
		}

		Random rand = new Random();
		if (linkIdsInZoneMax.size() > 0) {
			int ListItem = rand.nextInt(linkIdsInZoneMax.size());
			Id<Link> anticipatedLinkID = linkIdsInZoneMax.get(ListItem);
			Link anticipatedLink = links.get(anticipatedLinkID);
			smartLink = anticipatedLink;
		} else {
			smartLink = currentLink;
		}
		return smartLink;

	}

	public static Id<Link> randomLinkInZone(Zone closestZone, Network network) {
		Random rand = new Random();
		Link anticipatedLink = null;
		List<Id<Link>> parkingInZone = Counter_ParkingVehicles.getLinksWithFreeParkingInZone(closestZone);
		if (parkingInZone.size() > 0) {
			int ListItem = rand.nextInt(parkingInZone.size());
			Id<Link> anticipatedLinkID = parkingInZone.get(ListItem);
			int parkingOnThisLink = Counter_ParkingVehicles.getParkingOnLink(anticipatedLinkID);
			if (parkingOnThisLink > 0) {
				anticipatedLink = network.getLinks().get(anticipatedLinkID);
			}
		} else {
			return null;
		}

		return anticipatedLink.getId();
	}

	public static boolean getNoReloaction() {
		return noRelocation;
	}

	public static boolean getTimeLimitedInnerCityParking() {
		return timeLimitedInnerCityParking;
	}

	public static Double getTimeLimitInnerCityParking() {
		return timeLimitInnerCityParking;
	}

	public static boolean getLimitInnerCityParkingDaytime(Double currentTime) {
		boolean isDayTimeLimitedAndDayTime = false;
		if (currentTime > beginnDayTime && currentTime < endDayTime && noInnerCityParkingDayTime == true) {
			isDayTimeLimitedAndDayTime = true;
		}
		return isDayTimeLimitedAndDayTime;
	}

	public static Double getDayStartTime() {
		return beginnDayTime;
	}

	public static boolean getCruise() {
		return cruise;
	}

	public static boolean getZonalCruise() {
		return zoneCruise;
	}

	public static boolean getZonalDemandCruise() {
		return zoneDemandCruise;
	}

	public static boolean getOuterCityCruise() {
		return outerCityCruise;
	}

	public static boolean getInnerCityCruise() {
		return innerCityCruise;
	}

	public static boolean getNetworkCruise() {
		return networkCruise;
	}

	public static boolean getInitialCruise() {
		return initialCruise;
	}

	public static boolean getNoInnerCityParking() {
		return noInnerCityParking;
	}

	public static boolean zoneIsInnerCity(Id<Zone> zone) {
		String zoneId = zone.toString();
		boolean innerCity = innerCityZones.contains(zoneId);
		return innerCity;
	}

	public static double getParkingTimeLimitInnerCity(Double currentTime) {
		Double moveTime = 104400.0;
		if (noInnerCityParking == true || timeLimitedInnerCityParking == true) {
			moveTime = currentTime + timeLimitInnerCityParking;
		}

		if (noInnerCityParkingDayTime == true) {
			if (currentTime < beginnDayTime) {
				moveTime = beginnDayTime + timeLimitInnerCityParking;
			} else if (currentTime > (endDayTime - timeLimitInnerCityParking)) {
				moveTime = 104400.0;
			} else {
				moveTime = currentTime + timeLimitInnerCityParking;
			}
		}
		return moveTime;
	}

}

