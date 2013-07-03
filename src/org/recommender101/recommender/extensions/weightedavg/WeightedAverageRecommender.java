package org.recommender101.recommender.extensions.weightedavg;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.recommender101.data.Rating;
import org.recommender101.eval.impl.Recommender101Impl;
import org.recommender101.recommender.AbstractRecommender;
import org.recommender101.tools.Utilities101;

/**
 * An algorithm which calculates a weighted combination of user and item
 * averages
 * 
 * @author Dietmar
 * 
 */
public class WeightedAverageRecommender extends AbstractRecommender {

	/**
	 * The item averages
	 */
	Map<Integer, Float> itemAverages = new HashMap<Integer, Float>();

	/**
	 * The user averages
	 */
	Map<Integer, Float> userAverages = new HashMap<Integer, Float>();

	/**
	 * The global average
	 */
	double globalAverage = 0.0;

	/** user weights learned in the optimization step */
	Map<Integer, Float> userweights = new HashMap<Integer, Float>();

	/** item weights learned in the optimization step */
	Map<Integer, Float> itemweights = new HashMap<Integer, Float>();

	double initUserWeight = 0.5;
	double initItemweight = 0.5;

	/** parameters for gradient descent optimization */
	int iterations = 20;
	double gammaStepSize = 0.002;
	double lambdaForRegulation = 0.08;
	

	@Override
	public float predictRating(int user, int item) {
		float result = Float.NaN;
		Float userPred = Float.NaN;
		Float itemPred = Float.NaN;

		userPred = this.userAverages.get(user);
		itemPred = this.itemAverages.get(item);

		if (userPred == null && itemPred == null) {
			return (float) this.globalAverage;
		}
		if (userPred == null) {
			return itemPred;
		}
		if (itemPred == null) {
			return userPred;
		}
		
		result = (2 * userPred * itemPred) / (userPred + itemPred); // harmonic
																	// mean as a
																	// default
		// If we have weights - use them
		if (userweights.get(item) != null && itemweights.get(user) != null) {
			result = userPred * userweights.get(item) + itemPred
					* itemweights.get(user);
		}
		
		
		if (result > Recommender101Impl.MAX_RATING) {
			result = Recommender101Impl.MAX_RATING;
		} else if (result < Recommender101Impl.MIN_RATING) {
			result = Recommender101Impl.MIN_RATING;
		}
//		System.out.println("Predicting: " + result);
		
		return result;
	}

	@Override
	public List<Integer> recommendItems(int user) {
		// Use the standard method based on the rating prediction
		return recommendItemsByRatingPrediction(user);
	}

	@Override
	public void init() throws Exception {
		


		// Calculates the averages for the data model
		this.itemAverages = Utilities101.getItemAverageRatings(getDataModel()
				.getRatings());
		this.userAverages = Utilities101.getUserAverageRatings(getDataModel()
				.getRatings());
		this.globalAverage = Utilities101
				.getGlobalRatingAverage(getDataModel());
		gradientSolver(iterations, gammaStepSize, lambdaForRegulation);

	}

	// =====================================================================================

	/**
	 * Calculates optimal weights for the two recommenders
	 * 
	 * @param iteration
	 *            number of iterations
	 * @param gammaStepsize
	 *            step size in the correction step
	 * @param lambdaForRegulation
	 *            penalty
	 * @throws Exception
	 */
	public void gradientSolver(int iterations, double gamma, double lambda)
			throws Exception {

		// System.out.println("Using lambda: " + lambda);
		/** Preference of user u for item i. */
		double rui;
		/** Predicted preference for user u for item i. */
		double pui;
		/** Prediction error for the Preference of user u for item i. */
		double eui;

		float userWeight = Float.NaN; // deviation of user u from average.
		float itemWeight = Float.NaN; // deviation of item i from average.
		double sumForGradientStep;

		for (int i = 0; i < iterations; i++) {
			// System.out.println("Gradient iteration: "+(i+1));
			for (Rating r : dataModel.getRatings()) {
				sumForGradientStep = 0.0;

				// Pre-initialize with a random number giving
				if (i == 0) {
					userweights
							.put(r.user, (float) (this.initUserWeight + Math
									.random() * 0.01));
					itemweights
							.put(r.item, (float) (this.initItemweight + Math
									.random() * 0.01));
				}
				double estForUser = this.userAverages.get(r.user);
				double estForItem = this.itemAverages.get(r.item);
				pui = itemweights.get(r.item) * estForItem
						+ userweights.get(r.user) * estForUser;
				rui = r.rating;
				eui = rui - pui;

				// Sum for Gradient-Step on xi and yi.
				sumForGradientStep = sumForGradientStep + eui;

				// Gradient-Step on item weights.
				itemWeight = (float) (itemweights.get(r.item) + gamma
						* (eui - lambda * itemweights.get(r.item)));
				itemweights.put(r.item, itemWeight);

				// Gradient-Step on user weights.
				userWeight = (float) (userweights.get(r.user) + gamma
						* (eui - lambda * userweights.get(r.user)));
				userweights.put(r.user, userWeight);
			}
		}

//		System.out.println("Gradient solver finished");
//		System.out.println(MoreUtilities101.printMapSorted(this.userweights));
//		System.out.println("--------------");
//		System.out.println(MoreUtilities101.printMapSorted(this.itemweights));
	}


	public void setUserWeight(String w) {
		this.initUserWeight = Double.parseDouble(w);
		this.initItemweight = 1 - initUserWeight;
	}
	
	
	@Override
	public int getDurationEstimate() {
		return 3;
	}

}
