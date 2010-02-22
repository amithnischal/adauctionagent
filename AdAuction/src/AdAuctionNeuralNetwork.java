import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

import org.joone.engine.DirectSynapse;
import org.joone.engine.FullSynapse;
import org.joone.engine.Layer;
import org.joone.engine.LinearLayer;
import org.joone.engine.Monitor;
import org.joone.engine.NeuralNetEvent;
import org.joone.engine.NeuralNetListener;
import org.joone.engine.Pattern;
import org.joone.engine.SigmoidLayer;
import org.joone.engine.learning.TeachingSynapse;
import org.joone.io.JDBCInputSynapse;
import org.joone.net.NeuralNet;
import org.joone.net.NeuralNetLoader;
import org.joone.util.NormalizerPlugIn;


public class AdAuctionNeuralNetwork implements NeuralNetListener 
{
	/** The number of neurons in the input layer. */
	private static final int INPUT_LAYER_NEURONS = 4;
	/** The number of neurons in the hidden layer. */

	private static final int HIDDEN_LAYER_NEURONS = INPUT_LAYER_NEURONS + 2;
	/** The number of neurons in the Output layer. */
	
	private static final int OUTPUT_LAYER_NEURONS = 1;
	/** The number of rows in the training set. */
	
	private static final int TRAINING_ROWS = 400000;
	/** The number of cycles to train the network. */
	
	private static final int TRAINING_CYCLES = 200;
	/** The learning rate for the network. */ 
	
	private static final double LEARNING_RATE = 0.6;
	/** The momentum for training the network. */
	
	private static final double MOMENTUM = 0.6;	
	
	/** The filename of the saved network configuration for training. */
	private static final String NETWORK_FILE = "AdNetwork_TRAIN.snet";
	
	/** The filename of the saved network configuration for PREDICTION. */
	private static final String PREDICT_FILE = "PREDICTION.snet";
	
	/** The driver to used for database connections. */
	private static final String DB_DRIVER = "net.sourceforge.jtds.jdbc.Driver";
	
	/** The url to connect to the database. */
	private static final String DB_URL = "jdbc:jtds:sqlserver://T00506615:1433/TAC;loginTimeout=20;user=SA;password=wedd1ng04";
	
	/** The query to get the correct data from the database */
	private static final String DB_QUERY = "SELECT QueryTypeID, AD0, BID0, PROFIT0, BID2, AD2, PROFIT2" +
		" FROM [dbo].[TRAININGDATA];"  ;
	
	/** The error at which to halt the training. */
	private static final double MAX_ERROR = 5.0E-7;
	
	/** The neural network we are building. */
	private NeuralNet my_network;
	/** A monitor to manage the neural network. */
	private Monitor my_monitor;

	/** These Normalize the input independent variables and training dependent variable respectively */ 
	private ArrayList<NormalizerPlugIn> my_input_normalizers;
	private NormalizerPlugIn my_training_normalizer;
	
	/**
	 * Public constructor that builds a network from scratch.
	 */
	public AdAuctionNeuralNetwork()
	{
		/** this will contain all the layers of the network. */
		my_network = new NeuralNet();
		my_input_normalizers = new ArrayList<NormalizerPlugIn>();
		my_training_normalizer = new NormalizerPlugIn();
		setupMonitor();
		addLayers();
		addInput(my_network.getInputLayer());
		addTeacher();		
	}
	
	/**
	 * Public constructor that loads a previously built, serialized neural network.
	 * @param the_file_name The name of the file that represents the saved network.
	 */
	public AdAuctionNeuralNetwork(final String the_file_name)
	{
		my_input_normalizers = new ArrayList<NormalizerPlugIn>();
		NeuralNetLoader loader = new NeuralNetLoader(the_file_name);
		my_network = loader.getNeuralNet();
		
		configNetworkForPrediction();
		setupMonitor();
	}
	
	/**
	 * Main method used for training the network.
	 * @param the_args Ignored.
	 */
	public static void main(final String[] the_args)
	{
		final AdAuctionNeuralNetwork net = 
			new AdAuctionNeuralNetwork("PREDICTION.SNET");
		//final AdAuctionNeuralNetwork net = 
		//	new AdAuctionNeuralNetwork();
		//	net.trainNetwork();
		System.out.println(net.predict(1,0.55, 190.0, 0.58));
	}
	
	/** Trains the neural network with the input data. */
	public void trainNetwork()
	{
		my_monitor.setLearning(true);
		my_network.go();
	}
	
