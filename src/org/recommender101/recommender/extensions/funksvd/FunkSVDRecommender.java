/** DJ **/
package org.recommender101.recommender.extensions.funksvd;

import java.util.ArrayList;
import java.util.Collections;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import org.apache.mahout.clustering.fuzzykmeans.FuzzyKMeansClusterer;
import org.apache.mahout.clustering.fuzzykmeans.SoftCluster;
import org.apache.mahout.clustering.kmeans.RandomSeedGenerator;
import org.apache.mahout.common.distance.DistanceMeasure;
import org.apache.mahout.common.distance.EuclideanDistanceMeasure;
import org.apache.mahout.math.Vector;
import org.recommender101.data.Rating;
import org.recommender101.recommender.AbstractRecommender;
import org.recommender101.tools.Debug;
import org.recommender101.tools.Utilities101;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.SparseDoubleMatrix2D;
import cern.colt.matrix.linalg.SingularValueDecomposition;
import cern.colt.matrix.linalg.Algebra;

/**
 * Implements a baseline SVD recommender
 * http://sifter.org/~simon/journal/20061211.html Adapted from previous Apache
 * Mahout implementation (0.4)
 * 
 * @author DJ
 * 
 */
public class FunkSVDRecommender extends AbstractRecommender {

	// Default parameter settings
	int numFeatures = 50;
	int initialSteps = 1;
	int N = 7;

	private FastByIDMap<Integer> userMap = null;
	private FastByIDMap<Integer> itemMap = null;
	private GradientDescentSVD emSvd = null;
	private List<Rating> cachedPreferences = null;

	protected static Set<Integer> newItems = new HashSet<Integer>();

	// Calculate the user averages
	Map<Integer, Float> perUserAverage = new HashMap<Integer, Float>();

	// =====================================================================================

