package runnableFiles;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.data.Fleet;
import org.matsim.contrib.dvrp.data.Request;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Schedules;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.dvrp.schedule.Schedule.ScheduleStatus;
import org.matsim.contrib.taxi.data.TaxiRequest;
import org.matsim.contrib.taxi.optimizer.DefaultTaxiOptimizer;
import org.matsim.contrib.taxi.optimizer.rules.IdleTaxiZonalRegistry;
import org.matsim.contrib.taxi.optimizer.rules.RuleBasedTaxiOptimizerParams;
import org.matsim.contrib.taxi.optimizer.rules.UnplannedRequestZonalRegistry;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.contrib.taxi.schedule.TaxiStayTask;
import org.matsim.contrib.taxi.schedule.TaxiTask;
import org.matsim.contrib.taxi.schedule.TaxiTask.TaxiTaskType;
import org.matsim.contrib.taxi.scheduler.TaxiScheduler;
import org.matsim.contrib.zone.SquareGridSystem;
import org.matsim.contrib.zone.ZonalSystem;
import org.matsim.contrib.zone.Zone;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

public class MyOptimizer extends DefaultTaxiOptimizer {

	public static MyOptimizer create(TaxiConfigGroup taxiCfg, Fleet fleet, TaxiScheduler scheduler, Network network,MobsimTimer timer, TravelTime travelTime, TravelDisutility travelDisutility,RuleBasedTaxiOptimizerParams params, QSim qsim, Map<Id<Zone>, List<Id<Link>>> ZoneLink) {
		return create(taxiCfg, fleet, scheduler, network, timer, travelTime, travelDisutility, params, qsim,new SquareGridSystem(network, params.cellSize), ZoneLink);
	}

	public static MyOptimizer create(TaxiConfigGroup taxiCfg, Fleet fleet, TaxiScheduler scheduler, Network network,MobsimTimer timer, TravelTime travelTime, TravelDisutility travelDisutility,RuleBasedTaxiOptimizerParams params, QSim qsim, ZonalSystem zonalSystem,Map<Id<Zone>, List<Id<Link>>> ZoneLink) {
		IdleTaxiZonalRegistry idleTaxiRegistry = new IdleTaxiZonalRegistry(zonalSystem, scheduler);
		UnplannedRequestZonalRegistry unplannedRequestRegistry = new UnplannedRequestZonalRegistry(zonalSystem);
		MyInserter requestInserter = new MyInserter(scheduler, timer, network, travelTime, travelDisutility, params,qsim, ZoneLink, idleTaxiRegistry, unplannedRequestRegistry, fleet, zonalSystem);
		return new MyOptimizer(taxiCfg, timer, fleet, scheduler, network, params, idleTaxiRegistry,unplannedRequestRegistry, requestInserter, qsim, zonalSystem, ZoneLink, travelDisutility, travelTime);
	}

	private final TaxiScheduler scheduler;
	private final IdleTaxiZonalRegistry idleTaxiRegistry;
	private final UnplannedRequestZonalRegistry unplannedRequestRegistry;
	private final RuleBasedTaxiOptimizerParams params;
	private final MobsimTimer timer;
	private final Network network;
	private final Map<Id<Zone>, List<Id<Link>>> ZoneLink;
	private final QSim qsim;
	private final TravelDisutility travelDisutility;
	private final Fleet fleet;
	private final TravelTime travelTime;
	private final ZonalSystem zonalSystem;

	public MyOptimizer(TaxiConfigGroup taxiCfg, MobsimTimer timer, Fleet fleet, TaxiScheduler scheduler,Network network, 
			RuleBasedTaxiOptimizerParams params, IdleTaxiZonalRegistry idleTaxiRegistry,
			UnplannedRequestZonalRegistry unplannedRequestRegistry, MyInserter requestInserter, 
			QSim qsim,ZonalSystem zonalSystem, Map<Id<Zone>, List<Id<Link>>> ZoneLink, TravelDisutility travelDisutility,
			TravelTime travelTime) {
		
		super(taxiCfg, fleet, scheduler, params, requestInserter);
		this.scheduler = scheduler;
		this.idleTaxiRegistry = idleTaxiRegistry;
		this.unplannedRequestRegistry = unplannedRequestRegistry;
		this.params = params;
		this.timer = timer;
		this.network = network;
		this.qsim = qsim;
		this.ZoneLink = ZoneLink;
		this.fleet = fleet;
		this.travelDisutility = travelDisutility;
		this.travelTime = travelTime;
		this.zonalSystem = zonalSystem;
	}

	@Override
	public void requestSubmitted(Request request) {
		// here I could check for relocation tasks that can be interrupted
		super.requestSubmitted(request);
		unplannedRequestRegistry.addRequest((TaxiRequest) request);
	}

