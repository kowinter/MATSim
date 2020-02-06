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
import org.matsim.contrib.zone.Zone;
import org.matsim.contrib.zone.util.ZoneFinder;
import org.matsim.contrib.zone.util.ZoneFinderImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.w3c.dom.Document;
import runnableFiles.MyZoneReader;

import org.w3c.dom.Element;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

public class CreateParkingCurbsideAmsterdam {

	public static void main(String argv[]) {

		String inputFile = "Network.xml";
		String xmlZones = "zzones.xml";
		String shpZones = "zzones.shp";

		// load scenario
		File outputFile = new File("parking5000.xml");
		Double parkingFactor = 1.0 / 6.0; // per vehicle 6m for parking
		Double safetyDistanceAtIntersection = 6.0; // in meter
		double maxSpeedForParking = 13.889; // this is the max allowed speed in m/s, here set to 30 km/h(=8.34)  (50 kmh=13.88)
		int desiredParking = 5000;
		double scalingFactor = 1;

		final Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		final Network network = scenario.getNetwork();
		new MatsimNetworkReader(scenario.getNetwork()).readFile(inputFile);

		Map<Id<Zone>, Zone> zones = MyZoneReader.readZones(xmlZones, shpZones);
		ZoneFinder zoneFinder = new ZoneFinderImpl(zones, 1.0);
		Map<Id<Zone>, Integer> ZoneParking = new HashMap<Id<Zone>, Integer>();

		Map<Id<Link>, ? extends Link> linkList = network.getLinks();
		Set<Id<Link>> keySet = linkList.keySet();
		
		ArrayList<Id<Link>> list = keySet.stream().collect(Collectors.toCollection(ArrayList::new));
		Collections.shuffle(list);
		
		int linkCounter = 0;
		int parkingCounter = 0;
		
		try {
			// call document builder
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = docFactory.newDocumentBuilder();

			// output File
			Document outputDoc = dBuilder.newDocument();
			Element facs = outputDoc.createElement("facilities");
			outputDoc.appendChild(facs);

			// input File
			Iterator<Id<Link>> iter = list.iterator();
			int counter = 0;
			while (iter.hasNext()) {
				counter = counter + 1;
				Id<Link> linkId = iter.next();
				Link link = linkList.get(linkId);

				// extract info to use for transformation
				String linkID = link.getId().toString();
				String curbID = "curbside" + linkID;
				String length = String.valueOf(link.getLength());
				double freespeed = link.getFreespeed();

				// get coordinates of link
				List<Link> curbLinkList = NetworkUtils.getLinks(network, linkID);
				Link curbLink = curbLinkList.get(0);
				Coord coord = curbLink.getCoord();
				Double x = coord.getX();
				Double y = coord.getY();

				// get parking capacity of link in vehicles
				String capS = String.valueOf(0);

				// get zone of link
				Id<Zone> zoneOfLink = zoneFinder.findZone(coord).getId();

				if(desiredParking <= parkingCounter){
					capS = String.valueOf(0);	
				}else{
					if (!zoneOfLink.toString().contains("83")) {
						// for residential streets
						if (freespeed <= maxSpeedForParking) {
							int cap = getCapacity(length, parkingFactor, safetyDistanceAtIntersection, scalingFactor);
							if (cap > 1) {
								capS = String.valueOf(1);
								linkCounter = linkCounter + 1;
								parkingCounter = parkingCounter + 1;
							} else {
								capS = String.valueOf(0);
							}
						} else {
						// if this is not explicitly stated, than links with no facility count as a endless parking facility
						capS = String.valueOf(0);
						}	
					} else {
						capS = String.valueOf(0);
					}
				}
				createNewFacility(outputDoc, facs, capS, curbID, linkID, x, y);		
				
				// get zone of link
				if (ZoneParking.containsKey(zoneOfLink)) {
					int park = ZoneParking.get(zoneOfLink);
					ZoneParking.put(zoneOfLink, park + Integer.parseInt(capS));
				} else {
					ZoneParking.put(zoneOfLink, Integer.parseInt(capS));
				}	
			}
			
			// write content into xml file
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM,"http://www.matsim.org/files/dtd/facilities_v1.dtd");
			DOMSource source = new DOMSource(outputDoc);
			StreamResult result = new StreamResult(outputFile);
			transformer.transform(source, result);

			System.out.println(linkList.size() + " number of links in total");
			System.out.println(counter + " links for which parking lots have been created");
			System.out.println(linkCounter + " number of parking links in total");
			System.out.println(parkingCounter + " number of parking lots in total");
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
