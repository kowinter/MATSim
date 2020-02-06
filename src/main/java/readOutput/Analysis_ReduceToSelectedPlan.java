package readOutput;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;
import org.matsim.core.api.internal.MatsimReader;
import org.matsim.core.population.io.PopulationReader;

public class Analysis_ReduceToSelectedPlan {
	
	 
	@SuppressWarnings("resource")
	public static void main(String[] args) throws IOException {
		
		final String CONFIG_FILE = "ConfigWithTaxi.xml";
		Integer counter = 0;

		Config config = ConfigUtils.loadConfig(CONFIG_FILE, new DvrpConfigGroup(), new OTFVisConfigGroup(),new TaxiConfigGroup());
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Population population = scenario.getPopulation();
		Map<Id<Person>, ? extends Person> persons = population.getPersons();
		for (Map.Entry<Id<Person>, ? extends Person> entry : persons.entrySet()) {
		     Person person = entry.getValue();
		     List<? extends Plan> plans = person.getPlans();
		     Plan selecetedPlan = person.getSelectedPlan();
		     Double selectedPlanScore = selecetedPlan.getScore();
		     counter = counter +1;
		     plans.removeIf((Plan plan) -> (!(plan.getScore()).equals(selectedPlanScore)));
		}
		
		new PopulationWriter(population).write("selectedPlanOnly_75plans");
		System.out.println("number of plans" + counter);
		System.out.println("done");
	}
}