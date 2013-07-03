package org.recommender101.recommender.extensions.factorizednghbors;

import java.io.FileReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.recommender101.Recommender101;
import org.recommender101.data.Rating;
import org.recommender101.eval.interfaces.EvaluationResult;
import org.recommender101.recommender.AbstractRecommender;
import org.recommender101.tools.Utilities101;

/**
 * A class that implements Koren's factorized neighborhood algorithm (item-item
 * version)
 * 
 * @author Zeynep, Dietmar
 * 
 */
public class FactorizedNeighborhoodRecommender extends AbstractRecommender {

	// Algorithm parameters
	protected int iterations = 100;
	protected double gammaStepSize = 0.08;
	protected double lambdaForRegulation = 0.002;
	protected int nbFactors = 100;

	// some defaults
	protected double lowerBound = -0.00000000001;
	protected double rangeOfRandomValues = 0.01;

	/** Average of Preferences. */
	protected double mAvgOfAllPreferences;

	/** Preference of user u for item i. */
	protected double Rui;

	/** Predicted preference for user u for item i. */
	protected double Pui;

	/** Prediction error for the Preference of user u for item i. */
	protected double Eui;

	/**
	 * Deviation (bu) of User from average. Used for computing buj before
	 * parameters have learned.
	 */
	protected Map<Integer, Double> deviationsOfUserForPrecomputation = new HashMap<Integer, Double>();

	/**
	 * Deviation (bj) of Item from average. Used for computing buj before
	 * parameters have learned.
	 */
	protected Map<Integer, Double> deviationsOfItemsForPrecomputation = new HashMap<Integer, Double>();

	/** Deviations (bu) after learning the Parameter by gradient solver. */
	protected Map<Integer, Double> mapOfBu = new HashMap<Integer, Double>();

	/** Deviations (bi) after learning the Parameter by gradient solver. */
	protected Map<Integer, Double> mapOfBi = new HashMap<Integer, Double>();

	/** Every Item has an Array qi containing the factors */
	protected Map<Integer, double[]> mapOfQiArrays = new HashMap<Integer, double[]>();

	/** Every Item has an Array xi containing the factors */
	protected Map<Integer, double[]> mapOfXiArrays = new HashMap<Integer, double[]>();

	/** Every Item has an Array yi containing the factors */
	protected Map<Integer, double[]> mapOfYiArrays = new HashMap<Integer, double[]>();

	/** Pre-calculate the preferences for each item */
	protected Map<Integer, Set<Rating>> ratingsOfItems = new HashMap<Integer, Set<Rating>>();

	/** Pre-calculate the number of ratings for each item */
	protected Map<Integer, Integer> nbRatingsOfItems = new HashMap<Integer, Integer>();
	


