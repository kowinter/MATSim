package runnableFiles;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.data.*;
import org.matsim.contrib.dvrp.path.VrpPath;
import org.matsim.contrib.dvrp.schedule.*;
import org.matsim.contrib.dvrp.schedule.Schedule.ScheduleStatus;
import org.matsim.contrib.taxi.data.TaxiRequest;
import org.matsim.contrib.taxi.schedule.*;
import org.matsim.contrib.taxi.schedule.TaxiTask.TaxiTaskType;
import org.matsim.contrib.taxi.util.stats.TaxiStatsCalculators;
import org.matsim.contrib.util.LongEnumAdder;
import org.matsim.contrib.zone.Zone;
import org.matsim.contrib.zone.util.ZoneFinder;
import org.matsim.contrib.zone.util.ZoneFinderImpl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.io.IOUtils;

public class Calculator_TaxiParkingStats {

	private final String outputDirectory = Run.getOutputLocation();
	private final int hours;
	public static String DAILY_STATS_ID = "daily";
	private final MyTaxiParkingStats[] hourlyStats;
	private static MyTaxiParkingStats dailyStats = new MyTaxiParkingStats(DAILY_STATS_ID);
	private final List<MyTaxiParkingStats> taxiParkingStats;
	private Map<Id<Zone>, TreeMap<Integer, Integer>> ZoneINnOUT = new HashMap<Id<Zone>, TreeMap<Integer, Integer>>();
	private Map<Id<Vehicle>, TaxiChain> chains = new HashMap<>();
	private static Double counterIniParking = 0.0;
	private static Double counterShortWrongPark = 0.0;
	private static Double counterLongWrongPark = 0.0;
	private static Double counterParking = 0.0;
	private static Double counterLastParking = 0.0;
	private static Double counterPickUp = 0.0;
	private static Double counterDropOff = 0.0;
	private static Double counterOccDrive = 0.0;
	private static Double counterReloc = 0.0;
	private static Double counterIdleApproach = 0.0;
	private static Double occupied_drive_tt = 0.0;
	private static Double occupied_drive_td = 0.0;
	private static Double empty_relocation_tt = 0.0;
	private static Double empty_relocation_td = 0.0;
	private static Double empty_serving_tt = 0.0;
	private static Double empty_serving_td = 0.0;
	private static List<Double> ams_to_ams = new ArrayList<Double>();
	private static List<Double> ams_to_out = new ArrayList<Double>();
	private static List<Double> out_to_ams = new ArrayList<Double>();
	private static List<Double> out_to_out = new ArrayList<Double>();
	private static DescriptiveStatistics passengerServedPerVehicle = new DescriptiveStatistics();
	private static DescriptiveStatistics wrongfulParkingEvents = new DescriptiveStatistics();
	private static DescriptiveStatistics rightfulParkingEvents = new DescriptiveStatistics();
	private static DescriptiveStatistics rightfulParkingEventsLast = new DescriptiveStatistics();

	public Calculator_TaxiParkingStats(Iterable<? extends Vehicle> vehicles, MyZonalSystem myZonalSystem) {

		hours = calcHourCount(vehicles);
		hourlyStats = new MyTaxiParkingStats[hours];
		for (int h = 0; h < hours; h++) {
			hourlyStats[h] = new MyTaxiParkingStats(h + "");
		}
		taxiParkingStats = createStatsList(hourlyStats);
		dailyStats = new MyTaxiParkingStats("daily");

		// initiate minute based TREEMAP (sorts its entries by key automatically)
		Set<Id<Zone>> zoneIDs = myZonalSystem.getZones().keySet();
		for (Id<Zone> zone : zoneIDs) {
			TreeMap<Integer, Integer> INnOUT = new TreeMap<Integer, Integer>();
			Integer iniParking = Counter_ParkingVehicles.getInitialVehiclesParkedInZone(zone);
			INnOUT.put(0, iniParking);
			for (int i = 1; i < 1740; i++) {
				INnOUT.put(i, 0);
			}
			ZoneINnOUT.put(zone, INnOUT);
		}

		for (Vehicle v : vehicles) {
			analyzeTasks(v, myZonalSystem); // count the incoming and outgoing vehicles per zone
		}

		for (Id<Zone> zone : zoneIDs) {
			analyseZones(zone);
		}

		// write the taxi events file
		try {
			File f = new File(outputDirectory);
			System.out.println("Writing files to " + f.getAbsolutePath());
			if (!f.exists() && f.canWrite()) {
				System.err.println("Problem (Calculator): Cannot write to output directory. "
						+ "Check for existence and permissions");
				System.exit(1);
			}

		} catch (ArrayIndexOutOfBoundsException e) {
			System.err.println("Problem (Calculator):Cannot write file, l.110");
			System.exit(1);
		}

		try {
			writeSimulationResultsToTabSeparated(chains);
		} catch (IOException e) {
			System.err.println("Problem (Calculator):Cannot write file, l.117 ");
			System.exit(1);
		}
	}