	@Override
	public synchronized float predictRating(int user, int item) {
		// return 0;
		Integer useridx = userMap.get(user);
		Integer itemidx = itemMap.get(item);

		// return (float) emSvd.getDotProduct(useridx, itemidx);

		// LL 12.02.13
		if (useridx != null) {
			return (float) emSvd.getDotProduct(useridx, itemidx);
		}
		else {
			// This might happen during training test splits for super-sparse (test)
			// data
			// System.out.println("-- No entry for user: " + user);
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
	public void init() throws Exception {

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

		emSvd = new GradientDescentSVD(numUsers, numItems, numFeatures,
				defaultValue);
		//cachedPreferences = new ArrayList<Rating>(numUsers);
		//recachePreferences();

		//train(initialSteps);

		// System.out.println("Top N Recommendations using SVD of Reco101 :");
		// double itemMatrix[][] = emSvd.getItemMatrix();
		// System.out.println(itemMatrix.length + " " + itemMatrix[0].length);
		// int temp0[] = new int[numItems];
		// int topNReco0[][] = new int[numItems][N];
		// for(int i=0; i<numItems; i++)
		// {
		// temp0 = Similarity.similarVectors(itemMatrix, i ,0);
		// for(int j=0; j<N; j++)
		// {
		// topNReco0[i][j] = temp0[j];
		// }
		// }

		// for(int i=0; i<numItems; i++)
		// {
		// System.out.print(i);
		// System.out.print(":");
		// System.out.print(" ");
		// for(int j=0; j<N; j++)
		// {
		// System.out.print(topNReco0[i][j]);
		// System.out.print(" ");
		// }
		// System.out.println(" ");
		// }

		/*
		 * Stuff starts here \m/
		 */

		SparseDoubleMatrix2D A = new SparseDoubleMatrix2D(numItems, numUsers);
		for (Integer user : dataModel.getUsers()) {
			for (Rating rating : dataModel.getRatingsPerUser().get(user)) {
				int userid = rating.user;
				int useridx = userMap.get(userid);
				int itemid = rating.item;
				int itemidx = itemMap.get(itemid);
				A.set(itemidx, useridx, rating.rating);
			}
		}
		
		Algebra a = new Algebra();

		long t1 = System.currentTimeMillis();
		System.out.println("Started SVD:");
		SingularValueDecomposition svd = new SingularValueDecomposition(A);
			
		System.out.println("Rank of dataset: " + svd.rank());

		double[] values = svd.getSingularValues();

		double sum = 0;

		for (int j = 0; j < values.length; j++) {
			sum += values[j];
		}

		System.out.println("Contribution of factors:");
		double cum_sum = 0;
		for (int j = 0; j < values.length; j++) {
			cum_sum += (values[j] / sum) * 100;
			System.out.println(j + " : " + (values[j] / sum) * 100
					+ " % Cumulative : " + cum_sum + " %");
		}

		double cur_sum = 0;
		int pos = 0;
		for (int j = 0; j < values.length; j++) {
			if (cur_sum > 90) {
				pos = j;
				break;
			}
			cur_sum += (values[j] / sum) * 100;
		}

		System.out.println("Factor id upto threshold importance : " + pos);

		//DoubleMatrix2D V = svd.getV();
		//DoubleMatrix2D V_threshold = V.viewPart(0, 0, numItems, pos);
		//DoubleMatrix2D V_threshold_T = a.transpose(V_threshold);
		//DoubleMatrix2D S = svd.getS();
		//DoubleMatrix2D S_threshold = S.viewPart(0, 0, pos, pos);
		//DoubleMatrix2D itemReduced_T = a.mult(S_threshold, V_threshold_T);
		//DoubleMatrix2D itemReduced = a.transpose(itemReduced_T);
		DoubleMatrix2D U = svd.getU();
		DoubleMatrix2D U_threshold = U.viewPart(0, 0, numItems, pos);
		DoubleMatrix2D S = svd.getS();
		DoubleMatrix2D S_threshold = S.viewPart(0, 0, pos, pos);
		DoubleMatrix2D itemReduced = a.mult(U_threshold, S_threshold);
		double itemMatrix_SVD[][] = itemReduced.toArray();
		
		System.out
				.println("Top N Recommendations based on CosineSimilarity on Reduced Dimension Item Matrix:");
		int temp[] = new int[numItems];
		int topNReco[][] = new int[numItems][N];
		for (int i = 0; i < numItems; i++) {
			temp = Similarity.similarVectors(itemMatrix_SVD, i, 0);
			for (int j = 0; j < N; j++) {
				topNReco[i][j] = temp[j];
			}
		}

		for (int i = 0; i < numItems; i++) {
			System.out.print(i);
			System.out.print(":");
			System.out.print(" ");
			for (int j = 0; j < N; j++) {
				System.out.print(topNReco[i][j]);
				System.out.print(" ");
			}
			System.out.println(" ");
		}
		long t2 = System.currentTimeMillis();
		
		System.out.println("Time taken for brute force : " + (t2-t1)/1000 + " sec");
		
		
		System.out.println("Clustering begins : ");
		
		//Simple k-Means Clustering
		int k_kMeans = 7;  // no of clusters
		List<Vector> vectors = SimpleKMeansClustering.getPoints(itemMatrix_SVD);
		Clusters kMeans_cluster = new Clusters(k_kMeans, numItems);
		SimpleKMeansClustering.kmeansClustering(vectors, kMeans_cluster, k_kMeans);
		
		System.out.println("New Item Set :");
		for (Integer i : newItems) {
			System.out.print((int) i + ", ");
		}
		
		System.out.println("KMeans Cluster Details:");
		kMeans_cluster.printClusters();
		
		// Computing the TopN recommendations for each item from k-Means clusters
		kMeans_cluster.topNReco(numItems, N, itemMatrix_SVD);
		
		// Fuzzy k-Means Clustering
		int k_fuzzy = 7;
		Clusters fuzzykMeans_cluster = new Clusters(k_fuzzy, numItems);
		FuzzyKMeansClustering.fuzzyKMeansClustering(vectors, fuzzykMeans_cluster, k_fuzzy);
		
		//System.out.println("Fuzzy KMeans Cluster Details:");
		fuzzykMeans_cluster.printClusters();
		
		// Computing the TopN recommendations for each item from Fuzzy k-Means clusters
		fuzzykMeans_cluster.topNReco(numItems, N, itemMatrix_SVD);
		
		
		//DataHolder dh = new DataHolder();
		//for(int i=0; i<numItems; i++)
		//{
		//	DataPoint p = new DataPoint(itemMatrix_SVD[i]);
		//	dh.add(p);
		//}
		//Partition partition = new Partition(dh);
		//
		//partition.set(k, 0.001, 0.17, 10);  // no of clusters, precision, fuzziness, no of iterations
		//
		//partition.setProbab(0.68);		// threshold for probability - belongingness to a cluster
		//
		//partition.run(132);
		//System.out.println("\nalgorithm: " + partition.getName());
		//System.out.println("Compactness: " + partition.getCompactness());
		//System.out.println("No of final clusters: " + partition.getNclusters());
		//DataHolder Centers = partition.getCenters();
		//System.out.println("Positions of centers: ");
		//Centers.print();
		//	
		//int[] no_points = partition.getPoints();
		//for(int i=0; i<no_points.length; i++)
		//{
		//	System.out.print(no_points[i]);
		//	System.out.print(" ");
		//}
		//System.out.println();
		// show cluster association
		//for (int m = 0; m < dh.getSize(); m++) {
		//		DataPoint dp = dh.getRaw(m);
		//		int cluster_id = dp.getClusterNumber();
		//		System.out.println("Item no ="+m+" is associated with="+cluster_id);
		//	  }		    	
		
		/*
		 * Fuzzy Clustering Mahout In-Memory requires mahout-core 0.6 or less which conflicts with the
		 * need of KMeansDriver, hence I've left it out right now.
		 * Kindly look into this later.
		 */
		
		// System.out.println("Fuzzy K-Means Clustering begins : ");
		// List<SoftCluster> clusters = new ArrayList<SoftCluster>();
		// int clusterId = 0;
		// for(int i=0; i<k; i++)
		// {
		// Vector vec = vectors.get(i);
		// clusters.add(new SoftCluster(vec, clusterId++, new
		// EuclideanDistanceMeasure()));
		// }
		// FuzzyKMeansClusterer fkmc = new FuzzyKMeansClusterer();
		// List<List<SoftCluster>> finalClusters = fkmc.clusterPoints(vectors,
		// clusters, new EuclideanDistanceMeasure(),
		// 0.01, 3.0, 10);
		// for(SoftCluster cluster1 : finalClusters.get(finalClusters.size() - 1)) {
		// System.out.println("Fuzzy Cluster id: " + cluster1.getId()
		// + " center: " + cluster1.getCenter().asFormatString());
		// }
		
		System.out.println("End");

		// Load the user averages for the recommendation task
		this.perUserAverage = dataModel.getUserAverageRatings();
		Debug.log("FunkSVD:init: Initial training done");
	}

	public static void setNewItems(Set<Integer> s) {
		newItems = s;
	}

	// =====================================================================================

	// SVD-Specific things here
	public void train(int steps) {
		for (int i = 0; i < steps; i++) {
			// Debug.log("Training iteration for SVD: " + i);
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
				// System.out.println("Training useridx: " + useridx + ", itemidx: " +
				// // itemidx);
				emSvd.train(useridx, itemidx, i, rating.rating);
			}
		}
	}

	// =====================================================================================
	// DJ: Not used actually
	private void recachePreferences() {
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
	 * 
	 * @param n
	 */
	public void setNumFeatures(String n) {
		this.numFeatures = Integer.parseInt(n);
	}

	// =====================================================================================

	/**
	 * Setter for the initial steps
	 * 
	 * @param n
	 */
	public void setInitialSteps(String n) {
		this.initialSteps = Integer.parseInt(n);
	}

	// --------------------------------------
	private static final Random random = RandomUtils.getRandom();

	/**
	 * Returns the user vector in the latent space
	 * 
	 * @param user
	 *          the user id
	 * @return the array with the weights
	 */
	public double[] getUserVector(int u) {

		Integer user = this.userMap.get(u);
		if (user == null) {
			System.err.println("Cannot find internal ID for " + u);
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
