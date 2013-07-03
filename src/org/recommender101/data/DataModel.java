/** DJ **/
package org.recommender101.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.recommender101.tools.Utilities101;

/**
 * This class holds all the data required for the recommendation process.
 * @author DJ
 *
 */
public class DataModel {
	
	// The list of users
	Set<Integer> users = new HashSet<Integer>();
	
	// The item list
	Set<Integer> items = new HashSet<Integer>();
	
	// All the ratings / use sparse matrix later on 
	protected Set<Rating> ratings = new HashSet<Rating>();
	
	// HashMap ratings per user
	HashMap<Integer, Set<Rating>> ratingsPerUser = new HashMap<Integer, Set<Rating>>();

	// implicit ratings 
	protected Set<Rating> implicitRatings = new HashSet<Rating>();
	
	// HashMap ratings per user
	protected HashMap<Integer, Set<Rating>> implicitRatingsPerUser = new HashMap<Integer, Set<Rating>>();
	
	// boolean indicating if averages are "dirty"
	boolean averagesDirty = false;
	
	// Get the ratings per user from the outside.
	public HashMap<Integer, Set<Rating>> getRatingsPerUser() {
		return ratingsPerUser;
	}

	// A generic pointer to add-on information as key-value pairs
	// Can be set by specific data loaders and recommenders
	Map<Object, Object> extraInformation = new HashMap<Object, Object>();
	
	/**
	 * A map that contains the user averages. We initialize it on first use and 
	 * a call to getUserAverageRating() or to getUserAverageRatings();
	 */
	private Map<Integer, Float> userAverageRatings;
	
	
	// the minimum rating value
	int minRatingValue;
	
	// the maximum rating value
	int maxRatingValue;

	// =====================================================================================

	/**
	 * Stores a rating in the data model
	 * @param user the user
	 * @param item the rated item
	 * @param value the value. No decimals allowed.
	 * @return the newly added rating
	 */
	public Rating addRating(int user, int item, byte value) {
		Rating r = new Rating(user,item,value);
		ratings.add(r);
		Set<Rating> userRatings = ratingsPerUser.get(user);
		if (userRatings == null) {
			userRatings = new HashSet<Rating>();
		}
		ratingsPerUser.put(user, userRatings);
		userRatings.add(r);
		users.add(user);
		items.add(item);
		averagesDirty = true;
		return r;
		
	}
	
	// =====================================================================================

	/**
	 * Stores a rating in the data model
	 * @param user the user
	 * @param item the rated item
	 * @param value the value. No decimals allowed.
	 * @return the newly added rating
	 */
	public Rating addRating( Rating r ) {
		ratings.add(r);
		Set<Rating> userRatings = ratingsPerUser.get(r.user);
		if (userRatings == null) {
			userRatings = new HashSet<Rating>();
		}
		ratingsPerUser.put(r.user, userRatings);
		userRatings.add(r);
		users.add(r.user);
		items.add(r.item);
		averagesDirty = true;
		return r;
		
	}
	
	
	// =====================================================================================
	/**
	 * Stores an implicit rating in the data model
	 * @param user the user
	 * @param item the rated item
	 * @param value the value. No decimals allowed.
	 * @return the newly added rating
	 */
	public void addImplicitRating(Rating r){
		implicitRatings.add(r);
		
		Set<Rating> userImplicitRatings = implicitRatingsPerUser.get(r.user);
		if (userImplicitRatings == null) {
			userImplicitRatings = new HashSet<Rating>();
		}
		implicitRatingsPerUser.put(r.user, userImplicitRatings);
		userImplicitRatings.add(r);
	}
	
