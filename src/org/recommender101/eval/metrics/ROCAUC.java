package org.recommender101.eval.metrics;

import java.util.ArrayList;
import java.util.List;

import org.recommender101.data.Rating;
import org.recommender101.eval.interfaces.RecommendationlistEvaluator;

/**
 * This metric creates the ROC curve for each user and calculates the
 * corresponding AUC value (area under curve). It returns the arithmetic mean of
 * all computed AUCs.
 * 
 * @author timkraemer
 * 
 */
public class ROCAUC extends RecommendationlistEvaluator {

	/**
	 * A private attribute which holds the number of users for which the AUC has
	 * been calculated
	 */
	private int avgCounter;

	/**
	 * The arithmetic mean of all previously calculated AUC values.
	 */
	private double aucAvg;

	/**
	 * Initialize the metric
	 */
	@Override
	public void initialize() {
		this.avgCounter = 0;
		this.aucAvg = 0;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void addRecommendations(Integer user, List<Integer> list) {
		// Check if list is null or empty
		if (list == null || list.size() == 0)
			return;

//		Debug.log("ROCAUC: AddRecommendations called (user: " + user + ")");

		int relevantItemsInTestSet = 0;
		int nonRelevantItemsInTestSet = 0;

		List<Integer> relevantList = new ArrayList<Integer>();
		for (Rating r : getTestDataModel().getRatingsPerUser().get(user)) {

			if (isItemRelevant(r.item, r.user)) {
				relevantItemsInTestSet++;
				relevantList.add(r.item);
			} else
				nonRelevantItemsInTestSet++;
		}

		if (relevantItemsInTestSet == 0 || nonRelevantItemsInTestSet == 0)
			return;

		/*System.out.println("relevant: " + relevantItemsInTestSet
				+ ", non relevant: " + nonRelevantItemsInTestSet);*/

		double stepSizeX = 1.0 / (double) nonRelevantItemsInTestSet;
		double stepSizeY = 1.0 / (double) relevantItemsInTestSet;

		double lastCoords[] = new double[] { 0.0, 0.0 };
		double auc = 0;


		for (int currItem : list) {
			int rating = getTestDataModel().getRating(user, currItem);

			if (rating == -1) {
				// No rating in test set found, discard
			} else {

				if (relevantList.contains(currItem)) {
					// Correct prediction
					lastCoords[1] += stepSizeY;
				} else {
					// Wrong predicition
					auc += stepSizeX * lastCoords[1];
					lastCoords[0] += stepSizeX;
				}

				// Print coordinates of the ROC curve. This should only be
				// commented in for debugging purposes.
				/*
				 * System.out.println(roundDouble(lastCoords[0]) + ";" +
				 * roundDouble(lastCoords[1]));
				 */
			}

		}

		this.aucAvg = (this.aucAvg * this.avgCounter + auc)
				/ (++this.avgCounter);
	}

	/**
	 * Returns the arithmetic mean of all calculated AUC values.
	 */
	@Override
	public float getEvaluationResult() {

		return (float) this.aucAvg;
	}

	/**
	 * A private helper method which cuts off all decimal places off a double
	 * variable except the first two.
	 * 
	 * @param d
	 *            The double variable.
	 * @return The given variable with all decimal places >2 cut off.
	 */
//	private String roundDouble(double d) {
//		DecimalFormat df = new DecimalFormat("0.00");
//		return df.format(d);
//	}

}
