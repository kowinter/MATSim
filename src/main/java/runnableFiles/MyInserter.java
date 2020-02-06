package runnableFiles;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.data.Fleet;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.passenger.PassengerRequests;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Schedules;
import org.matsim.contrib.taxi.data.TaxiRequest;
import org.matsim.contrib.taxi.optimizer.BestDispatchFinder;
import org.matsim.contrib.taxi.optimizer.UnplannedRequestInserter;
import org.matsim.contrib.taxi.optimizer.rules.IdleTaxiZonalRegistry;
import org.matsim.contrib.taxi.optimizer.rules.RuleBasedTaxiOptimizerParams;
import org.matsim.contrib.taxi.optimizer.rules.UnplannedRequestZonalRegistry;
import org.matsim.contrib.taxi.schedule.TaxiEmptyDriveTask;
import org.matsim.contrib.taxi.schedule.TaxiStayTask;
import org.matsim.contrib.taxi.schedule.TaxiTask;
import org.matsim.contrib.taxi.schedule.TaxiTask.TaxiTaskType;
import org.matsim.contrib.taxi.scheduler.TaxiScheduler;
import org.matsim.contrib.zone.ZonalSystem;
import org.matsim.contrib.zone.Zone;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;


public class MyInserter implements UnplannedRequestInserter {
	private final TaxiScheduler scheduler;
	private final BestDispatchFinder dispatchFinder;
	private final MobsimTimer timer;
	private final IdleTaxiZonalRegistry idleTaxiRegistry;
	private final UnplannedRequestZonalRegistry unplannedRequestRegistry;
	private final RuleBasedTaxiOptimizerParams params;
	private final Network network;
	private final Map<Id<Zone>, List<Id<Link>>> ZoneLink;
	private final QSim qsim;
	private final TravelDisutility travelDisutility;
	private final Fleet fleet;
	private final TravelTime travelTime;
	private final ZonalSystem zonalSystem;

	public MyInserter(TaxiScheduler scheduler, MobsimTimer timer, Network network, TravelTime travelTime,
			TravelDisutility travelDisutility, RuleBasedTaxiOptimizerParams params, QSim qsim,
			Map<Id<Zone>, List<Id<Link>>> ZoneLink, IdleTaxiZonalRegistry idleTaxiRegistry, UnplannedRequestZonalRegistry unplannedRequestRegistry, Fleet fleet,
			ZonalSystem zonalSystem) {
		this(scheduler, timer, network, travelTime, travelDisutility,
				new BestDispatchFinder(scheduler, network, timer, travelTime, travelDisutility), params, qsim, ZoneLink,
				idleTaxiRegistry, unplannedRequestRegistry, fleet, zonalSystem);
	}
	public MyInserter(TaxiScheduler scheduler, MobsimTimer timer, Network network, TravelTime travelTime,
			TravelDisutility travelDisutility, BestDispatchFinder dispatchFinder, RuleBasedTaxiOptimizerParams params,
			QSim qsim, Map<Id<Zone>, List<Id<Link>>> ZoneLink, IdleTaxiZonalRegistry idleTaxiRegistry, UnplannedRequestZonalRegistry unplannedRequestRegistry, Fleet fleet,
			ZonalSystem zonalSystem) {
		this.scheduler = scheduler;
		this.timer = timer;
		this.params = params;
		this.dispatchFinder = dispatchFinder;
		this.idleTaxiRegistry = idleTaxiRegistry;
		this.unplannedRequestRegistry = unplannedRequestRegistry;
		this.network = network;
		this.qsim = qsim;
		this.ZoneLink = ZoneLink;
		this.fleet = fleet;
		this.travelDisutility = travelDisutility;
		this.travelTime = travelTime;
		this.zonalSystem = zonalSystem;	
	}
	@Override
	public void scheduleUnplannedRequests(Collection<TaxiRequest> unplannedRequests) {		
		boolean requestsToServe = false;
		if (unplannedRequests.size() > 0){
			requestsToServe = true;
		}		
		if (isReduceTP(unplannedRequests)) {
			// reduce T_P to increase throughput (demand > supply)
			scheduleIdleVehiclesImpl(unplannedRequests);
		} else {
			// reduce T_W (regular NOS)
			scheduleUnplannedRequestsImpl(unplannedRequests);
		}
		// implement here a double check, if all vehicles that are not used are correctly parked 
		checkIfAllIdleVehiclesAreParkedCorrectly(requestsToServe);
	}
	public enum Goal {
		MIN_WAIT_TIME, MIN_PICKUP_TIME, DEMAND_SUPPLY_EQUIL;
	};
	private boolean isReduceTP(Collection<TaxiRequest> unplannedRequests) {
		switch (params.goal) {
		case MIN_PICKUP_TIME:
			return true;

		case MIN_WAIT_TIME:
			return false;

		case DEMAND_SUPPLY_EQUIL:
			double now = timer.getTimeOfDay();
			long awaitingReqCount = unplannedRequests.stream().filter(r -> PassengerRequests.isUrgent(r, now)).count();
			return awaitingReqCount > idleTaxiRegistry.getVehicleCount();

		default:
			throw new IllegalStateException();

		}
	}