	@Override
	public void notifyMobsimBeforeSimStep(@SuppressWarnings("rawtypes") MobsimBeforeSimStepEvent e) {
	if (isNewDecisionEpoch(e, params.reoptimizationTimeStep)) {
		if (Relocator.getNoReloaction() == false ) {		
			if (timer.getTimeOfDay()> 60 && Relocator.getCruise() == true) {
				Map<Id<Vehicle>, ? extends Vehicle> taxis = fleet.getVehicles();
				for (Entry<Id<Vehicle>, ? extends Vehicle> entry : taxis.entrySet()) {
					if (unplannedRequestRegistry.getRequestCount() == 0) {
						Id<Vehicle> vehicleId = entry.getKey();
						Vehicle vehicle = taxis.get(vehicleId);
						Schedule schedule = vehicle.getSchedule();	
						TaxiTask currentTask = (TaxiTask) schedule.getCurrentTask();
						if(currentTask.getTaxiTaskType() == TaxiTaskType.STAY){	
							if (currentTask.getEndTime() == 104400.0) {
								Relocator_Cruise.relocate(vehicle, timer, network, qsim, ZoneLink, fleet, zonalSystem,idleTaxiRegistry, travelDisutility, travelTime, scheduler);
							}
						}else {
							Counter_ParkingVehicles.addPotentialRelocatingCandidate(vehicle);	
						}
					}
				}
			}
			
			if (Relocator.getCruise() == false) {
				Map<Vehicle, Double> vehiclesThatMightNeedRelocation = Counter_ParkingVehicles.getTaxisThatNeedToBeParkedOutsideOfCityCentre();
				if (vehiclesThatMightNeedRelocation != null && !vehiclesThatMightNeedRelocation.isEmpty()) {

					boolean hasToMoveFromInnerCity = false;
					if (Relocator.getTimeLimitedInnerCityParking() == true) {
						hasToMoveFromInnerCity = true;
					}
					if (Relocator.getLimitInnerCityParkingDaytime(e.getSimulationTime()) == true) {
						hasToMoveFromInnerCity = true;
					}
					if (Relocator.getNoInnerCityParking() == true) {
						hasToMoveFromInnerCity = true;
					}
					List<Vehicle> vehThatNeedRelocation = new ArrayList<Vehicle>();

					if (hasToMoveFromInnerCity == true) {
						for (Map.Entry<Vehicle, Double> entry : vehiclesThatMightNeedRelocation.entrySet()) {
							Vehicle vehicle = entry.getKey();
							Double timeToMove = entry.getValue();
							double currentTime = e.getSimulationTime();
							if (timeToMove <= currentTime) {
								vehThatNeedRelocation.add(vehicle);
							}
						}
						for (int i = 0; i < vehThatNeedRelocation.size(); i++) {
							Vehicle veh = vehThatNeedRelocation.get(i);
							Task currentTask = veh.getSchedule().getCurrentTask();
							double endTime = currentTask.getEndTime();
							if (endTime == 104400.0) {
								// current task is a stay task, the last in the schedule --> move vehicle out of the inner city
								unparkVehicleIfNeeded(veh, idleTaxiRegistry);
								relocateAndPark(veh, timer, network, qsim, ZoneLink, fleet, zonalSystem, travelDisutility,travelTime, scheduler, hasToMoveFromInnerCity,idleTaxiRegistry);
							} else {
								// the current task is a relocation task, the vehicle does not need to relocate again just yet, include a buffer of 1 minute
								Counter_ParkingVehicles.addTaxisThatNeedToBeParkedOutsideOfCityCentre(veh,(veh.getSchedule().getCurrentTask().getEndTime() + 60));
							}
						}
					}
				}
			}
		}
		}
		super.notifyMobsimBeforeSimStep(e);
	}

