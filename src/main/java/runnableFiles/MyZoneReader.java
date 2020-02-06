package runnableFiles;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.zone.Zone;
import org.matsim.contrib.zone.io.ZoneXmlReader;
import org.opengis.feature.simple.SimpleFeature;
import com.vividsolutions.jts.geom.MultiPolygon;


public class MyZoneReader {
	
	public static Map<Id<Zone>, Zone> readZones(String zonesXmlFile, String zonesShpFile) {
		ZoneXmlReader xmlReader = new ZoneXmlReader();
		xmlReader.readFile(zonesXmlFile);
		Map<Id<Zone>, Zone> zones = xmlReader.getZones();
		
		Map<String, URL> map = new HashMap<String, URL>();
		URL url= Run.class.getClassLoader().getResource(zonesShpFile);
		map.put("url", url);
		DataStore dataStore = null;
		String typeName = null;
		SimpleFeatureSource source = null;
		SimpleFeatureCollection collection = null;
		try {
			dataStore = DataStoreFinder.getDataStore(map);
			typeName = dataStore.getTypeNames()[0];
			source = dataStore.getFeatureSource(typeName);
			collection = source.getFeatures();
		} catch (IOException e) {
			e.printStackTrace();
		}
	
		SimpleFeatureIterator it = collection.features();
		List<SimpleFeature> featureSet = new ArrayList<SimpleFeature>();
		while (it.hasNext()) {
			SimpleFeature ft = it.next();
			featureSet.add(ft);
		}
		it.close();
		dataStore.dispose();
		
		for (SimpleFeature ft : featureSet) {
				String id = "zone." + ft.getAttribute(1).toString();
				Zone z = zones.get(Id.create(id, Zone.class));
				Object geometry = ft.getDefaultGeometry();
				z.setMultiPolygon((MultiPolygon) geometry);
				zones.put(Id.create(id, Zone.class), z);
		}
		return zones;
	}

}


	
	