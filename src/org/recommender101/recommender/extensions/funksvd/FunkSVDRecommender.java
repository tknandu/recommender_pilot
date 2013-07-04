/** DJ **/
package org.recommender101.recommender.extensions.funksvd;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.recommender101.data.Rating;
import org.recommender101.recommender.AbstractRecommender;
import org.recommender101.tools.Debug;
import org.recommender101.tools.Utilities101;

/**
 * Implements a baseline SVD recommender
 * http://sifter.org/~simon/journal/20061211.html
 * Adapted from previous Apache Mahout implementation (0.4)
 * 
 * @author DJ
 *
 */
public class FunkSVDRecommender extends AbstractRecommender {

	
	// Default parameter settings
	int numFeatures = 50;
	int initialSteps = 100;
	
	private FastByIDMap<Integer> userMap = null;
	private FastByIDMap<Integer> itemMap = null;
	private GradientDescentSVD emSvd = null;
	private List<Rating> cachedPreferences = null;
	
	// Calculate the user averages
	Map<Integer, Float> perUserAverage = new HashMap<Integer, Float>();
	

  // =====================================================================================

	@Override
	public synchronized float predictRating(int user, int item) {
		// return 0;
		Integer useridx = userMap.get(user);
		Integer itemidx = itemMap.get(item);

		//return (float) emSvd.getDotProduct(useridx, itemidx);
		
		
		// LL 12.02.13
		if (useridx != null) {
			return (float) emSvd.getDotProduct(useridx, itemidx);
		}
		else {
			// This might happen during training test splits for super-sparse (test) data
//			System.out.println("-- No entry for user: " + user);
			return Float.NaN;
		}
	}

  // =====================================================================================

	/**
	 * This method recommends items.
	 */
	@Override
	public List<Integer> recommendItems(int user) {
		// Use the standard method based on the rating prediction
		return recommendItemsByRatingPrediction(user);
	}

  // =====================================================================================

	@Override
	public void init() {
		
		Debug.log("FunkSVD:init: Starting to train model");
		int numUsers = dataModel.getUsers().size();
		userMap = new FastByIDMap<Integer>(numUsers);
		int idx = 0;
		for (Integer user : dataModel.getUsers()) {
			userMap.put(user, idx++);
		}

		int numItems = dataModel.getItems().size();
		itemMap = new FastByIDMap<Integer>(numItems);

		idx = 0;
		for (Integer item : dataModel.getItems()) {
			itemMap.put(item, idx++);
		}
		
		double average = Utilities101.getGlobalRatingAverage(dataModel);
		double defaultValue = Math.sqrt((average - 1.0) / numFeatures);
		
		emSvd = new GradientDescentSVD(numUsers, numItems, numFeatures, defaultValue);
		cachedPreferences = new ArrayList<Rating>(numUsers);
		recachePreferences();
		
		train(initialSteps);
		
		for(int k=0;k< emSvd.getLeftVector((Integer) dataModel.getUsers().toArray()[1]).length; k++)
			System.out.println((emSvd.getLeftVector((Integer) dataModel.getUsers().toArray()[1]))[k]);
		
		System.out.println("XXXXXXXXXXXXX");
		int ct=0;
		for(Integer i : recommendItems((Integer) dataModel.getUsers().toArray()[1]))
		{
			if(ct==10) break;
			ct++;
			System.out.println(i);
		}
		
		// Load the user averages for the recommendation task
		this.perUserAverage = dataModel.getUserAverageRatings();		
		Debug.log("FunkSVD:init: Initial training done");
	}
	
  // =====================================================================================

	// SVD-Specific things here
	public void train(int steps) {
		for (int i = 0; i < steps; i++) {
//			Debug.log("Training iteration for SVD: " + i);
			nextTrainStep();
		}
	}
	
  // =====================================================================================

	private void nextTrainStep() {
		Collections.shuffle(cachedPreferences, random);
		long userid;
		long itemid;
		for (int i = 0; i < numFeatures; i++) {
			for (Rating rating : cachedPreferences) {
				userid = rating.user;
				itemid = rating.item;
				int useridx = userMap.get(userid);
				int itemidx = itemMap.get(itemid);
				// System.out.println("Training useridx: " + useridx + ", itemidx: " + // itemidx);
				emSvd.train(useridx, itemidx, i, rating.rating);
			}
		}
	}

  // =====================================================================================
	// DJ: Not used actually
	private void recachePreferences()  {
		cachedPreferences.clear();
		// DJ: reload the preferences differently.
		for (Integer user : dataModel.getUsers()) {
			for (Rating rating : dataModel.getRatingsPerUser().get(user)) {
				cachedPreferences.add(rating);
			}
		}
	}
	
  // =====================================================================================

	/**
	 * Setter for factory
	 * @param n
	 */
	public void setNumFeatures(String n) {
		this.numFeatures = Integer.parseInt(n);
	}
	
  // =====================================================================================

	/**
	 * Setter for the initial steps
	 * @param n
	 */
	public void setInitialSteps(String n) {
		this.initialSteps = Integer.parseInt(n);
	}
	
	// --------------------------------------
	private static final Random random = RandomUtils.getRandom();
	
	
	/**
	 * Returns the user vector in the latent space
	 * @param user the user id
	 * @return the array with the weights
	 */
	public double[] getUserVector(int u)  {
		
		Integer user = this.userMap.get(u);
		if (user == null) {
			System.err.println("Cannot find internal ID for " +u);
			System.exit(1);
			return null;
		}
		else {
			return this.emSvd.getLeftVector(user);
		}
	}
	
	
	@Override
	public int getDurationEstimate() {
		return 4;
	}	
	
}
