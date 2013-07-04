package org.recommender101.eval.metrics;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.recommender101.data.Rating;

/**
 * Implements variants of the precision metric
 * @author dietmar
 */

public class Precision extends PrecisionRecall {

	/**
	 * Increments the statistics based on the lit elements
	 */
	@Override
	public void addRecommendations(Integer user, List<Integer> list) {
		
		if (list.size() == 0) {
			System.out.println("Got an empty list - doing nothing here");
			return;
		}
		
		// Special treatment required for position in random set
		// Standard behavior first.
		if (targetSetType != PrecisionRecall.evalTypes.positioninrandomset) {
			// If there are no such items for the user, return
			Set<Rating> ratingsOfUser = getTestDataModel().getRatingsPerUser().get(user);
			int truePositives = nbOftruePositives(ratingsOfUser);
			// Cannot measure precision
			if (truePositives == 0) {
				return;
			}
			// We're still here - get a suitable list of recommendations from the full list and add the precision
			double precision = calculatePrecisionPerUser(user, list, truePositives, considerOnlyItemsRatedByUser);
			if (!Double.isNaN(precision)) {
				accumulatedValue += precision;
				counter++;
			}
		}
		else {
			// special treatment here. Calculate recall.
			// Set flag to true to calculate precision, which can be 
			// obtained from the precision value
			addRecallForRandomSetProcedure(user, true, list);
		}
	}
	
	 // =====================================================================================
		/**
		 * Calculates the precision per user
		 * @param user the user
		 * @param list the recommendation list
		 * @param useravg the user average
		 * @param truePositives the number of existing true positives
		 * @return the precision for this user
		 */
		private double calculatePrecisionPerUser(Integer user, 
												 List<Integer> list,
												 int truePositives, 
												 boolean considerOnlyItemsRatedByUser) {
			List<Integer> finalRecommendation = new ArrayList<Integer>();
			int hitCounter = 0;
			int recCounter = 0;
			byte rating = -1;
			
			/**
			 * Special case: No recommendations but true positives exist. Precision is 0.
			 */
//			System.out.println("Recommendation list size: " + list.size());
			if (list.size() == 0 && truePositives > 0) {
				return 0.0;
			}
			// Go through the list of recommendations and filter out what is not relevant
			for (Integer item: list) {
				// If we have a rating for the item in the test set, add it to the true recommendations
				rating = getTestDataModel().getRating(user, item);
				if (considerOnlyItemsRatedByUser) {
					if (rating != -1) {
						// OK, we recommend it
						recCounter++;
						finalRecommendation.add(item);
						// if this is a hit, we count it
						if (isItemRelevant(item, user)) {
							hitCounter++;
						}
					}
				}
				else {
					// Always add the recommendation
					recCounter++;
					finalRecommendation.add(item);
					// if this is a hit, we count it
					if (isItemRelevant(item, user)) {
						hitCounter++;
					}
				}
				if (recCounter >= getTopN()) {
//					System.out.println("Enough items in list");
					break;
				}
			}
			// Calculate the precision depending on the true number of positives 
			// DJ: wrong -> what if there are more true positives than the list length
			// perhaps we should take at most the true positive number as upper limit?
			// divisor = Math.min(truePositives,this.getTopN());
			// double precision = hitCounter / (double) truePositives;

			// If the list is shorter than topN, use the shorter entry
			int divisor = Math.min(finalRecommendation.size(),this.getTopN());
			if (divisor == 0) {
				return 0;
			} 
			double precision = hitCounter / (double) divisor;
			
//			System.out.println("User " + user + " Recommended: " + finalRecommendation.size() + " items " + (this.getConfigurationFileString()));
//			System.out.println("Nb hits: " + hitCounter);
//			System.out.println("truepos: " + truePositives);
//			System.out.println("falsecounter: " + falseCounter);
//			System.out.println("divisor: " + divisor);
//			System.out.println("Precision: " + precision);

			// Give it in percent
			return precision;
		}


}
