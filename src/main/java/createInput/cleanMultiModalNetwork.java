package createInput;

import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.algorithms.MultimodalNetworkCleaner;

public class cleanMultiModalNetwork {
	public static void main(String[] args) {
		String inputFile = "./Amsterdam/Resources/mergedM.xml";
		String outputFile1 = "./Amsterdam/Resources/NEWmergedClean.xml";
		//String outputFile2 = "./Amsterdam/Resources/1largeclean.xml";

		final Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		final Network network = scenario.getNetwork();
		new MatsimNetworkReader(scenario.getNetwork()).readFile(inputFile);

		
		Set<String> cleaningModes = new HashSet<String>();
		cleaningModes.add("car");
		
		MultimodalNetworkCleaner netClean = new MultimodalNetworkCleaner (network);
		netClean.run(cleaningModes);
		new NetworkWriter(network).write(outputFile1);
		
		//NetworkCleaner netClean2 = new NetworkCleaner ();
		//netClean2.run(network);
		//new NetworkWriter(network).write(outputFile2);
		
	}
	

}