	private void analyzeTasks(Vehicle v, MyZonalSystem myZonalSystem) {
		Schedule schedule = v.getSchedule();
		if (schedule.getStatus() == ScheduleStatus.UNPLANNED) {
			return;// do not evaluate
		}
		if (schedule.getTaskCount() == 1) {
			// vehicle has not been used and remained at its original parking position
			Id<Zone> zone = Counter_ParkingVehicles.getZoneInWhichVehicleParkesInitially(v, myZonalSystem);
			TreeMap<Integer, Integer> currentZoneEntry = ZoneINnOUT.get(zone);
			for (int e = 1; e < 1740; e++) {
				Integer entry = currentZoneEntry.get(e);
				currentZoneEntry.put(e, entry + 1);
			}
			counterIniParking = counterIniParking + 1;
			rightfulParkingEvents.addValue(1740);
			hourlyStats[0].parkingDuration.addValue(104400.0);
			dailyStats.parkingDuration.addValue(104400.0);

			// add hour
			DescriptiveStatistics zonalDurationStats = hourlyStats[0].zonalParkingDuration.get(zone);
			if (zonalDurationStats == null) {
				DescriptiveStatistics du = new DescriptiveStatistics();
				du.addValue(1740);
				hourlyStats[0].zonalParkingDuration.put(zone, du);
			} else {
				zonalDurationStats.addValue(1740);
				hourlyStats[0].zonalParkingDuration.put(zone, zonalDurationStats);
			}

			for (int h = 0; h < hours; h++) {
				DescriptiveStatistics zonalInitParkingVehicles = hourlyStats[h].zonalInitParkingVehicles.get(zone);
				if (zonalInitParkingVehicles == null) {
					DescriptiveStatistics ini = new DescriptiveStatistics();
					ini.addValue(1.0);
					hourlyStats[h].zonalInitParkingVehicles.put(zone, ini);
				} else {
					zonalInitParkingVehicles.addValue(1.0);
					hourlyStats[h].zonalInitParkingVehicles.put(zone, zonalInitParkingVehicles);
				}
			}

			// add day
			DescriptiveStatistics zonalDurationStatsD = dailyStats.zonalParkingDuration.get(zone);
			if (zonalDurationStatsD == null) {
				DescriptiveStatistics du = new DescriptiveStatistics();
				du.addValue(1740.0);
				dailyStats.zonalParkingDuration.put(zone, du);
			} else {
				zonalDurationStatsD.addValue(1740.0);
				dailyStats.zonalParkingDuration.put(zone, zonalDurationStatsD);
			}

			DescriptiveStatistics zonalIniD = dailyStats.zonalInitParkingVehicles.get(zone);
			if (zonalIniD == null) {
				DescriptiveStatistics ini = new DescriptiveStatistics();
				ini.addValue(1.0);
				dailyStats.zonalInitParkingVehicles.put(zone, ini);
			} else {
				zonalIniD.addValue(1.0);
				dailyStats.zonalInitParkingVehicles.put(zone, zonalIniD);
			}
		} else {
			// more than 1 task, vehicle has been used 
			Map<Id<Zone>, Zone> zones = myZonalSystem.getZones();
			ZoneFinder zoneFinder = new ZoneFinderImpl(zones, 1.0);
			TaxiChain chain = new TaxiChain();
			@SuppressWarnings("unchecked")
			LongEnumAdder<TaxiTaskType>[] vehicleHourlySums = new LongEnumAdder[hours];
			Double passengersServed = 0.0;

			for (int i = 0; i < schedule.getTasks().size(); i++) {
				TaxiActivity taxiAct = chain.addActivity();
				Double distance = 0.0;
				Double duration = 0.0;
				Task t = schedule.getTasks().get(i);
				TaxiTask tt = (TaxiTask) t;

				int[] hourlyDurations = TaxiStatsCalculators.calcHourlyDurations((int) t.getBeginTime(),(int) t.getEndTime());
				int fromHour = TaxiStatsCalculators.getHour(t.getBeginTime());
				for (int j = 0; j < hourlyDurations.length; j++) {
					includeTaskIntoHourlySums(vehicleHourlySums, fromHour + j, tt, hourlyDurations[j]);
				}

				////////////////// DROPOFF TASK ////////////////////////
				if (tt.getTaxiTaskType() == TaxiTaskType.DROPOFF) {
					counterDropOff = counterDropOff + 1;
					TaxiRequest req = ((TaxiDropoffTask) t).getRequest();
					Coord coord = req.getToLink().getCoord();
					Id<Zone> zone = zoneFinder.findZone(coord).getId();
					int hour = TaxiStatsCalculators.getHour(req.getDropoffTask().getBeginTime());
					DescriptiveStatistics zonalRequestStats = hourlyStats[hour].zonalTaxiRequestServed.get(zone);
					if (zonalRequestStats == null) {
						DescriptiveStatistics iniOD = new DescriptiveStatistics();
						iniOD.addValue(1.0);
						hourlyStats[hour].zonalTaxiRequestServed.put(zone, iniOD);
					} else {
						zonalRequestStats.addValue(1.0);
						hourlyStats[hour].zonalTaxiRequestServed.put(zone, zonalRequestStats);
					}
					// add day
					DescriptiveStatistics zonalRequestStatsD = dailyStats.zonalTaxiRequestServed.get(zone);
					if (zonalRequestStatsD == null) {
						DescriptiveStatistics iniOD = new DescriptiveStatistics();
						iniOD.addValue(1.0);
						dailyStats.zonalTaxiRequestServed.put(zone, iniOD);
					} else {
						zonalRequestStatsD.addValue(1.0);
						dailyStats.zonalTaxiRequestServed.put(zone, zonalRequestStatsD);
					}
					taxiAct.setId(v.getId());
					taxiAct.setType("dropoff");
					taxiAct.setStartTime(tt.getBeginTime());
					taxiAct.setEndTime(tt.getEndTime());
					taxiAct.setDuration(tt.getEndTime() - tt.getBeginTime());
					taxiAct.setFromZone(null);
					taxiAct.setToZone(null);
					taxiAct.setInZone(zone);
					taxiAct.setDistance(distance);
				}

				//////////////// PICKUP TASK ///////////////////////////
				if (tt.getTaxiTaskType() == TaxiTaskType.PICKUP) {
					counterPickUp = counterPickUp + 1;
					passengersServed = passengersServed + 1;
					TaxiRequest req = ((TaxiPickupTask) t).getRequest();
					Coord coord = req.getFromLink().getCoord();
					Id<Zone> zone = zoneFinder.findZone(coord).getId();
					double waitTime = Math.max(t.getBeginTime() - req.getEarliestStartTime(), 0);
					int hour = TaxiStatsCalculators.getHour(req.getEarliestStartTime());
					hourlyStats[hour].passengerWaitTime.addValue(waitTime);
					dailyStats.passengerWaitTime.addValue(waitTime);
					// add the zonal waiting time by the hour
					DescriptiveStatistics zonalWTHourStats = hourlyStats[hour].zonalWaitingTime.get(zone);
					if (zonalWTHourStats == null) {
						DescriptiveStatistics iniPU = new DescriptiveStatistics();
						iniPU.addValue(waitTime);
						hourlyStats[hour].zonalWaitingTime.put(zone, iniPU);
					} else {
						zonalWTHourStats.addValue(waitTime);
						hourlyStats[hour].zonalWaitingTime.put(zone, zonalWTHourStats);
					}
					// add the zonal waiting time by the day
					DescriptiveStatistics zonalWTDailyStats = dailyStats.zonalWaitingTime.get(zone);
					if (zonalWTDailyStats == null) {
						DescriptiveStatistics iniPUd = new DescriptiveStatistics();
						iniPUd.addValue(waitTime);
						dailyStats.zonalWaitingTime.put(zone, iniPUd);
					} else {
						zonalWTDailyStats.addValue(waitTime);
						dailyStats.zonalWaitingTime.put(zone, zonalWTDailyStats);
					}

					taxiAct.setId(v.getId());
					taxiAct.setType("pickup");
					taxiAct.setStartTime(tt.getBeginTime());
					taxiAct.setEndTime(tt.getEndTime());
					taxiAct.setDuration(tt.getEndTime() - tt.getBeginTime());
					taxiAct.setFromZone(null);
					taxiAct.setToZone(null);
					taxiAct.setInZone(zone);
					taxiAct.setDistance(distance);
				}

				/////////////////////// STAY TASK /////////////////////////
				// add the parking duration and count the used parking spots
				if (tt.getTaxiTaskType() == TaxiTaskType.STAY) {
					String stayType = null;
					TaxiStayTask stayTask = (TaxiStayTask) tt;
					Coord coord = stayTask.getLink().getCoord();
					Id<Zone> zone = zoneFinder.findZone(coord).getId();
					int beginStayTask = (int) Math.ceil(stayTask.getBeginTime() / 60); // round up
					int endStayTask = (int) Math.ceil(stayTask.getEndTime() / 60); // round up
					duration = stayTask.getEndTime() - stayTask.getBeginTime(); // check in config the update time
					int hour = TaxiStatsCalculators.getHour(stayTask.getBeginTime());
					if (beginStayTask > 1740) {
						duration = 0.0;
					}
					if (endStayTask > 1740) {
						endStayTask = 1740;
						duration = stayTask.getEndTime() - stayTask.getBeginTime();
					}
					if (duration > 60.0) {
						TreeMap<Integer, Integer> currentZoneEntry = ZoneINnOUT.get(zone);
						for (int e = beginStayTask; e < endStayTask; e++) {
							Integer entry = currentZoneEntry.get(e);
							currentZoneEntry.put(e, entry + 1);
						}
					}
					
					// add hour
					hourlyStats[hour].parkingDuration.addValue(duration);
					DescriptiveStatistics zonalDurationStats = hourlyStats[hour].zonalParkingDuration.get(zone);
					if (zonalDurationStats == null) {
						DescriptiveStatistics du = new DescriptiveStatistics();
						du.addValue(duration);
						hourlyStats[hour].zonalParkingDuration.put(zone, du);
					} else {
						zonalDurationStats.addValue(duration);
						hourlyStats[hour].zonalParkingDuration.put(zone, zonalDurationStats);
					}
					for (int h = 0; h < 29; h++) {
						DescriptiveStatistics zonalInitParkingVehicles = hourlyStats[h].zonalInitParkingVehicles
								.get(zone);
						if (zonalInitParkingVehicles == null) {
							DescriptiveStatistics ini = new DescriptiveStatistics();
							ini.addValue(1.0);
							hourlyStats[h].zonalInitParkingVehicles.put(zone, ini);
						} else {
							zonalInitParkingVehicles.addValue(1.0);
							hourlyStats[h].zonalInitParkingVehicles.put(zone, zonalInitParkingVehicles);
						}
					}

					// add day
					dailyStats.parkingDuration.addValue(duration);
					DescriptiveStatistics zonalDurationStatsD = dailyStats.zonalParkingDuration.get(zone);
					if (zonalDurationStatsD == null) {
						DescriptiveStatistics du = new DescriptiveStatistics();
						du.addValue(duration);
						dailyStats.zonalParkingDuration.put(zone, du);
					} else {
						zonalDurationStatsD.addValue(duration);
						dailyStats.zonalParkingDuration.put(zone, zonalDurationStatsD);
						if (t.getTaskIdx() != 0) {
							TaxiTask prev = (TaxiTask) schedule.getTasks().get(i - 1);
							if (!prev.toString().contains("DROPOFF")) {
								stayType = "stay_parking";
								counterParking = counterParking + 1;
								rightfulParkingEvents.addValue(duration);
							} else {
								if (duration > 60) {
									stayType = "stay_wrongfulParking(no relocation happend)";
									counterLongWrongPark = counterLongWrongPark + 1;
									wrongfulParkingEvents.addValue(duration);
									if (duration > 1200) {
										System.out.println(duration);
									}
								} else {
									stayType = "stay_wrongfulParking(less than 60 seconds)";
									counterShortWrongPark = counterShortWrongPark + 1;
									wrongfulParkingEvents.addValue(duration);
								}
							}
						} else {
							stayType = "stay_initial";
							rightfulParkingEvents.addValue(duration);
							DescriptiveStatistics zonalIniD = dailyStats.zonalInitParkingVehicles.get(zone);
							if (zonalIniD == null) {
								DescriptiveStatistics ini = new DescriptiveStatistics();
								ini.addValue(1);
								dailyStats.zonalInitParkingVehicles.put(zone, ini);
							} else {
								zonalIniD.addValue(1);
								dailyStats.zonalInitParkingVehicles.put(zone, zonalIniD);
							}
						}	
					}
					taxiAct.setId(v.getId());
					taxiAct.setType(stayType);
					taxiAct.setStartTime(tt.getBeginTime());
					taxiAct.setEndTime(tt.getEndTime());
					taxiAct.setDuration(tt.getEndTime() - tt.getBeginTime());
					taxiAct.setFromZone(null);
					taxiAct.setToZone(null);
					taxiAct.setInZone(zone);
					taxiAct.setDistance(distance);
				}
				////////////////// EmptyDrive////////////////////////
				if (tt.getTaxiTaskType() == TaxiTaskType.EMPTY_DRIVE) {
					String relocatingType = null;
					duration = tt.getEndTime() - tt.getBeginTime();
					int hour = TaxiStatsCalculators.getHour(tt.getBeginTime());

					DriveTaskImpl emptyDrive = (DriveTaskImpl) tt;
					VrpPath path = emptyDrive.getPath();
					for (int l = 0; l < path.getLinkCount(); l++) {
						Link link = path.getLink(l);
						double linklength = link.getLength();
						distance = distance + linklength;
					}
					Coord coordFrom = path.getFromLink().getCoord();
					Id<Zone> zoneFrom = zoneFinder.findZone(coordFrom).getId();
					Coord coordTo = path.getToLink().getCoord();
					Id<Zone> zoneTo = zoneFinder.findZone(coordTo).getId();

					Boolean relocTask = false;
					if (i < schedule.getTasks().size() - 1) {
						TaxiTask tPlus1 = (TaxiTask) schedule.getTasks().get(i + 1);
						if (!tPlus1.toString().contains("PICKUP")) {
							relocTask = true;
						}
					}

					if (relocTask == true) {
						relocatingType = "empty_relocation";
						counterReloc = counterReloc + 1;
						empty_relocation_tt = empty_relocation_tt + duration;
						empty_relocation_td = empty_relocation_td + distance;

						// add the zonal waiting time by the hour
						DescriptiveStatistics zonalRelocFromHourStats = hourlyStats[hour].zonalRelocationFrom
								.get(zoneFrom);
						if (zonalRelocFromHourStats == null) {
							DescriptiveStatistics relocFrom = new DescriptiveStatistics();
							relocFrom.addValue(distance);
							hourlyStats[hour].zonalRelocationFrom.put(zoneFrom, relocFrom);
						} else {
							zonalRelocFromHourStats.addValue(distance);
							hourlyStats[hour].zonalRelocationFrom.put(zoneFrom, zonalRelocFromHourStats);
						}
						DescriptiveStatistics zonalRelocFromDailyStats = dailyStats.zonalRelocationFrom.get(zoneFrom);
						if (zonalRelocFromDailyStats == null) {
							DescriptiveStatistics relocFromD = new DescriptiveStatistics();
							relocFromD.addValue(distance);
							dailyStats.zonalRelocationFrom.put(zoneFrom, relocFromD);
						} else {
							zonalRelocFromDailyStats.addValue(distance);
							dailyStats.zonalRelocationFrom.put(zoneFrom, zonalRelocFromDailyStats);
						}

						DescriptiveStatistics zonalRelocToHourStats = hourlyStats[hour].zonalRelocationTo.get(zoneTo);
						if (zonalRelocToHourStats == null) {
							DescriptiveStatistics relocTo = new DescriptiveStatistics();
							relocTo.addValue(distance);
							hourlyStats[hour].zonalRelocationTo.put(zoneTo, relocTo);
						} else {
							zonalRelocToHourStats.addValue(distance);
							hourlyStats[hour].zonalRelocationTo.put(zoneTo, zonalRelocToHourStats);
						}
						DescriptiveStatistics zonalRelocToDailyStats = dailyStats.zonalRelocationTo.get(zoneTo);
						if (zonalRelocToDailyStats == null) {
							DescriptiveStatistics relocToD = new DescriptiveStatistics();
							relocToD.addValue(distance);
							dailyStats.zonalRelocationTo.put(zoneTo, relocToD);
						} else {
							zonalRelocToDailyStats.addValue(distance);
							dailyStats.zonalRelocationTo.put(zoneTo, zonalRelocToDailyStats);
						}

						if (zoneFrom.equals(zoneTo)) {
							DescriptiveStatistics zonalRelocInHourStats = hourlyStats[hour].zonalRelocationIn
									.get(zoneTo);
							if (zonalRelocInHourStats == null) {
								DescriptiveStatistics relocIn = new DescriptiveStatistics();
								relocIn.addValue(distance);
								hourlyStats[hour].zonalRelocationIn.put(zoneTo, relocIn);
							} else {
								zonalRelocInHourStats.addValue(distance);
								hourlyStats[hour].zonalRelocationIn.put(zoneTo, zonalRelocInHourStats);
							}
							DescriptiveStatistics zonalRelocInDailyStats = dailyStats.zonalRelocationIn.get(zoneTo);
							if (zonalRelocInDailyStats == null) {
								DescriptiveStatistics relocInD = new DescriptiveStatistics();
								relocInD.addValue(distance);
								dailyStats.zonalRelocationIn.put(zoneTo, relocInD);
							} else {
								zonalRelocInDailyStats.addValue(distance);
								dailyStats.zonalRelocationIn.put(zoneTo, zonalRelocInDailyStats);
							}
						}
					} else {
						relocatingType = "empty_serving";
						counterIdleApproach = counterIdleApproach + 1;
						empty_serving_tt = empty_serving_tt + duration;
						empty_serving_td = empty_serving_td + distance;
					}

					taxiAct.setId(v.getId());
					taxiAct.setType(relocatingType);
					taxiAct.setStartTime(tt.getBeginTime());
					taxiAct.setEndTime(tt.getEndTime());
					taxiAct.setDuration(tt.getEndTime() - tt.getBeginTime());
					taxiAct.setFromZone(zoneFrom);
					taxiAct.setToZone(zoneTo);
					taxiAct.setInZone(null);
					taxiAct.setDistance(distance);
				}

				////////////////// OccupiedDrive////////////////////////
				if (tt.getTaxiTaskType() == TaxiTaskType.OCCUPIED_DRIVE) {

					passengerServedPerVehicle.addValue(passengersServed);

					TaxiRequest req = ((TaxiOccupiedDriveTask) t).getRequest();
					Coord coordFrom = req.getFromLink().getCoord();
					Id<Zone> zoneFrom = zoneFinder.findZone(coordFrom).getId();
					Coord coordTo = req.getToLink().getCoord();
					Id<Zone> zoneTo = zoneFinder.findZone(coordTo).getId();

					DriveTaskImpl emptyDrive = (DriveTaskImpl) tt;
					VrpPath path = emptyDrive.getPath();
					for (int l = 0; l < path.getLinkCount(); l++) {
						Link link = path.getLink(l);
						double linklength = link.getLength();
						distance = distance + linklength;
					}

					duration = tt.getEndTime() - tt.getBeginTime();

					taxiAct.setId(v.getId());
					taxiAct.setType("occupied_drive");
					taxiAct.setStartTime(tt.getBeginTime());
					taxiAct.setEndTime(tt.getEndTime());
					taxiAct.setDuration(duration);
					taxiAct.setFromZone(zoneFrom);
					taxiAct.setToZone(zoneTo);
					taxiAct.setInZone(null);
					taxiAct.setDistance(distance);

					counterOccDrive = counterOccDrive + 1;
					occupied_drive_tt = occupied_drive_tt + duration;
					occupied_drive_td = occupied_drive_td + distance;

					if (zoneFrom.toString().contains("83") && zoneTo.toString().contains("83")) {
						out_to_out.add(distance - distance);
					}
					if (zoneFrom.toString().contains("83") && !zoneTo.toString().contains("83")) {
						out_to_ams.add(distance - distance);
					}
					if (!zoneFrom.toString().contains("83") && zoneTo.toString().contains("83")) {
						ams_to_out.add(distance - distance);
					}
					if (!zoneFrom.toString().contains("83") && !zoneTo.toString().contains("83")) {
						ams_to_ams.add(distance - distance);
					}

				}
			}
			chains.put(v.getId(), chain);
		}
	}

