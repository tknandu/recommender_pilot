/** DJ **/
package org.recommender101.eval.impl;

import java.io.FileInputStream;
import java.io.FileReader;
import java.lang.reflect.Field;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.recommender101.data.DataModel;
import org.recommender101.data.DataSplitter;
import org.recommender101.data.DefaultDataLoader;
import org.recommender101.data.DefaultDataSplitter;
import org.recommender101.eval.interfaces.EvaluationResult;
import org.recommender101.recommender.AbstractRecommender;
import org.recommender101.tools.CSVOutputWriter;
import org.recommender101.tools.ClassInstantiator;
import org.recommender101.tools.DataSetStatistics;
import org.recommender101.tools.Debug;
import org.recommender101.tools.Utilities101;

/**
 * The central class managing all sorts of stuff
 * 
 * @author DJ
 * 
 */
public class Recommender101Impl {

	public static String VERSION_INFO = "Recommender101 v0.36, 2013-05-07";
	// constants and defaults
	public static int MIN_RATING = 1;
	public static int MAX_RATING = 5;
	public static String csvPath = null;
	public static boolean csvAppend = false;

	// Default number of threads
	public static int NUM_OF_THREADS = 4;

	/**
	 * A setter to determine when an item is relevant
	 */
	public static int PREDICTION_RELEVANCE_MIN_PERCENTAGE_ABOVE_AVERAGE = 0;

	/**
	 * Determines which rating threshold should be used
	 */
	public static int PREDICTION_RELEVANCE_MIN_RATING_FOR_RELEVANCE = -1;
	
	/**
	 * Should the recommender remove non-relevant items when recommending?
	 *
	 */
	public static boolean FILTER_NON_RELEVANT_ITEMS_FOR_RECOMMENDATION = false; 
	
	/**
	 * path to the csv file to append the results
	 */
	private static String PROP_CSV_PATH = "CSVOutputPath";
	
	/**
	 * CSV output mode
	 */
	private static String PROP_CSV_MODE = "CSVOutputMode";
	
	/**
	 * property name of the experiment title
	 */
	private static String PROP_EXPERIMENT_TITLE = "ExperimentTitle";
	
	/**
	 * title of the evaluation run to be printed in the csv file
	 */
	private String experimentTitle = null;

	/**
	 * The global top n value
	 */
	public static int TOP_N = 10;
	
	public enum evaluationTypes {
		crossvalidation, giventrainingtestsplit
	}

	/**
	 * A handle to the defined properties to be read by everyone
	 */
	public static Properties properties = null;

	/**
	 * A pointer to the data model
	 */
	DataModel dataModel = null;

	/**
	 * A getter for the global data model
	 * 
	 * @return
	 */
	public DataModel getDataModel() {
		return dataModel;
	}

	/**
	 * The list of recommenders to compare
	 */
	List<AbstractRecommender> recommenders;

	/**
	 * The names of the evaluator classes
	 */
	String evaluators;

	/**
	 * A handle to a (custom or default) data splitter
	 */
	DataSplitter splitter;

	/**
	 * A flag indicating if the (two) splits by the data splitter contain a
	 * given training test split
	 */
	public static boolean givenTrainingTest = false;

	/**
	 * A pointer to the detailed results of the last evaluation round
	 * 
	 */
	Map<Integer, List<EvaluationResult>> lastDetailedResults;

	/**
	 * A pointer to the last (averaged) results calculated in the experiments
	 */
	List<EvaluationResult> lastResults;
	
	/**
	 * Here we store the given n configuration (n/percentage of users)
	 */
	public static String givenNConfiguration = null;

	
	/** The internal crossvalidation runner */
	public CrossValidationRunner runner;
	
	public int nbFolds = -1;
	
	// =====================================================================================

	/**
	 * A constructor which takes a set of properties
	 * 
	 * @param configuration, a set of properties
	 */
	public Recommender101Impl(Properties configuration) throws Exception {
		properties = configuration;
		init();
	}

	/**
	 * A constructor which accepts the name of a configuration file
	 * 
	 * @param confFileName
	 */
	public Recommender101Impl(String configurationFile) throws Exception {
		Properties props = new Properties();
		props.load(new FileInputStream(configurationFile));
		properties = props;
		init();
	}

	// =====================================================================================

	/**
	 * A default constructor which loads the properties from a standard location
	 * and inits things
	 */
	public Recommender101Impl() throws Exception {
		properties = new Properties();
		properties.load(new FileReader(CONFIGURATION_FILE));
		init();
	}

