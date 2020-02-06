package createInput;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.utils.objectattributes.attributable.Attributes;
import org.matsim.vis.otfvis.OTFVisConfigGroup;


public class reduceToSelectedPlan {
	
	 
	@SuppressWarnings("resource")
	public static void main(String[] args) throws IOException {
		
		final String CONFIG_FILE = "ConfigWithTaxi.xml";
		String outputFile = "15.plans_reduced.xml";

		Config config = ConfigUtils.loadConfig(CONFIG_FILE, new DvrpConfigGroup(), new OTFVisConfigGroup(),new TaxiConfigGroup());
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Network network = scenario.getNetwork();
		Population population = scenario.getPopulation();
		Map<Id<Person>, ? extends Person> persons = population.getPersons();
		for (Map.Entry<Id<Person>, ? extends Person> entry : persons.entrySet()) {
		     Person person = entry.getValue();
		     List<? extends Plan> plans = person.getPlans();
		     plans.removeIf((Plan plan) -> (!(plan.equals(person.getSelectedPlan())))); 
		}	
		new PopulationWriter(population, network).write(outputFile);
		
		System.out.println("Done. Don't forget to refresh your project folder now :)" );
	}
}