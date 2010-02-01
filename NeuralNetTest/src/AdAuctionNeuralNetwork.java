import java.io.FileOutputStream;
import java.io.ObjectOutputStream;

import org.joone.engine.DelayLayer;
import org.joone.engine.FullSynapse;
import org.joone.engine.Layer;
import org.joone.engine.LinearLayer;
import org.joone.engine.Monitor;
import org.joone.engine.NeuralNetEvent;
import org.joone.engine.NeuralNetListener;
import org.joone.engine.SigmoidLayer;
import org.joone.engine.learning.TeachingSynapse;
import org.joone.io.JDBCInputSynapse;
import org.joone.net.NeuralNet;
import org.joone.net.NeuralNetLoader;
import org.joone.util.NormalizerPlugIn;


public class AdAuctionNeuralNetwork implements NeuralNetListener 
{
	/** The number of neurons in the input layer. */
	private static final int INPUT_LAYER_NEURONS = 2;
	/** The number of neurons in the hidden layer. */
	private static final int HIDDEN_LAYER_NEURONS = 3;
	/** The number of neurons in the Output layer. */
	private static final int OUTPUT_LAYER_NEURONS = 1;
	/** The number of rows in the training set. */
	private static final int TRAINING_ROWS = 4;
	/** The number of cycles to train the network. */
	private static final int TRAINING_CYCLES = 200;
	/** The learning rate for the network. */ 
	private static final double LEARNING_RATE = 0.1;
	/** The momentum for training the network. */
	private static final double MOMENTUM = 0.3;	
	/** The filename of the saved network configuration to save/load */
	private static final String NETWORK_FILE = "AdNetwork.snet";
	/** The driver to used for database connections. */
	private static final String DB_DRIVER = "net.sourceforge.jtds.jdbc.Driver";
	/** The url to connect to the database. */
	private static final String DB_URL = "jdbc:jtds:sqlserver://localhost:1433/adauction;loginTimeout=20;user=admin;password=password";
	/** The query to get the correct data from the database */
	private static final String DB_QUERY = "SELECT * FROM TEST;";
	
	
	/** The neural network we are building. */
	private NeuralNet my_network;
	/** A monitor to manage the neural network. */
	private Monitor my_monitor;
	
	
	
	/**
	 * Public constructor that builds a network from scratch.
	 */
	public AdAuctionNeuralNetwork()
	{
		/** this will contain all the layers of the network. */
		my_network = new NeuralNet();
		setupMonitor();
		addLayers();
		addInput(my_network.getInputLayer());
		addTeacher();		
	}
	
	public static void main(final String[] the_args)
	{
		final AdAuctionNeuralNetwork net = new AdAuctionNeuralNetwork();
		net.trainNetwork();
	}
	
	/**
	 * Public constructor that loads a previously built, serialized neural network.
	 * @param the_file_name The name of the file that represents the saved network.
	 */
	public AdAuctionNeuralNetwork(final String the_file_name)
	{
		NeuralNetLoader loader = new NeuralNetLoader(the_file_name);
		my_network = loader.getNeuralNet();
		setupMonitor();
	}
	
	/** Trains the neural network with the input data. */
	public void trainNetwork()
	{
		my_monitor.setLearning(true);
		my_network.go();
	}

	@Override
	public void cicleTerminated(final NeuralNetEvent the_event) 
	{
		final Monitor mon = (Monitor) the_event.getSource();
		long c = mon.getCurrentCicle();
		/** We want to print the results every 100 epochs */
		if (c % 100 == 0)
		{
			System.out.println(c + " epochs remaining - RMSE = " 
					+ mon.getGlobalError());
		}
		
	}

	@Override
	public void errorChanged(final NeuralNetEvent the_event) 
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void netStarted(final NeuralNetEvent the_event) 
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void netStopped(final NeuralNetEvent the_event) 
	{
		System.out.println("Training finished");
		saveNetwork();
	}

	@Override
	public void netStoppedError(NeuralNetEvent arg0, String arg1) 
	{
		// TODO Auto-generated method stub
		
	}
	
	
	private void setupMonitor()
	{
		my_monitor = my_network.getMonitor();
		my_monitor.setLearningRate(LEARNING_RATE);
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
		SigmoidLayer output_layer = new SigmoidLayer();
		
		/** Used to make the network recurrent, currently not used */
		DelayLayer delay_layer = new DelayLayer();
		
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
		NormalizerPlugIn normalizer = new NormalizerPlugIn();
		normalizer.setMin(0.0);
		normalizer.setMax(1.0);
		
		
		JDBCInputSynapse inputStream  = new JDBCInputSynapse(DB_DRIVER, DB_URL,
                DB_QUERY,"1,2",1, 0, false);
		
		//normalize the inputs so that they are in the range of -1.0 to 1.0
		inputStream.setPlugIn(normalizer);
                
		/** add the input synapse to the first layer. */
		the_input_layer.addInputSynapse(inputStream);
	}
	
	/**Connects the supervised training values to the output and the network. */
	private void addTeacher()
	{
		TeachingSynapse trainer = new TeachingSynapse();
		
		/** Setting of the input containing the desired response. */
		JDBCInputSynapse samples  = new JDBCInputSynapse(DB_DRIVER, DB_URL,
                DB_QUERY,"3",1, 0, false);
		
		trainer.setDesired(samples);
		my_network.getOutputLayer().addOutputSynapse(trainer);
		my_network.setTeacher(trainer);
	}
	
	/**
	 * Saves a serialized version of the network
	 */
	private void saveNetwork()
	{
		try
		{
			FileOutputStream stream = new FileOutputStream(NETWORK_FILE);
			ObjectOutputStream out = new ObjectOutputStream(stream);
			out.writeObject(my_network);
			out.close();
		}
		catch(final Exception the_exception)
		{
			the_exception.printStackTrace();
		}
		
	}
	
	

}
