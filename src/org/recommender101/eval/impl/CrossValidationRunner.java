/** DJ **/
package org.recommender101.eval.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.recommender101.data.DataModel;
import org.recommender101.data.DataSplitter;
import org.recommender101.data.Rating;
import org.recommender101.eval.interfaces.EvaluationResult;
import org.recommender101.recommender.AbstractRecommender;
import org.recommender101.tools.Debug;

/**
 * This class splits the data and runs the cross validation
 * 
 * @author DJ
 * 
 */
public class CrossValidationRunner {

	/**
	 * The data model containing all the data to be split
	 */
	DataModel dataModel;
	/**
	 * A set of algorithms to test
	 */
	List<AbstractRecommender> algorithms;
	/**
	 * A list of strings describing evaluation methods
	 */
	String evaluators;
	/**
	 * + A method to split the data for cross validation randomly or using
	 * another method
	 */
	DataSplitter dataSplitter;
	/**
	 * A flag indicating if the (two) splits returned by the splitter are a
	 * given training test split
	 */
	boolean givenTrainingTestSplit;
	
	/**
	 * If set to false, a different method for split creation is used
	 */
	boolean useDifferentialSplitCreation = true;
	
	/**
	 * Remember the users to consider in a given n scenario
	 * Make this a public static information...
	 */
	public static Map<DataModel, Set<Integer>> givenNTestUsers = new HashMap<DataModel, Set<Integer>>();
	
	
	public long computationTime = 0;
	

	// =====================================================================================

	/**
	 * The main class to do cross validation. The constructor accepts the
	 * parameters
	 * 
	 * @param dm
	 *            the data model
	 * @param algorithms
	 *            algorithms to test
	 * @param evaluators
	 *            evaluation metrics to collect
	 * @param dataSplitter
	 *            a data splitter to use
	 * 
	 */
	public CrossValidationRunner(DataModel dm, List<AbstractRecommender> algos,
			String evals, DataSplitter splitter, boolean givenTrainingTest) {
		
		
		// Simply store the values
		this.dataModel = dm;
		this.algorithms = algos;
		this.evaluators = evals;
		this.dataSplitter = splitter;
		this.givenTrainingTestSplit = givenTrainingTest;
		
		
	}

