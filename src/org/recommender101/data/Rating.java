/** DJ **/
package org.recommender101.data;

/**
 * A class to store ratings
 * @author DJ
 *
 */
public class Rating {
	public int user;
	public int item;
	public byte rating;

	/**
	 * A simple constructor
	 * @param u
	 * @param i
	 * @param r
	 */
	public Rating(int u, int i, byte r) {
		user = u; item = i; rating = r;
	}
	
	/**
	 * A convenient helper for debugging
	 * @param u
	 * @param i
	 * @param r
	 */
	public Rating(int u, int i, int r) {
		this(u,i,(byte)r);
	}
	
	
	/** String representation of rating */
	public String toString() {
		return "[Rating: u: " + this.user + " i: " + this.item + " v: " + this.rating + "]";  
	}
	
	/**
	 * Two ratings are equal if they have the same user and item id.
	 * (Rating values are not important as we assume to have only one rating per user and item)
	 */
	@Override
	public boolean equals(Object other) {
		Rating otherRating = (Rating) other;
		if (this.item == otherRating.item && this.user == otherRating.user) {
			return true;
		}
		else {
			return false;
		}
		
	}
	
	
}
