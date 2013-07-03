/** DJ **/
package org.recommender101.recommender.baseline;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.recommender101.data.DataModel;
import org.recommender101.data.DefaultDataLoader;
import org.recommender101.data.Rating;
import org.recommender101.recommender.AbstractRecommender;
import org.recommender101.tools.Debug;
import org.recommender101.tools.Utilities101;

/**
 * Implements the most common baseline. A nearest neighbor method (user/user, item/item) with
 * Pearson correlation or Cosine Similarity as a metric
 * @author DJ
 *
 */
public class NearestNeighbors extends AbstractRecommender {

	
	public Map<Integer, Float> averages;
	
	// Number of neighbors to consider (default 30)
	int nbNeighbors = 30;
	
	// The minimum similarity threshold
	double simThreshold = 0.0;
	
	// Some minimum overlap (co-rated items)
	int minRatingOverlap = 3;
	
	/**
	 * As a default, we will use the user-based method
	 */
	boolean itemBased = false;
	
	/**
	 * As a default we will use Pearson similarity
	 */
	boolean useCosineSimilarity = false;
	
	/**
	 * The minimum number of neighbors we need
	 */
	int minNeighbors = 1;
	
	/**
	 * Stores the similarities user-id-> map of other users and their similarities
	 */ 
	public Map<Integer, Map<Integer, Double>> theSimilarities = new HashMap<Integer, Map<Integer, Double>>();


	public Map<Integer, Set<Rating>> ratingsPerItem;
	
	
	/**
	 * Predict the rating based on the neighbors opinions.
	 * Use a classical weighting scheme and n neighbors
	 */
	@Override
	public synchronized float predictRating(int user, int item) {
		// Iterate over all users and rank them
		// A map of similarities
		
		Map<Integer, Double> similarities;
		if (itemBased) {
			similarities = this.theSimilarities.get(item);
		}
		else {
			similarities = this.theSimilarities.get(user);
		}
		// Check if we have enough neighbors
		if (similarities == null || (similarities.size() < this.minNeighbors)) {
			return Float.NaN;
		}
		// The prediction function.
		// Take the user's average and add the weighted deviation of the neighbors.
		double totalSimilarity = 0;
		
		Float objectAverage;
		if (itemBased) {
			objectAverage = this.averages.get(item);
			if (objectAverage == null) {
//				System.err.println("NearestNeighbors: There's no average rating for item " + item);
				return Float.NaN;
			}
		}
		else {
			objectAverage = averages.get(user);
			if (objectAverage == null) {
//				System.err.println("NearestNeighbors: There's no average rating for user " + item);
				return Float.NaN;
			}
		}
		
		
		// go through all the neighbors 
		int cnt = 0;
		double totalBias = 0;
		for (Integer otherObject : similarities.keySet()) {

			float neighborRating;
			if (itemBased) {
				neighborRating = dataModel.getRating(user, otherObject);
			}
			else {
//				System.out.println("Getting other object rating " + otherObject + "," + item);
				neighborRating = dataModel.getRating(otherObject, item);
			}
			if (!Float.isNaN(neighborRating) && neighborRating != -1) {
				float neighborBias = neighborRating - averages.get(otherObject); 
				neighborBias = (float) (neighborBias * similarities.get(otherObject));
				totalBias += neighborBias;
				totalSimilarity += similarities.get(otherObject);
				// enough neighbors
				cnt++;
				if (cnt >= this.nbNeighbors) {
					break;
				}
			}
		}
		
		double result = objectAverage + (totalBias / totalSimilarity);
		return (float) result;
	}

	// =====================================================================================
	/**
	 * Returns the set of ratings of a given item id 
	 * @param itemid the itemid
	 * @return the ratings, or null if no ratings exist
	 */
	public Set<Rating> getRatingsPerItem(Integer itemid) {
		return ratingsPerItem.get(itemid);
	}

	// =====================================================================================

