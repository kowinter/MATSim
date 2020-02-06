package runnableFiles;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.MapConfiguration;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.dvrp.data.Fleet;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.router.DvrpRoutingNetworkProvider;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelTimeModule;
import org.matsim.contrib.taxi.optimizer.TaxiOptimizer;
import org.matsim.contrib.taxi.optimizer.rules.RuleBasedTaxiOptimizerParams;
import org.matsim.contrib.taxi.run.*;
import org.matsim.contrib.taxi.run.examples.TaxiDvrpModules;
import org.matsim.contrib.taxi.scheduler.TaxiScheduler;
import org.matsim.contrib.zone.Zone;
import org.matsim.contrib.zone.util.ZoneFinder;
import org.matsim.contrib.zone.util.ZoneFinderImpl;
import org.matsim.core.config.*;
import org.matsim.core.config.groups.QSimConfigGroup.SnapshotStyle;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.router.DijkstraFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacility;
import org.matsim.vis.otfvis.OTFVisConfigGroup;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;

public class Run {

	private static final String CONFIG_FILE = "resources/ConfigWithTaxi.xml";
	private static final String xmlZones ="resources/zzones.xml";
	private static final String shpZones ="resources/zzones.shp";
	//private static final String CONFIG_FILE = "ConfigWithTaxi.xml";
	//private static final String xmlZones = "zzones.xml";
	//private static final String shpZones = "zzones.shp";
	private static final String OUTPUT_FILE = "output/Amsterdam/Relocation2/zonal/4";
	private static LeastCostPathCalculator router;
	private static final Integer lastIteration = 1;

	public static void main(String[] args) {	
		Config config = ConfigUtils.loadConfig(CONFIG_FILE, new DvrpConfigGroup(), new OTFVisConfigGroup(),new TaxiConfigGroup());
		config.controler().setOutputDirectory(OUTPUT_FILE);
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setLastIteration(lastIteration);
		config.addConfigConsistencyChecker(new TaxiConfigConsistencyChecker());

		config.qsim().setSnapshotStyle(SnapshotStyle.withHoles);
		config.checkConsistency();

		// load scenario
		Scenario scenario = ScenarioUtils.loadScenario(config);

		// setup controler
		Controler controler = new Controler(scenario);

		// choose taxi optimizer
		controler.addOverridingModule(TaxiDvrpModules.create(MyZonalTaxiOptimizerProvider.class));
		controler.addOverridingModule(new MyTaxiModule());
		
		//TravelTime experiencedTravelTimes = TravelTimeUtils.createTravelTimesFromEvents(scenario, Event_File);
		//controler.addOverridingModule(new AbstractModule() {
		//	public void install() {
		//         addTravelTimeBinding(DvrpTravelTimeModule.DVRP_INITIAL).toInstance(experiencedTravelTimes);
		// }});

		// run simulation
		controler.run();
	}

	public static class MyZonalTaxiOptimizerProvider implements Provider<TaxiOptimizer> {
		public static final String TAXI_OPTIMIZER = "taxi_optimizer";
		public static final String TYPE = "type";

		public enum OptimizerType {
			ASSIGNMENT, FIFO, RULE_BASED, ZONAL;
		}

		private final TaxiConfigGroup taxiCfg;
		private final Fleet fleet;
		private final Network network;
		private final MobsimTimer timer;
		private final TravelTime travelTime;
		private final TravelDisutility travelDisutility;
		private final TaxiScheduler scheduler;
		private final QSim qsim;

		@Inject
		public MyZonalTaxiOptimizerProvider(TaxiConfigGroup taxiCfg, Fleet fleet,
				@Named(DvrpRoutingNetworkProvider.DVRP_ROUTING) Network network, MobsimTimer timer, QSim qsim,
				@Named(DvrpTravelTimeModule.DVRP_ESTIMATED) TravelTime travelTime,
				@Named(TAXI_OPTIMIZER) TravelDisutility travelDisutility, TaxiScheduler scheduler) {
			this.taxiCfg = taxiCfg;
			this.fleet = fleet;
			this.network = network;
			this.timer = timer;
			this.travelTime = travelTime;
			this.travelDisutility = travelDisutility;
			this.scheduler = scheduler;
			this.qsim = qsim;
		}