	// =====================================================================================
	/**
	 * A method that returns the user's average rating in this data model
	 * @param user the user id
	 * @return the average or -1 in case we have no ratings.
	 */
	public float getUserAverageRating(Integer user) {
		if (this.userAverageRatings == null) {
			userAverageRatings = new HashMap<Integer, Float>();
			userAverageRatings = Utilities101.getUserAverageRatings(this.ratings);
		}
		Float avg = userAverageRatings.get(user);
		if (avg != null) {
			return avg;
		}
		return -1;
	}

	
	/**
	 * Returns the set of ratins of a given user
	 * @param user the user id
	 * @return the ratings or null if the user has no ratings
	 */
	public Set<Rating> getRatingsOfUser(Integer user) {
		return this.ratingsPerUser.get(user);
	}
	
	
	// =====================================================================================
	
	/**
	 * Returns the set of implicit ratings of a given user
	 * @param user the user id
	 * @return the ratings or null if the user has no ratings
	 */
	public Set<Rating> getImplicitRatingsOfUser(Integer user) {
		Set<Rating> ratings = this.implicitRatingsPerUser.get(user);
		if (ratings == null){
			ratings = new HashSet<Rating>();
		}
		
		return ratings;
	}
	
	
	// =====================================================================================


	
	/**
	 * The method returns the average ratings of a user of a map of user ids to floats. 
	 * @return the map of averages
	 */
	public Map<Integer, Float> getUserAverageRatings() {
		if (this.userAverageRatings == null) {
			userAverageRatings = new HashMap<Integer, Float>();
			userAverageRatings = Utilities101.getUserAverageRatings(this.ratings);
		}
		// Check if someone has removed something
		if (averagesDirty) {
			recalculateUserAverages();
		}
		return  userAverageRatings;
	}
	

	// =====================================================================================

	/**
	 * The constructor
	 */
	public DataModel() {
		extraInformation = new HashMap<Object, Object>();
	}

	// =====================================================================================

	/**
	 * A copy constructor. Has to be overwritten in case we have extra info
	 * @param dm
	 */
	public DataModel(DataModel dm) {
		// Copy things
		this.ratings = new HashSet<Rating>(dm.getRatings());
		this.ratingsPerUser = new HashMap<Integer, Set<Rating>>();
		for (Integer i : dm.ratingsPerUser.keySet()) {
			this.ratingsPerUser.put(i, new HashSet<Rating>(dm.ratingsPerUser.get(i)));
		}
		this.users = new HashSet<Integer>(dm.users);
		this.items = new HashSet<Integer>(dm.items);
		// This should not be a pointer?
		this.extraInformation = new HashMap<Object, Object>(dm.extraInformation);
		this.minRatingValue = dm.minRatingValue;
		this.maxRatingValue = dm.maxRatingValue;
		this.implicitRatings = dm.getImplicitRatings();
		this.implicitRatingsPerUser = new HashMap<Integer, Set<Rating>>();
		for (Integer i : dm.implicitRatingsPerUser.keySet()) {
			this.implicitRatingsPerUser.put(i, new HashSet<Rating>(dm.implicitRatingsPerUser.get(i)));
		}
	}

	// =====================================================================================
	
	/**
	 * A convenient function accepting integers
	 * @param user the user ID
	 * @param item the item ID
	 * @param value the rating value
	 */
	public Rating addRating(int user, int item, int value) {
		return addRating(user, item, (byte) value);
	}

	// =====================================================================================

	/**
	 * Retrieve a rating for a given user-item pair. 
	 * @param user the user ID
	 * @param item the item ID
	 * @return the rating value or -1 in case there is no rating 
	 */
	public byte getRating(int user, int item) {
		Set<Rating> userRatings = ratingsPerUser.get(user);
		byte result = -1;
		if (userRatings != null) {
			// implementation to be improved
			for (Rating r : userRatings) {
				if (r.user == user && r.item == item) {
					return r.rating;
				}
			}
		}
		return result;
	}

	// =====================================================================================

	/**
	 * Returns the handle to the extra info
	 * @return a point to application specific objects for a given key
	 */
	public Object getExtraInformation(Object key) {
		return extraInformation.get(key);
	}


	// =====================================================================================