	@Override
	public void nextTask(Vehicle vehicle) {
		super.nextTask(vehicle);	
		Schedule schedule = vehicle.getSchedule();	
		// end of simulation
		if (schedule.getStatus() == ScheduleStatus.COMPLETED) { 					
			TaxiStayTask lastTask = (TaxiStayTask) Schedules.getLastTask(schedule);
			if (lastTask.getBeginTime() < vehicle.getServiceEndTime()) {
				Vehicle idleVeh = idleTaxiRegistry.vehicles().filter(Vehicle -> vehicle.equals(Vehicle)).findAny().orElse(null); // this check is necessary for the veh relocated in the Inserter...
				if (idleVeh != null){
					idleTaxiRegistry.removeVehicle(vehicle);
				}
			}
		}else if (scheduler.isIdle(vehicle)) {
			// check if vehicle is idle and should be relocated
				if (unplannedRequestRegistry.getRequestCount() == 0 && schedule.getCurrentTask().getTaskIdx() > 1) { 
					// only call relocator if no idle requests 	
					TaxiTask previousTask = (TaxiTask) Schedules.getPreviousTask(schedule);
					TaxiTask currentTask = (TaxiTask) schedule.getCurrentTask();
					
					if(Relocator.getCruise() == true) {
						Vehicle idleVeh = idleTaxiRegistry.vehicles().filter(Vehicle -> vehicle.equals(Vehicle)).findAny().orElse(null); // this check is necessary for the veh relocated in the Inserter...
						if (idleVeh == null){
							idleTaxiRegistry.addVehicle(vehicle);
						}
					} else {
						if(previousTask.getTaxiTaskType() == TaxiTaskType.EMPTY_DRIVE){	
							// vehicle has just relocated, park 
							Vehicle idleVeh = idleTaxiRegistry.vehicles().filter(Vehicle -> vehicle.equals(Vehicle)).findAny().orElse(null); // this check is necessary for the veh relocated in the Inserter...
							if (idleVeh == null){
								idleTaxiRegistry.addVehicle(vehicle);
							}
						}else if (previousTask.getTaxiTaskType() == TaxiTaskType.DROPOFF && currentTask.getTaxiTaskType() == TaxiTaskType.STAY) {
							// vehicle has just dropped off passenger, relocate if needed
							if (Relocator.getNoReloaction() == false ) {
								boolean hasToMoveFromInnerCity = false;
								relocateAndPark(vehicle, timer, network, qsim, ZoneLink, fleet, zonalSystem, travelDisutility, travelTime, scheduler,hasToMoveFromInnerCity, idleTaxiRegistry);	
							}else {
							//	idleTaxiRegistry.addVehicle(vehicle);
							}
						}
					}
				}else{
					// there are open request and vehicle might not need to relocate
					Vehicle idleVeh = idleTaxiRegistry.vehicles().filter(Vehicle -> vehicle.equals(Vehicle)).findAny().orElse(null); // this check is necessary for the veh relocated in the Inserter...
					if (idleVeh == null){
						idleTaxiRegistry.addVehicle(vehicle);
					}	
					if (schedule.getCurrentTask().getTaskIdx() > 1) {
						Counter_ParkingVehicles.addPotentialRelocatingCandidate(vehicle);	
					}
				}
		}else{ 
			// vehicle is not idle
			if (schedule.getCurrentTask().getTaskIdx() != 0) {// not first task
				Vehicle idleVeh = idleTaxiRegistry.vehicles().filter(Vehicle -> vehicle.equals(Vehicle)).findAny().orElse(null); 
				if (idleVeh != null){ // this check is necessary for the veh relocated in the Inserter...
					idleTaxiRegistry.removeVehicle(vehicle);
					unparkVehicleIfNeeded(vehicle,idleTaxiRegistry);
				}
			}
		}
	}

	@Override
	protected boolean doReoptimizeAfterNextTask(TaxiTask newCurrentTask) {
		return isWaitStay(newCurrentTask);
	}

	protected boolean isWaitStay(TaxiTask task) {
		return task.getTaxiTaskType() == TaxiTaskType.STAY;
	}

	public static void unparkVehicleIfNeeded(Vehicle vehicle, IdleTaxiZonalRegistry idleTaxiRegistry) {
		/// REMOVE "PARKING RESERVATION" AFTER REQUEST ASSIGNMENT
		Map<Vehicle, Link> taxisWithParkingReservation = Counter_ParkingVehicles.getTaxisWithParkingReservation();
		if (taxisWithParkingReservation.containsKey(vehicle)) {
			Link linkWithReservation = taxisWithParkingReservation.get(vehicle);
			Counter_ParkingVehicles.unpark(linkWithReservation, vehicle);
		}
	}

	public static void relocateAndPark(Vehicle vehicle, MobsimTimer timer, Network network, QSim qsim,
			Map<Id<Zone>, List<Id<Link>>> ZoneLink, Fleet fleet, ZonalSystem zonalSystem,
			TravelDisutility travelDisutility, TravelTime travelTime, TaxiScheduler scheduler,
			Boolean hasToMoveFromInnerCity, IdleTaxiZonalRegistry idleTaxiRegistry) {
		Relocator.relocate(vehicle, timer, network, qsim, ZoneLink, fleet, zonalSystem, travelDisutility, 
				travelTime,scheduler, hasToMoveFromInnerCity);
	}

	public static boolean isNewDecisionEpoch(@SuppressWarnings("rawtypes") MobsimBeforeSimStepEvent e,
			int epochLength) {
		return e.getSimulationTime() % epochLength == 0;
	}
}