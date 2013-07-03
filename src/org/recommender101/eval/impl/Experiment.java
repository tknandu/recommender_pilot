/** DJ **/
package org.recommender101.eval.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.recommender101.data.DataModel;
import org.recommender101.data.Rating;
import org.recommender101.eval.interfaces.EvaluationResult;
import org.recommender101.eval.interfaces.Evaluator;
import org.recommender101.eval.interfaces.PredictionEvaluator;
import org.recommender101.eval.interfaces.RecommendationlistEvaluator;
import org.recommender101.recommender.AbstractRecommender;
import org.recommender101.tools.ClassInstantiator;
import org.recommender101.tools.Debug;
import org.recommender101.tools.Utilities101;

/**
 * A wrapper for experiments. An experiment is based on a data model and
 * comprises several measurements (evaluators) to be performed. The experiments
 * are started by the cross-validation component.
 * 
 * @author DJ
 * 
 */
public class Experiment {

	/**
	 * A handle to the recommender algorithm
	 */
	AbstractRecommender recommender;

	/**
	 * The internally used data model
	 */
	DataModel dataModel;

	/**
	 * A pointer to the evaluators
	 */
	List<Evaluator> evaluators;

	/**
	 * The different types of evaluators
	 */
	List<PredictionEvaluator> predictionMetrics = new ArrayList<PredictionEvaluator>();

	/**
	 * List metrics
	 */
	List<RecommendationlistEvaluator> listMetrics = new ArrayList<RecommendationlistEvaluator>();

	/**
	 * recommendation metrics
	 */
	List<Evaluator> recommendationMetrics = new ArrayList<Evaluator>();

	/**
	 * A list of original names with parameters
	 */
	Map<Object, String> evaluatorNames;

	/**
	 * A counter indicating the evaluation round
	 */
	int evaluationRound;

	// =====================================================================================

	/**
	 * A constructor that accepts a recommender and a data model as well as a set
	 * of evaluators
	 * 
	 * @param recommender
	 * @param dataModel
	 * @param evals
	 *          the evaluator classes
	 */
	@SuppressWarnings("unchecked")
	public Experiment(AbstractRecommender recommender,
			DataModel trainingDataModel, DataModel testDataModel, String evals,
			int evalRound) throws Exception {
		super();
		this.recommender = recommender;
		this.evaluators = new ArrayList<Evaluator>();
		this.dataModel = testDataModel;
		this.evaluationRound = evalRound;

		this.evaluatorNames = new HashMap<Object, String>();

		// create a new evaluator instance for each evaluator
		List<Object> evalObjects = ClassInstantiator
				.instantiateClassesByProperties(evals);
		for (Object obj : evalObjects) {
			evaluators.add((Evaluator) obj);
		}

		for (Evaluator e : evaluators) {
			// System.out.println("evaluator: " + e);
			e.setTrainingDataModel(trainingDataModel);
			e.setTestDataModel(testDataModel);
			e.initialize();
			e.setRecommenderName(recommender.toString());
			e.setRecommender(recommender);

			if (e instanceof PredictionEvaluator) {
				predictionMetrics.add((PredictionEvaluator) e);
			} else if (e instanceof RecommendationlistEvaluator) {
				listMetrics.add((RecommendationlistEvaluator) e);
			}// TODO: Other types of evaluators
		}
	}
	
	
	

	// =====================================================================================