	/**
	 * This method creates the needed data combinations for cross validation and
	 * executes the defined evaluations / experiments
	 * 
	 * @throws Exception
	 */
	public Map<Integer, List<EvaluationResult>> runExperiments()
			throws Exception {
		

		// Save current time for performance measuring
		long startTime = System.currentTimeMillis();

		// DEBUG setting the number of threads here
		int numOfThreads = Recommender101Impl.NUM_OF_THREADS;

		List<Set<Rating>> dataSplits = dataSplitter.splitData(dataModel);

		// DEBUG: Limit nb of checks
		int xvalidationRounds = dataSplits.size();

		// DEBUG Option
		String maxValRoundsStr = Recommender101Impl.properties.getProperty("Debug.MaxValidationRounds");
		if (maxValRoundsStr != null) {
			int rounds = Integer.parseInt(maxValRoundsStr);
			if (rounds > 0) {
				xvalidationRounds = rounds;
			}
		}
		// If we have only two data splits which were given, only do the first
		// iteration
		if (givenTrainingTestSplit) {
			xvalidationRounds = 1;
		}

		// Experiments are now stored in a BlockingQueue in order to be thread
		// safe
		BlockingQueue<Experiment> experiments;

		// This BlockingQueue will store the experiment results
		BlockingQueue<EvaluationResult> evaluationResults;

		// Start the validation rounds and create experiment objects; no
		// parallelism so far
		Set<Rating> trainingData = new HashSet<Rating>();
		Set<Rating> testData = new HashSet<Rating>();
		// run them (sequentially at the moment)
		HashMap<Integer, List<EvaluationResult>> resultPerEvaluationRound = new HashMap<Integer, List<EvaluationResult>>();

		// Initialize BlockingQueues (used for cross-thread-communication)
		experiments = new PriorityBlockingQueue<Experiment>(50,new ExperimentComparator());
//		experiments = new LinkedBlockingQueue<Experiment>();
		evaluationResults = new LinkedBlockingQueue<EvaluationResult>();

		// Start the specified number of worker threads
		// experimentWorker[] workers = new experimentWorker[Math.min(
		// algorithms.size() * xvalidationRounds, numOfThreads)];
		ExperimentWorker[] workers = new ExperimentWorker[Math.min(algorithms.size(), numOfThreads)];
		for (int i = 0; i < workers.length; i++) {
			workers[i] = new ExperimentWorker(experiments, evaluationResults);
			// Create a new Thread and start it (will be blocked until there
			// is work to do)
			new Thread(workers[i]).start();
		}

		// innerLoopSize contains the number of experiments that have to be assigned on each validation round 
		int innerLoopSize = algorithms.size()/xvalidationRounds;
		// Main loop iterating over the validation rounds
		for (int vround = 0; vround < xvalidationRounds; vround++) {

			if (dataSplitter.getSpecialTestSplits() == null) {
				// Set the test data set (the current index)
				testData = dataSplits.get(vround);
				// Get the rest
				trainingData.clear();
				for (int i = 0; i < dataSplits.size(); i++) {
					if (i != vround) {
						trainingData.addAll(dataSplits.get(i));
					}
				}
			}
			else {
				testData = dataSplitter.getSpecialTestSplits().get(vround);
				System.out.println("Have special test data of size: " + testData.size());
				trainingData.clear();
				// Set the training data to the full model and remove the current test data
				trainingData.addAll(dataSplits.get(0));
				trainingData.removeAll(testData);
				System.out.println("Remaining training data: " + trainingData.size());
			}

			DataModel trainingDM;
            DataModel testDM;

            // Choose split creation method
            if (useDifferentialSplitCreation) {

                   trainingDM = DataModel.copyDataModelAndRemoveRatings(
                          dataModel, testData);
                   testDM = DataModel.copyDataModelAndRemoveRatings(
                          dataModel, trainingData);
            }
            else {
            	trainingDM = new DataModel();
            	testDM = new DataModel();
            	for (Rating r : trainingData){
            		trainingDM.addRating(r.user, r.item, r.rating);
            	}
            	// Add additional training data
    			for (Rating r : dataSplitter.getAdditionalTrainingData()){
    				trainingDM.addRating( r );
    				trainingDM.addImplicitRating(r);
    			}
    			if ( dataSplitter.getAdditionalTrainingData().size() > 0)
    				System.out.println("Additional training data added: "+dataSplitter.getAdditionalTrainingData().size());
    			
    			trainingDM.recalculateUserAverages();
    			
            	for (Rating r : testData){
            		if (trainingDM.getUsers().contains(r.user))
            			testDM.addRating(r.user, r.item, r.rating);
            	}
            	testDM.recalculateUserAverages();

            } 

			
			
			
			// --------------------------------------------------------------
			// If we have to handle a given-n situation, we need to update things here
			if (Recommender101Impl.givenNConfiguration != null) {
				String[] tokens = Recommender101Impl.givenNConfiguration.split("/");
				int givenN = Integer.parseInt(tokens[0]);
				int percentage = Integer.parseInt(tokens[1]);
				
				// Sample a set of users
				List<Integer> sampledUsers = new ArrayList<Integer> (trainingDM.getUsers());
				Collections.shuffle(sampledUsers);
				// how many users to modify
//				System.out.println("I have " + sampledUsers.size() + " users in my data model");
				int nbUsers = (int) Math.round(((double) percentage / (double) 100) * sampledUsers.size());
//				System.out.println("Sampling " + nbUsers + " for givenN Configuration");
				
				/**
				 * Remove whats at the end of the list
				 */
				sampledUsers = sampledUsers.subList(0, nbUsers);
//				for (int i=nbUsers;i<nBSampled-1;i++) {
//					sampledUsers.remove(i);
//				}
//				System.out.println("Storing " + sampledUsers.size() + " in my environemnt");
				// remember it for the test set for the evaluation process
				givenNTestUsers.put(testDM,new HashSet<Integer>(sampledUsers));
				
				// now remove the all but n ratings of the user
				for (Integer user : sampledUsers) {
					Set<Rating> ratingsOfUser = trainingDM.getRatingsOfUser(user);
					ArrayList<Rating> ratingsCopy = new ArrayList<Rating>(ratingsOfUser);
					// randomly remove ratings
					if (givenN < ratingsCopy.size()) {
						Collections.shuffle(ratingsCopy);
						// Remove everything behind the n-th rating
						for (int i=givenN; i<ratingsCopy.size();i++) {
							trainingDM.removeRating(ratingsCopy.get(i));
						}
					}
//					System.out.println("Remaining ratings of user: " + ratingsOfUser.size());
				}
				trainingDM.recalculateUserAverages();
			}
			
			// --------------------------------------------------------------

			// Set up the experiments
			
			for (int i=0; i<innerLoopSize; i++){
				AbstractRecommender recommender = algorithms.get(0);
				algorithms.remove(0);
				
//				System.out.println("Doing algorithm: " + recommender);
				recommender.setDataModel(trainingDM);
				// recommender.init(); <- moved to another thread

				// put is thread safe. Some of the threads will already begin
				// working while experiments are still being inserted.
				experiments.put(new Experiment(recommender, trainingDM, testDM, evaluators, vround + 1));
				//System.out.println("exp added");
			}
			
			// Create an empty list for this evaluation round
			resultPerEvaluationRound.put(vround + 1, new ArrayList<EvaluationResult>());
		}


		// End all worker threads once all experiments have been finished
		while (true) {
			if (experiments.size() == 0) {
				for (ExperimentWorker w : workers) {
					w.stopThread();
				}
				Debug.log("All experiments have been assigned. " );
				// Wait for all threads to finish their work
				boolean stillWorking = true;
				while (stillWorking) {
					// Block this thread for for 250ms
					Thread.sleep(250);

					// Check if the worker threads have finished their work
					stillWorking = false;
					for (ExperimentWorker w : workers)
						if (w.isWorking())
							stillWorking = true;
				}
				Debug.log("All Threads have finished working.");
				workers = null;
				break;
			} else {
				// Block this thread for for 250ms
				Thread.sleep(250);
			}

		}

		// Store the evaluation round results.

		for (EvaluationResult r : evaluationResults) {
			// Insert the result into the matching list
			resultPerEvaluationRound.get(r.getEvaluationRound()).add(r);
		}
		
		computationTime = System.currentTimeMillis() - startTime;

		Debug.log("runExperiments() has finished, it took "
				+ (computationTime) + " ms.");
		return resultPerEvaluationRound;
	}

