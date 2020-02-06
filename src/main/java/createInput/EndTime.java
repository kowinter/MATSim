package createInput;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class EndTime {
static double randomGenerator;

	public static void main() {
		int homeTime = getRandomHomeTime();
		int workTime = getRandomWorkTime(homeTime);
		}

	public static int getRandomHomeTime() {
		randomGenerator = new Random().nextGaussian();
		double randomEnd = randomGenerator * (0.5*3600) + (2*3600);
		int homeTime = (int) (6 * 3600 + randomEnd);
		return homeTime;
	}

	public static int getRandomWorkTime(int homeTime) {
		randomGenerator = new Random().nextGaussian();
		double randomEnd = randomGenerator * (1*3600) + (2*3600);
		int workTime = homeTime + (int) (7*3600 + randomEnd);
		return workTime;
	}
}