	// request-initiated scheduling
	private void scheduleUnplannedRequestsImpl(Collection<TaxiRequest> unplannedRequests) {
		
		// vehicles are not immediately removed so calculate 'idleCount' locally
		int idleCount = idleTaxiRegistry.getVehicleCount();
		
		Iterator<TaxiRequest> reqIter = unplannedRequests.iterator();
		while (reqIter.hasNext() && idleCount > 0) {
			TaxiRequest req = reqIter.next();

			Stream<Vehicle> selectedVehs = idleCount > params.nearestVehiclesLimit//
					? idleTaxiRegistry.findNearestVehicles(req.getFromLink().getFromNode(), params.nearestVehiclesLimit)
					: idleTaxiRegistry.vehicles();

			BestDispatchFinder.Dispatch<TaxiRequest> best = dispatchFinder.findBestVehicleForRequest(req, selectedVehs);
			if (best == null) {
				return;
			}
			
			scheduler.scheduleRequest(best.vehicle, best.destination, best.path);

			reqIter.remove();
			unplannedRequestRegistry.removeRequest(req);
			idleCount--;
			Counter_ParkingVehicles.removePotentialRelocatingCandidate(best.vehicle);
			MyOptimizer.unparkVehicleIfNeeded (best.vehicle, idleTaxiRegistry);
		}	
	}
	
	// vehicle-initiated scheduling
	private void scheduleIdleVehiclesImpl(Collection<TaxiRequest> unplannedRequests) {
		
		Iterator<Vehicle> vehIter = idleTaxiRegistry.vehicles().iterator();
		while (vehIter.hasNext() && !unplannedRequests.isEmpty()) {
			Vehicle veh = vehIter.next();
			Link link = ((TaxiStayTask) veh.getSchedule().getCurrentTask()).getLink();

			Stream<TaxiRequest> selectedReqs = unplannedRequests.size() > params.nearestRequestsLimit
					? unplannedRequestRegistry.findNearestRequests(link.getToNode(), params.nearestRequestsLimit)
					: unplannedRequests.stream();

			BestDispatchFinder.Dispatch<TaxiRequest> best = dispatchFinder.findBestRequestForVehicle(veh, selectedReqs);
			
			scheduler.scheduleRequest(best.vehicle, best.destination, best.path);

			unplannedRequests.remove(best.destination);
			unplannedRequestRegistry.removeRequest(best.destination);
			Counter_ParkingVehicles.removePotentialRelocatingCandidate(best.vehicle);
			MyOptimizer.unparkVehicleIfNeeded  (best.vehicle, idleTaxiRegistry);
		}
	}
		
	private void checkIfAllIdleVehiclesAreParkedCorrectly(boolean requestsToServe) {
		ArrayList<Vehicle> checkTheseVehicles = Counter_ParkingVehicles.getPotentialRelocatingCandidate();
		if (requestsToServe == true && !checkTheseVehicles.isEmpty()){
			for (int i = 0; i < checkTheseVehicles.size(); i ++){
				Vehicle veh = checkTheseVehicles.get(i);
				Schedule schedule = veh.getSchedule();
				TaxiTask previousTask = (TaxiTask) Schedules.getPreviousTask(schedule);
				TaxiTask currentTask = (TaxiTask) schedule.getCurrentTask();
				Counter_ParkingVehicles.removePotentialRelocatingCandidate(veh);
				if (Relocator.getCruise() == false) {
					if (previousTask.getTaxiTaskType() == TaxiTaskType.DROPOFF && currentTask.getTaxiTaskType() == TaxiTaskType.STAY) {
						boolean hasToMoveFromInnerCity = false;
						MyOptimizer.relocateAndPark(veh, timer, network, qsim, ZoneLink, fleet, zonalSystem, travelDisutility, travelTime, scheduler,hasToMoveFromInnerCity, idleTaxiRegistry);	
					}
				}else{
					if(currentTask.getTaxiTaskType() == TaxiTaskType.STAY){	
						if (currentTask.getEndTime() == 104400.0) {
							Relocator_Cruise.relocate(veh, timer, network, qsim, ZoneLink, fleet, zonalSystem,idleTaxiRegistry, travelDisutility, travelTime, scheduler);
						}
					}
				}
			}
		}
	}	
}