		@Override
		public TaxiOptimizer get() {
			Configuration optimizerConfig = new MapConfiguration(taxiCfg.getOptimizerConfigGroup().getParams());
			Config config = ConfigUtils.loadConfig(CONFIG_FILE);
			Scenario scenario = ScenarioUtils.loadScenario(config);
			Map<Id<Link>, ? extends Link> links = network.getLinks();

			Map<Id<Zone>, Zone> zones = MyZoneReader.readZones(xmlZones, shpZones);
			ZoneFinder zoneFinder = new ZoneFinderImpl(zones, 1.0);
			MyZonalSystem myZonalSystem = new MyZonalSystem(zones, zoneFinder, network);
		
			// get parking facilities
			String PARKACTIVITYTYPE = "car interaction";
			TreeMap<Id<ActivityFacility>, ActivityFacility> taxiParkingFacilities = scenario.getActivityFacilities().getFacilitiesForActivityType(PARKACTIVITYTYPE);

			// make map counting zones, links and parking facilities
			Map<Id<Link>, Integer> LinkParking = new TreeMap<Id<Link>, Integer>(); 				// counts the free (leftover) parking per link
			Map<Id<Link>, Integer> LinksParking = new TreeMap<Id<Link>, Integer>();				// counts the total parking per link
			Map<Id<Zone>, List<Id<Link>>> ZoneLink = new HashMap<Id<Zone>, List<Id<Link>>>(); 	// list of links per zone
			Map<Id<Zone>, Integer> ZoneParking = new HashMap<Id<Zone>, Integer>(); 			// counts the free (leftover) parking per zone
			Map<Id<Zone>, Integer> ParkingZones = new HashMap<Id<Zone>, Integer>(); 				// counts the total parking per zone
			Map<Id<Zone>, Integer> ZoneParkingINI = new HashMap<Id<Zone>, Integer>(); 		// counts the free (leftover) parking per zone
			Map<Id<Link>, Integer> LinkParkingINI = new TreeMap<Id<Link>, Integer>(); 			// counts the free (leftover) parking per link
			Map<Vehicle, Link> taxisWithParkingReservation = new HashMap<Vehicle, Link>();
			Map<Id<Zone>, List<Id<Link>>> iniZoneFreeParkingLinks = new HashMap<Id<Zone>, List<Id<Link>>>();
			Map<Vehicle, Id<Link>> VehOnLinkINI = new HashMap <Vehicle, Id<Link>>();
			Counter_ParkingVehicles.setTaxisThatMightNeedToBeRelocated();
			
			Iterator<Id<Zone>> itZones = zones.keySet().iterator();
			while (itZones.hasNext()) {
				Id<Zone> zoneId = itZones.next();
				ZoneParking.put(zoneId, 0);
				ZoneParkingINI.put(zoneId, 0);
				ParkingZones.put(zoneId, 0);
				List<Id<Link>> plinklist = new ArrayList <Id<Link>>();
				iniZoneFreeParkingLinks.put(zoneId, plinklist);
			}
					
			Iterator<ActivityFacility> it = taxiParkingFacilities.values().iterator();
			int parkingCounter = 0;
			while (it.hasNext()) {
				ActivityFacility act = it.next();
				Id<Link> link = act.getLinkId();

				if (links.containsKey(link)) {
					Integer parking = (int) (act.getActivityOptions().get("car interaction").getCapacity());
					parkingCounter = parkingCounter + parking;
					Coord coord = act.getCoord();

					Id<Zone> zone = zoneFinder.findZone(coord).getId();
					LinkParking.put(link, parking);
					LinkParkingINI.put(link, parking);
					LinksParking.put(link, parking);
					List<Id<Link>> linkList = ZoneLink.get(zone);

					if (linkList == null) {
						List<Id<Link>> linkList2 = new ArrayList<Id<Link>>();
						linkList2.add(link);
						linkList = linkList2;
					} else {
						linkList.add(link);
					}
					ZoneLink.put(zone, linkList);
					
					int park = ParkingZones.get(zone);
					ParkingZones.put(zone, park + parking);
					ZoneParking.put(zone, park + parking);
					ZoneParkingINI.put(zone, park + parking);
					
						
					if (parking>0) {
						List<Id<Link>> plinklist = iniZoneFreeParkingLinks.get(zone);
						plinklist.add(link);	
					}
				} 
			}
			
			// cleaning up after the mess
			for (final Id<Link> key : links.keySet()) {
				if (LinkParking.containsKey(key)) {
				} else {
					Link link = links.get(key);
					int parking = 0;
					Coord coord = link.getCoord();
					Id<Zone> zone = zoneFinder.findZone(coord).getId();
					LinkParking.put(link.getId(), parking);
					LinkParkingINI.put(link.getId(), parking);
					LinksParking.put(link.getId(),parking);
					List<Id<Link>> linkList = ZoneLink.get(zone);
					if (linkList == null) {
						List<Id<Link>> linkList2 = new ArrayList<Id<Link>>();
						linkList2.add(link.getId());
						linkList = linkList2;
					} else {
						linkList.add(link.getId());
					}
					ZoneLink.put(zone, linkList);
				}
			}
			
			// deduct used parking from the free parking spots
			Map<Id<Zone>, Integer> InitialZoneParkingCounter = new HashMap<Id<Zone>, Integer>();
			int parkedRegularly = 0;
			Map<Id<Vehicle>, ? extends Vehicle> parkedVehicles = fleet.getVehicles();
			ArrayList<Vehicle> vehicles = new ArrayList<Vehicle>();
			Double tMoveFromInnerCity = setRelocationtime();
			for (Entry<Id<Vehicle>, ? extends Vehicle> entry : parkedVehicles.entrySet()) {
				
				Vehicle vehicle = entry.getValue();
				if (Counter_ParkingVehicles.getIniVehOnLink() == null) {
					VehOnLinkINI.put(vehicle, vehicle.getStartLink().getId());	
				}
				
				vehicles.add(vehicle);
				Coord coord = entry.getValue().getStartLink().getCoord();
				Id<Zone> zoneP = zoneFinder.findZone(coord).getId();
				boolean innerCityParking = Relocator.zoneIsInnerCity(zoneP);
				Id<Link> parkingLink = entry.getValue().getStartLink().getId();
				Integer availParking = LinkParking.get(parkingLink);
				
				taxisWithParkingReservation.put(vehicle,links.get(parkingLink));
				if (availParking > 0) {
					LinkParking.put(parkingLink, availParking - 1);
					LinkParkingINI.put(parkingLink, availParking - 1);
					parkedRegularly = parkedRegularly + 1;
				} else {
					System.out.print("Problem (Run.java l257): vehicle is parked on link with not enough parking");
				}	
				if (InitialZoneParkingCounter.containsKey(zoneP)) {
					Integer parkedVehInZone = InitialZoneParkingCounter.get(zoneP);
					InitialZoneParkingCounter.put(zoneP, parkedVehInZone + 1);
				} else {
					InitialZoneParkingCounter.put(zoneP, 1);
				}
				
				if (ZoneParking.containsKey(zoneP)) {
					Integer parkedVehInZone = ZoneParking.get(zoneP);
					ZoneParking.put(zoneP, parkedVehInZone - 1);
					ZoneParkingINI.put(zoneP, parkedVehInZone - 1);
				} else {
					ZoneParking.put(zoneP, 0);
					ZoneParkingINI.put(zoneP, 0);
				}
				
				availParking = LinkParking.get(parkingLink);
				if (availParking < 1) {
					List<Id<Link>> plinklist = iniZoneFreeParkingLinks.get(zoneP);
					if (plinklist.contains(parkingLink)) {
						plinklist.remove(parkingLink);
					}else {
						System.out.print("Problem (Run.java l279)");
					}
				}
				
				if (innerCityParking == true) {
					Counter_ParkingVehicles.addTaxisThatNeedToBeParkedOutsideOfCityCentre(vehicle,tMoveFromInnerCity);
				}	
			}		
			
				
			Counter_ParkingVehicles.setZones(zones);						// zones
			Counter_ParkingVehicles.setZoneLink(ZoneLink);					// links per zone
			Counter_ParkingVehicles.setLinkParking(LinkParking); 			// counts the free (leftover) parking per link
			Counter_ParkingVehicles.setZoneParking(ZoneParking); 			// counts the free (leftover) parking per zone
			Counter_ParkingVehicles.setParkingLinks(LinksParking);			// counts the total parking per link
			Counter_ParkingVehicles.setParkingZones(ParkingZones);			// counts the total parking per ZONE
			Counter_ParkingVehicles.setZoneFreeParkingLinks(iniZoneFreeParkingLinks);		// linksWithParkingInZone
			
			Counter_ParkingVehicles.setLinkParkingINI(LinkParkingINI); 		// counts the free (leftover) parking per link
			Counter_ParkingVehicles.setZoneParkingINI(ZoneParkingINI); 		// counts the free (leftover) parking per zone
			Counter_ParkingVehicles.setVehicleOnLinkINI(VehOnLinkINI);
			
			Counter_ParkingVehicles.setTaxisWithParkingReservation(taxisWithParkingReservation); // assignes parking reservation to all vehicles
			
			router = new DijkstraFactory().createPathCalculator(network,travelDisutility, travelTime);
			
			Map<Double, List<Id<Person>>> personsTaxiPlans = getPersonsWithTaxiPlansByTheSecond();
			Counter_ParkingVehicles.setPersonsWithTaxiPlans(personsTaxiPlans);
			Counter_ParkingVehicles.setHorizonPlans(qsim, network, zoneFinder, links);
			
			return MyOptimizer.create(taxiCfg, fleet, scheduler, network, timer, travelTime, travelDisutility,new RuleBasedTaxiOptimizerParams(optimizerConfig), qsim, myZonalSystem, ZoneLink);
			//return MyOptimizerCruise.create(taxiCfg, fleet, scheduler, network, timer, travelTime, travelDisutility,new RuleBasedTaxiOptimizerParams(optimizerConfig), qsim, myZonalSystem, ZoneLink);
				
		}

