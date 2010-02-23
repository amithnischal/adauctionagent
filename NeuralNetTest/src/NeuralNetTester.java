
public class NeuralNetTester {

	/**
	 * @param args Ignored.
	 */
	public static void main(String[] args) 
	{
		/** This section is for running a prediction */
		final AdAuctionNeuralNetwork net = 
		new AdAuctionNeuralNetwork("PREDICTION.SNET");
		
		for (double bid = 0.40; bid < 10.00; bid += 0.01)
		{
			//double result = net.predict(1, 0.4611, 1177.15044, 0.406956,5042, 1, 1123, 134);
			double result = net.predict(1, 0.4611, 1177.15044, bid,5042, 1, 1123, 134);
			System.out.println(Normalizer.scaleLinear(result, 0.0, 1.0, -5000.0, 5000.0));
		}
	}

}
