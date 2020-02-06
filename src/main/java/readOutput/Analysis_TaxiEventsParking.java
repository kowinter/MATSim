package readOutput;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

public class Analysis_TaxiEventsParking {
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
		
		Map<Integer, TreeMap<Double, Integer>> ZoneINnOUT = new HashMap <Integer, TreeMap<Double, Integer>>();
		Map<Integer, TreeMap<Double, Integer>> ZoneINnOUTlast = new HashMap <Integer, TreeMap<Double, Integer>>();
		Map<Integer, TreeMap<Double, Integer>> ZoneINnOUTlastOnly = new HashMap <Integer, TreeMap<Double, Integer>>();
		Map<Integer, TreeMap<Double, Integer>> hZoneINnOUT = new HashMap <Integer, TreeMap<Double, Integer>>();
		Map<Integer, TreeMap<Double, Integer>> hZoneINnOUTlast = new HashMap <Integer, TreeMap<Double, Integer>>();
		Map<Integer, TreeMap<Double, Integer>> hZoneINnOUTlastOnly = new HashMap <Integer, TreeMap<Double, Integer>>();
		List<String> words = new ArrayList<String>();
		String[] wordsArray;
		String taxiId = null;
		String previousTask = null;
		String currentTask = null;
		DescriptiveStatistics passengerServedPerVehicle = new DescriptiveStatistics();
		DescriptiveStatistics wrongfulParkingEvents = new DescriptiveStatistics();
		DescriptiveStatistics rightfulParkingEvents = new DescriptiveStatistics();
		DescriptiveStatistics rightfulParkingEventsLast = new DescriptiveStatistics();
		Double passengersServed = 0.0;
		
