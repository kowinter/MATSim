package runnableFiles;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.zone.Zone;
import org.matsim.contrib.zone.util.ZoneFinder;
import org.matsim.contrib.zone.util.ZoneFinderImpl;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.contrib.dvrp.data.Vehicle;

public class Counter_ParkingVehicles {

	static Map<Id<Link>, Integer> LinkParking;
	static Map<Id<Link>, Integer> ParkingLinks;
	static Map<Id<Zone>, Integer> ZoneParking;
	static Map<Id<Zone>, Integer> ParkingZones;
	static Map<Id<Zone>, List<Id<Link>>> ZoneLink;
	static Map<Id<Zone>, Zone> zones;
	
	static Map<Id<Link>, Integer> IniLinkParking;
	static Map<Id<Zone>, Integer> IniZoneParking;
	static Map<Vehicle, Id<Link>> IniVehOnLink;
	
	static Map<Double, List<Id<Person>>> personsTaxiPlans;
	static HashMap<Integer, Map<Id<Zone>,Integer>> horizonRequest;
	
	static ArrayList<Vehicle> taxisThatMightNeedToBeRelocated; // this is checked in the inserter
	
	static Map<Vehicle, Link> taxisWithParkingReservation;
	static Map<Vehicle, Double> taxisWithParkingReservationInInnerCity;
	
	static int relocatingActionCounter;
	static int relocatingActionCounterAlternativeZoneParking;
	static int relocatingActionCounterAlternativeRandomParking;
	static int relocatingActionCounterAlternativeTeleporting;
	static int taxiRideCounterAfterParking;
	static int taxiRideCounterAfterDiversion;
	static int parkingActionCounter;
	static int unparkingActionCounter;
	static int movedAfterMaxParkingDurationCounter;
	static int movedFromInnerCityCounter;
		
	// INFRASTRUCUTURE	
	public static void setZoneLink(Map<Id<Zone>, List<Id<Link>>> iniZoneLink) {
		ZoneLink = iniZoneLink;
		relocatingActionCounter = 0;
		relocatingActionCounterAlternativeZoneParking = 0;
		relocatingActionCounterAlternativeRandomParking = 0;
		taxiRideCounterAfterParking = 0;
		taxiRideCounterAfterDiversion = 0;
		movedAfterMaxParkingDurationCounter = 0;
		movedFromInnerCityCounter = 0;
	}
	public static void setZones(Map<Id<Zone>, Zone> iniZones) {
		zones = iniZones;
	}
	public static void setParkingLinks(Map<Id<Link>, Integer> parkingLinks) {
		ParkingLinks = parkingLinks;
	}
	public static void setParkingZones(Map<Id<Zone>, Integer> ParkingZonesIn) {
		ParkingZones = ParkingZonesIn;
	}
	public static Map<Id<Zone>, Integer> getZoneParkingTotal() {
		if (ParkingZones == null){
			return null;
		}
		HashMap<Id<Zone>, Integer> temp = new HashMap<Id<Zone>, Integer>(ParkingZones);
		return temp; // returns total parking spots per zone
	}
	public static Map<Id<Zone>, List<Id<Link>>> getZoneLink(){
		return ZoneLink;
	}
	