	// =====================================================================================

	/**
	 * The method initializes things
	 */
	public void init() throws Exception {
		System.out.println(VERSION_INFO);

		DefaultDataLoader dataLoader = null;
		dataLoader = (DefaultDataLoader) ClassInstantiator.instantiateClassByProperty(
				properties, PROP_DATA_LOADER, DefaultDataLoader.class);
		
		
		// Load the data model
		dataModel = (DataModel) ClassInstantiator.instantiateClassByProperty(
				properties, PROP_DATA_MODEL, DataModel.class);

		dataLoader.loadData(dataModel);

		
		/**
		 * Print out the statistics at the beginning
		 */
		//DataSetStatistics stats = new DataSetStatistics();
		//stats.printBasicStatistics(getDataModel());

		splitter = (DataSplitter) ClassInstantiator.instantiateClassByProperty(
				properties, PROP_DATA_SPLITTER, DefaultDataSplitter.class);
		
		this.nbFolds = splitter.getNbFolds();

		recommenders = loadRecommenders();
		evaluators = loadEvaluators();

		String evaluationType = properties.getProperty(PROP_EVALUATION_TYPE);
		// The default
		evaluationTypes evalType = evaluationTypes.crossvalidation;
		if (evaluationTypes.giventrainingtestsplit.toString().equalsIgnoreCase(
				evaluationType)) {
			evalType = evaluationTypes.giventrainingtestsplit;
		}

		givenTrainingTest = false;
		if (evalType == evaluationTypes.giventrainingtestsplit) {
			// Given training test things.
			givenTrainingTest = true;
		}
		
		// Check if csv output should be used
		csvPath = properties.getProperty(PROP_CSV_PATH);
		
		// Check if csv mode append or overwrite should be used
		if (properties.getProperty(PROP_CSV_MODE) != null &&
				properties.getProperty(PROP_CSV_MODE).equals("append"))
			csvAppend = true;
		
		// Title of this run for csv output
		experimentTitle = properties.getProperty(PROP_EXPERIMENT_TITLE);
		if (experimentTitle == null) {
			experimentTitle = "Experiment results";
		}

		/**
		 * Read the property values
		 */
		readProperty("PROP_GLOBAL_NUM_OF_THREADS", "NUM_OF_THREADS");
		readProperty("PROP_GLOBAL_MAX_RATING", "MAX_RATING");
		readProperty("PROP_GLOBAL_MIN_RATING", "MIN_RATING");
		readProperty("PROP_GLOBAL_GIVEN_N_CONFIGURATION", "givenNConfiguration");
		readProperty("PROP_GLOBAL_TOP_N", "TOP_N");
		readProperty("PROP_GLOBAL_FILTER_NON_RELEVANT_ITEMS_FOR_RECOMMENDATION", "FILTER_NON_RELEVANT_ITEMS_FOR_RECOMMENDATION");
		readProperty("PROP_GLOBAL_PREDICTION_RELEVANCE_MIN_PERCENTAGE_ABOVE_AVERAGE", "PREDICTION_RELEVANCE_MIN_PERCENTAGE_ABOVE_AVERAGE");
		readProperty("PROP_GLOBAL_PREDICTION_RELEVANCE_MIN_RATING", "PREDICTION_RELEVANCE_MIN_RATING_FOR_RELEVANCE");

		// More settings
		dataModel.setMaxRatingValue(MAX_RATING);
		dataModel.setMinRatingValue(MIN_RATING);
		
		/**
		 * Print out the statistics at the end
		 */
		DataSetStatistics stats = new DataSetStatistics();
		stats.printBasicStatistics(getDataModel());
	}

	// =====================================================================================

	/**
	 * Runs the experiments whose descriptions have been loaded in the init()
	 * phase. We use the CrossValidationRunner both for cross-validation and
	 * given training/test splits
	 * 
	 * @throws Exception
	 */
	public void runExperiments() throws Exception {
		runner = new CrossValidationRunner(dataModel, recommenders, evaluators, splitter, givenTrainingTest);
		lastDetailedResults = runner.runExperiments();
		//System.out.println(lastDetailedResults);
		lastResults = calcualteAverageResults(lastDetailedResults);
		
		if ( csvPath != null){
			Debug.log("Appending the results to csv file");
			try  {
				CSVOutputWriter.writeToCSV(experimentTitle, lastResults, csvPath, csvAppend , Utilities101.getEvaluatorList(evaluators));
			}
			catch (Exception e) {
				System.err.println("[Error] Writing to file " + csvPath + " failed: " + e.getMessage());
			}
		}
		
			
	}