	/** Returns a prediction for a user */
	@Override
	public float predictRating(int user, int item) {
		double buj = 0.0;
		double ruj = 0.0;
		double prediction = Double.NaN;
		double normalizePreferences = 0.0;
		double scalarPuQi = 0.0;
		int numOfPreferences = 0;
		int numOfFactors;
		Integer itemJ;

		double[] qiarray = mapOfQiArrays.get(item);
		if (qiarray == null) {
//			System.err.println("No qi values for " + item + ", cannot make a prediction");
			return Float.NaN;
		}
		
		numOfFactors = mapOfQiArrays.get(item).length;
		numOfPreferences = dataModel.getRatingsOfUser(user).size();
		normalizePreferences = Math.pow(numOfPreferences, -0.5);
		double[] xiArray = mapOfXiArrays.get(item);
		double[] yiArray = mapOfYiArrays.get(item);
		double[] qiArray = mapOfQiArrays.get(item);
		double[] sumXiArray = new double[numOfFactors];
		double[] sumYiArray = new double[numOfFactors];
		double[] puArray = new double[numOfFactors];

		for (Rating r : dataModel.getRatingsOfUser(user)) {
			itemJ = r.item;
			if (itemJ != item) {
				buj = mAvgOfAllPreferences + mapOfBu.get(user)
						+ mapOfBi.get(itemJ);
				ruj = r.rating;
				for (int a = 0; a < numOfFactors; a++) {
					sumXiArray[a] += (ruj - buj) * xiArray[a];
					sumYiArray[a] += yiArray[a];
				}
			}
		}
		// Part of Prediction-Function independent of item.
		for (int a = 0; a < numOfFactors; a++) {
			sumXiArray[a] = normalizePreferences * sumXiArray[a];
			sumYiArray[a] = normalizePreferences * sumYiArray[a];
			puArray[a] = sumXiArray[a] + sumYiArray[a];
		}
		// compute scalarproduct of qi*pu.
		scalarPuQi = 0.0;
		for (int a = 0; a < numOfFactors; a++) {
			scalarPuQi += qiArray[a] * puArray[a];
		}
		
//		System.out.println(scalarPuQi);
		
		Double mapOfBuValue = mapOfBu.get(user);
		Double mapOfBivalue = mapOfBi.get(item);
		
		double bu = 0.0;
		double bi = 0.0;
		if (mapOfBuValue != null) {
			bu = mapOfBuValue;
		}
		if (mapOfBivalue != null) {
			bi = mapOfBivalue;
		}
			
		
		prediction = mAvgOfAllPreferences + bu + bi + scalarPuQi;
//		 System.out.println( "Prediction for User " + user + " and Item: " + item + " is => " + prediction + "\n" );
		return (float) prediction;
	}

	/**
	 * Rank the items by rating prediction
	 */
	@Override
	public List<Integer> recommendItems(int user) {
		return super.recommendItemsByRatingPrediction(user);
	}

	@Override
	public void init() throws Exception {
		
		// Initialize the stats
		this.calculateRatingsPerItem();

		double sumOfAllPreferences = 0;
		int numOfAllPreferences = 0;
		// TODO: Optimize - already done somewhere els.
//		System.out.println("Average");
		for (Integer user : dataModel.getUsers()) {
			for (Rating rating : dataModel.getRatingsOfUser(user)) {
				sumOfAllPreferences += rating.rating;
				numOfAllPreferences++;
			}
		}
		mAvgOfAllPreferences = sumOfAllPreferences / numOfAllPreferences;
//		System.out.println("ItemEffects");
		itemEffects(25.0);
//		System.out.println("UserEffects");
		userEffects(10.0);
		gradientSolver(iterations, gammaStepSize, lambdaForRegulation, nbFactors, this.rangeOfRandomValues);
//		Debug.log("Factorized NB: Initial training done");
	}

	/**
	 * This method computes the deviation of the Preferences by each User from
	 * the average.
	 * 
	 * @param regularizationForBu
	 *            value for regularization.
	 */
	public void userEffects(double regularizationForBu) throws Exception {
		double bu, bi;
		int numOfPrefs;
		double sumUserDeviations;
		Integer item;
		for (Integer user : dataModel.getUsers()) {
			sumUserDeviations = 0;
			for (Rating rating : dataModel.getRatingsOfUser(user)) {
				item = rating.item;
				bi = deviationsOfItemsForPrecomputation.get(item);
				sumUserDeviations += (rating.rating - mAvgOfAllPreferences - bi);
			}
			numOfPrefs = dataModel.getRatingsOfUser(user).size();
			bu = sumUserDeviations / (regularizationForBu + numOfPrefs);
//			if (Double.isNaN(bu)) {
//				System.out.println("Problem in item effects");
//			}

			deviationsOfUserForPrecomputation.put(user, bu);
		}
	}

