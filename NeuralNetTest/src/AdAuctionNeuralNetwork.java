/*
 * Class AdAuctionNeural Network.
 * Constructs, Trains and Predicts values 
 * from a neural Network trained on Ad Auction report data.
 */
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;

import org.joone.engine.DirectSynapse;
import org.joone.engine.FullSynapse;
import org.joone.engine.Layer;
import org.joone.engine.LinearLayer;
import org.joone.engine.Monitor;
import org.joone.engine.NeuralNetEvent;
import org.joone.engine.NeuralNetListener;
import org.joone.engine.SigmoidLayer;
import org.joone.engine.learning.TeachingSynapse;
import org.joone.helpers.factory.JooneTools;
import org.joone.io.JDBCInputSynapse;
import org.joone.net.NeuralNet;
import org.joone.net.NeuralNetLoader;
import org.joone.util.NormalizerPlugIn;

public class AdAuctionNeuralNetwork implements NeuralNetListener 
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
	
	private static final int TRAINING_EPOCHS = 110;
	/** The learning rate for the network. */ 
	
	private static final double LEARNING_RATE = 0.8;
	/** The momentum for training the network. */
	
	private static final double MOMENTUM = 0.3;	
	
	/** The filename of the saved network configuration for training. */
	private static final String NETWORK_FILE = "AdNetwork_TRAIN.snet";
	
	/** The filename of the saved network configuration for PREDICTION. */
	private static final String PREDICT_FILE = "PREDICTION.snet";
	
	/** The driver to used for database connections. */
	private static final String DB_DRIVER = "net.sourceforge.jtds.jdbc.Driver";
	
	/** The url to connect to the database. */
	private static final String DB_URL = "jdbc:jtds:sqlserver://T00506615:1433/TAC;loginTimeout=20;user=SA;password=wedd1ng04";
	
	/** The query to get the correct data from the database */
	private static final String DB_QUERY = "SELECT QUERYTYPEID, BID0, PROFIT0, BID2, Impressions, Position, Clicks, Conversions, PROFIT2 FROM [dbo].[NNTrainingView]";
	
	/** The error at which to halt the training. */
	private static final double MAX_ERROR = 0.013;
	
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
		setupMonitor();
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
	
	/**
	 * Main method used for training the network.
	 * @param the_args Ignored.
	 */
	public static void main(final String[] the_args)
	{	
		/* The following two lines are for training the network */
		final AdAuctionNeuralNetwork net =new AdAuctionNeuralNetwork();		
		net.trainNetwork();
	
		/** This section is for running a prediction */
//		final AdAuctionNeuralNetwork net = 
//		new AdAuctionNeuralNetwork("PREDICTION.SNET");
//		
//		for (double bid = 0.40; bid < 10.00; bid += 0.01)
//		{
//			//double result = net.predict(1, 0.4611, 1177.15044, 0.406956,5042, 1, 1123, 134);
//			double result = net.predict(1, 0.4611, 1177.15044, bid,5042, 1, 1123, 134);
//			System.out.println(Normalizer.scaleLinear(result, 0.0, 1.0, -5000.0, 5000.0));
//		}
	
	}
	
	/** Trains the neural network with the input data. */
	public void trainNetwork()
	{
		my_monitor.setLearning(true);
		my_network.go(true);
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
	public void cicleTerminated(final NeuralNetEvent the_event) 
	{
		final Monitor mon = (Monitor) the_event.getSource();
		long c = mon.getCurrentCicle();
		/** We want to print the results every 10 epochs */
		if (c % 10 == 0)
		{
			System.out.println(c + " epochs remaining - RMSE = " 
					+ mon.getGlobalError());
		}	
	}

	@Override
	public void errorChanged(final NeuralNetEvent the_event) 
	{
      Monitor mon = (Monitor)the_event.getSource();
      if (mon.getGlobalError() <= MAX_ERROR && mon.getGlobalError() != 0.0)
      {
    	  my_network.stop();
      }		
	}

	@Override
	public void netStarted(final NeuralNetEvent the_event) 
	{
		System.out.println("Network Started");
	}

	@Override
	public void netStopped(final NeuralNetEvent the_event) 
	{
		if(my_monitor.isLearning())
		{
			System.out.println("Training finished");
			saveNetwork();		
		}
		else
		{
			System.out.println("Network Stopped");			
		}
	}

	@Override
	public void netStoppedError(NeuralNetEvent arg0, String arg1) 
	{
		System.out.println("Net Stopped Error " + arg1 );
	}
	
	
	private void setupMonitor()
	{
		my_monitor = my_network.getMonitor();
		my_monitor.setLearningRate(LEARNING_RATE);
		my_monitor.setMomentum(MOMENTUM);		
		my_monitor.setTrainingPatterns(TRAINING_ROWS);
		my_monitor.setTotCicles(TRAINING_EPOCHS);
		my_monitor.addNeuralNetListener(this);
	}
	
	/** 
	 * Constructs the neural network of layers and synapses.
	 */
	private void addLayers()
	{
		//These are the input, hidden, and output layers of the network.
		SigmoidLayer input_layer = new SigmoidLayer();
		SigmoidLayer hidden_layer = new SigmoidLayer();
		LinearLayer output_layer = new LinearLayer();
		
		/** Used to make the network recurrent, currently not used.
		 * To use this, replace the current input_layer. */ 
		
		/** These are the neurons for each layer. */
		FullSynapse synapse_IH  = new FullSynapse();
		FullSynapse synapse_HO = new FullSynapse();
	
		input_layer.setRows(INPUT_LAYER_NEURONS);
		hidden_layer.setRows(HIDDEN_LAYER_NEURONS);
		output_layer.setRows(OUTPUT_LAYER_NEURONS);
		
		/** Connect the network, connecting the three layers with the synapses. */
		input_layer.addOutputSynapse(synapse_IH);
		hidden_layer.addInputSynapse(synapse_IH);
		hidden_layer.addOutputSynapse(synapse_HO);
		output_layer.addInputSynapse(synapse_HO);
		
		addInput(input_layer);
		addTeacher(output_layer);
		
		/** Add the layers to the neural network object for management. */
		my_network.addLayer(input_layer, NeuralNet.INPUT_LAYER);
		my_network.addLayer(hidden_layer, NeuralNet.HIDDEN_LAYER);
		my_network.addLayer(output_layer, NeuralNet.OUTPUT_LAYER);	
	}
	
	/** Defines the input data for the neural network. */
	private void addInput(final Layer the_input_layer)
	{	
//		CenterOnZeroPlugIn norm = new CenterOnZeroPlugIn();
//		norm.setAdvancedSerieSelector("1,2,3,4");
		
		String query = DB_QUERY ;
		JDBCInputSynapse inputStream  = new JDBCInputSynapse(DB_DRIVER, DB_URL,
                query,"1-7",1, TRAINING_ROWS,true);
		/** add the input synapse to the first layer. */
		inputStream.setName("training_inputs");
//		inputStream.addPlugIn(norm);
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

		String query = DB_QUERY;
		
		/** Setting of the input containing the desired response. */
		JDBCInputSynapse prediction_data  = new JDBCInputSynapse(DB_DRIVER, DB_URL,
				query,"8",1, TRAINING_ROWS, true);
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
	
	/** Configures the Neural Network for use in prediction mode. */
	private void configNetworkForPrediction()
	{
		Layer input = my_network.getInputLayer();
		input.removeAllInputs();

		DirectSynapse memInp = new DirectSynapse();
		input.addInputSynapse(memInp);
				
		/** add the input synapse to the first layer. */
		my_network.getInputLayer().addInputSynapse(memInp);
		
		Layer output = my_network.getOutputLayer();
		output.removeAllOutputs();

		DirectSynapse memOut = new DirectSynapse();
		output.addOutputSynapse(memOut);		
		my_network.getMonitor().setTotCicles(1);
		my_network.getMonitor().setTrainingPatterns(1);
		my_network.getMonitor().setLearning(false);
	}

	/**
	 * Saves a serialized version of the network for later recall.
	 */
	private void saveNetwork()
	{
		try
		{
			FileOutputStream stream = new FileOutputStream(NETWORK_FILE);
			FileOutputStream predict = new FileOutputStream(PREDICT_FILE);
			
			ObjectOutputStream out = new ObjectOutputStream(stream);
			ObjectOutputStream predict_out = new ObjectOutputStream(predict);
			
			out.writeObject(my_network);
			out.close();
			
			configNetworkForPrediction();
			predict_out.writeObject(my_network);
			predict_out.close();
		}
		catch(final Exception the_exception)
		{
			the_exception.printStackTrace();
		}	
	}
	
	

}