	private void analyseZones(Id<Zone> zone) {
		Map<Id<Zone>, Integer> ZoneParkingAvailabilty = Counter_ParkingVehicles.getZoneParkingTotal();
		TreeMap<Integer, Integer> INnOUT = ZoneINnOUT.get(zone);
		Integer parkingSpotsTotal = ZoneParkingAvailabilty.get(zone);
		if (parkingSpotsTotal != null) {
			if (parkingSpotsTotal > 0.0) {
				for (int t = 0; t < INnOUT.keySet().size(); t++) {
					Integer hour = (int) Math.floor(t / 60);
					Double parkingSpotsUsed = (double) INnOUT.get(t);

					// add hour
					if (hourlyStats[hour].zonalParkingVehicles.get(zone) == null) {
						DescriptiveStatistics newEntry = new DescriptiveStatistics();
						newEntry.addValue(parkingSpotsUsed);
						hourlyStats[hour].zonalParkingVehicles.put(zone, newEntry);
					} else {
						DescriptiveStatistics hourlySt = hourlyStats[hour].zonalParkingVehicles.get(zone);
						hourlySt.addValue(parkingSpotsUsed);
						hourlyStats[hour].zonalParkingVehicles.put(zone, hourlySt);
					}

					// add day
					DescriptiveStatistics zonalParkingVehiclesD = dailyStats.zonalParkingVehicles.get(zone);
					if (zonalParkingVehiclesD == null) {
						DescriptiveStatistics St = new DescriptiveStatistics();
						St.addValue(parkingSpotsUsed);
						dailyStats.zonalParkingVehicles.put(zone, St);
					} else {
						zonalParkingVehiclesD.addValue(parkingSpotsUsed);
						dailyStats.zonalParkingVehicles.put(zone, zonalParkingVehiclesD);
					}
				}
			}
		}
	}

