/** DJ **/
package org.recommender101;

import java.io.FileReader;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.recommender101.data.DataModel;
import org.recommender101.eval.impl.Recommender101Impl;
import org.recommender101.eval.interfaces.EvaluationResult;
import org.recommender101.tools.MovieLens100kDataDownLoader;
import org.recommender101.tools.Utilities101;

/**
 * The interface for the library
 * @author DJ
 *
 */
public class Recommender101 {

	Recommender101Impl impl;
	
	/**
	 * A getter for the global data model
	 * @return
	 */
	public DataModel getDataModel() {
		return impl.getDataModel();
	}

	// =====================================================================================
	
	/**
	 * A constructor which takes a set of properties
	 * @param confFileName
	 */
	public Recommender101(Properties configuration) throws Exception {
		impl = new Recommender101Impl(configuration);
	}
	
  // =====================================================================================

	/**
	 * A default constructor which loads the properties from a standard location and inits things
	 */
	public Recommender101() throws Exception {
		impl = new Recommender101Impl();
	}
	
  // =====================================================================================

	/**
	 * Runs the experiments whose descriptions have been loaded in the init() phase.
	 * We use the CrossValidationRunner both for cross-validation and given training/test 
	 * splits
	 * @throws Exception
	 */
	public void runExperiments() throws Exception {
		impl.runExperiments();
	}
	// =====================================================================================
	/**
	 * A method that computes the average value of all cross-validation results.
	 * We compute the 
	 * @param resultsPerEvalRun
	 * @return the EvaluationResult list with averaged values
	 */
	public List<EvaluationResult> calcualteAverageResults(Map<Integer, List<EvaluationResult>> resultsPerEvalRun)  
	throws Exception {
		return impl.calcualteAverageResults(resultsPerEvalRun);
		
  }
	// =====================================================================================

	/**
	 * Test only to print the evaluation results.
	 * To be extended to use the algorithm classes themselves and a more detailed result presentation
	 * @param results
	 */
	public void printExperimentResults(List<EvaluationResult> results) {
		impl.printExperimentResults(results);
	}

	// =====================================================================================

	/**
	 * Print results of evaluation arranged per metric in descending order
	 * @param results
	 */
	public void printSortedEvaluationResults(List<EvaluationResult> results) {
		impl.printSortedEvaluationResults(results);
		
		System.out.println("Overall time in seconds: "  + (impl.runner.computationTime / (double) 1000));
	}

  // =====================================================================================

	/**
	 * Returns the results of the last experiment run
	 * @return
	 */
	public Map<Integer, List<EvaluationResult>> getLastDetailedResults() {
		return impl.getLastDetailedResults();
	}
	
	// =====================================================================================

	/**
	 * Returns the averaged results
	 * @return
	 */
	public List<EvaluationResult> getLastResults() {
		return impl.getLastResults();
	}


	// =====================================================================================
	/**
	 * A test method which runs the default configuration file
	 * @param args no args accepted
	 */
	public static void main(String[] args) {
		try {
			
			System.out.println("Recommender101 called with parameters: " + Utilities101.printArray(args));
			
			Properties props = null;
			String propertyFile = "";
			
			boolean downloadML = true;
			
			if (args.length > 0) {
				propertyFile = args[0];
				System.out.println("Looking for property file: " + propertyFile);
				
				try {
					props = new Properties();
					props.load(new FileReader(propertyFile));					
				}
				catch (Exception e) {
					System.out.println("Problem loading property file: " + propertyFile);
					e.printStackTrace();
					System.out.println("USAGE: org.recommender101.Recommender101 [propertyFileName] [--noMovieLensDownload]" );
					System.exit(1);
				}
				
			}
			// Doing the default.
			// Make sure to have at least 2 GB of main memory for these tests
			// Check for arguments
			if (cmdLineArgumentExists(args, "noMovieLensDownload")) {
				downloadML = false;
			}

			if (downloadML) {
				System.out.println("Checking of test data has to be downloaded..");
				MovieLens100kDataDownLoader downloader = new MovieLens100kDataDownLoader();
				MovieLens100kDataDownLoader.downloadML100k();
			}

			
			Recommender101Impl r101;
			if (props == null) {
				System.out.println("Starting evaluation with default configuration file");
				// Initialize the recommender
				r101 = new Recommender101Impl();
			}
			else {
				r101 = new Recommender101Impl(props);
			}
			
			// Start the experiments
			r101.runExperiments();
			
			// Show results
			List<EvaluationResult> finalResult = r101.getLastResults();
			System.out.println("Evaluation results:");
			r101.printSortedEvaluationResults(finalResult);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("Program ended");
	}
	
	
	/**
	 * Returns true if a certain parameter is given with a "--" prefix
	 * @param args the arguments to the main class
	 * @param thearg the arg we search for (without "--")
	 * @return true if the argument exists
	 */
	public static boolean cmdLineArgumentExists(String[] args, String thearg) {
		boolean result = false;
		for (String arg : args) {
			if (arg.startsWith("--") && arg.length() > 2) {
				if (((String) arg.subSequence(2, arg.length())).equalsIgnoreCase(thearg)) {
					return true;
				}
			}
		}
		return result;
	}
	
}