	// =====================================================================================

	/**
	 * A method that computes the average value of all cross-validation results.
	 * 
	 * 
	 * @param resultsPerEvalRun
	 * @return the EvaluationResult list with averaged values
	 */
	public List<EvaluationResult> calcualteAverageResults(Map<Integer, List<EvaluationResult>> resultsPerEvalRun) throws Exception {
		List<EvaluationResult> result = new ArrayList<EvaluationResult>();

		// Go through some set of results and see what algorithms and
		// evaluations we have
		Set<String> recommenderStrings = new HashSet<String>();
		Set<String> evalStrings = new HashSet<String>();

		List<Integer> keyList = new ArrayList<Integer>(resultsPerEvalRun.keySet());
		List<EvaluationResult> firstResult = resultsPerEvalRun.get(keyList.get(0));

		for (EvaluationResult r : firstResult) {
			recommenderStrings.add(r.getAlgorithm());
			evalStrings.add(r.getMethodName());
		}

		
		// Create an evaluation result for everything that we expect
		for (String rec : recommenderStrings) {
			for (String ev : evalStrings) {
				result.add(new EvaluationResult(rec, ev, Double.NaN));
			}
		}

		// Now go through the evaluation rounds and aggregate the results
		for (Integer round : resultsPerEvalRun.keySet()) {
			List<EvaluationResult> resultsOfRound = resultsPerEvalRun.get(round);
			for (EvaluationResult aResult : resultsOfRound) {

				if (aResult.getValue() == Double.NaN) {
					Debug.log("NaN value for the following round: "
							+ aResult.getAlgorithm() + ":"
							+ aResult.getMethodName());
				}
				addResultOfRound(result, aResult);
			}
		}
		// Now compute the average value
		for (EvaluationResult finalResult : result) {
					finalResult.setValue(finalResult.getValue() / resultsPerEvalRun.keySet().size());
		}

		return result;
	}

	/**
	 * Adds the evaluationresult to the existing results
	 * 
	 * @param resultsSoFar
	 * @param aResult
	 */
	void addResultOfRound(List<EvaluationResult> resultsSoFar,
			EvaluationResult aResult) throws Exception {
		// find the correct entry
		EvaluationResult existingResult = null;
		for (EvaluationResult res : resultsSoFar) {
			if (res.equals(aResult)) {
				existingResult = res;
				break;
			}
		}
		// Set the values.
		if (existingResult == null) {
			throw new Exception(
					"Recommender101.averageResult: Cannot find Evalution Result for "
							+ aResult.getAlgorithm() + ":\n"
							+ aResult.getMethodName());
		}
		if (Double.isNaN(existingResult.getValue())) {
			existingResult.setValue(aResult.getValue());
		} else {
			existingResult.setValue(existingResult.getValue()
					+ aResult.getValue());
		}
	}

	// =====================================================================================

	/**
	 * Test only to print the evaluation results. To be extended to use the
	 * algorithm classes themselves and a more detailed result presentation
	 * 
	 * @param results
	 */
	public void printExperimentResults(List<EvaluationResult> results) {
		System.out.println("-------------------------------------------");
		System.out.println("Results of the evaluation :");
		for (EvaluationResult result : results) {
			String algorithm = result.getAlgorithm();
			String evalMethod = result.getMethodName();
			double value = result.getValue();

			algorithm = Utilities101.removePackageQualifiers(algorithm);
			evalMethod = Utilities101.removePackageQualifiers(evalMethod);

			// System.out.print(algorithm + "\t\t" + evalMethod + "\t\t" +
			// decimalFormat.format(value) + "\n");
			System.out.format("%-32s%-20s%-8s", algorithm, evalMethod,
					decimalFormat.format(value));
			System.out.println();

		}
		System.out.println("-------------------------------------------");
	}



	// =====================================================================================

