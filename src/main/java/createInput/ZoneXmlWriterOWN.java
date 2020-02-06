package createInput;

import java.util.*;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.zone.Zone;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.MatsimXmlWriter;

import com.vividsolutions.jts.geom.MultiPolygon;

public class ZoneXmlWriterOWN extends MatsimXmlWriter {
	private Map<Id<Zone>, Zone> zones;

	public ZoneXmlWriterOWN(Map<Id<Zone>, Zone> zones) {
		this.zones = zones;
	}

	public void write(String file ) {
		openFile(file);
		writeDoctype("zones", "http://matsim.org/files/dtd/zones_v1.dtd");
		writeStartTag("zones", Collections.<Tuple<String, String>> emptyList());
		writeZones();
		writeEndTag("zones");
		close();
	}

	private void writeZones() {
		for (Zone z : zones.values()) {
			List<Tuple<String, String>> atts = new ArrayList<>();
			atts.add(new Tuple<String, String>("id", z.getId().toString()));

			String type = z.getType();
			if (type != null && !type.isEmpty()) {
				atts.add(new Tuple<String, String>("type", type));
			}
			
			MultiPolygon mP = z.getMultiPolygon();
			if (mP != null && !mP.isEmpty()) {
				atts.add(new Tuple<String, String>("multipolygon", mP.toString()));
			}

			writeStartTag("zone", atts, true);
		}
	}
}