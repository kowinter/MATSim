package runnableFiles;

import org.apache.commons.configuration.Configuration;
import org.matsim.contrib.taxi.optimizer.rules.RuleBasedTaxiOptimizerParams;

public class MyParams extends RuleBasedTaxiOptimizerParams {
	public static final String ZONES_XML_FILE = "zonesXmlFile";
	public static final String ZONES_SHP_FILE = "zonesShpFile";
	public static final String EXPANSION_DISTANCE = "expansionDistance";

	public final String zonesXmlFile;
	public final String zonesShpFile;
	public final double expansionDistance;

	public MyParams(Configuration optimizerConfig) {
		super(optimizerConfig);

		zonesXmlFile = optimizerConfig.getString(ZONES_XML_FILE);
		zonesShpFile = optimizerConfig.getString(ZONES_SHP_FILE);
		expansionDistance = optimizerConfig.getDouble(EXPANSION_DISTANCE);
	}
}