	public List<MyTaxiParkingStats> getTaxiParkingStats() {
		return taxiParkingStats;
	}

	public MyTaxiParkingStats getDailyStats() {
		return dailyStats;
	}

	private void includeTaskIntoHourlySums(LongEnumAdder<TaxiTaskType>[] hourlySums, int hour, TaxiTask task,
			int duration) {
		if (duration > 0) {
			if (hourlySums[hour] == null) {
				hourlySums[hour] = new LongEnumAdder<>(TaxiTaskType.class);
			}
			hourlySums[hour].add(task.getTaxiTaskType(), duration);
		}
	}

	public static int calcHourCount(Iterable<? extends Vehicle> vehicles) {
		double maxEndTime = 0;
		for (Vehicle v : vehicles) {
			double endTime = v.getSchedule().getEndTime();
			if (endTime > maxEndTime) {
				maxEndTime = endTime;
			}
		}

		return (int) Math.ceil(maxEndTime / 3600);
	}

	public static <T> List<T> createStatsList(T[] hourlyStats) {
		List<T> allStats = new ArrayList<>(hourlyStats.length + 1);
		Collections.addAll(allStats, hourlyStats);
		return Collections.unmodifiableList(allStats);
	}

	public static int[] calcHourlyDurations(int from, int to) {
		int firstHour = (int) from / 3600;
		int lastHour = (int) to / 3600;

		if (firstHour == lastHour) {
			return new int[] { to - from };
		}

		int[] hourlyDurations = new int[lastHour - firstHour + 1];
		hourlyDurations[0] = 3600 - from % 3600;
		hourlyDurations[hourlyDurations.length - 1] = to % 3600;
		for (int i = 1; i < hourlyDurations.length - 1; i++) {
			hourlyDurations[i] = 3600;
		}

		return hourlyDurations;
	}

