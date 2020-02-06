package createInput;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.parsers.DocumentBuilder;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.zone.Zone;
import org.matsim.contrib.zone.util.ZoneFinder;
import org.matsim.contrib.zone.util.ZoneFinderImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.router.DijkstraFactory;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacility;
import org.w3c.dom.Document;

import runnableFiles.Counter_ParkingVehicles;
import runnableFiles.MyZoneReader;

import org.w3c.dom.Element;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.Random;

public class CreateParkingDepots {
	
	public static void main(String argv[]) {

		String inputFile = "Network.xml";
		String xmlZones = "zzones.xml";
		String shpZones = "zzones.shp";
		
		//List<String> zonesWithDepot= Arrays.asList("zone.16" , "zone.18" , "zone.19" ,  "zone.20" , "zone.21" , "zone.22" , "zone.23" , "zone.24" , "zone.26" ,  "zone.28" , "zone.29" , "zone.32" , "zone.34" , "zone.35" ,  "zone.37" ,  "zone.39" , "zone.40" , "zone.41" ,  "zone.42" , "zone.43" , "zone.44" , "zone.46" , "zone.47" ,  "zone.49" , "zone.50" , "zone.55" , "zone.56" , "zone.57" , "zone.58" , "zone.63" , "zone.65"); // inner city  	
		//List<String> zonesWithDepot= Arrays.asList ("zone.1" , "zone.2" , "zone.3" , "zone.4" , "zone.5" , "zone.6" , "zone.7" , "zone.8" , "zone.9" , "zone.10" ,  "zone.11" , "zone.12" , "zone.13" , "zone.21" , "zone.15" , "zone.17" , "zone.25" , "zone.27" , "zone.30" , "zone.31" , "zone.33" , "zone.36" , "zone.38" ,  "zone.45" ,  "zone.48" , "zone.51" , "zone.52" , "zone.53" ,  "zone.54" , "zone.59" , "zone.60" , "zone.61" , "zone.62" , "zone.64" , "zone.66" , "zone.67" , "zone.68" , "zone.69" , "zone.22" , "zone.71" , "zone.72" , "zone.73" , "zone.74" , "zone.75" , "zone.76" , "zone.77" , "zone.78" , "zone.79" , "zone.80" , "zone.81" , "zone.82" , "zone.83"); // outer city
		//List<String> zonesWithDepot= Arrays.asList("zone.1","zone.2","zone.3","zone.4","zone.5","zone.6","zone.7","zone.8","zone.9","zone.10","zone.11","zone.12","zone.13","zone.15","zone.16","zone.17","zone.18","zone.19","zone.20","zone.21","zone.22","zone.23","zone.24","zone.25","zone.26","zone.27","zone.28","zone.29","zone.30","zone.31","zone.32","zone.33","zone.34","zone.35", "zone.36","zone.37", "zone.38", "zone.39","zone.40","zone.41", "zone.42","zone.43","zone.44","zone.45","zone.46","zone.47","zone.48","zone.49","zone.50","zone.51","zone.52","zone.53","zone.54","zone.55","zone.56","zone.57", "zone.58","zone.59","zone.60","zone.61","zone.62","zone.63","zone.64","zone.65",  "zone.66","zone.67","zone.68","zone.69","zone.71","zone.72","zone.73","zone.74", "zone.75","zone.76","zone.77","zone.78","zone.79","zone.80","zone.81","zone.82"); // all
		List<String> zonesWithDepot= Arrays.asList("zone.2","zone.3","zone.5","zone.8","zone.10","zone.11","zone.12","zone.13","zone.15","zone.17","zone.21","zone.24","zone.26","zone.28","zone.31","zone.32","zone.34","zone.37","zone.38","zone.40", "zone.41","zone.44","zone.45","zone.46","zone.47","zone.48","zone.49", "zone.50","zone.51","zone.52","zone.54","zone.58","zone.61","zone.65","zone.68","zone.73","zone.76","zone.77","zone.79","zone.81"); // 40 random zones
		//List<String> zonesWithDepot= Arrays.asList("zone.2","zone.8","zone.11","zone.12","zone.17","zone.21","zone.26","zone.28","zone.32","zone.44","zone.45","zone.47","zone.48","zone.49","zone.50","zone.52","zone.68","zone.77","zone.79","zone.81"); // 20 random zones
		//List<String> zonesWithDepot= Arrays.asList("zone.2","zone.11","zone.28","zone.44","zone.45","zone.48","zone.50","zone.52","zone.77","zone.79"); // 10 random zones
		//List<String> zonesWithDepot= Arrays.asList("zone.50","zone.48","zone.11","zone.77","zone.45"); // 5 zones
		//List<String> zonesWithDepot= Arrays.asList("zone.50"); // 1 zone, central
		
		// load scenario
		File outputFile = new File("Depot_40.xml");
		final Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		final Network network = scenario.getNetwork();
		new MatsimNetworkReader(scenario.getNetwork()).readFile(inputFile);
		
		Map<Id<Zone>, Zone> zones = MyZoneReader.readZones(xmlZones,shpZones);
		ZoneFinder zoneFinder = new ZoneFinderImpl(zones,1.0);
		Map<Id<Zone>, Integer> ZoneParking = new HashMap<Id<Zone>, Integer>	(); 
		Set<Id<Zone>> keySetZones = zones.keySet();
		List<Id<Link>> linksWithDepots = new ArrayList<Id<Link>>();
			
		Map<Id<Link>, ? extends Link> linkList = network.getLinks();
		Set<Id<Link>> keySetLinks = linkList.keySet();
		Iterator<Id<Link>> iterL = keySetLinks.iterator();
		
		int linkCounter = 0;
		
			
		Map<Id<Zone>, List<Id<Link>>> ZoneLink = new HashMap<Id<Zone>, List<Id<Link>>>();
		for (final Id<Link> key : keySetLinks) {
			linkCounter = linkCounter +1;
			Link link = linkList.get(key);
			Coord coord = link.getCoord();
			Id<Zone> zone = zoneFinder.findZone(coord).getId();
						
			List<Id<Link>> linksInZone = ZoneLink.get(zone);
			if (linksInZone == null) {
				List<Id<Link>> linksInZoneIni = new ArrayList<Id<Link>>();
				linksInZoneIni.add(link.getId());
				linksInZone = linksInZoneIni;
			} else {
				linksInZone.add(link.getId());
			}
			
			ZoneLink.put(zone, linksInZone);
			
		}
		
	
		try {
			// call document builder
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = docFactory.newDocumentBuilder();

			// output File
			Document outputDoc = dBuilder.newDocument();
			Element facs = outputDoc.createElement("facilities");
			outputDoc.appendChild(facs);
			
			// zones for which parking depot is generated
			Iterator<Id<Zone>> iterz = keySetZones.iterator();
			while (iterz.hasNext()) {
				Id<Zone> zoneId = iterz.next();
				String zoneStringId = zoneId.toString();
				for (int i = 0; i<zonesWithDepot.size(); i ++){
					String zoneString = zonesWithDepot.get(i);
					if (zoneStringId.equals(zoneString)){
						Coord zoneCoords = zones.get(zoneId).getCoord();				
						Link depotLink = NetworkUtils.getNearestLink(network, zoneCoords);
						Link depotLink2 = NetworkUtils.getNearestLinkExactly(network, zoneCoords);
						
						List<Id<Link>> linksInZone = ZoneLink.get(zoneId);
						if (linksInZone != null) { // if there are no links in zone, no depot is created there
							if (!linksInZone.contains(depotLink.getId())) {
								if (linksInZone.contains(depotLink2.getId())) {
									depotLink = depotLink2;
								}else {
									Random rand = new Random(); 
									Id<Link> randLinkInZone = linksInZone.get(rand.nextInt(linksInZone.size())); 
									depotLink = linkList.get(randLinkInZone);
						}}
						
						int cap = 5000;
						String linkID = depotLink.getId().toString();
						String curbID = "depot" + linkID;
						String capS = String.valueOf(cap);
						Coord coord = depotLink.getCoord();
						Double x = coord.getX();
						Double y = coord.getY();
						createNewFacility(outputDoc, facs, capS, curbID, linkID, x, y);
						linksWithDepots.add(depotLink.getId());	
			}}}}

			while (iterL.hasNext()) {
				Id<Link> linkId = iterL.next();
				Link link = linkList.get(linkId);
				String linkID = link.getId().toString();
				String curbID = "link" + linkID;
				List<Link> curbLinkList = NetworkUtils.getLinks(network, linkID);
				Link curbLink = curbLinkList.get(0);
				Coord coord = curbLink.getCoord();
				Double x = coord.getX();
				Double y = coord.getY();
				String capS = String.valueOf("0");
				
				Id<Zone> zoneOfLink = zoneFinder.findZone(coord).getId();
				if(linksWithDepots.contains(linkId)){
					if (ZoneParking.containsKey(zoneOfLink)){
						int park = ZoneParking.get(zoneOfLink);
						ZoneParking.put(zoneOfLink, park + Integer.parseInt("5000"));
					}else{
						ZoneParking.put(zoneOfLink, Integer.parseInt("5000"));
					}
				}else{
				createNewFacility(outputDoc, facs, capS, curbID, linkID, x, y);
					if (ZoneParking.containsKey(zoneOfLink)){
						int park = ZoneParking.get(zoneOfLink);
						ZoneParking.put(zoneOfLink, park + Integer.parseInt(capS));
					}else{
						ZoneParking.put(zoneOfLink, Integer.parseInt(capS));
			}}}
			

			// write content into xml file
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM,"http://www.matsim.org/files/dtd/facilities_v1.dtd");
			DOMSource source = new DOMSource(outputDoc);
			StreamResult result = new StreamResult(outputFile);
			transformer.transform(source, result);

			System.out.println("");
			System.out.println("parking per zone: ");
			System.out.println(ZoneParking);
			

		} catch (ParserConfigurationException pce) {
			pce.printStackTrace();
		} catch (TransformerException tfe) {
			tfe.printStackTrace();
		}

	}

	public static int getCapacity(String length, Double parkingFactor, Double safetyDistanceAtIntersection,
			Double scaleFactor) {
		Double safetyDistance = 2 * safetyDistanceAtIntersection;
		Double capacity = (Double.parseDouble(length) - safetyDistance) * parkingFactor * scaleFactor;
		capacity = Math.floor(capacity);
		int cap = capacity.intValue();
		if (cap < 1) {
			cap = 0;
		}
		return cap;
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