	/**
	 * Sets a handle to the extra info
	 * @param extraInformation
	 */
	public void addExtraInformation(Object key, Object value) {
		this.extraInformation.put(key, value);
	}
	
	
	/**
	 * Get the whole map at once.
	 * @return the internal map
	 */
	public Map<Object, Object> getExtraInformationMap() {
		return this.extraInformation;
	}

	// =====================================================================================

	/**
	 * Returns a list of users
	 * @return
	 */
	public Set<Integer> getUsers() {
		return users;
	}

	// =====================================================================================

	/**
	 * Returns a list of items
	 * @return
	 */
	public Set<Integer> getItems() {
		return items;
	}

	// =====================================================================================

	/**
	 * Returns a handle to the list of ratings
	 * @return the ratings
	 */
	public Set<Rating> getRatings() {
		return ratings;
	}
	
	// =====================================================================================

	/**
	 * Returns a handle to the list of implicit ratings
	 * @return the ratings
	 */
	public Set<Rating> getImplicitRatings() {
		return implicitRatings;
	}
	

	// =====================================================================================

	/**
	 * Creates a new data model from a given set of ratings. Removes all ratings which are not in the 
	 * provided set
	 * @param ratings
	 * @return
	 */
	public static DataModel copyDataModelAndRemoveRatings(DataModel dm, Set<Rating> ratings2remove) {
		DataModel result = new DataModel(dm);
		for (Rating r : ratings2remove) {
			result.removeRating(r);
		}
		dm.recalculateUserAverages();
		return result;
	}
	
	// =====================================================================================
	
	/**
	 * Removes a rating from the data model
	 * @param r
	 */
	public void removeRating(Rating r) {
		// Remove from my ratings
		ratings.remove(r);
		// Remove from the map
		Set<Rating> userRatings = ratingsPerUser.get(r.user);
		if (userRatings != null) {
			for (Rating rating : userRatings) {
				if (rating.item == r.item) {
					userRatings.remove(r);
					averagesDirty = true;
					return;
				}
			}
		}
	}

	// =====================================================================================

	/**
	 * Removes the user and his ratings from the data model
	 * @param user the user id
	 */
	public void removeUserWithRatings(Integer user) {
		Set<Rating> ratingsOfUser = this.ratingsPerUser.get(user);
		if (ratingsOfUser != null) {
			for (Rating r : ratingsOfUser) {
				ratings.remove(r);
			}
		}
		this.ratingsPerUser.remove(user);
		this.users.remove(user);
	}

	// =====================================================================================
	// Debug -> get the extra info object
	/**
	 * @deprecated
	 * use the other method
	 * @return extra information map of datamodel
	 */
	@Deprecated
	public Map<Object, Object> getAllExtraInfos() {
		return getExtraInformationMap();
	}
	
	
	// =====================================================================================

	/**
	 * A method to recalculate the average ratings
	 */
	public void recalculateUserAverages() {
		this.userAverageRatings = Utilities101.getUserAverageRatings(this.ratings);
		averagesDirty = false;
	}
	
	// =====================================================================================

	/**
	 * A simple string representation returning basic stats
	 */
	public String toString() {
		String result = "Datamodel:\n------------\n";
		result +="Users:  \t" + this.users.size() + "\n";
		result +="Items:  \t" + this.items.size() + "\n";
		result +="Ratings:\t" + this.ratings.size() + "\n";
				
		return result;
	}

	/**
	 * A getter for the maximum rating value
	 * @return the minimum value
	 */
	public int getMaxRatingValue() {
		return maxRatingValue;
	}

	/**
	 * A setter for the maximum rating value
	 * @param maxRatingValue
	 */
	public void setMaxRatingValue(int maxRatingValue) {
		this.maxRatingValue = maxRatingValue;
	}

	/**
	 * Getter for the minimum rating
	 * @return
	 */
	public int getMinRatingValue() {
		return minRatingValue;
	}

	/**
	 * Sets the maximum rating value
	 * @param minRatingValue
	 */
	public void setMinRatingValue(int minRatingValue) {
		this.minRatingValue = minRatingValue;
	}
	
	

	
	
}
