package org.recommender101.tools;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.recommender101.eval.impl.Recommender101Impl;
import org.recommender101.eval.interfaces.EvaluationResult;

/**
 * Helper class for csv output
 * 
 * @author Christian Drescher
 * 
 */
public class CSVOutputWriter {

	/**
	 * Writes and appends results to csv file
	 * 
	 * @param experimentTitle Title as headline for csv output
	 * @param lastResults Evaluation results
	 * @param csvPath Path to CSV file
	 * @param csvMode 
	 * @throws IOException
	 */
	public static void writeToCSV(String experimentTitle,
			List<EvaluationResult> lastResults, String csvPath, boolean append, List<String> evalMethods)
			throws IOException {

		File file = new File(csvPath);
		FileWriter writer = new FileWriter(file, append);

		// print title of evaluation run
		writer.write(experimentTitle + " \n");

		ArrayList<EvaluationResult> results = (ArrayList<EvaluationResult>) lastResults;

		// A map that contains the eval method and a pointer to the results
		// per method
		Map<String, Map<String, Double>> allResults = new HashMap<String, Map<String, Double>>();

		// Go through the results and split up everyting
		for (EvaluationResult r : results) {
			Map<String, Double> resultsPerAlgorithm = allResults.get(r
					.getAlgorithm());
			if (resultsPerAlgorithm == null) {
				resultsPerAlgorithm = new HashMap<String, Double>();
				allResults.put(r.getAlgorithm(), resultsPerAlgorithm);
			}
			resultsPerAlgorithm.put(Utilities101.removePackageQualifiers(r.getMethodName()), r.getValue());

		}

		// print header columns containing the evalmethod
		writer.write("Algorithm;");
		for (String methodname : evalMethods) {
			writer.write(methodname + ";");
		}
		writer.write("\n");

		// print a line for each algorithm in the resultset
		for (String algorithm : allResults.keySet()) {
			Map<String, Double> resultsPerAlgorithm = allResults.get(algorithm);

			writer.write(Utilities101.removePackageQualifiers(algorithm) + ";");

			// print a column for each value of the used evaluation methods
			for (String methodname : evalMethods) {
				writer.write(Recommender101Impl.decimalFormat
						.format(resultsPerAlgorithm.get(methodname)) + ";");
			}

			writer.write("\n");
		}

		writer.write("\n");

		writer.flush();
		writer.close();

	}

}