	/**
	 * Test -> print results of evaluation arranged per metric in descending
	 * order
	 * 
	 * @param results
	 */
	public void printSortedEvaluationResults(List<EvaluationResult> results) {

		// A map that contains the eval method and a pointer to the results per
		// method
		Map<String, Map<String, Double>> allResults = new HashMap<String, Map<String, Double>>();

		// Go through the results and split up everyting
		for (EvaluationResult r : results) {
			Map<String, Double> resultsPerMethod = allResults.get(r.getMethodName());
			if (resultsPerMethod == null) {
				resultsPerMethod = new HashMap<String, Double>();
				allResults.put(r.getMethodName(), resultsPerMethod);
			}
			resultsPerMethod.put(r.getAlgorithm(), r.getValue());
		}
		System.out.println("--------------------------");
		System.out.println("Evaluation results: ");
		System.out.println("--------------------------");
		// Go through the different hashmaps and print the results
		for (String evalMethod : allResults.keySet()) {
			Map<String, Double> resultsPerMethod = allResults.get(evalMethod);
			resultsPerMethod = Utilities101.sortByValueDescending(resultsPerMethod);
			for (String algorithm : resultsPerMethod.keySet()) {
				double value = resultsPerMethod.get(algorithm);

				algorithm = Utilities101.removePackageQualifiers(algorithm);
				evalMethod = Utilities101.removePackageQualifiers(evalMethod);

				System.out.format("%-30s |%8s |%-80s", evalMethod,
						decimalFormat.format(value), algorithm);
				System.out.println();
			}
			System.out.println();
		}
		System.out.println("--------------------------");

		// System.out.println(allResults);

	}

	// =====================================================================================
	/**
	 * Loads and instantiates the recommenders from the configuration file TODO:
	 * make generic
	 * 
	 * @return
	 */
	public List<AbstractRecommender> loadRecommenders() throws Exception {
		List<AbstractRecommender> result = new ArrayList<AbstractRecommender>();
		String algoString = properties.getProperty(PROP_ALGORITHM_DESCRIPTIONS);

		if (algoString == null) {
			throw new Exception("No algorithms defined, property: "
					+ PROP_ALGORITHM_DESCRIPTIONS);
		}
		
		// From here till the end of the method: Modified by (timkraemer) 17.09.2012
		// Get the number of validation rounds 
		int roundsCount = 0;
		String maxValRoundsStr = Recommender101Impl.properties
				.getProperty("Debug.MaxValidationRounds");
		if (maxValRoundsStr != null) {
			int rounds = Integer.parseInt(maxValRoundsStr);
			if (rounds > 0) {
				roundsCount = rounds;
			}
		}
		
		if (roundsCount == 0) {
			roundsCount = this.nbFolds;
		}
		//System.out.println("Num of validation rounds: "+roundsCount);
		
		// The old version
		// result = ClassInstantiator.instantiateClassesByProperties(algoString);
		// Now result has to contain enough independent recommender objects in order to solve multithreading issues
		for (int i=0; i<roundsCount; i++)
			for (Object o : ClassInstantiator.instantiateClassesByProperties(algoString))
				result.add((AbstractRecommender)o);
		
		//System.out.println("Result length: "+result.size());
		return result;
	}

	// =====================================================================================

	/**
	 * Load the evaluator strings from the property files. Have to be
	 * instantiated later on..
	 * 
	 * @return
	 * @throws Exception
	 *             TODO: make generic
	 */
	public String loadEvaluators() throws Exception {
		String result;
		String metricsString = properties
				.getProperty(PROP_EVALUATOR_DESCRIPTIONS);
		if (metricsString == null) {
			throw new Exception("No metrics defined, property: "
					+ PROP_EVALUATOR_DESCRIPTIONS);
		} else {
			result = metricsString;
		}
		// result =
		// ClassInstantiator.instantiateClassesByProperties(metricsString);
		return result;
	}

	// =====================================================================================

	/**
	 * Returns the results of the last experiment run
	 * 
	 * @return
	 */
	public Map<Integer, List<EvaluationResult>> getLastDetailedResults() {
		return lastDetailedResults;
	}

	/**
	 * Returns the averaged results
	 * 
	 * @return
	 */
	public List<EvaluationResult> getLastResults() {
		return lastResults;
	}

	
	// THE CONSTANTS
	// Default location of property file
	public static String CONFIGURATION_FILE = "conf/recommender101.properties";

	// Property name for data loader
	public static String PROP_DATA_LOADER = "DataLoaderClass";
	// Property name for data splitter
	public static String PROP_DATA_SPLITTER = "DataSplitterClass";
	// Property name for algorithm loader
	public static String PROP_ALGORITHM_DESCRIPTIONS = "AlgorithmClasses";
	// Property name for evaluators
	public static String PROP_EVALUATOR_DESCRIPTIONS = "Metrics";
	// property name for data models
	public static String PROP_DATA_MODEL = "DataModelClass";
	// property name for data models
	public static String PROP_EVALUATION_TYPE = "EvaluationType";