	/**
	 * Computes the deviation of preferences for each Item from the average.
	 * 
	 * @param regularizationForBi
	 *            Value for regularization.
	 */
	public void itemEffects(double regularizationForBi) {
		double bi = 0.0;
		int numOfPrefs = 0;
		double sumItemDeviations;

		for (Integer item : dataModel.getItems()) {
			sumItemDeviations = 0.0;
			Set<Rating> ratingsOfItem = this.ratingsOfItems.get(item);
			if (ratingsOfItem != null) {
				for (Rating r : ratingsOfItem) {
					sumItemDeviations += (r.rating - mAvgOfAllPreferences);
				}
			}
			Integer nbPrefs = this.nbRatingsOfItems.get(item);
			if (nbPrefs != null) {
				numOfPrefs = nbPrefs;
				bi = sumItemDeviations / (regularizationForBi + numOfPrefs);
			}
//			if (Double.isNaN(bi)) {
//				System.out.println("Problem in item effects");
//			}
			deviationsOfItemsForPrecomputation.put(item, bi);
		}
	}

	/**
	 * Learn the parameter on the DataModel
	 * 
	 * @param iteration
	 *            Number of times the steps are repeated.
	 * @param gammaStepSize
	 *            Amount of step size.
	 * @param lambdaForRegulation
	 *            Amount of regulation.
	 * @param dataModel
	 *            The given data model.
	 * @throws TasteException
	 */
	public void gradientSolver(int iteration, double gammaStepSize,
			double lambdaForRegulation, int numOfFactors,
			double rangeOfRandomValues) throws Exception {
//		System.out.println("Starting gradient solver");
		int numOfPreferences = 0; // of user
		double normalizePreferences = 0.0;
		double bu = 0; // deviation of user u from average.
		double bi = 0; // deviation of item i from average.
		double buj, bui; // baseline estimate for user u and items i and j.
		double ruj; // Preference of user u and item j.
		double scalarPuQi = 0.0;
		// Weight qi and its latent factors for item i.
		double[] qiArray = new double[numOfFactors];
		// Weight xi and its latent factors for item i.
		double[] xiArray = new double[numOfFactors];
		// Weight yi and its latent factors for item i.
		double[] yiArray = new double[numOfFactors];
		double[] sumXiArray = new double[numOfFactors];
		double[] sumYiArray = new double[numOfFactors];
		// Part of prediction function.
		double puArray[] = new double[numOfFactors];
		double sumForGradientStep[] = new double[numOfFactors];
		double randomValue; // Random Value to initialize the vectors/arrays qi,
							// xi and yi.
		boolean containsItem, containsUser;
		Integer item;

		for (int i = 0; i < iteration; i++) {
//			System.out.println("Iteration:" + i);
			for (Integer user : dataModel.getUsers()) {
				numOfPreferences = dataModel.getRatingsOfUser(user).size();
				normalizePreferences = Math.pow(numOfPreferences, -0.5);
				for (int a = 0; a < numOfFactors; a++) {
					sumXiArray[a] = 0.0;
					sumYiArray[a] = 0.0;
					sumForGradientStep[a] = 0.0;
				}
				// Sum explicit and implicit weights.
				for (Rating r : dataModel.getRatingsOfUser(user)) {
					item = r.item;
					containsItem = mapOfXiArrays.containsKey(item);
					if (i == 0 && containsItem == false) {
						for (int a = 0; a < numOfFactors; a++) {
							randomValue = Math.round(-rangeOfRandomValues + Math.random() * 2 * rangeOfRandomValues);
							xiArray[a] = randomValue;
						}
						for (int a = 0; a < numOfFactors; a++) {
							randomValue = Math.round(-rangeOfRandomValues + Math.random() * 2 * rangeOfRandomValues);
							yiArray[a] = randomValue;
						}
						mapOfXiArrays.put(item, xiArray);
						mapOfYiArrays.put(item, yiArray);
					}
					xiArray = mapOfXiArrays.get(item);
					yiArray = mapOfYiArrays.get(item);
					// Preference for user u and item j.
					ruj = r.rating;
					// Baseline estimate for user u and item j.
					buj = mAvgOfAllPreferences
							+ deviationsOfUserForPrecomputation.get(user)
							+ deviationsOfItemsForPrecomputation.get(item);
					for (int a = 0; a < numOfFactors; a++) {
						sumXiArray[a] += (ruj - buj) * xiArray[a];
						sumYiArray[a] += yiArray[a];
					}
				}
				// Part of Prediction-Function which is independent of item i.
				for (int a = 0; a < numOfFactors; a++) {
					sumXiArray[a] = normalizePreferences * sumXiArray[a];
					sumYiArray[a] = normalizePreferences * sumYiArray[a];
					puArray[a] = sumXiArray[a] + sumYiArray[a];
				}
				for (Rating r : dataModel.getRatingsOfUser(user)) {
					item = r.item;
					// Pre-initialize QiMap, BiMap and BuMap.
					containsItem = mapOfQiArrays.containsKey(item);
					containsUser = mapOfBu.containsKey(user);
					if (i == 0) {
						if (containsItem == false) {
							for (int a = 0; a < numOfFactors; a++) {
								randomValue = (-rangeOfRandomValues + Math
										.random() * 2 * rangeOfRandomValues);
								qiArray[a] = randomValue;
							}
							mapOfQiArrays.put(item, qiArray);
							randomValue = -rangeOfRandomValues + Math.random() * 2 * rangeOfRandomValues; 
							mapOfBi.put(item,randomValue);
						}
						if (containsUser == false) {
							randomValue = -rangeOfRandomValues + Math.random() * 2 * rangeOfRandomValues; 
							mapOfBu.put(user,randomValue);
						}
					}
					qiArray = mapOfQiArrays.get(item);
					scalarPuQi = 0.0;
					for (int a = 0; a < numOfFactors; a++) {
						// Compute scalarproduct of qi*pu.
						scalarPuQi += (qiArray[a] * puArray[a]);
					}
					// Prediction for user u and item i.
					Pui = mAvgOfAllPreferences + mapOfBu.get(user)
							+ mapOfBi.get(item) + scalarPuQi;
					
					if (Double.isNaN(Pui) || Double.isInfinite(Pui)) {
//						System.out.println("Training pediction: " + Pui);
//						System.out.println("mavg: " + mAvgOfAllPreferences);
//						System.out.println("bu: " + mapOfBu.get(user));
//						System.out.println("bi: " + mapOfBi.get(item));
//						System.out.println("scalar: " + scalarPuQi);
//						System.out.println("Correcting prediction: ");
						Pui = lowerBound;
						
					}
//					else {
//						System.out.println("Pui " + Pui);
//						System.out.println("bu: " + mapOfBu.get(user));
//						System.out.println("bi: " + mapOfBi.get(item));
//						System.out.println("scalar: " + scalarPuQi);
//					}
					
					// Preference from user u for item i
					Rui = r.rating;
					// Prediction-Error for user u and item i.
					Eui = Rui - Pui;
					// Sum for Gradient-Step on xi and yi.
					for (int a = 0; a < numOfFactors; a++) {
						sumForGradientStep[a] = sumForGradientStep[a]
								+ (Eui * qiArray[a]);
					}
					// Gradient-Step on qi.
					for (int a = 0; a < numOfFactors; a++) {
						if (qiArray[a] > lowerBound) {
							qiArray[a] = qiArray[a]
									+ gammaStepSize
									* (Eui * puArray[a] - lambdaForRegulation
											* qiArray[a]);
						} else
							qiArray[a] = lowerBound;
					}
					mapOfQiArrays.put(item, qiArray);
					// Gradient-Step on bu.
					bu = mapOfBu.get(user) + gammaStepSize * (Eui - lambdaForRegulation * mapOfBu.get(user));
					
					if (Double.isNaN(bu) && Double.isInfinite(bu)) {
//						System.out.println("putting wrong value in gradient step .." + bu);
//						System.out.println("bu: " + mapOfBu.get(user));
//						System.out.println("Eui: " + Eui);
						bu = lowerBound;
						
					}
					mapOfBu.put(user, bu);
					// Gradient-Step on bi.
					bi = mapOfBi.get(item) + gammaStepSize
							* (Eui - lambdaForRegulation * mapOfBi.get(item));

					if (Double.isInfinite(bi)) {
						System.err.println("Correcting to small values");
						bi = lowerBound;
					}
					
					mapOfBi.put(item, bi);
				}
				for (Rating r : dataModel.getRatingsOfUser(user)) {
					item = r.item;
					xiArray = mapOfXiArrays.get(item);
					yiArray = mapOfYiArrays.get(item);
					// Baseline estimate for user u and item i.
					bui = mAvgOfAllPreferences + mapOfBu.get(user)
							+ mapOfBi.get(item);
					for (int a = 0; a < numOfFactors; a++) {
						// Gradient-Step on xi.
						if (xiArray[a] > lowerBound) {
							xiArray[a] = xiArray[a]
									+ gammaStepSize
									* (normalizePreferences * (Rui - bui)
											* sumForGradientStep[a] - lambdaForRegulation
											* xiArray[a]);
						} else
							xiArray[a] = lowerBound;
						// Gradient-Step on yi.
						if (yiArray[a] > lowerBound) {
							yiArray[a] = yiArray[a]
									+ gammaStepSize
									* (normalizePreferences
											* sumForGradientStep[a] - lambdaForRegulation
											* yiArray[a]);
						} else
							yiArray[a] = lowerBound;
					}
					mapOfXiArrays.put(item, xiArray);
					mapOfYiArrays.put(item, yiArray);
				}
			}
		}
//		System.out.println("Got " + mapOfQiArrays.size() + " item vectors");
	}