	public void writeSimulationResultsToTabSeparated(Map<Id<Vehicle>, TaxiChain> chains) throws IOException {
		BufferedWriter taxiWriter = IOUtils.getBufferedWriter(outputDirectory + "/" + "output_taxiEvents.txt.gz");
		taxiWriter.write(
				"taxi_id\tactivity_type\tstart_time\tend_time\tduration\tfrom_zone\tto_zone\tzone\tin_vehicledistance\n");

		// read a static field that increments with every inheriting object constructed
		// Counter counter = new Counter("Output lines written: ");

		for (Entry<Id<Vehicle>, TaxiChain> entry : chains.entrySet()) {
			TaxiChain chain = entry.getValue();
			for (TaxiActivity act : chain.getActs()) {
				try {
					taxiWriter.write(String.format("%s\t%s\t%f\t%f\t%f\t%s\t%s\t%s\t%f\n", act.getId(), act.getType(),
							(Double) act.getStartTime(), (Double) act.getEndTime(), (Double) act.getDuration(),
							act.getFromZone(), act.getToZone(), act.getInZone(), (Double) act.getDistance(),
							MatsimRandom.getRandom().nextDouble()));
				} catch (Exception e) {
					System.out.println("Problem (Calculator l.394): Couldn't print activity chain!");
				}
			}
		}
		taxiWriter.close();
	}

