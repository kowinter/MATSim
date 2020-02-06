//package org.matsim.contrib.taxi.util.stats;
package runnableFiles;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.data.Fleet;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.contrib.taxi.schedule.TaxiStayTask;
import org.matsim.contrib.taxi.util.stats.TaxiHistogramsWriter;
import org.matsim.contrib.taxi.util.stats.TaxiStats;
import org.matsim.contrib.taxi.util.stats.TaxiStatsCalculator;
import org.matsim.contrib.taxi.util.stats.TaxiStatsWriter;
import org.matsim.contrib.util.*;
import org.matsim.contrib.zone.Zone;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.*;
import org.matsim.core.controler.listener.*;
import org.matsim.core.utils.io.IOUtils;
import com.google.inject.Inject;


public class MyTaxiStatsDumper implements AfterMobsimListener, ShutdownListener {
	private static final String[] HEADER = { "iter", null, //
			"PassWaitTime_avg", "PassWaitTime_sd", "PassWaitTime_95%ile", "PassWaitTime_max", null, //
			"EmptyDriveRatio_fleetAvg", "EmptyDriveRatio_avg", "EmptyDriveRatio_sd", null, //
			"StayRatio_fleetAvg", "StayRatio_avg", "StayRatio_sd", null, //
			"OccupDriveRatio_fleetAvg", null, //
			"RelocationDriveRatio_fleetAvg", "RelocationDriveRatio_avg", "RelocationDriveRatio_sd" };
	
	private final Fleet fleet;
	private final TaxiConfigGroup taxiCfg;
	private final OutputDirectoryHierarchy controlerIO;
	private final CompactCSVWriter multiDayWriter;
	

	@Inject
	public MyTaxiStatsDumper(Fleet fleet, TaxiConfigGroup taxiCfg, OutputDirectoryHierarchy controlerIO) throws IOException {
		this.fleet = fleet;
		this.taxiCfg = taxiCfg;
		this.controlerIO = controlerIO;
		multiDayWriter = new CompactCSVWriter(IOUtils.getBufferedWriter(controlerIO.getOutputFilename("taxi_daily_stats.txt")));
		multiDayWriter.writeNext(HEADER);
	}

	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
			
		TaxiStatsCalculator calculator = new TaxiStatsCalculator(fleet.getVehicles().values());
		appendToMultiDayStats(calculator.getDailyStats(), event);
		writeDetailedStats(calculator.getTaxiStats(), event);
		
		Network network = Run.getNetwork();
		MyZonalSystem myZonalSystem = Run.getZones(taxiCfg, network);
		Calculator_TaxiParkingStats calculatorTaxi = new Calculator_TaxiParkingStats(fleet.getVehicles().values(),myZonalSystem);
		appendToMultiDayStatsTaxi(calculatorTaxi.getDailyStats(), event);
		writeDetailedStats2(calculatorTaxi.getTaxiParkingStats(), event, myZonalSystem, calculatorTaxi.getDailyStats(), calculator.getTaxiStats());
		
