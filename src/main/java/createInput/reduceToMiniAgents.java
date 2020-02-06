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


public class reduceToMiniAgents {
	
	 
	@SuppressWarnings("resource")
	public static void main(String[] args) throws IOException {
		final String CONFIG_FILE = "ConfigWithTaxi.xml";
		String outputFile = "MiniAgents.xml";

		Config config = ConfigUtils.loadConfig(CONFIG_FILE, new DvrpConfigGroup(), new OTFVisConfigGroup(),new TaxiConfigGroup());
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Network network = scenario.getNetwork();
		Population population = scenario.getPopulation();
		Map<Id<Person>, ? extends Person> persons = population.getPersons();
		List<Person> peapsToDelete = new ArrayList<Person>();
		int counter = 0;
		for (Map.Entry<Id<Person>, ? extends Person> entry : persons.entrySet()) {
		    Person person = entry.getValue();
		    List<? extends Plan> plans = person.getPlans();
		    plans.removeIf((Plan plan) -> (!(plan.equals(person.getSelectedPlan())))); 
		    counter = counter + 1;
		    if (counter >5000) {
		    	peapsToDelete.add(person);
		    }
		}
		
		for (int i = 0; i <peapsToDelete.size(); i ++) {
			Person person = peapsToDelete.get(i);
			population.removePerson(person.getId());
		}
		
		new PopulationWriter(population, network).write(outputFile);
		
		System.out.println("Done. Don't forget to refresh your project folder now :)" );
	}
	}