/*
 * Class AdAuctionNeural Network.
 * Constructs, Trains and Predicts values 
 * from a neural Network trained on Ad Auction report data.
 */
import org.joone.engine.FullSynapse;
import org.joone.engine.Layer;
import org.joone.engine.LinearLayer;
import org.joone.engine.Monitor;
import org.joone.engine.SigmoidLayer;
import org.joone.engine.learning.TeachingSynapse;
import org.joone.helpers.factory.JooneTools;
import org.joone.io.JDBCInputSynapse;
import org.joone.net.NeuralNet;
import org.joone.net.NeuralNetLoader;
import org.joone.net.NeuralValidationEvent;
import org.joone.net.NeuralValidationListener;
import org.joone.util.NormalizerPlugIn;

public class AdAuctionNeuralNetwork implements NeuralValidationListener 
{
	/** The number of neurons in the input layer. */
	private static final int INPUT_LAYER_NEURONS = 8;
	/** The number of neurons in the hidden layer. */

	private static final int OUTPUT_LAYER_NEURONS = 1;
	/** The number of rows in the training set. */
	
	private static final int HIDDEN_LAYER_NEURONS = 1 + (INPUT_LAYER_NEURONS + OUTPUT_LAYER_NEURONS) / 2;
	/** The number of neurons in the Output layer. */
	
	private static final int TRAINING_ROWS = 460000;
	/** The number of cycles to train the network. */
	
		
	/** The driver to used for database connections. */
	private static final String DB_DRIVER = "net.sourceforge.jtds.jdbc.Driver";
	
	/** The url to connect to the database. */
	private static final String DB_URL = "jdbc:jtds:sqlserver://T00506615:1433/TAC;loginTimeout=20;user=SA;password=wedd1ng04";
	
	/** The query to get the correct data from the database */
	private static final String DB_QUERY = "SELECT QUERYTYPEID, BID0, PROFIT0, BID2, Impressions, Position, Clicks, Conversions, PROFIT2 FROM [dbo].[NNTrainingView]";
	
	
	/** The neural network we are building. */
	private NeuralNet my_network;

	/** A monitor manages the neural network. */
	private Monitor my_monitor;

	private NeuralNetLoader my_loader;
	/** These Normalize the input independent variables and training dependent variable respectively */ 
	
	/**
	 * Public constructor that builds a network from scratch.
	 */
	public AdAuctionNeuralNetwork()
	{
		/** this will contain all the layers of the network. */
		my_network = new NeuralNet();
		addLayers();
	}
	
	/**
	 * Public constructor that loads a previously built, serialized neural network.
	 * @param the_file_name The name of the file that represents the saved network.
	 */
	public AdAuctionNeuralNetwork(final String the_file_name)
	{
		my_loader = new NeuralNetLoader(the_file_name);
		my_network = my_loader.getNeuralNet();
		my_monitor = my_network.getMonitor();
		my_monitor.setLearning(false);
	}
	
	/** Returns the neural network created. */
	public NeuralNet getNetwork()
	{
		return my_network;
	}
	
	/*
	 * Returns the number of rows used for training
	 */
	public int getTrainingRows()
	{
		return TRAINING_ROWS;
	}
	

	
	public double predict(int the_query_type_id, double the_bid_0, 
			double the_profit_0, double the_bid_2, int the_impressions, int the_position, int the_clicks, int the_conversions)
	{
		
		double query_id = the_query_type_id;		
		double[]inp = {query_id,the_bid_0,the_profit_0, the_bid_2, the_impressions, the_position, the_clicks, the_conversions};
		double[] output = JooneTools.interrogate(my_network, inp);
		
		return output[0];
	}

	
	@Override
	public void netValidated(final NeuralValidationEvent the_event)
	{
		NeuralNet net = (NeuralNet) the_event.getSource();
		System.out.println("     Validation Error: " + net.getMonitor().getGlobalError());
	}
	

	
	/** 
	 * Constructs the neural network of layers and synapses.
	 */
	private void addLayers()
	{
		//These are the input, hidden, and output layers of the network.
		SigmoidLayer input_layer = new SigmoidLayer();
		SigmoidLayer hidden_layer_1 = new SigmoidLayer();
		SigmoidLayer hidden_layer_2 = new SigmoidLayer();		
		LinearLayer output_layer = new LinearLayer();
		
		
		/** These are the neurons for each layer. */
		FullSynapse synapse_IH  = new FullSynapse();
		FullSynapse synapse_HH = new FullSynapse();
		FullSynapse synapse_HO = new FullSynapse();
	
		input_layer.setRows(INPUT_LAYER_NEURONS);
		hidden_layer_1.setRows(HIDDEN_LAYER_NEURONS);
		hidden_layer_2.setRows(HIDDEN_LAYER_NEURONS);		
		output_layer.setRows(OUTPUT_LAYER_NEURONS);
		
		/** Connect the network, connecting the three layers with the synapses. */
		input_layer.addOutputSynapse(synapse_IH);
		hidden_layer_1.addInputSynapse(synapse_IH);
		hidden_layer_1.addOutputSynapse(synapse_HH);
		hidden_layer_2.addInputSynapse(synapse_HH);
		hidden_layer_2.addOutputSynapse(synapse_HO);
		output_layer.addInputSynapse(synapse_HO);
		
		addInput(input_layer);
		addTeacher(output_layer);
		
		/** Add the layers to the neural network object for management. */
		my_network.addLayer(input_layer, NeuralNet.INPUT_LAYER);
		my_network.addLayer(hidden_layer_1, NeuralNet.HIDDEN_LAYER);
		my_network.addLayer(hidden_layer_2, NeuralNet.HIDDEN_LAYER);		
		my_network.addLayer(output_layer, NeuralNet.OUTPUT_LAYER);	
	}
	
	/** Defines the input data for the neural network. */
	private void addInput(final Layer the_input_layer)
	{	
		JDBCInputSynapse inputStream  = NeuralNetworkUtilities.createInput(1, TRAINING_ROWS,
				1, INPUT_LAYER_NEURONS, DB_URL, DB_DRIVER, DB_QUERY);
		
		/** add the input synapse to the first layer. */
		inputStream.setName("training_inputs");
		the_input_layer.addInputSynapse(inputStream);
	}
	
	
	/**Connects the supervised training values to the output and the network. */
	private void addTeacher(final Layer the_output_layer)
	{

		Monitor m = my_network.getMonitor();
		m.addLearner(0, "org.joone.engine.BasicLearner"); 
		m.addLearner(1, "org.joone.engine.BatchLearner"); 
		m.addLearner(2, "org.joone.engine.RpropLearner");
		
		m.setLearningMode(1);
		
		/** Setting of the input containing the desired response. */
		JDBCInputSynapse prediction_data  = new JDBCInputSynapse(DB_DRIVER, DB_URL,
				DB_QUERY,Integer.toString(INPUT_LAYER_NEURONS + 1),1, TRAINING_ROWS, true);
		prediction_data.setName("desired_training");
		
		NormalizerPlugIn norm = new NormalizerPlugIn();
		norm.setName("prediction_scalar");
		norm.setAdvancedSerieSelector("1");
		norm.setDataMin(-5000.0);
		norm.setDataMax(+5000.0);
		norm.setMin(0.0);
		norm.setMax(1.0);
		prediction_data.addPlugIn(norm);

		TeachingSynapse trainer = new TeachingSynapse();
		trainer.setDesired(prediction_data);
		
		//Connects the Teaching Synapse to the output of the neural network 
		the_output_layer.addOutputSynapse(trainer);
		
		my_network.setTeacher(trainer);
	}

	

	
	

}
