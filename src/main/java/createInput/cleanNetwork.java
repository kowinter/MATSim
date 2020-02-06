package createInput;

import org.matsim.run.NetworkCleaner;

public class cleanNetwork {
	public static void main(String[] args) {
		String inputFile = "./Amsterdam/Resources/newMergedUTM31N.xml";
		String outputFile = "./Amsterdam/Resources/mergedCleanNEW.xml";
		NetworkCleaner netClean = new NetworkCleaner ();
		netClean.run(inputFile, outputFile);
	}
	

}