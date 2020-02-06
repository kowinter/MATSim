package readOutput;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.TeleportationArrivalEvent;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.handler.TeleportationArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.pt.PtConstants;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.vehicles.Vehicle;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author pieterfourie, sergioo
 *         <p>
 *         Converts events into journeys, trips/stages, transfers and activities
 *         tables. Originally designed for transit scenarios with full transit
 *         simulation, but should work with most teleported modes
 *         </p>
 */

public class EventsToTravelDiaries implements
        TransitDriverStartsEventHandler, PersonEntersVehicleEventHandler,
        PersonLeavesVehicleEventHandler, PersonDepartureEventHandler,
        PersonArrivalEventHandler, ActivityStartEventHandler,
        ActivityEndEventHandler, PersonStuckEventHandler,
        LinkEnterEventHandler, LinkLeaveEventHandler,
        TeleportationArrivalEventHandler, VehicleArrivesAtFacilityEventHandler {

    private final Network network;
    private double walkSpeed;
    // Attributes
    private Map<Id, TravellerChain> chains = new HashMap<>();
    private Map<Id, Coord> locations = new HashMap<>();
    private Map<Id, PTVehicle> ptVehicles = new HashMap<>();
    private HashSet<Id> transitDriverIds = new HashSet<>();
    private HashMap<Id, Id> driverIdFromVehicleId = new HashMap<>();
    private int stuck = 0;
    private TransitSchedule transitSchedule;
    private boolean isTransitScenario = false;
   private AtomicInteger eventCounter = new AtomicInteger(0);
    private int maxEvents = Integer.MAX_VALUE;
    // those unscheduled modes that have been simulated on the network need to be processed the same way as car
    private Set<String> networkModes = new HashSet<>();
    
    Double travelTimeCarTotal= 0.0;
    Double travelTimeCarTotalJ= 0.0;
    Double travelTimeTaxiTotal = 0.0;
    Double travelTimeTaxiTotalJ = 0.0;
    Double travelTimeBikeTotal = 0.0;
    Double travelTimeWalkTotal = 0.0;
    Double travelTimePtTotal = 0.0;
    Double travelTimePtTotalJ = 0.0;
    Double travelDistanceCarTotal = 0.0;
    Double travelDistanceCarTotalJ = 0.0;
    Double travelDistanceTaxiTotal = 0.0;
    Double travelDistanceTaxiTotalJ = 0.0;
    Double travelDistanceBikeTotal = 0.0;
    Double travelDistanceWalkTotal = 0.0;
    Double travelDistancePtTotal = 0.0;
    Double travelDistancePtTotalJ = 0.0;
    Double carTrips = 0.0;
    Double carTripsJ = 0.0;
    Double taxiTrips = 0.0;
    Double taxiTripsJ = 0.0;
    Double bikeTrips = 0.0;
    Double walkTrips = 0.0;
    Double ptTrips = 0.0;
    Double ptTripsJ = 0.0;
    
    Double travelDistanceTaxiTotalEnter = 0.0;
    Double travelDistanceTaxiTotalExit = 0.0;
    
    Double travelDistanceCarTotalEnter = 0.0;
    Double travelDistanceCarTotalExit = 0.0;
   
    
    public EventsToTravelDiaries(TransitSchedule transitSchedule,Network network, Config config) {
        networkModes.addAll(config.qsim().getMainModes());
        this.network = network;
        this.walkSpeed = new TransitRouterConfig(config).getBeelineWalkSpeed();
        this.transitSchedule = transitSchedule;
        this.isTransitScenario = true;
    }

    public EventsToTravelDiaries(TransitSchedule transitSchedule,Network network, Config config, int maxEvents) {
        this(transitSchedule, network, config);
        this.maxEvents = maxEvents;
    }

    public EventsToTravelDiaries(Scenario scenario, int maxEvents) {
        this(scenario);
        this.maxEvents = maxEvents;
    }

    public EventsToTravelDiaries(Scenario scenario) {
        networkModes.addAll(scenario.getConfig().qsim().getMainModes());
        this.network = scenario.getNetwork();
        isTransitScenario = scenario.getConfig().transit().isUseTransit();
        if (isTransitScenario) {
            this.transitSchedule = scenario.getTransitSchedule();
            this.walkSpeed = new TransitRouterConfig(scenario.getConfig()).getBeelineWalkSpeed();
        }

    }

    public static void runEventsProcessing(Properties properties) {
        boolean isTransit = false;
        MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.loadConfig(properties.get("configFile").toString()));
        scenario.getConfig().transit().setUseTransit(true);
        if (!properties.get("transitScheduleFile").toString().equals("")) {
            new TransitScheduleReader(scenario).readFile(properties.get("transitScheduleFile").toString());
            isTransit = true;
        }
        new MatsimNetworkReader(scenario.getNetwork()).readFile(properties.get("networkFile").toString());

        EventsManager eventsManager = EventsUtils.createEventsManager();
        EventsToTravelDiaries test;
        
        if (isTransit) {
            test = new EventsToTravelDiaries(scenario.getTransitSchedule(), scenario.getNetwork(),scenario.getConfig());

        } else {
            test = new EventsToTravelDiaries(scenario);
        }
        // }
        eventsManager.addHandler(test);
        new MatsimEventsReader(eventsManager).readFile(properties.get("eventsFile").toString());
        
        
                
       try {
    	   test.writeSimulationResultsToTabSeparated(properties.get("outputPath").toString(),properties.get("tableSuffix").toString());
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        System.out.println("Number of stuck vehicles/passengers: "+ test.getStuck());

        
    }

    public static void main(String[] args) throws IOException {
        Properties properties = new Properties();
        properties.load(new FileInputStream(new File(args[0])));
        EventsToTravelDiaries.runEventsProcessing(properties);
    }

    @Override
    public void handleEvent(PersonStuckEvent event) {
        if (eventCounter.incrementAndGet() > maxEvents) return;
        try {
            if (!transitDriverIds.contains(event.getPersonId())) {
                TravellerChain chain = chains.get(event.getPersonId());
                setStuck(getStuck() + 1);
                if (chain.getJourneys().size() > 0)
                    chain.getJourneys().removeLast();
            }
        } catch (Exception e) {
            System.err.println(e.getStackTrace());
            System.err.println(event.toString());
        }
    }
 
    @Override
    public void handleEvent(TransitDriverStartsEvent event) {
        if (eventCounter.incrementAndGet() > maxEvents) 
        	return;
        try {
            ptVehicles.put(event.getVehicleId(),new PTVehicle(event.getTransitLineId(), event.getTransitRouteId()));
            transitDriverIds.add(event.getDriverId());
        } catch (Exception e) {
            System.err.println(e.getStackTrace());
            System.err.println(event.toString());
            ;
        }
    }
    
    @Override
    public void handleEvent(TeleportationArrivalEvent event) {
        if (eventCounter.incrementAndGet() > maxEvents) return;
        try {
            if (transitDriverIds.contains(event.getPersonId()))
                return;
            TravellerChain chain = chains.get(event.getPersonId());
            if (chain.traveledVehicle)
                chain.traveledVehicle = false;
        } catch (Exception e) {
            System.err.println(e.getStackTrace());
            System.err.println(event.toString());
        }
    }

    @Override
    public void handleEvent(VehicleArrivesAtFacilityEvent event) {
        if (eventCounter.incrementAndGet() > maxEvents) return;
        try {
            ptVehicles.get(event.getVehicleId()).lastStop = event.getFacilityId();
        } catch (Exception e) {
            System.err.println(e.getStackTrace());
            System.err.println(event.toString());
        }
    }
    
    @Override
    public void handleEvent(ActivityStartEvent event) {
        if (eventCounter.incrementAndGet() > maxEvents) return;
        try {
            if (isTransitScenario) {
                if (transitDriverIds.contains(event.getPersonId()))
                    return;
            }
           
           TravellerChain chain = chains.get(event.getPersonId());
            boolean beforeInPT = chain.isInPT();
            if (event.getActType().equals(PtConstants.TRANSIT_ACTIVITY_TYPE)) {
                chain.setInPT(true);

            } else {
                chain.setInPT(false);
                chain.traveling = false;
                Activity act = chain.addActivity();
                act.setCoord(network.getLinks().get(event.getLinkId()).getCoord());
                act.setFacility(event.getFacilityId());
                act.setStartTime(event.getTime());
                act.setType(event.getActType());
                // end the preceding journey
                Journey journey =  chain.getJourneys().getLast();
                journey.setDest(act.getCoord());
                journey.setEndTime(event.getTime());
                journey.setToAct(act);
                if (beforeInPT)
                    journey.getWalks().getLast().setEgressWalk(true);
            }
        } catch (Exception e) {
            System.err.println(e.getStackTrace());
            System.err.println(event.toString());
            ;
        }
    }
 
    @Override
    public void handleEvent(ActivityEndEvent event) {
        if (eventCounter.incrementAndGet() < maxEvents) {
            try {
                if (isTransitScenario) {
                    if (transitDriverIds.contains(event.getPersonId()))
                        return;
                }
                TravellerChain chain = chains.get(event.getPersonId());
                locations.put(event.getPersonId(),network.getLinks().get(event.getLinkId()).getCoord());
                if (chain == null) {
                    chain = new TravellerChain();
                    chains.put(event.getPersonId(), chain);
                    Activity act = chain.addActivity();
                    act.setCoord(network.getLinks().get(event.getLinkId()).getCoord());
                    act.setEndTime(event.getTime());
                    act.setFacility(event.getFacilityId());
                    act.setStartTime(0.0);
                    act.setType(event.getActType());

                } else if (!event.getActType().equals(
                        PtConstants.TRANSIT_ACTIVITY_TYPE)) {
                	Activity act = chain.getActs().getLast();act.setEndTime(event.getTime());
                }
            } catch (Exception e) {
                System.err.println(e.getStackTrace());
                System.err.println(event.toString());
            }
        }
    }
  
    @Override
    public void handleEvent(PersonEntersVehicleEvent event) {
        if (eventCounter.incrementAndGet() > maxEvents) return;
        try {
            if (transitDriverIds.contains(event.getPersonId()))
                return;
            if (ptVehicles.keySet().contains(event.getVehicleId())) {
                TravellerChain chain = chains.get(event.getPersonId());
                Journey journey = chain.getJourneys().getLast();
                // first, handle the end of the wait
                journey.getWaits().getLast().setEndTime(event.getTime());
                // now, create a new trip
                ptVehicles.get(event.getVehicleId()).addPassenger(event.getPersonId());
                Trip trip = journey.addTrip();
                PTVehicle vehicle = ptVehicles.get(event.getVehicleId());
                trip.setLine(vehicle.transitLineId);
                trip.setMode(transitSchedule.getTransitLines().get(vehicle.transitLineId).getRoutes().get(vehicle.transitRouteId).getTransportMode());
                trip.setBoardingStop(vehicle.lastStop);
                trip.setOrig(journey.getWaits().getLast().getCoord());
                trip.setRoute(ptVehicles.get(event.getVehicleId()).transitRouteId);
                trip.setStartTime(event.getTime());
                // check for the end of a transfer
                if (journey.getPossibleTransfer() != null) {
                    journey.getPossibleTransfer().setToTrip(trip);
                    journey.getPossibleTransfer().setEndTime(event.getTime());
                    journey.addTransfer(journey.getPossibleTransfer());
                    journey.setPossibleTransfer(null);
                }
            } else {
                // add the person to the map that keeps track of who drives what vehicle
                driverIdFromVehicleId.put(event.getVehicleId(),event.getPersonId());
            }
        } catch (Exception e) {
            System.err.println(e.getStackTrace());
            System.err.println(event.toString());
        }
    }

    @Override
    public void handleEvent(PersonLeavesVehicleEvent event) {
        if (eventCounter.incrementAndGet() > maxEvents) return;
        if (transitDriverIds.contains(event.getPersonId()))
            return;
        try {
            if (ptVehicles.keySet().contains(event.getVehicleId())) {
                TravellerChain chain = chains.get(event.getPersonId());
                chain.traveledVehicle = true;
                PTVehicle vehicle = ptVehicles.get(event.getVehicleId());
                double stageDistance = vehicle.removePassenger(event.getPersonId());
                Trip trip = chain.getJourneys().getLast().getTrips().getLast();
                trip.setDistance(stageDistance);
                trip.setAlightingStop(vehicle.lastStop);
            } else {
                driverIdFromVehicleId.remove(event.getVehicleId());
            }

        } catch (Exception e) {
            System.err.println(e.getStackTrace());
            System.err.println(event.toString());
        }
    }
    
    @Override
    public void handleEvent(PersonArrivalEvent event) {
        if (eventCounter.incrementAndGet() > maxEvents) return;
        try {
            if (isTransitScenario) {
                if (transitDriverIds.contains(event.getPersonId()))
                    return;
            }
            TravellerChain chain = chains.get(event.getPersonId());
            String legMode = event.getLegMode();
            String personid = event.getPersonId().toString();
            if (personid.contains("Taxis")){
            	legMode = "taxi";
            }
           switch (legMode) {
                case "walk": {
                	walkTrips = walkTrips +1;
                	Journey journey =  chain.getJourneys().getLast();
                	journey.setEndTime(event.getTime());
                    travelDistanceWalkTotal = travelDistanceWalkTotal  + journey.getDuration() * 1.38888888888889;
                    break;
                }
                case "bike":{
                	bikeTrips = bikeTrips +1;
                	Journey journey =  chain.getJourneys().getLast();
                	journey.setEndTime(event.getTime());
                	travelDistanceBikeTotal = travelDistanceBikeTotal  + journey.getDuration() * 4.166666666666667;
                    break;
                }
                case "transit_walk": {
                    Journey journey =  chain.getJourneys().getLast();
                    Walk walk = journey.getWalks().getLast();
                    walk.setDest(network.getLinks().get(event.getLinkId()).getCoord());
                    walk.setEndTime(event.getTime());
                    walk.setDistance(walk.getDuration() * walkSpeed);
                    break;
                }
                case "car": {
                    Journey journey =  chain.getJourneys().getLast();
                    journey.setDest(network.getLinks().get(event.getLinkId()).getCoord());
                    journey.setEndTime(event.getTime());
                    Trip trip = journey.getTrips().getLast();
                    trip.setDistance(journey.getDistance());
                    trip.setEndTime(event.getTime());
                    carTrips = carTrips + 1;
                    double dd1 = journey.getInVehDistance();
                    double dd2 = journey.getDistance();
                    if (dd1 != dd2){
                    	System.out.println("help");
                    }
                    double tt1 = journey.getInVehTime();
                    double tt2 = journey.getDuration();
                    if (tt1 != tt2){
                    	System.out.println("help");
                    }
                    
        			travelDistanceCarTotal =  travelDistanceCarTotal + journey.getInVehDistance();
        			travelTimeCarTotal =  travelTimeCarTotal + journey.getInVehTime();
        			break;
                }
                case "taxi": {
                	Journey journey =  chain.getJourneys().getLast();
                    journey.setDest(network.getLinks().get(event.getLinkId()).getCoord());
                    journey.setEndTime(event.getTime());
                    Trip trip = journey.getTrips().getLast();
                    trip.setDistance(journey.getDistance());
                    trip.setEndTime(event.getTime());
                    taxiTrips = taxiTrips + 1;
                    travelTimeTaxiTotal = travelTimeTaxiTotal + journey.getInVehTime();
                    travelDistanceTaxiTotal = travelDistanceTaxiTotal + journey.getTaxiDistance();
                    break;
                }
                case "pt":
                	 if (isTransitScenario) {
                		 Journey journey = chain.getJourneys().getLast();
                        Trip trip = journey.getTrips().getLast();
                        trip.setDest(network.getLinks().get(event.getLinkId()).getCoord());
                        trip.setEndTime(event.getTime());
                        journey.setPossibleTransfer(new Transfer());
                        journey.getPossibleTransfer().setStartTime(event.getTime());
                        journey.getPossibleTransfer().setFromTrip(trip);
                        travelTimePtTotal = travelTimePtTotal + journey.getDuration();
                        ptTrips = ptTrips+1;
                    } else {
                    	Journey journey = chain.getJourneys().getLast();
                    	journey.setEndTime(event.getTime());
                        journey.setDest(network.getLinks().get(event.getLinkId()).getCoord());
                        journey.setEndTime(event.getTime());
                        travelTimePtTotal = travelTimePtTotal + journey.getDuration();
                        ptTrips = ptTrips+1;
                    }
                    break;
                default:
                    Journey journey = chain.getJourneys().getLast();
                    journey.setEndTime(event.getTime());
                    journey.setDest(network.getLinks().get(event.getLinkId()).getCoord());
                    journey.setEndTime(event.getTime());
                    Trip trip = journey.getTrips().getLast();
                    trip.setDistance(journey.getDistance());
                    trip.setEndTime(event.getTime());
                    break;
            }
        } catch (Exception e) {
            System.err.println(e.getStackTrace());
            System.err.println(event.toString());
        }
    }

    @Override
    public void handleEvent(PersonDepartureEvent event) {
        if (eventCounter.incrementAndGet() > maxEvents) return;
        try {
            if (transitDriverIds.contains(event.getPersonId()))
                return;
            TravellerChain chain = chains.get(event.getPersonId());
            Journey journey;
            Trip trip;
            String legMode = event.getLegMode();
            
            if (event.getPersonId().toString().contains("Taxis")){
            	legMode = "taxi";
            }
            switch (legMode) {
                case TransportMode.walk:
                    //fall through to the next
                case TransportMode.transit_walk:
                    if (!chain.traveling) {
                        chain.traveling = true;
                        journey =  chain.addJourney();
                        journey.setOrig(network.getLinks().get(event.getLinkId()).getCoord());
                        journey.setFromAct(chain.getActs().getLast());
                        journey.setStartTime(event.getTime());
                        Walk walk = journey.addWalk();
                        walk.setAccessWalk(true);
                        walk.setStartTime(event.getTime());
                        walk.setOrig(journey.getOrig());
                    } else {
                        journey = chain.getJourneys().getLast();
                        Walk walk = journey.addWalk();
                        walk.setStartTime(event.getTime());
                        walk.setOrig(network.getLinks().get(event.getLinkId()).getCoord());
                        journey.getPossibleTransfer().getWalks().add(walk);
                    }
                    break;
                case "car":{
                	journey =  chain.addJourney();
                    journey.setCarJourney(true);
                    journey.setTaxiJourney(false);
                    journey.setOrig(network.getLinks().get(event.getLinkId()).getCoord());
                    journey.setFromAct(chain.getActs().getLast());
                    journey.setStartTime(event.getTime());
                    trip = journey.addTrip();
                    trip.setMode(legMode);
                    trip.setStartTime(event.getTime());
                    break;
                }
                case "taxi": {
                	journey =  chain.addJourney();
                	journey.setCarJourney(false);
                    journey.setTaxiJourney(true);
                    journey.setOrig(network.getLinks().get(event.getLinkId()).getCoord());
                    journey.setFromAct(chain.getActs().getLast());
                    journey.setStartTime(event.getTime());
                    trip = journey.addTrip();
                    trip.setMode("taxi");
                    trip.setStartTime(event.getTime());
                    break;
                }
                case TransportMode.pt:
                    if (isTransitScenario) {
                        // person waits till they enter the vehicle
                        journey = chain.getJourneys().getLast();
                        Wait wait = journey.addWait();
                        if (journey.getWaits().size() == 1)
                            wait.setAccessWait(true);
                        wait.setStartTime(event.getTime());
                        wait.setCoord(network.getLinks().get(event.getLinkId()).getCoord());
                        if (!wait.isAccessWait()) {
                            journey.getPossibleTransfer().getWaits().add(wait);
                        }
                    } else {
                        journey = chain.addJourney();
                        journey.setTeleportJourney(true);
                        journey.setOrig(network.getLinks().get(event.getLinkId()).getCoord());
                        journey.setFromAct(chain.getActs().getLast());
                        journey.setStartTime(event.getTime());
                        journey.setMainmode(legMode);
                        trip = journey.addTrip();
                        trip.setMode(legMode);
                        trip.setStartTime(event.getTime());
                    }
                    break;
                default:
                    journey = chain.addJourney();
                    journey.setTeleportJourney(true);
                    journey.setOrig(network.getLinks().get(event.getLinkId()).getCoord());
                    journey.setFromAct(chain.getActs().getLast());
                    journey.setStartTime(event.getTime());
                    journey.setMainmode(legMode);
                    trip = journey.addTrip();
                    trip.setMode(legMode);
                    trip.setStartTime(event.getTime());
                    break;

            }
        } catch (Exception e) {
            System.err.println(e.getStackTrace());
            System.err.println(event.toString());
        }
    }
 
    @Override
    public void handleEvent(LinkEnterEvent event) {
    	String vehID = event.getVehicleId().toString();
    	Id<Link> linkId = event.getLinkId();
   	  	Link link = network.getLinks().get(linkId);
   	  	Double linkLength = link.getLength();
    	
        if (eventCounter.incrementAndGet() > maxEvents) return;
        try {
            if (ptVehicles.keySet().contains(event.getVehicleId())) {
                PTVehicle ptVehicle = ptVehicles.get(event.getVehicleId());
                ptVehicle.in = true;
                ptVehicle.setLinkEnterTime(event.getTime());
                travelDistancePtTotal = travelDistancePtTotal + linkLength;
            } else {
                chains.get(driverIdFromVehicleId.get(event.getVehicleId())).setLinkEnterTime(event.getTime());
                if (vehID.startsWith("Taxis")){
                	travelDistanceTaxiTotalEnter = travelDistanceTaxiTotalEnter + linkLength; 
                }else{
                	travelDistanceCarTotalEnter = travelDistanceCarTotalEnter + linkLength;
                }
            }

        } catch (Exception e) {
            System.err.println(e.getStackTrace());
            System.err.println(event.toString());
        }

    }

    @Override
    public void handleEvent(LinkLeaveEvent event) {
    	if (eventCounter.incrementAndGet() > maxEvents) return;
        try {
            if (ptVehicles.keySet().contains(event.getVehicleId())) {
                PTVehicle vehicle = ptVehicles.get(event.getVehicleId());
                if (vehicle.in)
                    vehicle.in = false;
                vehicle.incDistance(network.getLinks().get(event.getLinkId()).getLength());

            } else {
                TravellerChain chain = chains.get(driverIdFromVehicleId .get(event.getVehicleId()));
                    String modeType = "car";
                    if (event.getVehicleId().toString().contains("Taxis")){
                    	 modeType = "taxi";
                    }
                    switch (modeType) {
                        case "taxi":{
                        	Journey journey = chain.getJourneys().getLast();  
                            journey.setTaxiJourney(true);
                        	journey.setCarJourney(false);
                        	journey.incrementTaxiDistance(network.getLinks().get(event.getLinkId()).getLength());
                        	journey.setMainmode("taxi");
                        	LinkedList<Trip> trips = journey.getTrips();
                        	Trip trip = trips.getFirst();
                        	trip.setMode("taxi");
                        	travelDistanceTaxiTotalExit = travelDistanceTaxiTotalExit + network.getLinks().get(event.getLinkId()).getLength();             	                            	 
                        	break;
                        }
                            
                        case "car":{
                        	Journey journey = chain.getJourneys().getLast();  
                            journey.setTaxiJourney(false);
                        	journey.setCarJourney(true);
                        	journey.incrementCarDistance(network.getLinks().get(event.getLinkId()).getLength());
                        	travelDistanceCarTotalExit = travelDistanceCarTotalExit + network.getLinks().get(event.getLinkId()).getLength();
                        	break;
                    }     
                }
            }
        } catch (Exception e) {
            System.err.println(e.getStackTrace());
            System.err.println(event.toString());
        }
    }

    // Methods
    @Override
    public void reset(int iteration) {
        chains = new HashMap<>();
        locations = new HashMap<>();
        ptVehicles = new HashMap<>();
        transitDriverIds = new HashSet<>();
        driverIdFromVehicleId = new HashMap<>();
    }

    public void writeSimulationResultsToTabSeparated(String path, String appendage) throws IOException {
      for (Entry<Id, TravellerChain> entry : chains.entrySet()) {
            TravellerChain chain = entry.getValue();
            for (Journey journey :  chain.getJourneys()) {

            	String modeType = journey.getMainMode();
        		double duration = journey.getInVehTime();
        		double distance = journey.getInVehDistance();
        		if (modeType.equals("taxi")){
        			modeType = "taxi";
        		}
        		
        		switch (modeType) {
	                case "taxi":{
	                	taxiTripsJ = taxiTripsJ + 1;
	        			travelDistanceTaxiTotalJ =  travelDistanceTaxiTotalJ + distance;
	        			travelTimeTaxiTotalJ =  travelTimeTaxiTotalJ + duration;
	        			}
	                break;
	                case "car":{
	                	carTripsJ = carTripsJ + 1;
	        			travelDistanceCarTotalJ =  travelDistanceCarTotalJ + distance;
	        			travelTimeCarTotalJ =  travelTimeCarTotalJ + duration;
	        			}
	                break;
	                
     
	                /*if (mode.equals("bike")){
            			bikeTripsJ = bikeTripsJ + J;
            			travelDistanceBikeTotalJ =  travelDistanceBikeTotalJ + distance;
            			travelTimeBikeTotalJ =  travelTimeBikeTotalJ + distance;
            			
            		}
            		if (mode.equals("walk")){
            			bikeTripsJ = bikeTripsJ + J;
            			travelDistanceWalkTotalJ =  travelDistanceWalkTotalJ + distance;
            			travelTimeWalkTotalJ =  travelTimeWalkTotalJ + distance;
            			
            		}
            		
            		*/
            	
            }
        }
      }

        // time in minutes or hours
        travelTimeCarTotal =  travelTimeCarTotal / 3600;
        travelTimeCarTotalJ =  travelTimeCarTotalJ / 60;
        travelTimeTaxiTotal = travelTimeTaxiTotal / 60;
        travelTimeTaxiTotalJ = travelTimeTaxiTotalJ / 60;
        travelTimePtTotal = travelTimePtTotal/60;
        travelTimePtTotalJ = travelTimePtTotalJ/60;
        
        //distances in km
        travelDistanceCarTotal = travelDistanceCarTotal/1000;
        travelDistanceCarTotalJ = travelDistanceCarTotalJ/1000;
        travelDistanceCarTotalEnter= travelDistanceCarTotalEnter/1000;
        travelDistanceCarTotalExit = travelDistanceCarTotalExit/1000;
        travelDistanceTaxiTotal = travelDistanceTaxiTotal/1000;
        travelDistanceTaxiTotalJ = travelDistanceTaxiTotalJ/1000;
        travelDistanceTaxiTotalEnter= travelDistanceTaxiTotalEnter/1000;
        travelDistanceTaxiTotalExit = travelDistanceTaxiTotalExit/1000;
        travelDistancePtTotal=  travelDistancePtTotal/1000;
        travelDistancePtTotalJ=  travelDistancePtTotalJ/1000;
    	
    	
    	
    	System.out.println("");
    	System.out.println("for excel sheet");
    	System.out.println("td car [km];  carTrips ; tt car [in h], , km/h, travelTimeCarTotal/trip (in min), travelDistanceCarTotal/trip");
    	System.out.println(travelDistanceCarTotal);
    	System.out.println(carTrips);
    	System.out.println(travelTimeCarTotal);
    	System.out.println(travelDistanceCarTotal/travelTimeCarTotal);
    	
    	
    	/*System.out.println("ttJ car [min]: " + travelTimeCarTotalJ );
    	System.out.println("tt taxi [min]: " + travelTimeTaxiTotal );
    	System.out.println("ttJ taxi [min]: " + travelTimeTaxiTotalJ );
    	System.out.println("tt pt [min]: " + travelTimePtTotal);*/
    	
    	//System.out.println("tdJ car [km]: " + travelDistanceCarTotalJ);
    	/*System.out.println("tdEnter car [km]: " + travelDistanceCarTotalEnter);
    	System.out.println("tdExit car[km]: " + travelDistanceCarTotalExit);
    	System.out.println("td taxi [km]: " + travelDistanceTaxiTotal);
    	System.out.println("tdJ taxi [km]: " + travelDistanceTaxiTotalJ);
    	System.out.println("tdEnter taxi [km]: " + travelDistanceTaxiTotalEnter);
    	System.out.println("tdExit taxi [km]: " + travelDistanceTaxiTotalExit);
    	System.out.println("td pt [km]: " + travelDistancePtTotal);
    	*/

    	/*System.out.println("carTripsJ: " + carTripsJ);
    	System.out.println("taxiTrips: " + taxiTrips);
    	System.out.println("taxiTripsJ: " + taxiTripsJ);
    	System.out.println("ptTrips: " + ptTrips);
    	System.out.println("bikeTrips: " + bikeTrips);
    	System.out.println("walkTrips: " + walkTrips);*/
    	

    	System.out.println(((travelTimeCarTotal * 60) / carTrips));
    	/*System.out.println("travelTimeTaxiTotal/trip: " + (travelTimeTaxiTotal / taxiTrips));
    	System.out.println("travelTimePtTotal/trip: " + (travelTimePtTotal / ptTrips));*/
    	
    	
    	System.out.println((travelDistanceCarTotal/ carTrips));
    	/*System.out.println("travelDistanceTaxiTotal/trip: " + (travelDistanceTaxiTotal/ taxiTrips));
    	System.out.println("travelDistancePtTotal/trip: " + (travelDistancePtTotal/ ptTrips));
    	System.out.println(" ");*/
      }
    	   

    public int getStuck() {
        return stuck;
    }

    void setStuck(int stuck) {
        this.stuck = stuck;
    }

     private class PTVehicle {

        // Attributes
        private final Id transitLineId;
        private final Id transitRouteId;
        private final Map<Id, Double> passengers = new HashMap<>();
        boolean in = false;
        Id lastStop;
        private double distance;
        private double linkEnterTime = 0.0;

        // Constructors
        public PTVehicle(Id transitLineId, Id transitRouteId) {
            this.transitLineId = transitLineId;
            this.transitRouteId = transitRouteId;
        }

        // Methods
        public void incDistance(double linkDistance) {
            distance += linkDistance;
        }

        public void addPassenger(Id passengerId) {
            passengers.put(passengerId, distance);
        }

        public double removePassenger(Id passengerId) {
            return distance - passengers.remove(passengerId);
        }

        public double getLinkEnterTime() {
            return linkEnterTime;
        }

        public void setLinkEnterTime(double linkEnterTime) {
            this.linkEnterTime = linkEnterTime;
        }

    }

}
