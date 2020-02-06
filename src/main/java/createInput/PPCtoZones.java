package createInput;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.accessibility.gis.FeatureSHP;
import org.matsim.contrib.zone.*;
import org.matsim.contrib.zone.io.ZoneXmlWriter;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

public class PPCtoZones {

	public static void main(String[] args) throws IOException {

		String shapeFileName = "./Amsterdam/Resources/Zones/edditedPolygons.shp";
		String output = "./Amsterdam/Resources/Zones/zonesPolygons.xml";
		
		Set<SimpleFeature> shapeSet = FeatureSHP.readFeatures(shapeFileName);
		Map<Id<Zone>, Zone> zones= new HashMap<Id<Zone>, Zone>();
		
		Iterator<SimpleFeature> setIterator = shapeSet.iterator();
		while (setIterator.hasNext()) {
			SimpleFeature sF = setIterator.next();
			Geometry g = (Geometry) sF.getDefaultGeometry();		
			Id<Zone> idZone = Id.create(sF.getID(), Zone.class);
			String type = null;
			GeometryFactory geofac = new GeometryFactory();
			Coordinate[] coords = g.getCoordinates();
			LinearRing ring = geofac.createLinearRing(coords);
			Boolean ringclosed = ring.isRing();
			if (ringclosed == true){
			Polygon p = geofac.createPolygon(ring );
			MultiPolygon mp = geofac.createMultiPolygon(new Polygon[] { p }); // no geotransformation necessary
			Zone z = new Zone(idZone, type, mp);
			zones.put(idZone,z);
			}
		}

		//new ZoneXmlWriterOWN(zones).write(output);
		new ZoneXmlWriter(zones).write(output);

	}
}
