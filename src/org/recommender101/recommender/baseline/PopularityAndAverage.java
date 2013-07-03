/** DJ **/
package org.recommender101.recommender.baseline;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.recommender101.data.Rating;
import org.recommender101.recommender.AbstractRecommender;
import org.recommender101.tools.Utilities101;


/**
 * A demo recommender which uses the average item rating for recommendations.
 * Prediction: Predict the average item rating across all users
 * Recommendation: Recommend the most popular items
 * @author DJ
 *
 */

public class PopularityAndAverage extends AbstractRecommender {
	

	/**
	 * Should the user average be used for the prediction?
	 */
	public boolean userAverage;

	/**
	 * Remember the averages
	 */
	Map<Integer, Float> userAverages;
	Map<Integer, Float> itemAverages;
	
	/*
	 * The global average
	 */
	float globalAverage = 0;
	

	/**
	 * The rating count per item (for the popularity-based recommendation)
	 */
	Map<Integer, Integer> ratingCountPerItem = new HashMap<Integer, Integer>();
	
	/**
	 * Should we use the item average for ranking the items
	 */
	public boolean useItemAverageForRecommendation = false;
	
	/**
	 * Should we take into account what the user already knows.
	 */
	public boolean hideKnownItems = true;


	
	
  // =====================================================================================

	/**
	 * Calculates the item average and returns it
	 */
	@Override
	public float predictRating(int user, int item) {
		
		Float result = null;
		
		if (userAverage) {
			result = this.userAverages.get(user);
		}
		else {
			result = this.itemAverages.get(item);
		}
		if (result == null) {
			result = globalAverage;
		}
		
		return result;
		
	}

  // =====================================================================================

		/**
	 * Sort the items by popularity or by item average
	 */
	@Override
	public List<Integer> recommendItems(int user) {
		List<Integer> result = null;
//		System.out.println("Recommending: " + this.useItemAverageForRecommendation);
		if (this.useItemAverageForRecommendation) {
			Map<Integer, Float> predictions = Utilities101.sortByValueDescending(itemAverages);
			predictions = filterElementsByRelevanceThreshold(predictions, user);
			result = new ArrayList<Integer>(predictions.keySet());
//			System.out.println("I have " + result + " entries to return ..");
		}
		else {
			Map<Integer, Integer> sortedPopularities = Utilities101.sortByValueDescending(ratingCountPerItem);
//			System.out.println("The popular ones: " + sortedPopularities);
			result = new ArrayList<Integer>(sortedPopularities.keySet());
		}
		
		// if we should hide things, remove items the user already knows
		if (this.hideKnownItems == true) {
			List<Integer> prunedresult = new ArrayList<Integer>();
			for (Integer item: result) {
				byte rating = getDataModel().getRating(user, item);
				if (rating == -1) {
					prunedresult.add(item);
				}
			}
			return prunedresult;
		}
		else {
			return result;
		}
	}

	// --------------------------------------------------------------
	@Override
	/**
	 * A new init method which uses the utilities class
	 */
	public void init() {
		Set<Rating> ratings = getDataModel().getRatings();
		
		if (userAverage) {
			userAverages = Utilities101.getUserAverageRatings(ratings);
		}
		else {
			itemAverages = Utilities101.getItemAverageRatings(ratings);
		}
		globalAverage = (float) Utilities101.getGlobalRatingAverage(getDataModel());
		
		// count the popularity of the items
		for (Rating r : ratings) {
			Utilities101.incrementMapValue(this.ratingCountPerItem, r.item);
		}
		

	}
	

	// =====================================================================================

	
	/**
	 * Use the user averages instead
	 * @param b
	 */
	public void setUserAverage(String b) {
		this.userAverage = Boolean.parseBoolean(b);
	}
	
	/**
	 * Use the item average for the ranking process
	 * @param b true if the item ranking should be used
	 */
	public void setUseItemAveragesForRecommendation(String b) {
		this.useItemAverageForRecommendation = Boolean.parseBoolean(b);
//		System.out.println("set item avg recommendations.." + this.useItemAverageForRecommendation);
	}
	

	/**
	 * Set this to false if it should be a truely unpersonalized method
	 * @param s
	 */
	public void setHideKnownItems(String s) {
		this.hideKnownItems = Boolean.parseBoolean(s);
	}
	
	@Override
	public int getDurationEstimate() {
		return 1;
	}	

	
	
}
