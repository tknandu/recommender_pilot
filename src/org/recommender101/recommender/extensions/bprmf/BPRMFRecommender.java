package org.recommender101.recommender.extensions.bprmf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.recommender101.data.Rating;
import org.recommender101.recommender.AbstractRecommender;
import org.recommender101.recommender.extensions.funksvd.RandomUtils;
import org.recommender101.tools.Debug;
import org.recommender101.tools.Utilities101;

/**
 * Bayesian Personalized Ranking - Ranking by pairwise classification
 * Literature: Steffen Rendle, Christoph Freudenthaler, Zeno Gantner, Lars
 * Schmidt-Thieme: BPR: Bayesian Personalized Ranking from Implicit Feedback.
 * UAI 2009. http://www.ismll.uni-hildesheim.de/pub/pdfs/Rendle_et_al2009-
 * Bayesian_Personalized_Ranking.pdf
 * 
 * Code based on MyMediaLite http://www.ismll.uni-hildesheim.de/mymedialite/
 * 
 * 
 * @author MW
 * 
 */
public class BPRMFRecommender extends AbstractRecommender {

	// / Sample uniformly from users, for the strategy of the original paper set
	// false
	public boolean UniformUserSampling = false;

	private static final Random random = RandomUtils.getRandom();

	// Regularization parameter for the bias term
	public double biasReg = 0;

	// number of columns in the latent matrices
	public int numFeatures = 100;

	// number of iterations over the data
	public int initialSteps = 100;

	// number of users
	protected int numUsers;

	// number of items
	public int numItems;

	// datamanagement-object
	public DataManagement data = new DataManagement();

	// Learning rate alpha
	public double learnRate = 0.05;

	// Regularization parameter for user factors
	public double regU = 0.0025;

	// Regularization parameter for positive item factors
	public double regI = 0.0025;

	// Regularization parameter for negative item factors</summary>
	public double regJ = 0.00025;

	// If set (default), update factors for negative sampled items during
	// learning
	public boolean updateJ = true;
	

	@Override
	/**
	 * Not implemented
	 * Returns rating of item by user
	 * @param user Number  - the user ID
	 * @param item Number  - the item ID
	 * @returns Float rating of the given item by the given user
	 */
	public float predictRating(int user, int item) {
		return Float.NaN;
	}

	/**
	 * Returns rating of item by user -> not usable for rating prediction
	 * @param user Number  - the user ID
	 * @param item Number  - the item ID
	 * @returns Float rating of the given item by the given user
	 */
	public float predictRatingBPR(int user, int item) {
		
		// Note: Predictions are only helpful for ranking and not for prediction
		// convert IDs in mapped values
		int itemidx = data.itemIndices.get(item);
		Integer useridx = data.userIndices.get(user);
		
		if (useridx != null) {
			return (float) (data.item_bias[itemidx] + data.rowScalarProduct( useridx, itemidx));
		}
		else {
			// This might happen during training test splits for super-sparse (test) data
//			System.out.println("-- No entry for user: " + user);
			return Float.NaN;
		}
	}
	// =====================================================================================

	/**
	 * This is similar to AbstractRecommender.recommendItemsByRatingPrediction but uses the internal function
	 */
	public List<Integer> recommendByPrediction(int user) {
		List<Integer> result = new ArrayList<Integer>();

		// If there are no ratings for the user in the training set,
		// there is no point of making a recommendation.
		Map<Integer, Set<Rating>> ratings = getDataModel().getRatingsPerUser();
		// If we have no ratings...
		if (ratings == null || ratings.size() == 0) {
			return Collections.emptyList();
		}

		// Calculate rating predictions for all items we know
		Map<Integer, Float> predictions = new HashMap<Integer, Float>();
		byte rating = -1;
		float pred = Float.NaN;
		// Go through all the items
		for (Integer item : dataModel.getItems()) {
			// check if we have seen the item already
			rating = dataModel.getRating(user, item);
			if (rating == -1) {
				// make a prediction and remember it in case the recommender
				// could make one
				pred = predictRatingBPR(user, item);
				if (!Float.isNaN(pred)) {
					predictions.put(item, pred);
				}
			}
		}
		
		predictions = filterElementsByRelevanceThreshold(predictions, user);
		predictions = Utilities101.sortByValueDescending(predictions);
		
		for (Integer item : predictions.keySet()) {
			result.add(item);
		}
		return result;
	}
	
	
	/**
	 * This method recommends items.
	 */
	@Override
	public List<Integer> recommendItems(int user) {
		return recommendByPrediction(user);
	}