	/**
	 * This class is intended to be instantiated as a separate Thread for the
	 * processing of experiments
	 * 
	 * @author timkraemer
	 * 
	 */
	private class ExperimentWorker implements Runnable {
		/**
		 * Needed to determine whether or not the thread is supposed to wait for
		 * work.
		 */
		private volatile boolean stillNeeded = true;
		/**
		 * After setting stillNeeded to false, the thread will finish its work
		 * and then set working to false.
		 */
		private volatile boolean working = true;

		/**
		 * The BlockingQueue which contains all unfinished experiments.
		 */
		private BlockingQueue<Experiment> experimentQueue;
		/**
		 * The BlockingQueue into which the worker will put the experiment
		 * results.
		 */
		private BlockingQueue<EvaluationResult> evaluationResultQueue;

		/**
		 * Creates a new experimentWorker object.
		 * 
		 * @param experiments
		 *            The BlockingQueue which contains all unfinished
		 *            experiments.
		 * @param evaluationResults
		 *            The BlockingQueue into which the worker will put the
		 *            experiment results.
		 */
		public ExperimentWorker(BlockingQueue<Experiment> experiments,
				BlockingQueue<EvaluationResult> evaluationResults) {
			Debug.log("New thread has been started.");
			this.experimentQueue = experiments;
			this.evaluationResultQueue = evaluationResults;
		}

		/**
		 * Call this method to inform the thread that there's no more work to do
		 */
		public void stopThread() {
			this.stillNeeded = false;
		}

		/**
		 * Call this method to determine whether or not the thread has finished
		 * its work. Call stopThread() before or this will always return true.
		 * 
		 * @return The value of the "working" attribute.
		 */
		public boolean isWorking() {
			return this.working;
		}

		@Override
		public void run() {
			Experiment e;
			// This thread will be blocked until there is an Experiment to work
			// on
			try {
				while (true) {
					if (!stillNeeded) {
						// All work has been done, finishing thread
						break;
					}

					// Check if there is an experiment in the Queue, break every
					// 250 seconds to check if stillNeeded is true.
					e = experimentQueue.poll(250, TimeUnit.MILLISECONDS);
					if (e != null) {
						// Run the experiments and insert the results into the
						// evaluationResultQueue (thread safe)
						try {
							e.recommender.init();
						} catch (Throwable e1) {
							e1.printStackTrace();
							System.exit(1);
							System.err.println("Exception while initializing in Worker-Thread! " + e1.toString());
						}
						try {
							for (EvaluationResult r : e.runExperiments()) {
								r.setEvaluationRound(e.getEvaluationRound());
								evaluationResultQueue.put(r);
							}
						}
						catch (Exception ex) {
							// If we have an exception here, we should give up
							System.err.println("Fatal exception when running experiments " + ex.getMessage() + " - will exit.");
							ex.printStackTrace();
							System.exit(1);
						}
					}
				}
			} catch (InterruptedException ex) {
				System.err.println("InterruptedException in Worker-Thread!");
			}

			Debug.log("Thread finished.");
			this.working = false;
		}
	}

	// =====================================================================================

	/**
	 * Retrieve the set of evaluators
	 * 
	 * @return
	 */
	public String getEvaluators() {
		return evaluators;
	}

	// =====================================================================================
	
	// Our own comparator (Test only)
	class ExperimentComparator implements Comparator<Experiment>{

		/**
		 * Use the run time estimator of the underlying algorithm
		 */
		@Override
		public int compare(Experiment e1, Experiment e2) {
			return e1.recommender.compareTo(e2.recommender);
		}
		
	}
	
	

}
