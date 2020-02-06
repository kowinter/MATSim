package readOutput;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;

public class Analysis_GiniCoefficient {
	public static void main(String[] args) throws Exception {
		String eventsFileName = "./output/5.taxi_passengerWaitingTimes.txt";
		File file = new File(eventsFileName);
		Scanner sc = new Scanner(file);
		BufferedReader abc = new BufferedReader(new FileReader(file));
		List<Double> list = new ArrayList<Double>();
		Double sumList = 0.0;
		Integer counter = 0;
		String line;

		// while (sc.hasNextLine()){
		while ((line = abc.readLine()) != null) {
			counter = counter + 1;
			if (counter>1) {
				Double x = Double.parseDouble(line);
				list.add(x);
				sumList = sumList + x;
			}
		}

		list.sort(Comparator.naturalOrder());

		Double sumTop = 0.0;
		Integer n = list.size();

		for (int i = 0; i < n; i++) {
			Double y = list.get(i);
			Double x = (n + 1 - (i + 1)) * y;
			sumTop = sumTop + x;
		}
		
		double aa = sumTop/sumList;
		aa = aa *2;
		double cc = n + 1 - aa;
		double bb = cc / n;

		Double gini = bb * 100;

		System.out.println("gini-coefficient: " + gini);
		System.out.println("# of taxi trips: " + n);

	}
}