	// =====================================================================================

	@Override
	/**
	 * Initialization of the needed objects and variables
	 * 
	 */
	public void init() {

		// ascertain number of users and items
		numItems = dataModel.getItems().size();
		numUsers = dataModel.getUsers().size();

//		System.out.println("Users, items: " + numUsers + " " + numItems + " ratings " + dataModel.getRatings().size());
		
		// Initialization of datamanagement-object
		data.init(dataModel, numUsers, numItems, numFeatures);

		// trainig of the data
		train();

		Debug.log("BPRMF:init: Initial training done");

	}

	// =====================================================================================


	// =====================================================================================
	/**
	 * Training of the given data
	 * 
	 */
	public void train() {
		for (int i = 0; i < initialSteps; i++) {
			iterate();
		}
	}

	// =====================================================================================

	/**
	 * Perform one iteration of stochastic gradient ascent over the training
	 * data
	 * 
	 */
	public void iterate() {
		// number of all positive ratings
		int num_pos_events = data.numPosentries;

		int user_id, pos_item_id, neg_item_id;

		
		if (UniformUserSampling) {

			//perfoming convergence-heuristic of LearnBPR
			for (int i = 0; i < num_pos_events; i++) {

				// sampling a triple, consisting of a user, a viewed item and an
				// unseen one, by given user
				int[] triple = sampleTriple();
				user_id = triple[0];
				pos_item_id = triple[1];
				neg_item_id = triple[2];

				updateFactors(user_id, pos_item_id, neg_item_id, true, true, updateJ);
			}

		} else {
			// runs over all possible user-item-combinations
			for (int k = 0; k < data.boolMatrix.length; k++) {

				for (int l = 0; l < data.boolMatrix[k].length; l++) {

					user_id = k;
					pos_item_id = l;
					neg_item_id = -1;

					// if the user has not seen the item, another item gets
					// chosen
					if (!data.boolMatrix[user_id][pos_item_id])
						continue;

					// sampling a triple for a given user and seen item
					int[] sampleTriple = sampleOtheritem(user_id, pos_item_id,
							neg_item_id);
					user_id = sampleTriple[0];
					pos_item_id = sampleTriple[1];
					neg_item_id = sampleTriple[2];

					updateFactors(user_id, pos_item_id, neg_item_id, true, true, updateJ);
				}
			}
		}

	}

	// =====================================================================================

	/**
	 * latent matrices and item_bias are updated according to the stochastic
	 * gradient descent update rule
	 * 
	 * @param u
	 *            Number - the mapped userID
	 * @param i
	 *            Number - the mapped itemID of a viewed item
	 * @param j
	 *            Number - the mapped itemID of an unviewed item
	 * @param update_u
	 *            Boolean - should u be updated
	 * @param update_i
	 *            Boolean - should i be updated
	 * @param update_j
	 *            Boolean - should j be updated
	 */
	public void updateFactors(int u, int i, int j, boolean update_u,
			boolean update_i, boolean update_j) {

		// calculating the estimator
		double x_uij = data.item_bias[i] - data.item_bias[j]
				+ data.rowScalarProductWithRowDifference(u, i, j);

		double one_over_one_plus_ex = 1 / (1 + Math.exp(x_uij));

		// adjust bias terms for seen item
		if (update_i) {
			double update = one_over_one_plus_ex - biasReg * data.item_bias[i];
			data.item_bias[i] += (learnRate * update);
		}

		// adjust bias terms for unseen item
		if (update_j) {
			double update = -one_over_one_plus_ex - biasReg * data.item_bias[j];
			data.item_bias[j] += (learnRate * update);
		}

		// adjust factors
		for (int f = 0; f < numFeatures; f++) {
			double w_uf = data.latentUserVector[u][f];
			double h_if = data.latentItemVector[i][f];
			double h_jf = data.latentItemVector[j][f];

			//adjust component of user-vektor
			if (update_u) {
				double update = (h_if - h_jf) * one_over_one_plus_ex - regU
						* w_uf;
				data.latentUserVector[u][f] = (w_uf + learnRate * update);
			}

			//adjust omponent of seen item-vektor
			if (update_i) {
				double update = w_uf * one_over_one_plus_ex - regI * h_if;
				data.latentItemVector[i][f] = (float) (h_if + learnRate
						* update);
			}
			//adjust component of unseen item-vektor	
			if (update_j) {
				double update = -w_uf * one_over_one_plus_ex - regJ * h_jf;
				data.latentItemVector[j][f] = (float) (h_jf + learnRate
						* update);
			}
		}
	}

