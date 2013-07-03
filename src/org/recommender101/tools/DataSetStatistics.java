package org.recommender101.tools;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.recommender101.Recommender101;
import org.recommender101.data.DataModel;
import org.recommender101.eval.impl.Recommender101Impl;

/**
 * A class to collect statistics about data sets
 * @author Dietmar
 *
 */
public class DataSetStatistics {

	/**
	 * Main entry point (no parameters) 
	 */
	public static void main(String[] args) {
		System.out.println("Starting data set stats");
		try {
			DataSetStatistics stats = new DataSetStatistics();
			stats.run();
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("Stats collection ended");
	}
	
	
	/**
	 * The main worker method for the tests
	 * @throws Exception
	 */
	public void run() throws Exception {
		Properties props = new Properties();
		props.load(new FileReader("conf/recommender101.properties"));
		Recommender101 r101 = new Recommender101(props);
		collectStatistics(r101.getDataModel());
	}

	
	/**
	 * Collects and prints the various statistics  
	 * @param dataModel
	 * @throws Exception
	 */
	public void collectStatistics(DataModel dataModel) throws Exception {
		printBasicStatistics(dataModel);
	}
	
	
	/**
	 * Prints the basic statistics such as #users, #items, #ratings, sparsity level
	 * @param dataModel
	 * @throws Exception
	 */
	public void printBasicStatistics(DataModel dataModel) throws Exception {
		System.out.println("Basic data set statistics ");
		System.out.println("-----------------------------");
		System.out.println("#Users: \t\t" + dataModel.getUsers().size());
		System.out.println("#Items: \t\t" + dataModel.getItems().size());
		System.out.println("#Ratings: \t\t" + dataModel.getRatings().size());
		System.out.println("Sparsity: \t\t" + Recommender101Impl.decimalFormat.format((double) dataModel.getRatings().size() / (dataModel.getItems().size() * dataModel.getUsers().size())));
		System.out.println("-----------------------------");
		// Global average
		double globalAverage = Utilities101.getGlobalRatingAverage(dataModel);
		System.out.println("Global avg: \t\t" + Recommender101Impl.decimalFormat.format(globalAverage));
		// Get the median rating
		int median = -1;
		median = Utilities101.getGlobalMedianRating(dataModel);
		System.out.println("Global median: \t\t" + Recommender101Impl.decimalFormat.format(median));
		
		// Rating statistics
		Map<Integer, Integer> frequencies = Utilities101.getRatingFrequencies(dataModel);
		System.out.println("Ratings freqs: \t\t" + frequencies);
		// Get the Gini index of the frequencies
		Map<Integer, Integer> sortedFreqs = Utilities101.sortByValueDescending(frequencies);
		//System.out.println("Sorted: " + sortedFreqs);
		List<Integer> levels = new ArrayList<Integer>();
		for (Integer key: sortedFreqs.keySet()) {
			levels.add(0,key);
		}
		//System.out.println("levels: " + levels);
		long[] bins = new long[levels.size()];
		for (int i = 0; i < levels.size(); i++){
			bins[i] = frequencies.get(levels.get(i));
		}
		/*for ( int i=0; i < levels.size();i++)
		{
			System.out.print("["+i+"|"+bins[i]+"]");
		}
		System.out.println();*/
		double gini = Utilities101.calculateGini(bins);
		System.out.println("Gini of freqs: \t\t" + gini);
		double ngini = Utilities101.calculateNormalizedGini(bins);
		System.out.println("norm. Gini of freqs: \t" + ngini);
		
		// Avg ratings per user and item
		System.out.println("Ratings/user: \t\t" + Recommender101Impl.decimalFormat.format((double) dataModel.getRatings().size() / dataModel.getUsers().size()));
		System.out.println("Ratings/item: \t\t" + Recommender101Impl.decimalFormat.format((double)dataModel.getRatings().size() / dataModel.getItems().size()));
		System.out.println("-----------------------------");
	}
	
 	
}