	//PEOPLE 
	public static void setPersonsWithTaxiPlans(Map<Double, List<Id<Person>>> personsWithTaxiPlans) {
		personsTaxiPlans = personsWithTaxiPlans;	
	}	
	public static  Map<Double, List<Id<Person>>> getPersonsWithTaxiPlans() {
		HashMap<Double, List<Id<Person>>> temp = new HashMap<Double, List<Id<Person>>>(personsTaxiPlans);
		return temp;	
	}	
	public static void setHorizonPlans(QSim qsim, Network network, ZoneFinder zoneFinder, Map<Id<Link>, ? extends Link> links) {
		HashMap<Integer, Map<Id<Zone>, Integer>> horReq = new HashMap<Integer, Map<Id<Zone>,Integer>>();
		int counterReq = 0;
		for (int i = 0; i < 1740; i++) {
			Iterator<Id<Zone>> itZones = zones.keySet().iterator();
			Map<Id<Zone>, Integer> zoneReqMap = new HashMap<Id<Zone>, Integer>();
			while (itZones.hasNext()) {
				Id<Zone> zoneId = itZones.next();
				zoneReqMap.put(zoneId, 0);
			}
			horReq.put(i, zoneReqMap);
		}
			
		Iterator<Id<Person>> agentsItr = qsim.getScenario().getPopulation().getPersons().keySet().iterator();
		while (agentsItr.hasNext()) {
			Person person = qsim.getScenario().getPopulation().getPersons().get(agentsItr.next());
			List<PlanElement> plan = person.getSelectedPlan().getPlanElements();
			for (int p = 0; p < plan.size(); p++) {
				PlanElement plE = plan.get(p);
				String planE = plE.toString();
				if (planE.matches(".*taxi.*")) {
					
					int index1 = planE.indexOf("depTime=") + 8;
					int index2 = planE.indexOf("][travTime");
					String depTime = planE.substring(index1, index2);
					String[] split = depTime.split(":");
					double taxiDemandTime = 0.0;
					taxiDemandTime += Double.parseDouble(split[0]) * 3600;
					taxiDemandTime += Double.parseDouble(split[1]) * 60;
					taxiDemandTime += Double.parseDouble(split[2]);
					taxiDemandTime = Math.floor(taxiDemandTime / 60);
					
					int index3 = planE.indexOf("startLinkId=") + 12;
					int index4 = planE.indexOf(" endLinkId");
					String linkIdStr = planE.substring(index3, index4);					
					
					Id<Link> linkId = null;
					for (final Id<Link> key : links.keySet()){
						if (key.toString().equals(linkIdStr)){
							linkId = key;
					}}							
					
					Id<Zone> zoneId = zoneFinder.findZone(network.getLinks().get(linkId).getCoord()).getId();
					
					Map<Id<Zone>, Integer> zoneReqMap = horReq.get((int)taxiDemandTime);
					Integer counter = zoneReqMap.get(zoneId);
					zoneReqMap.replace(zoneId, counter + 1);
					counterReq = counterReq + 1;
		}}}
		System.out.println ("taxiReq saved in horizon Request: " + counterReq);
		horizonRequest = horReq; 
	}
	public static HashMap<Integer, Map<Id<Zone>, Integer>> getHorzionRequest () {
	return horizonRequest;
	}
		
	// PARKING AVAILABILTY
	public static void setZoneFreeParkingLinks (Map<Id<Zone>, List<Id<Link>>> iniZoneFreeParkingLinks) {
		//ZoneFreeParkingLinks = iniZoneFreeParkingLinks;
	}
	public static void setLinkParking(Map<Id<Link>, Integer> iniLinkParking) {
		LinkParking = iniLinkParking;
	}	
	public static void setZoneParking(Map<Id<Zone>, Integer> ZoneParkingIn) {
		ZoneParking = ZoneParkingIn;
	}		
	public static void setLinkParkingINI(Map<Id<Link>, Integer> iniLinkParking) {
		IniLinkParking = iniLinkParking;		
	}
	public static void setZoneParkingINI(Map<Id<Zone>, Integer> FreeZoneParking) {
		IniZoneParking = FreeZoneParking;	
	}	
	public static void setVehicleOnLinkINI(Map<Vehicle, Id<Link>> VehOnLink) {
		IniVehOnLink = VehOnLink;	
	}	
	public static void setTaxisThatMightNeedToBeRelocated (){
		taxisThatMightNeedToBeRelocated = new ArrayList<Vehicle>();
		taxisWithParkingReservationInInnerCity = new HashMap<Vehicle, Double>();
	}	
	public static void setTaxisWithParkingReservation(Map<Vehicle, Link> settingTaxisWithParkingReservation){
		taxisWithParkingReservation = settingTaxisWithParkingReservation;	
	}
	
	
	public static Map<Id<Zone>, Integer> getIniZoneParkingCounter() {
		if (IniZoneParking == null){
			return null;
		}
		Map<Id<Zone>, Integer> temp = new HashMap<Id<Zone>, Integer>(IniZoneParking);
		return temp;
	}
	public static Map<Id<Link>, Integer> getIniParkingCounter() {
		if (IniLinkParking == null){
			return null;
		}
		Map<Id<Link>, Integer> temp = new HashMap<Id<Link>, Integer>(IniLinkParking);
		return temp;
	}	
	public static Map<Vehicle, Id<Link>> getIniVehOnLink() {
		if (IniVehOnLink == null){
			return null;
		}
		
		Map<Vehicle, Id<Link>> temp = new HashMap<Vehicle, Id<Link>>(IniVehOnLink);
		return temp;
	}	
	
