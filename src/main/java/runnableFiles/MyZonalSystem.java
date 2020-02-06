package runnableFiles;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.zone.ZonalSystem;
import org.matsim.contrib.zone.Zone;
import org.matsim.contrib.zone.util.NetworkWithZonesUtils;
import org.matsim.contrib.zone.util.ZoneFinder;

public class MyZonalSystem implements ZonalSystem{
	
	private final Map<Id<Zone>, Zone> zones;
	private final Map<Id<Node>, Zone> nodeToZoneMap;

	public MyZonalSystem(Map<Id<Zone>, Zone> zones, ZoneFinder zoneFinder, Network network) {
		this.zones = zones;

		nodeToZoneMap = NetworkWithZonesUtils.createNodeToZoneMap(network, zoneFinder);
	}

	@Override
	public Map<Id<Zone>, Zone> getZones() {
		return zones;
	}

	@Override
	public Zone getZone(Node node) {
		Zone zone = nodeToZoneMap.get(node.getId());
		return zone;
	}

}