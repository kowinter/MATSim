package readOutput;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.contrib.zone.Zone;
import org.matsim.contrib.zone.util.ZoneFinder;
import org.matsim.contrib.zone.util.ZoneFinderImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;
import runnableFiles.MyZoneReader;

public class Analysis_SelectedPlansCheckForTaxi {

	@SuppressWarnings("resource")
	public static void main(String[] args) throws IOException {

		Integer counter = 0;

		final String CONFIG_FILE = "ConfigWithTaxi.xml";
		final String xmlZones = "zzones.xml";
		final String shpZones = "zzones.shp";
		Config config = ConfigUtils.loadConfig(CONFIG_FILE, new DvrpConfigGroup(), new OTFVisConfigGroup(),
				new TaxiConfigGroup());
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Network network = scenario.getNetwork();
		Map<Id<Link>, ? extends Link> links = network.getLinks();
		Map<Id<Zone>, Zone> zones = MyZoneReader.readZones(xmlZones, shpZones);
		ZoneFinder zoneFinder = new ZoneFinderImpl(zones, 1.0);
		List<Id<Link>> Zone83Link = new ArrayList<Id<Link>>();

		for (final Id<Link> key : links.keySet()) {
			Link link = links.get(key);
			Coord coord = link.getCoord();
			Id<Zone> zone = zoneFinder.findZone(coord).getId();
			if (zone.toString().contains("83")) {
				Zone83Link.add(link.getId());
			}
		}

		Population population = scenario.getPopulation();
		int popbefore = population.getPersons().size();
		List<Person> popWrong = new ArrayList<Person>();
		Map<Id<Person>, ? extends Person> persons = population.getPersons();
		for (Map.Entry<Id<Person>, ? extends Person> entry : persons.entrySet()) {
			Person person = entry.getValue();
			int after2 = population.getPersons().size();
			Plan selectedPlan = person.getSelectedPlan();
			List<PlanElement> taxiCheck = selectedPlan.getPlanElements();
			String str = taxiCheck.toString();
			if (str.contains("taxi")) {
				Iterator<PlanElement> itr = taxiCheck.iterator();
				while (itr.hasNext()) {
					PlanElement element = itr.next();
					if (element.toString().contains("taxi")) {
						int indexStartLink = element.toString().indexOf("startLinkId") + 12;
						int indexEndLink = element.toString().indexOf("endLinkId") + 10;
						int indexTravTime = element.toString().indexOf("travTime", indexEndLink);

						String start = element.toString().substring(indexStartLink, indexEndLink - 11);
						String end = element.toString().substring(indexEndLink, indexTravTime - 1);

						for (int i = 0; i < Zone83Link.size(); i++) {
							String currentLink = Zone83Link.get(i).toString();
							boolean checking = false;
							if (start.equals(currentLink)) {
								checking = true;
								//Leg leg = (Leg) element;
								//leg.setMode("pt");
							
								counter = counter + 1;
							} else if (end.equals(currentLink)) {
								checking = true;
								Leg leg = (Leg) element;
								//leg.setMode("pt");
								//counter = counter + 1;
							}

							if (checking == true) {
								popWrong.add(person);
							}
						}
					}
				}
			}
		}

		Set<Person> uniques = new HashSet<Person>(popWrong);
		int wrongs = uniques.size();

		for (int i = 0; i < popWrong.size(); i++) {			
			Person person = popWrong.get(i);
			population.removePerson(person.getId());
		}

		int popafter = population.getPersons().size();
		
		new PopulationWriter(population).write("selectedPlanOnly_taxiCheck_removed");
		System.out.println("number of before" + popbefore);
		System.out.println("number of after" + popafter);
		System.out.println("done");
	}
}