	public static Map<Id<Zone>, Integer> getZoneParking() {
		HashMap<Id<Zone>, Integer> temp = new HashMap<Id<Zone>, Integer>(ZoneParking);
		return temp; // returns free parking spots per zone
	}
	public static Map<Id<Link>, Integer> getLinkParking() {
		Map<Id<Link>, Integer> temp = new HashMap<Id<Link>, Integer>(LinkParking);
		return temp;
	}
	public static int getParkingOnLink(Id<Link> linkID) {
		int temp = LinkParking.get(linkID);
		return temp;
	}	
	public static int getParkingInZoneOfLink(Link link) {
		ZoneFinder zoneFinder = new ZoneFinderImpl(zones, 1.0);
		Zone zone = zoneFinder.findZone(link.getCoord());
		int parkingOnLinkZone = ZoneParking.get(zone.getId());
		return parkingOnLinkZone;
	}
	public static int getParkingInZone(Zone zone) {
		int parkingInZone = ZoneParking.get(zone.getId());
		return parkingInZone;
	}
	public static List<Id<Link>> getLinksWithFreeParkingInZone(Zone zone) {
			
		Map<Id<Link>, Integer> linksWithFreeParkingInZone = new HashMap<Id<Link>, Integer>();
		List<Id<Link>> linksWithParking = new ArrayList <Id<Link>>();
		List<Id<Link>> linksInZone = new ArrayList <Id<Link>>(ZoneLink.get(zone.getId()));		
		for (int i = 0; i < linksInZone.size(); i++) {
			Id<Link> linkID = linksInZone.get(i);
			Integer freeParking = LinkParking.get(linkID);
			if (freeParking > 0) {
				linksWithFreeParkingInZone.put(linkID, freeParking);
				linksWithParking.add(linkID);
			}
		}	
		return linksWithParking;
	}
	public static  Map<Vehicle, Link> getTaxisWithParkingReservation(){
		return taxisWithParkingReservation;
	}
	
	
	/////////////////////PARKING///////////////////////////////////////////
	public static void park(Link link, Vehicle vehicle, MobsimTimer timer) {	
		Boolean check = doPlausabilityCheck(link);
		//// update LinkParking
		Integer parkingLink = LinkParking.get(link.getId());		
		LinkParking.replace(link.getId(), (parkingLink - 1));
		
		//// update ZoneParking
		ZoneFinder zoneFinder = new ZoneFinderImpl(zones, 1.0);
		Zone zone = zoneFinder.findZone(link.getCoord());
		Integer parkingZone = ZoneParking.get(zone.getId());
		ZoneParking.replace(zone.getId(), parkingZone - 1);
		
		//// update the other lists
		parkingActionCounter = parkingActionCounter + 1;
		taxisWithParkingReservation.put(vehicle, link);	
		if (Relocator.zoneIsInnerCity(zone.getId()) == true) {
			Double vehHasToMove = Relocator.getParkingTimeLimitInnerCity(timer.getTimeOfDay());
			taxisWithParkingReservationInInnerCity.put(vehicle, vehHasToMove);
		}
	}

	public static void unpark(Link link, Vehicle vehicle) {		
		//// update LinkParking
		int newParking = LinkParking.get(link.getId()); // free parking spot becomes available
		LinkParking.replace(link.getId(), (newParking + 1));	
		
		//// update ZoneParking
		ZoneFinder zoneFinder = new ZoneFinderImpl(zones, 1.0);
		Zone zone = zoneFinder.findZone(link.getCoord());
		if (!ZoneParking.containsKey(zone.getId())) {
			System.out.println("PROBLEM: counter_parkingVehicles l.290)");
		}else {
			Integer parkingZone = ZoneParking.get(zone.getId());
			ZoneParking.replace(zone.getId(), (parkingZone + 1));
		}
		
		//// update the other lists
		unparkingActionCounter = unparkingActionCounter + 1;
		taxisWithParkingReservation.remove(vehicle);
		if (taxisWithParkingReservationInInnerCity.containsKey(vehicle)) {
			taxisWithParkingReservationInInnerCity.remove(vehicle);
		}		
	}
	
