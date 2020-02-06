package readOutput;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.dvrp.data.Vehicle;

import runnableFiles.TaxiChain;

public class Analysis_TaxiEventsMileageNew {
	public static void main(String[] args) throws Exception {
		String eventsFileName = "./output/output_taxiEvents.txt";
		File file = new File(eventsFileName);
		Scanner sc = new Scanner(file);
		BufferedReader abc = new BufferedReader(new FileReader(file));
		List<Double> list = new ArrayList<Double>();
		String line;
		Integer counter = 0;
		Double counterIniParking = 0.0;
		Double counterShortWrongPark = 0.0;
		Double counterLongWrongPark = 0.0;
		Double counterParking = 0.0;
		Double counterLastParking = 0.0;
		Double counterPickUp = 0.0;
		Double counterDropOff = 0.0;
		Double counterOccDrive = 0.0;
		Double counterReloc = 0.0;
		Double counterIdleApproach = 0.0;
		Double occupied_drive_tt = 0.0;
		Double occupied_drive_td = 0.0;
		Double empty_relocation_tt = 0.0;
		Double empty_relocation_td = 0.0;
		Double empty_serving_tt = 0.0;
		Double empty_serving_td = 0.0;
		Integer vehiclesNotHavingServedAPassenger = 0;
		Integer sameZoneTrip = 0;
		
		List<Double> ams_to_ams = new ArrayList <Double>();
		List<Double> ams_to_out = new ArrayList <Double>();
		List<Double> out_to_ams = new ArrayList <Double>();
		List<Double> out_to_out = new ArrayList <Double>();

		Map<Integer, List<String>> initialParkingVehiclesPerZone = new HashMap<Integer, List<String>>();
		List<String> words = new ArrayList<String>();
		String[] wordsArray;
		String taxiId = null;
		Double localDistance = 0.0;
		Integer counterIfTaxiServedPassenger = 1;

		while ((line = abc.readLine()) != null) {
			counter = counter + 1;
			if (counter > 1) {
				wordsArray = line.split("\t");
				for (String each : wordsArray) {
					if (!"".equals(each)) {
						words.add(each);
					}
				}
				
				
				String thisTaxiId = words.get(0);
				if (!thisTaxiId.equals(taxiId)){
					// we have new taxi. distance has to be altered, as mistake in TaxiEventsFile 
					localDistance = 0.0;
					vehiclesNotHavingServedAPassenger = vehiclesNotHavingServedAPassenger + counterIfTaxiServedPassenger;
					counterIfTaxiServedPassenger = 1;
				}
				
				if (line.contains("pickup")) {
					String zone = words.get(7);
					counterPickUp = counterPickUp + 1;
					counterIfTaxiServedPassenger = 0;

				}
				
				if (line.contains("occupied_drive")){
					String fromzone = words.get(5);
					String tozone = words.get(6);
					
					if (fromzone.equals(tozone)) {
						sameZoneTrip = sameZoneTrip + 1;
					}
					
					counterOccDrive = counterOccDrive + 1;
					occupied_drive_tt = occupied_drive_tt + Double.parseDouble(words.get(4)) ;
					Double distance = Double.parseDouble(words.get(8));
					occupied_drive_td = occupied_drive_td + distance ;
					
					
					if (fromzone.contains("83") && tozone.contains("83")){
						out_to_out.add(distance );
					}
					if (fromzone.contains("83") && !tozone.contains("83")){
						out_to_ams.add(distance );
					}
					if (!fromzone.contains("83") && tozone.contains("83")){
						ams_to_out.add(distance );
					}
					if(!fromzone.contains("83") && !tozone.contains("83")){
						ams_to_ams.add(distance );
					}
				}
				
				if (line.contains("empty_relocation")) {
					String zone = words.get(7);
					counterReloc = counterReloc + 1;
					empty_relocation_tt = empty_relocation_tt+ Double.parseDouble(words.get(4)) ;
					Double distance = Double.parseDouble(words.get(8));
					//double thisis = distance - localDistance;
					empty_relocation_td = empty_relocation_td + distance;
					//empty_relocation_td = empty_relocation_td +  distance - localDistance ;
					localDistance = distance;
				}
				
				if (line.contains("empty_serving")) {
					String zone = words.get(7);
					counterIdleApproach = counterIdleApproach + 1;
					empty_serving_tt = empty_serving_tt + Double.parseDouble(words.get(4)) ;
					Double distance = Double.parseDouble(words.get(8));
					//empty_serving_td = empty_serving_td + distance - localDistance ;
					empty_serving_td = empty_serving_td + distance ;
					localDistance = distance;
				}
				
				words.clear();
				taxiId = thisTaxiId;
			}
		}
		
		
		occupied_drive_tt = occupied_drive_tt /60 ;
		 occupied_drive_td = occupied_drive_td /1000;
		 empty_relocation_tt = empty_relocation_tt/60;
		 empty_relocation_td = empty_relocation_td/1000;
		 empty_serving_tt = empty_serving_tt/60;
		 empty_serving_td = empty_serving_td/1000;
		 DoubleSummaryStatistics statsAmsAms = ams_to_ams.stream().mapToDouble((x) -> x).summaryStatistics();
		 DoubleSummaryStatistics statsAmsOut = ams_to_out.stream().mapToDouble((x) -> x).summaryStatistics();
		 DoubleSummaryStatistics statsOutAms = out_to_ams.stream().mapToDouble((x) -> x).summaryStatistics();
		 DoubleSummaryStatistics statsOutOut = out_to_out.stream().mapToDouble((x) -> x).summaryStatistics();

		System.out.println("counterIniParking: " + counterIniParking);
		System.out.println("counterShortWrongPark: " + counterShortWrongPark);
		System.out.println("counterLongWrongPark: " + counterLongWrongPark);
		System.out.println("counterParking: " + counterParking);
		System.out.println("counterLastParking: " + counterLastParking);
		System.out.println("counterPickUp: " + counterPickUp);
		System.out.println("counterDropOff: " + counterDropOff);
		System.out.println("counterOccDrive: " + counterOccDrive);
		System.out.println("counterReloc: " + counterReloc);
		System.out.println("counterIdleApproach: " + counterIdleApproach);
		System.out.println("occupied_drive_tt (min): " + occupied_drive_tt );
		System.out.println("occupied_drive_td (km): " + occupied_drive_td);
		System.out.println("empty_relocation_tt (min): " + empty_relocation_tt);
		System.out.println("empty_relocation_td(km): " + empty_relocation_td);
		System.out.println("empty_serving_tt(min): " + empty_serving_tt);
		System.out.println("empty_serving_td(km): " + empty_serving_td);

		System.out.println("PickUp - DropOff: " + (counterPickUp - counterDropOff));

		System.out.println("OccDrive: tt/trips: " + (occupied_drive_tt / counterOccDrive));
		System.out.println("OccDrive: td/trips: " + (occupied_drive_td / counterOccDrive));

		System.out.println("Reloc: tt/reloctrips: " + (empty_relocation_tt / counterReloc));
		System.out.println("Reloc: td/reloctrips: " + (empty_relocation_td / counterReloc));

		System.out.println("EmptyServing: tt/estrips: " + (empty_serving_tt / counterIdleApproach));
		System.out.println("EmptyServing: td/estrips: " + (empty_serving_td / counterIdleApproach));

		Double tot_km = empty_relocation_td + empty_serving_td + occupied_drive_td;
		Double tot_km_idle = empty_relocation_td + empty_serving_td ;
		
		System.out.println("totalIdle: tt/idletrips: "
				+ ((empty_relocation_tt + empty_serving_tt) / (counterReloc + counterIdleApproach)));
		System.out.println("totalIdle: td/idletrips: " + (tot_km_idle / (counterReloc + counterIdleApproach)));

		System.out.println("total: tt/trips: " + ((empty_relocation_tt + empty_serving_tt + occupied_drive_tt) / counterOccDrive));
		System.out.println("total : td/trips: "	+ ((tot_km) / counterOccDrive));
		
		System.out.println("");
		System.out.println("trips ams_ams");
		System.out.println(statsAmsAms);
		System.out.println("trips out_ams");
		System.out.println(statsOutAms);
		System.out.println("trips ams_out");
		System.out.println(statsAmsOut);
		System.out.println("trips out_out");
		System.out.println(statsOutOut);
		System.out.println ("sameZoenTrip" + sameZoneTrip);
		
		//System.out.println("");
		//System.out.println("for excel sheet");
		
		//System.out.println("total drive (km); occupied drive (km);  total empty drive (km); relocation drive (km); idle_appraoching drive (km); ams-ams(#); ams-ams(av.dist.km); out-ams(#); out-ams(av.dist.km); ams-out(#); ams-out(av.dist.km);out-out(#); out-out(av.dist.km) ");
		//System.out.println(tot_km);
		//System.out.println(occupied_drive_td);
		//System.out.println( tot_km_idle );
		//System.out.println(empty_relocation_td );
		//System.out.println( empty_serving_td);
		//System.out.println(statsAmsAms.getCount());
		//System.out.println(statsAmsAms.getAverage()/1000);
		//System.out.println(statsOutAms.getCount());
		//System.out.println(statsOutAms.getAverage()/1000);
		//System.out.println(statsAmsOut.getCount());
		//System.out.println(statsAmsOut.getAverage()/1000);
		//System.out.println(statsOutOut.getCount());
		//System.out.println(statsOutOut.getAverage()/1000);
		//System.out.println("");
		System.out.println ("number vehicles never used: " + (vehiclesNotHavingServedAPassenger -1));
		}
}