	/**
	 * Pre-calculates the ratings per item from the data model
	 * @return the map of item-ids to ratings
	 */
	public Map<Integer, Set<Rating>> calculateRatingsPerItem() {
		Map<Integer, Set<Rating>> result = new HashMap<Integer, Set<Rating>>();
		
		Set<Rating> ratings = dataModel.getRatings();
		
		Set<Rating> ratingsOfItem;
		for (Rating r : ratings) {
			ratingsOfItem = result.get(r.item);
			if (ratingsOfItem == null) {
				ratingsOfItem = new HashSet<Rating>();
				result.put(r.item,ratingsOfItem);
			}
			ratingsOfItem.add(r);
		}
		return result;
	}

	// =====================================================================================
	
	/**
	 * This method recommends items.
	 */
	@Override
	public List<Integer> recommendItems(int user) {
//		Debug.log("UserBasedKnn: Recommending items for : " + user);
		// Use the standard method based on the rating prediction
		return recommendItemsByRatingPrediction(user);
	}

	/**
	 * Initialization: Compute the user averages
	 */
	@Override
	public void init() throws Exception {
//		System.out.println("Getting the averages avgs");
		if (itemBased) {
			averages = Utilities101.getItemAverageRatings(dataModel.getRatings());
			ratingsPerItem = calculateRatingsPerItem();
//			System.out.println("ratings per item: " + ratingsPerItem + "\n: count:" + ratingsPerItem.size());
		}
		else {
			averages = dataModel.getUserAverageRatings();
		}
//		System.out.println(averages);
//		this.similarities = new HashMap<String, Double>();
		// Pre-compute the similarities between all users in the test set
		// Pre-compute the similarities between all users first
		double sim = Double.NaN;
		
		int similaritiesToCompute; 
		List<Integer> objects;
		if (itemBased) {
			similaritiesToCompute = (dataModel.getItems().size() * dataModel.getItems().size())/2;
			objects = new ArrayList<Integer>(dataModel.getItems());
		}
		else {
			similaritiesToCompute = (dataModel.getUsers().size() * dataModel.getUsers().size())/2; 
			objects = new ArrayList<Integer>(dataModel.getUsers());
		}
		Debug.log("NearestNeighbors: Calculating up to " + similaritiesToCompute + " similarities in the test set.. This may take some time.");
	
		long start = System.currentTimeMillis();
		
		// sort in ascending order
		Collections.sort(objects);
		Integer[] objectArr = (Integer[]) objects.toArray(new Integer[objects.size()]);
		int counter = 0;
		int tenpercent = similaritiesToCompute / 10;
		for (int i=0;i<objectArr.length;i++) {
			for (int j=i+1;j<objectArr.length;j++) {
				counter++;
				if (counter % tenpercent == 0) {
					Debug.log("Similarity computation at : " + Math.round(((counter  / (double) similaritiesToCompute * 100))) + " %");
				}
				if (objectArr[i] != null && objectArr[j] != null) { // both must exist...
					sim = similarity(objectArr[i], objectArr[j]);
					if (!Double.isNaN(sim)) {
						if (sim > simThreshold) {
//						System.out.println("Putting: " + userArr[i] + ":" + userArr[j] + ":" + sim);
//							this.similarities.put(userArr[i] + ":" + userArr[j], sim);
							
							Map<Integer, Double> objectSimilarites1 = theSimilarities.get(objectArr[i]);
							if (objectSimilarites1 == null) {
								objectSimilarites1 = new HashMap<Integer, Double>();
								theSimilarities.put(objectArr[i], objectSimilarites1);
							}
							objectSimilarites1.put(objectArr[j], sim);
							
							// Copy things
							Map<Integer, Double> objectSimilarites2 = theSimilarities.get(objectArr[j]);
							
							if (objectSimilarites2 == null) {
								objectSimilarites2 = new HashMap<Integer, Double>();
								theSimilarities.put(objectArr[j], objectSimilarites2);
							}
							objectSimilarites2.put(objectArr[i], sim);
						}
					}
				}
			}
		}
		
		// go through all the user similarities and sort and prune them.
		for (Integer object : theSimilarities.keySet()) {
			Map<Integer, Double> sims = theSimilarities.get(object);
			sims = Utilities101.sortByValueDescending(sims);
			if (sims.size() > this.nbNeighbors) {
				Map<Integer, Double> copiedSims = new  LinkedHashMap<Integer, Double>();
				int cnt = 0;
				for (Integer simEntry : sims.keySet() ){
					copiedSims.put(simEntry, sims.get(simEntry));
					cnt++;
					if (cnt >= this.nbNeighbors) {
						break;
					}
				}
				sims = copiedSims;
			}
		}
		
		Debug.log("Nearest neighbors: Computed " + this.theSimilarities.size() + " similarities");
		Debug.log("Nearest neighbors: Time: " + (System.currentTimeMillis() - start) / 1000 + " secs");
	}
	
	
	/**
	 * Calculates the Pearson or cosine similarity for two objects. Returns Double.NaN if
	 * there are not enough co-rated items
	 * @param object1 the first object
	 * @param object2 the second object
	 * @return a similarity value between -1 and 1
	 */
	double similarity (Integer object1, Integer object2) {
//		System.out.println("Calculating similarity for " + user1 + " and " + user2);
		// Get the ratings of the users 
		
		if (object1 == null || object2 == null) {
			System.out.println("---- No objects provided for similarity calculation");
			return Double.NaN;
		}
		
		// Ratings to compare
		Set<Rating> ratings1 = null;
		Set<Rating> ratings2 = null;

		if (itemBased) {
			// Need the pre-computed sets of ratings per items.
			ratings1 = getRatingsPerItem(object1);
			ratings2 = getRatingsPerItem(object2);
			
			
		}
		else {
			ratings1 = dataModel.getRatingsPerUser().get(object1);
			ratings2 = dataModel.getRatingsPerUser().get(object2);
		}
		
		if (ratings1 == null || ratings2 == null) {
//			System.out.println("issue here.. "+ object1 + " " + object2);
			return Double.NaN;
		}
		
		
		// Determine the ids of the co-rated items or the co-rating users in case of the item-based
		// approach
		Set<Integer> r1 = new HashSet<Integer>();
		Set<Integer> r2 = new HashSet<Integer>();
		
		if (itemBased) {
			for (Rating r : ratings1) {
				r1.add(r.user);
			}
			for (Rating r : ratings2) {
				r2.add(r.user);
			}
		}
		else {
			for (Rating r : ratings1) {
				r1.add(r.item);
			}
			for (Rating r : ratings2) {
				r2.add(r.item);
			}
			
		}
		
		
		// Calculate the overlap (intersection)
		// was rating all r1 before
		r1.retainAll(r2);
		
		if (r1.size() == 0 || r1.size() < this.minRatingOverlap) {
//			System.out.println("Too small overlap");
			return Double.NaN;
		}
//		System.out.println("going ahead");
		// calculate the similarity / correlation
		return calculateSimilarity(object1, object2, r1);
	}
	
	
	/**
	 * An internal function (to be overwritten in a subclass) to calculate the Pearson correlation of two 
	 * users
	 * @param user1 id of user 1
	 * @param user2 id of user 2
	 * @param overlap the set of co-rated items
	 * @return returns the similarity value;
	 */
	protected double calculateSimilarity(Integer object1, Integer object2, Set<Integer> overlap) {
		double result = Double.NaN;
		
		// Cosine similarity computation (and not adjusted cosine)
		if (useCosineSimilarity) {
			int commonUsers = overlap.size();
			// get the ratings
			double[] ratings1 = new double[commonUsers];
			double[] ratings2 = new double[commonUsers];
			
			int i = 0;
			// copy the ratings into arrays
			for (Integer user : overlap) {
				ratings1[i] = dataModel.getRating(user, object1);
				ratings2[i] = dataModel.getRating(user, object2);
				i++;
			}
			result = Utilities101.dot( ratings1, ratings2) / ( Math.sqrt( Utilities101.dot( ratings1, ratings1 ) ) * Math.sqrt( Utilities101.dot( ratings2, ratings2 ) ) );
			return result;
		}
		// Use Pearson correlation
		else {
			double mean1 = Double.NaN;
			double mean2 = Double.NaN; 
			try {
				mean1 = averages.get(object1);
				mean2 = averages.get(object2);
			}
			catch (Exception e) {
				System.out.println("EXCEPTION");
				System.out.println(object1 + " / " + object2);
				System.out.println(averages);
				e.printStackTrace();
			}
			
			double rating1;
			double rating2;
			
			double numerator = 0.0;
			
			double squaredDev1 = 0.0;
			double squaredDev2 = 0.0;
			
			// Iterate through all and sum things up
			for (Integer item : overlap) {
				rating1 = dataModel.getRating(object1, item);
				rating2 = dataModel.getRating(object2, item);
				
				numerator += (rating1 - mean1) * (rating2 - mean2);
				squaredDev1 += Math.pow((rating1 - mean1),2);
				squaredDev2 += Math.pow((rating2 - mean2),2);
			}
			
			result = numerator / (Math.sqrt(squaredDev1) * Math.sqrt(squaredDev2));
			return result;
		}
	}
	