	// The minimum rating
	public static String PROP_GLOBAL_MIN_RATING = "GlobalSettings.minRating";
	// the max rating
	public static String PROP_GLOBAL_MAX_RATING = "GlobalSettings.maxRating";
	// the global list length default
	public static String PROP_GLOBAL_TOP_N= "GlobalSettings.topN";

	// threshold for relevance for list metrics
	public static String PROP_GLOBAL_PREDICTION_RELEVANCE_MIN_PERCENTAGE_ABOVE_AVERAGE = "GlobalSettings.listMetricsRelevanceMinPercentageAboveAverage";
	// threshold for min rating for relevance for list metrics
	public static String PROP_GLOBAL_PREDICTION_RELEVANCE_MIN_RATING = "GlobalSettings.listMetricsRelevanceMinRating";
	// filtering of items
	public static String PROP_GLOBAL_FILTER_NON_RELEVANT_ITEMS_FOR_RECOMMENDATION = "GlobalSettings.filterNonRelevantItemsForRecommendation";

	// Number of threads to use
	public static String PROP_GLOBAL_NUM_OF_THREADS = "GlobalSettings.numOfThreads";

	// The given-n configuration string
	public static String PROP_GLOBAL_GIVEN_N_CONFIGURATION = "GlobalSettings.givenNConfiguration";

	public static DecimalFormat decimalFormat = new DecimalFormat("#.###");
	
//	// --------------------------------------------------------------
//	// TEST METHOD
//	// --------------------------------------------------------------
//	public static void main(String[] args) {
//		System.out.println("R101 test ...");
//		try {
//			Recommender101Impl r101 = new Recommender101Impl();
//			
//			System.out.println(r101.properties);
//			
//			r101.readProperty("PROP_GLOBAL_NUM_OF_THREADS", "NUM_OF_THREADS");
//			r101.readProperty("PROP_GLOBAL_MIN_RATING", "MIN_RATING");
//			r101.readProperty("PROP_GLOBAL_GIVEN_N_CONFIGURATION", "givenNConfiguration");
//			r101.readProperty("PROP_GLOBAL_FILTER_NON_RELEVANT_ITEMS_FOR_RECOMMENDATION", "FILTER_NON_RELEVANT_ITEMS_FOR_RECOMMENDATION");
//						
//			
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		System.out.println("Program ended");
//	}

	
	/**
	 * Sets s a property based on the field
	 * @param field
	 */
	public void readProperty(String fieldname, String fieldToStore) {
		try {
			// Get the setting from the property file
			Field f1 = Recommender101Impl.class.getDeclaredField(fieldname);
			String key = f1.get(this).toString();
			String propertyValue = properties.getProperty(key);
			if (propertyValue != null) {
				Debug.log("Recommender101Impl: readProperty : " + key + " = " + propertyValue);
			}

			// set the field if there is a value
			if (propertyValue != null) {
				propertyValue = propertyValue.trim();
				Field f2 = Recommender101Impl.class.getDeclaredField(fieldToStore);
//				System.out.println("field type to set|" + f2.getType() + "|");
				if (f2.getType().toString().equals("int")) {
//					System.out.println("Setting int value");
					f2.setInt(this, Integer.parseInt(propertyValue));
//					System.out.println(Recommender101Impl.MAX_RATING);
				}
				else if (f2.getType().toString().equals("class java.lang.String")) {
//					System.out.println("Setting String value");
					f2.set(this, propertyValue);
				}
				else if (f2.getType().toString().equals("boolean")) {
//					System.out.println("Setting Boolean value");
					f2.set(this, Boolean.parseBoolean(propertyValue));
				}
			}
		} catch (Exception e) {
			System.err.println("[FATAL:] Cannot set property field " + fieldname);
			e.printStackTrace();
			System.exit(1);
		}
		
		
	} 
	
	/*
	// number of threads
	if (properties.getProperty(PROP_GLOBAL_NUM_OF_THREADS) != null) {
		NUM_OF_THREADS = Integer.parseInt(properties
				.getProperty(PROP_GLOBAL_NUM_OF_THREADS));
		Debug.log("Recommender101Impl: Number of Threads: " + NUM_OF_THREADS);
	}
	
	*/

}