		// reset the parking counter
		Map<Id<Link>, Integer> iniParkingCounter = Counter_ParkingVehicles.getIniParkingCounter();
		Map<Id<Zone>, Integer> iniZoneCounter = Counter_ParkingVehicles.getIniZoneParkingCounter();
		Counter_ParkingVehicles.setLinkParking(iniParkingCounter);
		Counter_ParkingVehicles.setZoneParking(iniZoneCounter);
	}

	private void appendToMultiDayStats(TaxiStats s, AfterMobsimEvent event) {
		multiDayWriter.writeNext(new CSVLineBuilder().add(event.getIteration() + "") //
				.addEmpty() //
				.addf("%.1f", s.passengerWaitTime.getMean()).addf("%.1f", s.passengerWaitTime.getStandardDeviation()) //
				.addf("%.0f", s.passengerWaitTime.getPercentile(95)) //
				.addf("%.0f", s.passengerWaitTime.getMax()) //
				.addf("pop.variance", s.passengerWaitTime.getPopulationVariance())//
				.addEmpty() //
				.addEmpty() //
				.addEmpty() //
				.addf("%.4f", s.getFleetEmptyDriveRatio()) //
				.addf("%.4f", s.vehicleEmptyDriveRatio.getMean()) //
				.addf("%.4f", s.vehicleEmptyDriveRatio.getStandardDeviation()) //
				.addEmpty() //
				.addEmpty() //
				.addEmpty() //
				.addf("%.4f", s.getFleetStayRatio()) //
				.addf("%.4f", s.vehicleStayRatio.getMean()) //
				.addf("%.4f", s.vehicleStayRatio.getStandardDeviation()) //
				.addEmpty() //
				.addEmpty() //
				.addEmpty() //
				.addf("%.4f", s.getOccupiedDriveRatio()));
		multiDayWriter.flush();
	}
	
	private void writeDetailedStats(List<TaxiStats> taxiStats, AfterMobsimEvent event) {
		String prefix = controlerIO.getIterationFilename(event.getIteration(), "taxi_");
		new TaxiStatsWriter(taxiStats).write(prefix + "stats.txt");
		new TaxiHistogramsWriter(taxiStats).write(prefix + "histograms.txt");
	}
	
	private void appendToMultiDayStatsTaxi(MyTaxiParkingStats s, AfterMobsimEvent event) {
		multiDayWriter.writeNext(new CSVLineBuilder().add(event.getIteration() + "") //
				.addEmpty() //
				.addf("%.1f", s.passengerWaitTime.getMean()).addf("%.1f", s.passengerWaitTime.getStandardDeviation()) //
				.addf("%.0f", s.passengerWaitTime.getPercentile(95)) //
				.addf("%.0f", s.passengerWaitTime.getMax()) //
				.addEmpty() //
				.addf("%.4f", s.getFleetEmptyDriveRatio()) //
				.addf("%.4f", s.vehicleEmptyDriveRatio.getMean()) //
				.addf("%.4f", s.vehicleEmptyDriveRatio.getStandardDeviation()) //
				.addEmpty() //
				.addf("%.4f", s.getFleetStayRatio()) //
				.addf("%.4f", s.vehicleStayRatio.getMean()) //
				.addf("%.4f", s.vehicleStayRatio.getStandardDeviation()) //
				.addEmpty() //
				.addf("%.4f", s.getOccupiedDriveRatio())
				.addEmpty() //
				.addf("%.4f", s.getFleetRelocatingDriveRatio()) //
				.addf("%.4f", s.vehicleRelocatingDriveRatio.getMean()) //
				.addf("%.4f", s.vehicleRelocatingDriveRatio.getStandardDeviation()));
		multiDayWriter.flush();
	}

	private void writeDetailedStats2(List<MyTaxiParkingStats> taxiParkingStats, AfterMobsimEvent event,MyZonalSystem myZonalSystem, MyTaxiParkingStats myTaxiParkingStats, List<TaxiStats> taxiStats) {
		String prefix = controlerIO.getIterationFilename(event.getIteration(), "taxi_");
		new Writer_TaxiParkingZonalUseStats(taxiParkingStats, myZonalSystem).write(prefix + "detatiledZonalParkingUseStats.txt");
		new Writer_TaxiParkingZonalWaitingTimeStats(taxiParkingStats, myZonalSystem).write(prefix + "detatiledZonalWaitingTimeStats.txt");
		new Writer_TaxiParkingCompleteWaitingTimes(taxiParkingStats).write(prefix + "rawPassengerWaitingTimes.txt");
		new Writer_TaxiParkingZonalDurationStats(taxiParkingStats, myZonalSystem).write(prefix + "detatiledZonalParkingDurationStats.txt");
		new Writer_TaxiParkingZonalAll(taxiParkingStats, myZonalSystem, myTaxiParkingStats).write(prefix + "taxiZonalOutputToExcel.txt");
		new Writer_TaxiParkingStats(taxiParkingStats, myTaxiParkingStats, taxiStats ).write(prefix + "taxiOutputToExcel.txt");
		//new Writer_TaxiParkingRelocations().write(prefix + "rawTaxiRelocations.txt");
		//new Writer_TaxiParkingCompleteParkingTimes(taxiParkingStats).write(prefix + "taxiParkingTimes.txt");
		//new TaxiParkingCompleteCounterWriter(taxiParkingStats, myZonalSystem).write(prefix + "taxiParkingCounterPerZone(RAW).txt");
		//new TaxiParkingZonalCounterRawDataWriter(taxiParkingStats, myZonalSystem).write(prefix + "zonalParkingUseRawData.txt");
		
		// for debuggging only
		try (CompactCSVWriter writer = new CompactCSVWriter(IOUtils.getBufferedWriter(prefix + "DEBUGGINGstartLinks.txt"))) {
			writer.writeNext("vehicle", "StartLink", "FirstStayTaskLink");
			for (Entry<Id<Vehicle>, ? extends Vehicle> entry : fleet.getVehicles().entrySet()) {
				Vehicle veh= entry.getValue();						
				TaxiStayTask stayTask = (TaxiStayTask) veh.getSchedule().getTasks().get(0);
				CSVLineBuilder lineBuilder = new CSVLineBuilder()
						.add(veh.getId().toString())
						.add(veh.getStartLink().getId().toString())
						.add(stayTask.getLink().getId().toString() + "");
				writer.writeNext(lineBuilder);
			}
		}		
	}

	@Override
	public void notifyShutdown(ShutdownEvent event) {
		multiDayWriter.close();
	}
}
