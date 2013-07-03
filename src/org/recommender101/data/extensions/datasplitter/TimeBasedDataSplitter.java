/** DJ **/
package org.recommender101.data.extensions.datasplitter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.recommender101.data.DataModel;
import org.recommender101.data.DataSplitter;
import org.recommender101.data.Rating;
import org.recommender101.data.extensions.dataloader.DefaultDataLoaderWithTimeStamp;
import org.recommender101.tools.Utilities101;

public class TimeBasedDataSplitter extends DataSplitter {

	// =====================================================================================
	/**
	 * This method sorts the ratings by time stamp and provides one data split where
	 * the last split contains the latest ratings
	 */
	
	@SuppressWarnings("unchecked")
	@Override
	public List<Set<Rating>> splitData(DataModel dataModel) throws Exception {
		// Split the data set according to the given percentage
		// Check if the time stamp info is there

		List<Set<Rating>> result = new ArrayList<Set<Rating>>();
		Set<Rating> trainingSet = new HashSet<Rating>();
		Set<Rating> testSet = new HashSet<Rating>();
		result.add(trainingSet); 
		result.add(testSet);

		Map<Rating, Long> timestamps = (Map<Rating, Long>) dataModel
						.getExtraInformation(DefaultDataLoaderWithTimeStamp.DM_EXTRA_INFO_TIMESTAMP_KEY);
		if (timestamps == null) {
			throw new Exception( "TimeBasedDataSplitter: No timestamp information available");
		}
		// For each user, retain testPercentage ratings
		Set<Rating> userRatings = null;
		for (Integer user : dataModel.getUsers()) {
			// get a copy of the user ratings
			userRatings = new HashSet<Rating>(dataModel.getRatingsPerUser().get(user));
			double ratingsToKeep = userRatings.size() * (testPercentage / (double) 100);
			double ratingsToRemove = userRatings.size() - ratingsToKeep;
			ratingsToKeep = Math.ceil(ratingsToKeep);
			// create a map for the user
			Map<Rating, Long> userRatingsWithTimeStamp = new HashMap<Rating, Long>();
			for (Rating r : userRatings) {
				userRatingsWithTimeStamp.put(r, timestamps.get(r));
			}
			// Sort in descending order or timestamp
			Map<Rating, Long> sortedRatings = Utilities101.sortByValueDescending(userRatingsWithTimeStamp);
			int counter = 1;
			for (Rating r: sortedRatings.keySet()) {
				if (counter <= ratingsToRemove) {
					// The newest go to the testset
					testSet.add(r);
				}
				else {
					trainingSet.add(r);
				}
				counter++;
			}
//			System.out.println("items in the training set " + trainingSet.size());
//			System.out.println("items in the test set " + testSet.size());
//			System.out.println("Training " + trainingSet);
//			System.out.println("test: " + testSet);

		}
	 
	 return result;
	}

	// =====================================================================================


	/**
	 * Remember the size (in percent) of the training split
	 */
	int testPercentage = 0;
	
	// =====================================================================================

	public void setTestPercentage(String n) {
		testPercentage = Integer.parseInt(n);
	}
	
	
}