	// =====================================================================================
	


	/**
	 * Setter for the factory
	 * @param n the max number of neighbors
	 */
	public void setNeighbors(String n) {
		this.nbNeighbors = Integer.parseInt(n);
	}
	
	/**
	 * Sets the similarity threshold
	 * @param s
	 */
	public void setMinSimilarity(String s) {
		this.simThreshold = Double.parseDouble(s);
	}
	
	/**
	 * Setter for the min overlap value
	 * @param overlap
	 */
	public void setMinOverlap(String overlap) {
		this.minRatingOverlap = Integer.parseInt(overlap);
	}

	/**
	 * Setter for the min number of neighbors
	 * @param min
	 */
	public void setMinNeighbors(String min) {
		this.minNeighbors = Integer.parseInt(min);
	}
	
	/**
	 * Set this flag to do item based computations
	 * @param itembased should be "true"
	 */
	public void setItemBased(String itembased) {
		if ("true".equalsIgnoreCase(itembased)) {
			this.itemBased = true;
		}
	}
	
	/**
	 * Sets cosine similarity as the metric
	 * @param cosine should be "true"
	 */
	public void setCosineSimilarity(String cosine) {
		if ("true".equalsIgnoreCase(cosine)) {
			this.useCosineSimilarity = true;
		}
	}
	
	
	// =====================================================================================
	
