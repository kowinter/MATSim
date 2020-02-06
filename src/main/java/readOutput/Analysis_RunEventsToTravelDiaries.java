package readOutput;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.vehicles.VehicleReaderV1;

import java.io.File;
import java.io.IOException;

/**
 * @author pieterfourie
 *         <p>
 *         Running this class with or without arguments prints out instructions for use.
 *         </p>
 */
public class Analysis_RunEventsToTravelDiaries {

    /**
     * @param args
     */
    public static void main(String[] args) {
        String eventsFileName = "output_events.xml.gz";
    	//String eventsFileName = "1.events.xml.gz";
    	
        Config config = null;
        config = ConfigUtils.loadConfig("ConfigWithTaxi.xml"); 
        String appendage = "";
        String outputDirectory = "output";
        try {
            
            //test the output directory first before running the whole analysis
            File f = new File(outputDirectory);
            System.out.println("Writing files to " + f.getAbsolutePath());
            if (!f.exists() && f.canWrite()) {
                System.err.println("Cannot write to output directory. " +"Check for existence and permissions");
                System.exit(1);
            }

        } catch (ArrayIndexOutOfBoundsException e) {
            System.exit(1);
        }
        Scenario scenario = ScenarioUtils.createScenario(config);

        new MatsimNetworkReader(scenario.getNetwork()).parse(config.network().getInputFileURL(config.getContext()));

        if (config.transit().isUseTransit() ) {

            new TransitScheduleReader(scenario).readFile(config.transit().getTransitScheduleFile());

            new VehicleReaderV1(scenario.getTransitVehicles()).readFile(config.transit().getVehiclesFile());

        }


        EventsToTravelDiaries handler =new EventsToTravelDiaries(scenario);

        EventsManager events = new EventsManagerImpl();

        events.addHandler(handler);

        new MatsimEventsReader(events).readFile(eventsFileName);

        try {
            handler.writeSimulationResultsToTabSeparated(outputDirectory, appendage);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
   
}