	// check 1: zonal Level
	public static boolean doPlausabilityCheck(Link link) {
		boolean check = true;
		
		if (LinkParking.get(link.getId()) == 0 ){
			check = false;
			System.out.println ("Problem Plausability Check in Counter_ParkingVehicles: linkparking = 0");
		}
		if (LinkParking.get(link.getId()) > ParkingLinks.get(link.getId())){
			check = false;
			System.out.println ("Problem Plausability Check in Counter_ParkingVehicles: more vehicles parked on link than parking spots");
		}
		
		ZoneFinder zoneFinder = new ZoneFinderImpl(zones, 1.0);
		Zone zone = zoneFinder.findZone(link.getCoord());
		if (ZoneParking.get(zone.getId()) == 0 ){
			check = false;
			System.out.println ("Problem Plausability Check in Counter_ParkingVehicles: zoneparking = 0");
		}
		if (ZoneParking.get(zone.getId()) > ParkingZones.get(zone.getId())){
			check = false;
			System.out.println ("Problem Plausability Check in Counter_ParkingVehicles: more vehicles parked in zone than parking spots");
		}		
		return check;
	}
	
	/////////////////////PARKING///////////////////////////////////////////

	public static boolean parkingOnLink(Link link){
		boolean parkingOnLinkB = false;
		if (ParkingLinks.containsKey(link.getId())){  
				if (ParkingLinks.get(link.getId()) > 0){
					parkingOnLinkB = true;
				}
		}
		return parkingOnLinkB;		
	}
	
	// counting the parking actions
		public static void countRelocation(){
			relocatingActionCounter = relocatingActionCounter + 1;
		}
		
		public static void countRelocationZoneAlternative(){
			relocatingActionCounterAlternativeZoneParking = relocatingActionCounterAlternativeZoneParking + 1;
		}
	
		public static void countRelocationRandomAlternative(){
			relocatingActionCounterAlternativeRandomParking = relocatingActionCounterAlternativeRandomParking + 1;
		}
		
		public static void countMovedFromInnerCityCounter(){
			movedFromInnerCityCounter = movedFromInnerCityCounter + 1;
		}
		
		public static  int getCountRelocation(){
			return relocatingActionCounter;
		}
		
		public static int getCountRelocationZoneAlternative(){
			return relocatingActionCounterAlternativeZoneParking; 
		}
		
		public static int getCountRelocationRandomAlternative(){
			return relocatingActionCounterAlternativeRandomParking;
		}
		
		public static int getCountMovedFromInnerCityCounter(){
			return movedFromInnerCityCounter; 
		}
		
		public static void addPotentialRelocatingCandidate (Vehicle candidate){
			taxisThatMightNeedToBeRelocated.add(candidate);
		}
		
		public static void removePotentialRelocatingCandidate (Vehicle candidate){
			if (taxisThatMightNeedToBeRelocated.contains(candidate)){
			taxisThatMightNeedToBeRelocated.remove(candidate);
			}
		}
		
		public static ArrayList<Vehicle> getPotentialRelocatingCandidate (){
			return taxisThatMightNeedToBeRelocated;
		}
		
		public static void addTaxisThatNeedToBeParkedOutsideOfCityCentre (Vehicle vehicle, Double timeToRelocate){
			taxisWithParkingReservationInInnerCity.put(vehicle, timeToRelocate);
		}
				
		public static boolean checkTaxisThatNeedToBeParkedOutsideOfCityCentre (Vehicle vehicle, Double currentTime){
			boolean needsToMove = false;
			if (taxisWithParkingReservationInInnerCity.containsKey(vehicle)) {
				Double timeToRelocate = taxisWithParkingReservationInInnerCity.get(vehicle);
				if (timeToRelocate <= (currentTime + 60)) {
					needsToMove = true;
				}	
			}
			return needsToMove;
		}
				
		public static Map<Vehicle, Double> getTaxisThatNeedToBeParkedOutsideOfCityCentre(){
			return taxisWithParkingReservationInInnerCity;
		}	
		
		public static Integer getInitialVehiclesParkedInZone(Id<Zone> zone){
			Integer iniparkedveh = ParkingZones.get(zone) - IniZoneParking.get(zone);
			return iniparkedveh;
		}
		
		public static Integer getParkingZones(Id<Zone> zone) {
			return ParkingZones.get(zone);
		}
		
		public static Id<Zone>  getZoneInWhichVehicleParkesInitially (Vehicle vehicle, MyZonalSystem zonalSystem){
			Link parkingLink = taxisWithParkingReservation.get(vehicle);
			Map<Id<Zone>, Zone> zones = zonalSystem.getZones();
			ZoneFinder zoneFinder = new ZoneFinderImpl(zones, 1.0);
			Zone zone = zoneFinder.findZone(parkingLink.getCoord());
			return zone.getId();
		}	
}
