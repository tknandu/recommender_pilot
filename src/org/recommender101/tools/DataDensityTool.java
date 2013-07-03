package org.recommender101.tools;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.recommender101.data.DataModel;
import org.recommender101.data.DefaultDataLoader;
import org.recommender101.data.Rating;

// A class to pre-process data files and retain only users and items with at least n ratings
public class DataDensityTool {
	
	static int minPerUser = 10;
	static int minPerItem = 10;
	static String inputFile = "/projects/recommender101-extensions/data/extensions/netflix/myFile_u10_i10.txt";
	static String outputFile = "/projects/recommender101-extensions/data/extensions/netflix/myFile_u10_i10-10-10.txt";
//	static String inputFile = "/projects/recommender101-extensions/data/extensions/movielens5m/ratingsSample5000users.txt";
//	static String outputFile = "/projects/recommender101-extensions/data/extensions/movielens5m/ratingsSample5000users-10-10.txt";
	

	public static Map<Integer, Integer> userratingcount = new HashMap<Integer,Integer>();
	public static Map<Integer, Integer> itemratingcount = new HashMap<Integer,Integer>();

	
	/**
	 * Main entry point. Loads the data and runs the density method
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.println("Starting data density tool");
		try {
			// Load the data
			DefaultDataLoader dataLoader = new DefaultDataLoader();
			dataLoader.setFilename(inputFile);
			DataModel dm = new DataModel();
			dataLoader.loadData(dm);
			System.out.println("Loaded data, users: " + dm.getUsers().size() + ", items: " + dm.getItems().size());
			DataDensityTool.applyConstraints(minPerUser, minPerItem, dm);
			System.out.println("Applied constraints, users: " + dm.getUsers().size() + ", items: " + dm.getItems().size());
			Utilities101.writeDataModelToFile(dm, outputFile, inputFile);

		}
		catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("Program ended.");
	}
	
	/**
	 * Calculates the set of ratings obeying the given constraints
	 * @param minPerUser
	 * @return a set of ratings..
	 */
	public static void applyConstraints(int minPerUser, int minPerItem, DataModel dm) throws Exception {
		
		
		// reduce the data
		int max_iterations = 100;
		int it_count = 0;
		do  {
			System.out.println("Round: " + it_count);
			  it_count++;
				deleteNonHeavyUsers(dm, minPerUser);
				deleteNonFrequentItems(dm, minPerItem);
		} while (!densityReached(dm, minPerUser, minPerItem) && (it_count < max_iterations));
	}

	/**
	 * Removes all users from the data  model with less then minPerUser ratings
	 * @param dm
	 * @param minPerUser
	 */
	public static void deleteNonHeavyUsers(DataModel dm, int minPerUser) {
		int removed = 0;
		Set<Integer> usersCopy = new HashSet<Integer>(dm.getUsers());
		for (Integer user: usersCopy) {
//			System.out.println("User has rated : " + dm.getRatingsOfUser(user).size() + " items");
			if (dm.getRatingsOfUser(user).size() < minPerUser) {
				dm.removeUserWithRatings(user);
				removed++;
			}
		}
//		System.out.println("Removed "+ removed + " users");
	}
	
	/**
	 * Removes items which have not enough ratings
	 * @param dm
	 * @param minPerItem
	 */
	public static void deleteNonFrequentItems(DataModel dm, int minPerItem) {
//		System.out.println("Deleting items");
		int removed = 0;
		HashMap<Integer, Integer> ratingsPerItem = new HashMap<Integer, Integer>();
		// count the ratings first
		for (Rating r : dm.getRatings()) {
			Utilities101.incrementMapValue(ratingsPerItem,r.item);
		}
		// Find out what to remove
		Set<Integer> itemsToRemove = new HashSet<Integer>();
		for (Integer item: dm.getItems()) {
			Integer count = ratingsPerItem.get(item);
			if (count == null || count < minPerItem) {
				itemsToRemove.add(item);
				removed ++;
			}
		}
//		System.out.println("Have to remove: " + removed + " items");
		// remove all items from the data model
		dm.getItems().removeAll(itemsToRemove);
//		System.out.println("remaining items " + dm.getItems().size());
		
		// remove all ratings
		Set<Rating> removedRatings = new HashSet<Rating>();
		Set<Rating> ratingsCopy = new HashSet<Rating>(dm.getRatings());
		for (Rating r : ratingsCopy){
			if (itemsToRemove.contains(r.item)) {
//				System.out.println("Have to remove a rating");
				dm.removeRating(r);
				removedRatings.add(r);
			}
		}
		dm.recalculateUserAverages();
//		// remove ratings of users
//		for (Integer user: dm.getUsers()) {
//			boolean removedSomething = dm.getRatingsOfUser(user).removeAll(removedRatings); 
//			if (removedSomething){
//				System.out.println("Removed rating...");
//			};
//		}
	}
	
	/**
	 * Checks if the densities are now ok
	 * @param dm
	 * @param minPerUser
	 * @return
	 */
	public static boolean densityReached(DataModel dm, int minPerUser, int intPerItem) {
		for (Integer user : dm.getUsers()) {
			if (dm.getRatingsOfUser(user).size() < minPerUser) {
				return false;
			}
		}
		HashMap<Integer, Integer> ratingsPerItem = new HashMap<Integer, Integer>();
		// count the ratings first
		for (Rating r : dm.getRatings()) {
			Utilities101.incrementMapValue(ratingsPerItem,r.item);
		}
		for (Integer item : dm.getItems()) {
			Integer countPerItem = ratingsPerItem.get(item);
			if (countPerItem != null && countPerItem < minPerItem) {
				return false;
			}
		}
		return true;
	}
	
}