	// =====================================================================================

	/**
	 * finds another unseen item
	 * 
	 * @param u
	 *            Number - the mapped userID
	 * @param i
	 *            Number - the mapped itemID of a viewed item
	 * @param j
	 *            Number - the mapped itemID of an unviewed item
	 * @return sampleTriple Array - an array containing the mapped userID, the
	 *         mapped view itemId and the mapped unviewed itemID
	 */
	public int[] sampleOtheritem(int u, int i, int j) {
		int[] sampleTriple = new int[3];
		sampleTriple[0] = u;
		sampleTriple[1] = i;
		sampleTriple[2] = j;
		boolean item_is_positive = data.boolMatrix[u][i];

		do
			sampleTriple[2] = random.nextInt(numItems);
		while (data.boolMatrix[u][sampleTriple[2]] == item_is_positive);

		return sampleTriple;
	}

	// =====================================================================================

	/**
	 * finds an user who has viewed at least one item but not all
	 * 
	 * @return u Number - the mapped userID
	 */
	public int sampleUser() {
		while (true) {

			int u = random.nextInt(numUsers);
			if (!data.userMatrix.containsKey(u))
				continue;
			List<Integer> viewedItemsList = data.userMatrix.get(u);

			if (viewedItemsList == null || viewedItemsList.size() == 0
					|| viewedItemsList.size() == numItems)
				continue;
			return u;
		}
	}

	// =====================================================================================

	/**
	 * calls the methods which find an user, a viewed item and an unviewed item
	 * 
	 * @return sampleTriple Array - an array containing the mapped userID, the
	 *         mapped view itemId and the mapped unviewed itemID
	 */
	public int[] sampleTriple() {
		int[] triple = new int[3];
		triple[0] = sampleUser();
		return sampleItempair(triple);
	}

	// =====================================================================================

	/**
	 * finds a seen item and an unseen item
	 * 
	 * @return sampleTriple Array - an array containing the mapped userID, the
	 *         mapped view itemId and the mapped unviewed itemID
	 */
	public int[] sampleItempair(int[] triple) {
		int u = triple[0];

		List<Integer> user_items = data.userMatrix.get(u);
		triple[1] = user_items.get((random.nextInt(user_items.size())));
		do
			triple[2] = random.nextInt(numItems);
		while (user_items.contains(triple[2]));

		return triple;
	}

	// =====================================================================================

	/**
	 * Setter for factory
	 * 
	 * @param n
	 */
	public void setNumFeatures(String n) {
		this.numFeatures = Integer.parseInt(n);
	}

	/**
	 * Setter for regI
	 * 
	 * @param n
	 */
	public void setRegI(String n) {
		this.regI = Double.parseDouble(n);
	}
	
	/**
	 * Setter for regJ
	 * 
	 * @param n
	 */
	public void setRegJ(String n) {
		this.regJ = Double.parseDouble(n);
	}
	
	/**
	 * Setter for regU
	 * 
	 * @param n
	 */
	public void setRegU(String n) {
		this.regU = Double.parseDouble(n);
	}
	
	/**
	 * Setter for UpdateJ
	 * 
	 * @param n
	 */
	public void setUpdateJ(String n) {
		this.updateJ = Boolean.parseBoolean(n);
	}
	
	
	/**
	 * Setter for BiasReg
	 * 
	 * @param n
	 */
	public void setBiasReg(String n) {
		this.biasReg = Double.parseDouble(n);
	}
	
	/**
	 * Setter for LearnRate
	 * 
	 * @param n
	 */
	public void setLearnRate(String n) {
		this.learnRate = Double.parseDouble(n);
	}
	

	/**
	 * Setter for the initial steps
	 * 
	 * @param n
	 */
	public void setInitialSteps(String n) {
		this.initialSteps = Integer.parseInt(n);
	}
	
	/**
	 * Setter for the uniform Sampling
	 * 
	 * @param n
	 */
	public void setUniformSampling(String n) {
		this.UniformUserSampling = Boolean.parseBoolean(n);
	}
	
	@Override
	public int getDurationEstimate() {
		return 3;
	}
	
	/**
	 * Should the global relevance threshold be chosen or not
	 * @param u
	 */
	public void setUseRelevanceThreshold(String u)  throws Exception {
		data.useRatingThreshold = Boolean.parseBoolean(u);
	}

}