	public static void main(String[] args) {
		// Simple test method.
		System.out.println(" Testing kNN");
		try {
			DataModel dm = new DataModel();
			DefaultDataLoader loader = new DefaultDataLoader();
			loader.setMinNumberOfRatingsPerUser("250");
			loader.loadData(dm);
			NearestNeighbors rec = new NearestNeighbors();
			rec.setDataModel(dm);
			rec.setCosineSimilarity("true");
			rec.setItemBased("true");
			
			rec.init();
			
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("-- kNN ended");
		

	}
	
	@Override
	public int getDurationEstimate() {
		return 8;
	}
	
	// =====================================================================================
	// This is a test method to remove later on
//	public static void main(String args[]) {
//		UserBasedkNN kNN = new UserBasedkNN();
//		Map<Integer, Double> neighbors = new HashMap<Integer, Double>();
//		
//		DataModel dm = new DataModel();
//		dm.addRating(1, 1000, 4);
//		dm.addRating(2, 1000, 5);
//		dm.addRating(3, 1000, 3);
//		dm.addRating(4, 1000, 4);
//		
//		kNN.setDataModel(dm);
//		
//		kNN.userAverages = new HashMap<Integer, Float>();
//		
//		kNN.userAverages.put(1, 3.0f);
//		kNN.userAverages.put(2, 4.0f);
//		kNN.userAverages.put(3, 4.0f);
//		kNN.userAverages.put(4, 3.0f);
//		kNN.userAverages.put(1000, 3.0f);
//		
//		neighbors.put(1,0.9);
//		neighbors.put(2,0.2);
//		neighbors.put(3,0.01);
//		neighbors.put(4,0.5);
//		
//		kNN.theSimilarities.put(1000,neighbors);
//		
//		System.out.println("ratings: " + dm.getRatings());
//		
//		float prediction = kNN.predictRating(1000, 1000);
//		System.out.println("Prediction ");
//		
//	}
	
}