	/**
	 * Calculate the list of preferences per item
	 * 
	 * @throws Exception
	 */
	protected void calculateRatingsPerItem() throws Exception {
		for (Rating r : dataModel.getRatings()) {
			Set<Rating> ratingsOfItem = this.ratingsOfItems.get(r.item);
			if (ratingsOfItem == null) {
				ratingsOfItem = new HashSet<Rating>();
			}
			this.ratingsOfItems.put(r.item, ratingsOfItem);
			ratingsOfItem.add(r);
			Utilities101.incrementMapValue(this.nbRatingsOfItems, r.item);
		}
	}

	/**
	 * A test method
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			System.out.println("-- Koren test starting");

			Properties props = new Properties();
			props.load(new FileReader("conf/recommender101.properties"));

			Recommender101 r101 = new Recommender101(props);
			r101.runExperiments();
			System.out.println(r101.getLastDetailedResults());

			List<EvaluationResult> finalResult = r101.getLastResults();
			r101.printSortedEvaluationResults(finalResult);

		} catch (Exception e) {
			e.printStackTrace();
		}

		System.out.println("-- Program ended");
		System.out.println("-- Test ended");

	}

	/**
	 * Number of iterations for training
	 * 
	 * @param iterations
	 */
	public void setIterations(String iterations) {
		this.iterations = Integer.parseInt(iterations);
	}

	/**
	 * Step size for learning
	 * 
	 * @param gammaStepSize
	 */
	public void setGamma(String gammaStepSize) {
		this.gammaStepSize = Double.parseDouble(gammaStepSize);
	}

	/**
	 * Regularization value
	 * 
	 * @param lambdaForRegulation
	 */
	public void setLambda(String lambdaForRegulation) {
		this.lambdaForRegulation = Double.parseDouble(lambdaForRegulation);
	}

	/**
	 * Number of factors for factorization
	 * 
	 * @param n
	 */
	public void setNbFactors(String n) {
		this.nbFactors = Integer.parseInt(n);
	}
	
	@Override
	public int getDurationEstimate() {
		return 3;
	}

}