	private Map<Double, List<Id<Person>>> getPersonsWithTaxiPlansByTheSecond() {
			Map<Double, List <Id<Person>>> taxiPeopleByTheSecond = new HashMap<Double, List <Id<Person>>>();
			Iterator<Id<Person>> agentsItr2 = qsim.getScenario().getPopulation().getPersons().keySet().iterator();
			while (agentsItr2.hasNext()){
				Person person = qsim.getScenario().getPopulation().getPersons().get(agentsItr2.next());
				List<PlanElement> plan = person.getSelectedPlan().getPlanElements();
				for (int i = 0; i < plan.size(); i ++){
					PlanElement plE = plan.get(i);
					String planE = plE.toString();
					if (planE.matches(".*taxi.*")){
						int index1 = planE.indexOf("depTime=") + 8;
						int index2 = planE.indexOf("][travTime");
						String depTime = planE.substring(index1, index2);
						String[] split = depTime.split(":"); 
						double taxiDemandTime = 0.0;
						taxiDemandTime += Double.parseDouble(split[0])*3600;  
						taxiDemandTime += Double.parseDouble(split[1])*60;  
						taxiDemandTime += Double.parseDouble(split[2]); 
						
						// case 1: map already has entry for that second
						if (taxiPeopleByTheSecond.containsKey(taxiDemandTime)){
							List <Id<Person>> taxiPeople = taxiPeopleByTheSecond.get(taxiDemandTime);
							taxiPeople.add(person.getId());
						}else{
						// case 2: map has not yet entry for that second
							List <Id<Person>> taxiPeople = new ArrayList <Id<Person>>();
							taxiPeople.add(person.getId());
							taxiPeopleByTheSecond.put(taxiDemandTime, taxiPeople);
						}
					}		
				}	
			}
			return taxiPeopleByTheSecond;
		}
	}

