package createInput;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.data.VehicleImpl;
import org.matsim.contrib.dvrp.data.file.VehicleWriter;
import org.matsim.contrib.zone.Zone;
import org.matsim.contrib.zone.util.ZoneFinder;
import org.matsim.contrib.zone.util.ZoneFinderImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacility;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import filesFromRemote.MyZonalSystem;
import runnableFiles.MyZoneReader;
import org.matsim.core.network.NetworkUtils;

public class CreateReducedParkingOUTSIDE2 {
	public static void main(String[] args) throws SAXException, IOException {
		
		String taxisFile = "src/main/resources/Taxis_2500on3000nOUTSIDE.xml";
		String outputFile = "Parking2500nOUTSIDEnew.xml";
		final String CONFIG_FILE = "ConfigWithTaxi.xml";
		final String xmlZones = "zzones.xml";
		final String shpZones = "zzones.shp";
		Config config = ConfigUtils.loadConfig(CONFIG_FILE);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		List<Vehicle> vehicles = new ArrayList<>();
		Network network = scenario.getNetwork();
		List<Id<Link>> allLinks = new ArrayList<>();
		allLinks.addAll(scenario.getNetwork().getLinks().keySet());
		Map<Id<Link>, ? extends Link> links = network.getLinks();
		List<Id<Link>> checkLinks = new ArrayList<Id<Link>>();
		
		// read taxis	
		BufferedReader br = new BufferedReader(new FileReader(taxisFile)); 
		String st; 
		while ((st = br.readLine()) != null) {
			if (st.startsWith("	<vehicle")){
				int cutOff = st.indexOf(" start_link");
				String taxiId = st.substring(14, cutOff -1);
				Id<Vehicle> id = Id.create(taxiId, Vehicle.class);
				int cutOff2 = st.indexOf("link=");
				int cutOff3 = st.indexOf(" t_0");
				String startLink = st.substring(cutOff2 + 6, cutOff3-1);
				Link startingLink = null;
				for (int i = 0; i < allLinks.size(); i++) {
				    Id<Link> link = allLinks.get(i);
				    if (link.toString().equals(startLink)) {
				    	startingLink = links.get(link);
				    	checkLinks.add(startingLink.getId());
				    	double capacity = 5.0;
						double t0 = 0.0;
						double t1 = 104400.0;
						Vehicle v = new VehicleImpl(id, startingLink, capacity, t0, t1);
					    vehicles.add(v); 
				    }
				}	
			}
		 } 
		String PARKACTIVITYTYPE = "car interaction";
		TreeMap<Id<ActivityFacility>, ActivityFacility> taxiParkingFacilities = scenario.getActivityFacilities().getFacilitiesForActivityType(PARKACTIVITYTYPE);
		
		List<String> innerCityZones = Arrays.asList("zone.16","zone.17","zone.19","zone.20","zone.22",
				"zone.23","zone.24","zone.26","zone.28","zone.29","zone.32","zone.34","zone.35","zone.37","zone.39",
				"zone.40","zone.41","zone.42","zone.43","zone.44","zone.46","zone.47","zone.49","zone.50","zone.55",
				"zone.56","zone.57","zone.58","zone.65");
		List<String> outerCityZones = Arrays.asList("zone.1","zone.2","zone.3","zone.4","zone.5",
				"zone.6","zone.7","zone.8","zone.9","zone.10","zone.11","zone.12","zone.13","zone.15",
				"zone.18","zone.21","zone.25","zone.27","zone.30","zone.31","zone.33","zone.36","zone.38","zone.45",
				"zone.48","zone.51","zone.52","zone.53","zone.54","zone.59","zone.60","zone.61","zone.62","zone.63",
				"zone.64","zone.66","zone.67","zone.68","zone.69","zone.70","zone.71","zone.72","zone.73","zone.74","zone.75",
				"zone.76","zone.77","zone.78","zone.79","zone.80","zone.81","zone.82");	
		
				
		Map<Id<Zone>, Zone> zones = MyZoneReader.readZones(xmlZones, shpZones);
		ZoneFinder zoneFinder = new ZoneFinderImpl(zones, 1.0);
			
		try {
			// call document builder
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = docFactory.newDocumentBuilder();

			// output File
			Document outputDoc = dBuilder.newDocument();
			Element facs = outputDoc.createElement("facilities");
			outputDoc.appendChild(facs);
				
			// change where necessary
			int counter = 0;
			int ParkingOut = 0;
			int ParkingIn = 0;
			Iterator<ActivityFacility> it = taxiParkingFacilities.values().iterator();
			Map<Id<Zone>, Integer> ZoneParking = new HashMap<Id<Zone>, Integer>(); 
			while (it.hasNext()){
				ActivityFacility act = it.next();
				Id<Link> link = act.getLinkId();
				String linkID = link.toString();
				String curbID = "curbside" + linkID;
				Integer parking = (int) (act.getActivityOptions().get("car interaction").getCapacity());
				Coord coord = act.getCoord();
				Double x = coord.getX();
				Double y = coord.getY();
				Id<Zone> zone = zoneFinder.findZone(coord).getId();
				int occurances = Collections.frequency(checkLinks, link);
				if (occurances >0) {
					createNewFacility(outputDoc, facs, Integer.toString(occurances), curbID, linkID, x,  y);
					if (occurances >1) {
						System.out.println("debug");
					}
				}
			}
			
			Iterator<Id<Zone>> itZ = ZoneParking.keySet().iterator();
			while (itZ.hasNext()){
				Id<Zone> zMoveTo = itZ.next();
				Integer parking = ZoneParking.get(zMoveTo);
				Coord zoneCoord = zones.get(zMoveTo).getCoord();
	    		Link newLink = NetworkUtils.getNearestLink(network, zoneCoord);
	    		Coord newLinkCoord = newLink.getCoord();
	    		Double xNew = newLinkCoord.getX();
				Double yNew = newLinkCoord.getY();
	    		String linkID = newLink.getId().toString();
	    		String curbID = "curbside" + linkID;
	    		createNewFacility(outputDoc, facs, parking.toString(), curbID, linkID, xNew,  yNew);
	    		ParkingOut = ParkingOut + parking;
			}
			
			
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM,"http://www.matsim.org/files/dtd/facilities_v1.dtd");
			DOMSource source = new DOMSource(outputDoc);
			StreamResult result = new StreamResult(outputFile);
			transformer.transform(source, result);
			
		}catch (ParserConfigurationException pce) {
			pce.printStackTrace();
		} catch (TransformerException tfe) {
			tfe.printStackTrace();
		}
		
	
		System.out.println("Done. Don't forget to refresh the Resource folder now :)" );
	}
	
	
	
	
	public static void createNewFacility(Document outputDoc, Element facs, String capS, String curbID, String linkID,
			Double x, Double y) {
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