	public double predict(int the_query_type_id, double the_bid_0, 
			double the_profit_0, double the_bid_2)
	{
		my_network.start();
		my_network.go(false);
		
		System.out.println("Calling predict");
		
		double query_id = scaleNumber(the_query_type_id,0.0,16.0,0.0,1.0 );
		the_bid_0 = scaleNumber(the_bid_0,0.0,10.0,0.0,1.0);
		the_profit_0 = scaleNumber(the_profit_0,-5000.0, 5000.0,0.0,1.0);
		the_bid_2 = scaleNumber(the_bid_2,0.0,10.0,0.0,1.0);
		
		System.out.println("Calling Prediction");
		
		double[]inp = {query_id,the_bid_0,the_profit_0, the_bid_2};
		
		DirectSynapse memInp= (DirectSynapse) my_network.getInputLayer().getAllInputs().get(0);
		Pattern iPattern = new Pattern(inp);
		memInp.fwdPut(iPattern);

		DirectSynapse memOut = (DirectSynapse) my_network.getAllOutputs().get(0);
		Pattern oPattern = memOut.fwdGet();
		double result =scaleNumber(oPattern.getArray()[0],0.0,1.0,-5000.0,5000.0);		
		my_network.stop();
			return result;
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
		// TODO Auto-generated method stub
		
	}

	@Override
	public void netStopped(final NeuralNetEvent the_event) 
	{
		if(my_monitor.isLearning())
		{
			System.out.println("Training finished");
			saveNetwork();
			
		}
	}

	@Override
	public void netStoppedError(NeuralNetEvent arg0, String arg1) 
	{
		// TODO Auto-generated method stub
		
	}
	
	
	private void setupMonitor()
	{
		my_monitor = my_network.getMonitor();
		
		my_monitor.addLearner(0, "org.joone.engine.BasicLearner");
		my_monitor.addLearner(1, "org.joone.engine.BatchLearner");
		my_monitor.addLearner(2, "org.joone.engine.RpropLearner");

		my_monitor.setLearningMode(0);
		my_monitor.setLearningRate(LEARNING_RATE);
		//my_monitor.setLearningRate(1.0);

		my_monitor.setMomentum(MOMENTUM);		
		my_monitor.setTrainingPatterns(TRAINING_ROWS);
		my_monitor.setTotCicles(TRAINING_CYCLES);
		my_monitor.addNeuralNetListener(this);
	}
	
	/** 
	 * Constructs the neural network
	 */
	private void addLayers()
	{
		//These are the input, hidden, and output layers of the network.
		LinearLayer input_layer = new LinearLayer();
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
		
		/** Add the layers to the neural network object for management. */
		my_network.addLayer(input_layer, NeuralNet.INPUT_LAYER);
		my_network.addLayer(hidden_layer, NeuralNet.HIDDEN_LAYER);
		my_network.addLayer(output_layer, NeuralNet.OUTPUT_LAYER);	
	}
	
	/** Defines the input data for the neural network. */
	private void addInput(final Layer the_input_layer)
	{		
		JDBCInputSynapse inputStream  = new JDBCInputSynapse(DB_DRIVER, DB_URL,
                DB_QUERY,"1,3,4,5",1, 0,true);
		
		/** This does a linear scaling of the input parameters. */
		for(int i = 0; i < INPUT_LAYER_NEURONS; i++)
		{
		  NormalizerPlugIn temp = new NormalizerPlugIn();
		  temp.setAdvancedSerieSelector(Integer.toString(i + 1));
		  temp.setMax(1.0);
		  temp.setMin(0.0);
		  inputStream.addPlugIn(temp);
		  my_input_normalizers.add(temp);
		}
		/** Normalization of the Inputs to the Network */
		my_input_normalizers.get(0).setDataMin(0.0); //Query Type
		my_input_normalizers.get(0).setDataMax(16.0);//
		my_input_normalizers.get(1).setDataMin(0.0);//Bid 0
		my_input_normalizers.get(1).setDataMax(10.0);
		my_input_normalizers.get(2).setDataMin(-5000.0);//Profit 0
		my_input_normalizers.get(3).setDataMax(+5000.0);//Profit 0
		my_input_normalizers.get(3).setDataMin(0.0);//Bid 2
		my_input_normalizers.get(3).setDataMax(10.0);
		
		/** add the input synapse to the first layer. */
		the_input_layer.addInputSynapse(inputStream);
	}
	
	private double scaleNumber(double in, double inMin, double inMax, double outMin, double outMax)
	{
		return outMin + (outMax-outMin) * (in- inMin)/(inMax-inMin);
	}
	
	/**Connects the supervised training values to the output and the network. */
	private void addTeacher()
	{
		TeachingSynapse trainer = new TeachingSynapse();
		my_training_normalizer.setAdvancedSerieSelector("1");
		my_training_normalizer.setMin(0.0);
		my_training_normalizer.setMax(1.0);
		my_training_normalizer.setDataMin(-5000.0);
		my_training_normalizer.setDataMax(+5000.0);
		
		/** Setting of the input containing the desired response. */
		JDBCInputSynapse samples  = new JDBCInputSynapse(DB_DRIVER, DB_URL,
                DB_QUERY,"7",1, 0, true);
	
		samples.addPlugIn(my_training_normalizer);
		
		trainer.setDesired(samples);
		my_network.getOutputLayer().addOutputSynapse(trainer);
		my_network.setTeacher(trainer);
	}
	
	/** Configures the Neural Network for use in prediction mode. */
	private void configNetworkForPrediction()
	{
		my_input_normalizers.clear();
		
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
	 * Saves a serialized version of the network
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