        for (int z = 1; z <= 83; z ++ ) {
               TreeMap<Double, Integer> INnOUT = new TreeMap<Double, Integer>();
               TreeMap<Double, Integer> INnOUTlast = new TreeMap<Double, Integer>();
               TreeMap<Double, Integer> INnOUTlastOnly = new TreeMap<Double, Integer>();
               for (double i = 0.0; i < 2101.0; i++){
                     INnOUT.put(i, 0);
                     INnOUTlast.put(i, 0);
                     INnOUTlastOnly.put(i, 0);
               }
            ZoneINnOUT.put(z, INnOUT);
            ZoneINnOUTlast.put(z, INnOUTlast);
            ZoneINnOUTlastOnly.put(z, INnOUTlastOnly);
        }
        
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
					// we have new taxi. 
					passengerServedPerVehicle.addValue(passengersServed);
					passengersServed = 0.0;
				}
				
								
				if (line.contains("stay_initial")) {
					String zone = words.get(7);
					zone = zone.replaceAll("\\D+","");
					Integer zoneId = Integer.parseInt(zone);
					TreeMap<Double, Integer> zoneParking = ZoneINnOUT.get(zoneId);
					TreeMap<Double, Integer> zoneParkingLast = ZoneINnOUTlast.get(zoneId);
					
					// start time
					Double startTime = Double.parseDouble(words.get(2)) / 60 ;
					startTime = (double) Math.ceil(startTime);
					Integer parkedVehS = zoneParking.get(startTime);
					Integer parkedVehSL = zoneParkingLast.get(startTime);
					zoneParking.put(startTime, parkedVehS + 1);
					zoneParkingLast.put(startTime, parkedVehSL + 1);
					
					// end time
					Double endTime = Double.parseDouble(words.get(3)) /60;
					endTime = (double) Math.ceil(endTime);
					Integer parkedVehE = zoneParking.get(endTime);
					Integer parkedVehEL = zoneParkingLast.get(endTime);
					zoneParking.put(endTime, parkedVehE - 1);
					zoneParkingLast.put(endTime, parkedVehEL - 1);
					
					currentTask = "stay_initial";
					counterIniParking = counterIniParking + 1;
					Double duration = Double.parseDouble(words.get(4));
					rightfulParkingEvents.addValue(duration);
					rightfulParkingEventsLast.addValue(duration);
				}
				
				if (line.contains("stay_parking")) {
					String zone = words.get(7);
					zone = zone.replaceAll("\\D+","");
					Integer zoneId = Integer.parseInt(zone);
					TreeMap<Double, Integer> zoneParking = ZoneINnOUT.get(zoneId);
					TreeMap<Double, Integer> zoneParkingLast = ZoneINnOUTlast.get(zoneId);
					
					// start time
					Double startTime = Double.parseDouble(words.get(2)) / 60 ;
					startTime = (double) Math.ceil(startTime);
					Integer parkedVehS = zoneParking.get(startTime);
					Integer parkedVehSL = zoneParkingLast.get(startTime);
					zoneParking.put(startTime, parkedVehS + 1);
					zoneParkingLast.put(startTime, parkedVehSL + 1);
					// end time
					Double endTime = Double.parseDouble(words.get(3)) /60;
					endTime = (double) Math.ceil(endTime);
					Integer parkedVehE = zoneParking.get(endTime);
					Integer parkedVehEL = zoneParkingLast.get(endTime);
					zoneParking.put(endTime, parkedVehE - 1);
					zoneParkingLast.put(endTime, parkedVehEL - 1);
					
					currentTask = "stay_parking";
					counterIniParking = counterIniParking + 1;
					Double duration = Double.parseDouble(words.get(4));
					rightfulParkingEvents.addValue(duration);
					rightfulParkingEventsLast.addValue(duration);
				}
				
				if (line.contains("stay_lastParking")) {
					currentTask = "stay_lastParking";
					String zone = words.get(7);
					zone = zone.replaceAll("\\D+","");
					Integer zoneId = Integer.parseInt(zone);
					TreeMap<Double, Integer> zoneParking = ZoneINnOUT.get(zoneId);
					TreeMap<Double, Integer> zoneParkingLast = ZoneINnOUTlast.get(zoneId);
					TreeMap<Double, Integer> zoneParkingLastOnly = ZoneINnOUTlastOnly.get(zoneId);
					
					// start time
					Double startTime = Double.parseDouble(words.get(2)) / 60 ;
					startTime = (double) Math.ceil(startTime);
					Integer parkedVehS = zoneParking.get(startTime);
					Integer parkedVehSL = zoneParkingLast.get(startTime);
					Integer parkedVehSO = zoneParkingLastOnly.get(startTime);
					if (previousTask.contains("empty_relocation")){
						zoneParking.put(startTime, parkedVehS + 1);
					}else{
						parkedVehS.toString();
					}
					zoneParkingLast.put(startTime, parkedVehSL + 1);
					zoneParkingLastOnly.put(startTime, parkedVehSO + 1);
					// end time
					Double endTime = Double.parseDouble(words.get(3)) /60;	
					endTime = (double) Math.ceil(endTime);
					Integer parkedVehE = zoneParking.get(endTime);
					Integer parkedVehEL = zoneParkingLast.get(endTime);
					Integer parkedVehEO = zoneParkingLast.get(endTime);
					if (previousTask.contains("empty_relocation")){
						zoneParking.put(endTime, parkedVehE + 1);
					}
					zoneParkingLast.put(endTime, parkedVehEL - 1);
					zoneParkingLastOnly.put(endTime, parkedVehEO - 1);
								
					counterLastParking = counterLastParking + 1;
					Double duration = Double.parseDouble(words.get(4));
					rightfulParkingEventsLast.addValue(duration);
				}
				
				if (line.contains("stay_wrongfulParking(no relocation happend)")) {
					currentTask = "stay_wrongfulParking(no relocation happend)";
					counterLongWrongPark = counterLongWrongPark + 1;
					Double duration = Double.parseDouble(words.get(4));
					wrongfulParkingEvents.addValue(duration);
				}
				
				if (line.contains("stay_wrongfulParking(less than 60 seconds)")) {
					currentTask = "stay_wrongfulParking(less than 60 seconds)";
					counterShortWrongPark = counterShortWrongPark + 1;
					Double duration = Double.parseDouble(words.get(4));
					wrongfulParkingEvents.addValue(duration);
				}
				
				if (line.contains("dropoff")) {
					currentTask = "dropoff";
					counterDropOff = counterDropOff + 1;
					passengersServed = passengersServed + 1;
				}
				
				if (line.contains("occupied_drive")) {
					currentTask = "occupied_drive";
				}
				
				if (line.contains("empty_relocation")) {
					currentTask = "empty_relocation";
				}
				
				if (line.contains("empty_serving")) {
					currentTask = "empty_serving";
				}
				
				words.clear();
				previousTask = line;
				taxiId = thisTaxiId;
			}
		}
		
		Map<Integer, TreeMap<Double, DescriptiveStatistics>> zoneParking = new HashMap <Integer, TreeMap<Double, DescriptiveStatistics>>();
		Map<Integer, TreeMap<Double, DescriptiveStatistics>> zoneParkinglast = new HashMap <Integer, TreeMap<Double, DescriptiveStatistics>>();
		Map<Integer, TreeMap<Double, DescriptiveStatistics>> zoneParkinglastOnly = new HashMap <Integer, TreeMap<Double, DescriptiveStatistics>>();
		
		for (int z = 1; z < 84; z++){
			TreeMap<Double, Integer> parkedVeh = ZoneINnOUT.get(z);
			TreeMap<Double, Integer> parkedVehL = ZoneINnOUTlast.get(z);
			TreeMap<Double, Integer> parkedVehLO = ZoneINnOUTlastOnly.get(z);
			
			TreeMap<Double, DescriptiveStatistics> parkedVehPerHour= new TreeMap<Double, DescriptiveStatistics>();
			TreeMap<Double, DescriptiveStatistics> parkedVehPerHourL= new TreeMap<Double, DescriptiveStatistics>();
			TreeMap<Double, DescriptiveStatistics> parkedVehPerHourLO= new TreeMap<Double, DescriptiveStatistics>();
			
			DescriptiveStatistics hour = new DescriptiveStatistics();
			DescriptiveStatistics hourL = new DescriptiveStatistics();
			DescriptiveStatistics hourLO = new DescriptiveStatistics();
			
			double h_prev = 1.0;
			Integer pV = parkedVeh.get(0.0);
			Integer pVL = parkedVehL.get(0.0);
			Integer pVLO = parkedVehL.get(0.0);
			
			for (double i = 1.0; i < 2101.0; i++){	
				Double h = (double) Math.ceil(i/60);
				if (h != h_prev){	
					parkedVehPerHour.put(h_prev, hour);
					parkedVehPerHourL.put(h_prev, hourL);
					parkedVehPerHourLO.put(h_prev, hourLO);
					
					hour = new DescriptiveStatistics();
					hourL = new DescriptiveStatistics();
					hourLO = new DescriptiveStatistics();
				}
				pV = parkedVeh.get(i) + pV;
				hour.addValue(pV);
				
				pVL = parkedVehL.get(i) + pVL;
				hourL.addValue(pVL);
				
				pVLO = parkedVehLO.get(i) + pVLO;
				hourLO.addValue(pVLO);
				
				h_prev = h;
			}
			
			zoneParking.put(z, parkedVehPerHour);
			zoneParkinglast.put(z, parkedVehPerHour); 
			zoneParkinglastOnly.put(z, parkedVehPerHour);
		}
		
		
		double wrongFullDurationN = wrongfulParkingEvents.getN();
		double wrongFullDurationMean = wrongfulParkingEvents.getMean();
		double wrongFullDurationDev = wrongfulParkingEvents.getStandardDeviation();
		double wrongFullDurationMax = wrongfulParkingEvents.getMax();
		
		double rightfulParkingEventsN = rightfulParkingEvents.getN();
		double rightfulParkingEventsMean = rightfulParkingEvents.getMean();
		double rightfulParkingEventsDev = rightfulParkingEvents.getStandardDeviation();
		double rightfulParkingEventsMax = rightfulParkingEvents.getMax();
		
		double rightfulParkingEventsLastN = rightfulParkingEventsLast.getN();
		double rightfulParkingEventsLastMean = rightfulParkingEventsLast.getMean();
		double rightfulParkingEventsLastDev = rightfulParkingEventsLast.getStandardDeviation();
		double rightfulParkingEventsLastMax = rightfulParkingEventsLast.getMax();
		
		
		System.out.println("counterIniParking " + "\t" + "counterShortWrongPark: " + "\t" + "counterLongWrongPark " + "\t" + "counterParking " + "\t" + "counterLastParking " + "\t" + "counterPickUp" + "\t" + " counterDropOff " + "\t" + "counterOccDrive" + "\t" + " counterReloc" + "\t" + " counterIdleApproach");
		System.out.println(counterIniParking + "\t" +  counterShortWrongPark+ "\t" +  counterLongWrongPark + "\t" +  counterParking+ "\t" +  counterLastParking+ "\t" + counterPickUp  + "\t" +  counterDropOff + "\t" +   counterOccDrive  + "\t" +  counterReloc  + "\t" +  counterIdleApproach);
		System.out.println(" ");
		
		System.out.println("wrongFullDurationN" + "\t" + "wrongFullDurationMean" + "\t" + " wrongFullDurationDev" + "\t" + " wrongFullDurationDev" + "\t" + " wrongFullDurationMax");
		System.out.println(wrongFullDurationN+ "\t" +  wrongFullDurationMean+ "\t" +wrongFullDurationDev + "\t" + wrongFullDurationMax);
		System.out.println(" ");
		
		System.out.println("rightfulParkingEventsN "+ "\t" +"rightfulParkingEventsMean "+ "\t" + "rightfulParkingEventsDev "+ "\t"+ "rightfulParkingEventsMax "+ "\t" +"rightfulParkingEventsLastN "+ "\t" + "rightfulParkingEventsLastMean "+ "\t" + "rightfulParkingEventsLastDev "+ "\t" + "rightfulParkingEventsLastMax ");
		System.out.println(rightfulParkingEventsN + "\t" + rightfulParkingEventsMean+ "\t" + rightfulParkingEventsDev + "\t" +rightfulParkingEventsMax+ "\t" + rightfulParkingEventsLastN + "\t" + rightfulParkingEventsLastMean + "\t" +rightfulParkingEventsLastDev + "\t" +rightfulParkingEventsLastMax);
		
		
		System.out.println("");
		
		File outputfile = new  File("./output/parkingAV.txt");
		File outputfile2 = new  File("./output/parkingAV_minute.txt");
		if (!outputfile.exists()) {
			outputfile.createNewFile();
			outputfile2.createNewFile();
        }
		
		FileWriter fw = new FileWriter(outputfile.getAbsoluteFile());
        BufferedWriter bw = new BufferedWriter(fw);
        bw.write("zoneParking");
        bw.write("\n");
        for (int z = 1; z < 84; z++){
        	bw.write("\n");
        	 bw.write("zone " + z);
        	 bw.write("\n");
        	 bw.write("\n");
        	 bw.write(String.format("hour" + "\t" + "average_number_of_parked_vehicles" + "\t" + "min" + "\t" + "max" + "\t" + "st_div" + "\t" + "minute overview") );
        	// bw.write("hour" + " " + "average number of parked vehicles" + " " +  "min" + " " +  "max" + " " +  "st.div." + " " +  "minute overview " );
        	 bw.write("\n");
        	 TreeMap<Double, DescriptiveStatistics> parkingToPrint = zoneParking.get(z);
        	 for (double i = 1.0 ; i < 30.0; i++){
				DescriptiveStatistics entry = parkingToPrint.get(i);
				bw.write(String.format(Double.toString(i)));
				bw.write("\t");
				bw.write(Double.toString(entry.getMean()));
				bw.write("\t");
				bw.write(Double.toString(entry.getMin()));
				bw.write("\t");
				bw.write(Double.toString(entry.getMax()));
				bw.write("\t");
				bw.write(Double.toString(entry.getStandardDeviation()));
				bw.write("\t");
				bw.write(Arrays.toString(entry.getValues()));
				//bw.write(String.format(Double.toString(i),entry.getMean(),entry.getMin(),entry.getMax(),entry.getStandardDeviation(),Arrays.toString(entry.getValues())) );
				//bw.write(i \ entry.getMean() \ entry.getMin()\ entry.getMax()\ entry.getStandardDeviation() \ Arrays.toString(entry.getValues()));
				// bw.write(i + " " + entry.getMean() + " " + entry.getMin()+ " " + entry.getMax()+ " " + entry.getStandardDeviation()+ " " + Arrays.toString(entry.getValues()));
				bw.write("\n");			
			}
		}
		 bw.close();
		 
		 
		 FileWriter fw2 = new FileWriter(outputfile2.getAbsoluteFile());
	     BufferedWriter bw2 = new BufferedWriter(fw2);
	   //  bw2.write(String.format("zone" + "\t" + "parked vehicles per minute" ));
	     bw2.write("\n");	
	     for (int z=1; z < 84; z++){
	    	 bw2.write("\t");
	    	 	TreeMap<Double, DescriptiveStatistics> parkingToPrint = zoneParking.get(z);
	        	for (double i = 1.0 ; i < 28; i++){
	        	DescriptiveStatistics entry = parkingToPrint.get(i);
	        		double[] thisis = entry.getValues();
	        		for (int j = 0 ; j < thisis.length; j++){
						double isis = thisis[j];
						bw2.write(String.format(Double.toString(isis)));
						bw2.write("\t");
	        		}
	        	}
	        	bw2.write("\n");	
			}
		bw2.close();
	}
}