	/**
	 * Runs an evaluation for an algorithm using different metrics
	 * 
	 * @return
	 */
	List<EvaluationResult> runExperiments() {
		
		List<EvaluationResult> result = new ArrayList<EvaluationResult>();
		
		// DEBUGGING: Stop at n ratings, users
		int maxRatingPredictions = 0;
		int maxRecommendations = 0;
		
		if (Recommender101Impl.properties != null) {
			
			String maxRatingPredictionsStr = Recommender101Impl.properties.getProperty("Debug.MaxRatingPredictions");
			if (maxRatingPredictionsStr != null) {
				maxRatingPredictions = Integer.parseInt(maxRatingPredictionsStr);
			}
			String maxRecommendationsStr = Recommender101Impl.properties.getProperty("Debug.MaxRecommendations");
			if (maxRecommendationsStr != null) {
				maxRecommendations = Integer.parseInt(maxRecommendationsStr);
			}
		}
		
		// --------------------------------------------------------------
		// If we have a given-n configuration, we will only evaluate these users
		if (Recommender101Impl.givenNConfiguration != null) {
			Set<Integer> usersToTest = CrossValidationRunner.givenNTestUsers.get(dataModel);
			if (usersToTest == null) {
				System.err.println("Got a givenN-Configuration but no set of test users");
			}
			else {
				System.out.println("Have to test " + usersToTest.size() + " users in given-N configuration");
				Set<Integer> usersCopy = new HashSet<Integer>(dataModel.getUsers());
				for (Integer user : usersCopy) {
					if (!usersToTest.contains(user)) {
						dataModel.removeUserWithRatings(user);
					}
				}
			}
					
		}
		// ------------------------------------------------
		// If there are prediction metrics -> generate predictions and send them to the prediction
		// evaluators
		if (predictionMetrics.size() > 0) {
			Debug.log("------------------------------------");
			Debug.log("Starting to measure prediction metrics for: " + this.recommender.getConfigurationFileString());
			Debug.log("------------------------------------");
			
			int counter = 0;
			
			// --------------------------------------------------------------
			
			int tenpercent = dataModel.getRatings().size() / 10;
			
			for (Rating r : this.dataModel.getRatings()) {
				float prediction = Float.NaN;
				prediction = recommender.predictRating(r.user, r.item);
				// Apply clamping.
				prediction = Utilities101.applyRatingBounds(prediction);
				
				
				// Note that we also pass NaN predictions, which should not be counted
				// by the metric
				// Iterate over all prediction metrics
				for (PredictionEvaluator e : predictionMetrics) {
					e.addTestPrediction(r, prediction);
				}
				counter++;
//				System.out.println("----------------- Counter: " + counter + " max: " + maxRatingPredictions);
				if (counter % tenpercent == 0) {
//					System.out.println("Completed " + counter + " predictions");
//					Debug.log("Experiment completed: " + Math.round(((counter  / (double) dataModel.getRatings().size() * 100))) + " %");
				}
				if (maxRatingPredictions > 0 && counter > maxRatingPredictions) {
					break;
				}
			}
			// We are through with all ratings. Add the results we have to the result collector
			for (PredictionEvaluator e : predictionMetrics) {
				result.add(new EvaluationResult(recommender.getConfigurationFileString(),e.getConfigurationFileString(), e.getPredictionAccuracy()));				
			}
		}
		
		if (listMetrics.size() > 0) {
			Debug.log("------------------------------------");
			Debug.log("Starting to measure list metrics for: " + this.recommender.getConfigurationFileString());
			Debug.log("------------------------------------");
			
			int counter = 0;
			int tenpercent = dataModel.getRatings().size() / 10;
			
			Set<Integer> testUsers = new HashSet<Integer>();
			for (Rating r : this.dataModel.getRatings()) {
				testUsers.add(r.user);
			}

			for (Integer user: testUsers) {
				List<Integer> recommendedList = recommender.recommendItems(user);
				
				for (RecommendationlistEvaluator e : listMetrics) {
					e.addRecommendations(user, recommendedList);
				}
				counter++;
				if (counter % tenpercent == 0) {
//					System.out.println("Completed " + counter + " recommendations");
					Debug.log("Experiment completed: " + Math.round(((counter  / (double) testUsers.size() * 100))) + " %");
				}
				// Debugging
				if (maxRecommendations > 0 && counter > maxRecommendations) {
					break;
				}
			}

			// We are through with all ratings. Add the results we have to the result collector
			for (RecommendationlistEvaluator e : listMetrics) {
				result.add(new EvaluationResult(recommender.getConfigurationFileString(),e.getConfigurationFileString(), e.getEvaluationResult()));				
			}
		}
		/**
		 * Remove the pointer to the recommender object
		 */
		recommender = null;
		return result;
	}
		

	// =====================================================================================

	/**
	 * A string representation of the experiment
	 */
	public String toString() {
		String result = "Experiment: \nRecommender class:" + recommender.getClass()
				+ "\nEvaluators:";
		for (Evaluator e : evaluators) {
			result += "\n\t" + e.getClass() + "\n";
		}
		return result;
	}

	// =====================================================================================

	/**
	 * Returns the evaluation round of the experiment
	 * 
	 * @return the round
	 */
	public int getEvaluationRound() {
		return evaluationRound;
	}

}
