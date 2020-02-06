package runnableFiles;
import org.matsim.contrib.dvrp.data.Fleet;
import org.matsim.contrib.dvrp.data.file.FleetProvider;
import org.matsim.contrib.dvrp.router.TimeAsTravelDisutility;
import org.matsim.contrib.taxi.optimizer.DefaultTaxiOptimizerProvider;
import org.matsim.contrib.taxi.passenger.SubmittedTaxiRequestsCollector;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.contrib.taxi.util.TaxiSimulationConsistencyChecker;
import org.matsim.contrib.taxi.util.stats.TaxiStatusTimeProfileCollectorProvider;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;

import com.google.inject.name.Names;

/**
 * @author michalm
 */
public final class MyTaxiModule extends AbstractModule {
	public static final String TAXI_MODE = "taxi";

	@Override
	public void install() {
		TaxiConfigGroup taxiCfg = TaxiConfigGroup.get(getConfig());
		bind(Fleet.class).toProvider(new FleetProvider(taxiCfg.getTaxisFileUrl(getConfig().getContext()))).asEagerSingleton();
		bind(TravelDisutilityFactory.class).annotatedWith(Names.named(DefaultTaxiOptimizerProvider.TAXI_OPTIMIZER)).toInstance(travelTime -> new TimeAsTravelDisutility(travelTime));
		
		bind(SubmittedTaxiRequestsCollector.class).toInstance(new SubmittedTaxiRequestsCollector());
		addControlerListenerBinding().to(SubmittedTaxiRequestsCollector.class);

		addControlerListenerBinding().to(TaxiSimulationConsistencyChecker.class);
		addControlerListenerBinding().to(MyTaxiStatsDumper.class);

		if (taxiCfg.getTimeProfiles()) {
			addMobsimListenerBinding().toProvider(TaxiStatusTimeProfileCollectorProvider.class);
		}
	}
}