	public static MyZonalSystem getZones(TaxiConfigGroup taxiCfg, Network network) {
		Map<Id<Zone>, Zone> zones = MyZoneReader.readZones(xmlZones, shpZones);
		ZoneFinder zoneFinder = new ZoneFinderImpl(zones, 1.0);
		MyZonalSystem myZonalSystem = new MyZonalSystem(zones, zoneFinder, network);
		return myZonalSystem;
	}

	public static Network getNetwork() {
		Config config = ConfigUtils.loadConfig(CONFIG_FILE);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Network network = scenario.getNetwork();
		return network;
	}
	
	public static LeastCostPathCalculator getRouter(){
		return router;
	}
	
	public static String getOutputLocation () {
		return OUTPUT_FILE;
	}
	
	public static Double setRelocationtime() {
		Double firstTimeVehicleHasToMove = 0.0;
		
		// Relocator set to:  noInnerCityParking = true;
		if (Relocator.getNoInnerCityParking() == true) {
			firstTimeVehicleHasToMove = 60.0; // kept like that so we can clearly see this 
		}

		// Relocator set to:  noInnerCityParkingDayTime = true;
		Double DayStart = Relocator.getDayStartTime();
		if (Relocator.getLimitInnerCityParkingDaytime(DayStart + 30) == true ) {
			firstTimeVehicleHasToMove = DayStart;
		}
		
		// Relocator set to: timeLimitedInnerCityParking = true;
		if (Relocator.getTimeLimitedInnerCityParking() == true) {
			firstTimeVehicleHasToMove = Relocator.getTimeLimitInnerCityParking();
		}
				
		return firstTimeVehicleHasToMove;
	}

	public static void createNewFacility(Document outputDoc, Element facs, String capS, String curbID, String linkID,Double x, Double y) {
		Element newFacility = outputDoc.createElement("facility");
		Element newActivity = outputDoc.createElement("activity");
		Element newCapacity = outputDoc.createElement("capacity");

		newCapacity.setAttribute("value", capS);

		newActivity.appendChild(newCapacity);
		newActivity.setAttribute("type", "car interaction");

		newFacility.appendChild(newActivity);
		newFacility.setAttribute("id", curbID);
		newFacility.setAttribute("linkId", linkID);
		newFacility.setAttribute("x", x.toString());
		newFacility.setAttribute("y", y.toString());

		facs.appendChild(newFacility);
	}
	
}