	public static double getTotalTaxiKM() {
		return (empty_relocation_td + empty_serving_td + occupied_drive_td);
	}

	public static double getPassengerTaxiKM() {
		return occupied_drive_td;
	}

	public static double getTotalIdleTaxiKM() {
		return empty_relocation_td + empty_serving_td;
	}

	public static double getRelocationTaxiKM() {
		return empty_relocation_td;
	}

	public static double getApproachingTaxiKM() {
		return empty_serving_td;
	}

	public static DescriptiveStatistics getPassengerServedPervehicle() {
		return passengerServedPerVehicle;
	}

	public static Double getCounterIniParking() {
		return counterIniParking;
	}

	public static Double getCounterShortWrongPark() {
		return counterShortWrongPark;
	}

	public static Object getCounterLongWrongPark() {
		return counterLongWrongPark;
	}

	public static Double getCounterParking() {
		return counterParking;
	}

	public static Double getCounterLastParking() {
		return counterLastParking;
	}

	public static Double getCounterApproach() {
		return counterIdleApproach;
	}

	public static Double getCounterReloc() {
		return counterReloc;
	}

	public static Double getCounterOccDrive() {
		return counterOccDrive;
	}

	public static Double getCounterDropOff() {
		return counterDropOff;
	}

	public static Double getCounterPickUp() {
		return counterPickUp;
	}

	public static DescriptiveStatistics getWrongfulParkingEvents() {
		return wrongfulParkingEvents;
	}

	public static DescriptiveStatistics getRightfulParkingEvents() {
		return rightfulParkingEvents;
	}

	public static DescriptiveStatistics getParkingEventsLast() {
		return rightfulParkingEventsLast;
	}

	public static Double getOccupied_drive_tt() {
		return occupied_drive_tt;
	}

	public static Double getEmpty_relocation_tt() {
		return empty_relocation_tt;
	}

	public static Object getEmpty_approaching_tt() {
		return empty_serving_tt;
	}

}
