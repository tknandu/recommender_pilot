package org.recommender101.recommender.extensions.funksvd;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class Clusters {
    ArrayList<Set<Integer>> cluster;// = new ArrayList<Set<Integer>>();
    ArrayList<Integer> ItemClusterMap;// = new ArrayList<Integer>();
    
    public void addItem(int itemId,String clusterId)
    {
        int clusId = Integer.parseInt(clusterId);
        if(cluster.get(clusId)==null)
        {
            Set<Integer> s= new HashSet<Integer>();
            cluster.add(clusId,s);
        }
        cluster.get(clusId).add(itemId);
        ItemClusterMap.add(itemId,clusId);
    }
    
    public Clusters(int size,int numItems)
    {
        cluster = new ArrayList<Set<Integer>>(size);
        ItemClusterMap = new ArrayList<Integer>(numItems);
        for (int j=0;j<numItems;j++) {
            int a= -1;
            ItemClusterMap.add(a);
        }
        for(int i =0;i<size;i++)
        {
            Set<Integer> s= new HashSet<Integer>();
            cluster.add(s);
        }
    }
    
    public void printClusters() {
        for(int i =0;i<cluster.size();i++)
        {
            System.out.println("Cluster "+ i+":");
            for(int j : cluster.get(i))
            {
                System.out.print(j+", ");
            }
            System.out.println();
        }
    }
    
    public ArrayList<ArrayList<Integer>> topNReco(int numItems, int N, double[][] itemMatrix_SVD, int k) throws IOException {
  		// Computing the TopN recommendations for each item from Clusters
  		ArrayList<ArrayList<Integer>> topNReco2 = new ArrayList<ArrayList<Integer>>(
  				numItems);
  		for (int h = 0; h < numItems; h++) {
  			ArrayList<Integer> s = new ArrayList<Integer>(Collections.nCopies(N, -1));
  			topNReco2.add(s);
  		}
  		for (int i = 0; i < numItems; i++) {
  			double temp2;
  			ArrayList<Integer> tempReco = new ArrayList<Integer>(Collections.nCopies(
  					N, -1));
  			int ClusterID = ItemClusterMap.get(i);
  			if (ClusterID != -1) {
  				Set<Integer> ItemsInCluster = new HashSet<Integer>(
  						cluster.get(ClusterID));
  				HashMap<Integer, Double> SimilarityAll = new HashMap<Integer, Double>();

  				for (Integer j : ItemsInCluster) {
  					if (i != j) {
  						temp2 = Similarity.calculateSimilarity(itemMatrix_SVD[i],
  								itemMatrix_SVD[j], 0);
  						SimilarityAll.put(j, temp2);
  					}
  				}
  				ValueComparator bvc = new ValueComparator(SimilarityAll);
  				Map<Integer, Double> sorted_Similarity = new TreeMap<Integer, Double>(
  						bvc);
  				sorted_Similarity.putAll(SimilarityAll);
  				int i1 = 0;
  				for (Integer key : sorted_Similarity.keySet()) {

  					if (i1 < N) {
  						tempReco.add(i1, key);
  					}
  					else
  						break;
  					i1++;
  				}
  				topNReco2.add(i, tempReco);
  			}
  		}
  		HashMap<Integer, NewMovieContent> MovieDetails = new HashMap<Integer, NewMovieContent>(numItems);
	    BufferedReader reader2 = new BufferedReader(new FileReader("data/movielens/ItemContent.txt"));
		String line2;
		line2 = reader2.readLine();
		String[] token2;
		int i1=0;
		while (line2 != null) {
			token2 = line2.split("\\|");
			System.out.println("Contents" + (token2[7])+ (token2[8]));
			MovieDetails.put(i1, new NewMovieContent((Integer.parseInt(token2[0])), token2[1], Integer.parseInt(token2[5]), Integer.parseInt(token2[6]), Integer.parseInt(token2[7]), Integer.parseInt(token2[8]), Integer.parseInt(token2[9]), Integer.parseInt(token2[10]), Integer.parseInt(token2[11]), Integer.parseInt(token2[12]), Integer.parseInt(token2[13]), Integer.parseInt(token2[14]), Integer.parseInt(token2[15]), Integer.parseInt(token2[16]), Integer.parseInt(token2[17]), Integer.parseInt(token2[18]), Integer.parseInt(token2[19]), Integer.parseInt(token2[20]), Integer.parseInt(token2[21]), Integer.parseInt(token2[22]),  Integer.parseInt(token2[23])));
			line2 = reader2.readLine();
			i1++;
		}
  		System.out.println("Recommendation Using Clustering: ");
  		for (int i = 0; i < numItems; i++) {
  			System.out.print(i);
  			System.out.print("\t"+ MovieDetails.get(i).title);
  			System.out.print(":");
  			System.out.print(" ");
  			for (int j = 0; j < N; j++) {
  				if(topNReco2.get(i).get(j) != -1) {
  				System.out.print(MovieDetails.get(topNReco2.get(i).get(j)).title);
  				System.out.print(" ");
  				}
  				else {
  					System.out.print(topNReco2.get(i).get(j));
  					System.out.println(" ");
  				}
  			}
  			System.out.println(" ");
  		}
  		System.out.println("Cluster Details ");
	    for (int i=0;i<k ;i++) {
	    	Set<Integer> ItemsInCluster1 = new HashSet<Integer>(
			cluster.get(i));
	    	int Unknown = 0;
	    	int Action = 0;
	    	int Adventure = 0;
	    	int Animation = 0;
	    	int Children = 0;
	    	int Comedy = 0 ;
	    	int Crime = 0;
	    	int Documentary = 0;
	    	int Drama = 0;
	    	int Fantasy = 0;
	    	int FilmNoir = 0;
	    	int Horror = 0 ;
	    	int Musical = 0;
	    	int Mystery = 0 ;
	    	int Romance = 0;
	    	int SciFi = 0;
	    	int Thriller = 0 ;
	    	int War = 0;
	    	int Western = 0;
	    	for (Integer j : ItemsInCluster1) {
	    		if(MovieDetails.get(j).Unknown == 1){
	    			Unknown++;
	    		}
	    		else if(MovieDetails.get(j).Action == 1){
	    			Action++;
	    		}
	    		else if(MovieDetails.get(j).Adventure == 1){
	    			Adventure++;
	    		}
	    		if(MovieDetails.get(j).Animation == 1){
	    			Animation++;
	    		}
	    		if(MovieDetails.get(j).Children == 1){
	    			Children++;
	    		}
	    		if(MovieDetails.get(j).Comedy == 1){
	    			Comedy++;
	    		}
	    		if(MovieDetails.get(j).Crime == 1){
	    			Crime++;
	    		}
	    		if(MovieDetails.get(j).Documentary == 1){
	    			Documentary++;
	    		}
	    		if(MovieDetails.get(j).Drama == 1){
	    			Drama++;
	    		}
	    		if(MovieDetails.get(j).Fantasy == 1){
	    			Fantasy++;
	    		}
	    		if(MovieDetails.get(j).FilmNoir == 1){
	    			FilmNoir++;
	    		}
	    		if(MovieDetails.get(j).Horror == 1){
	    			Horror++;
	    		}
	    		if(MovieDetails.get(j).Musical == 1){
	    			Musical++;
	    		}
	    		if(MovieDetails.get(j).Mystery == 1){
	    			Mystery++;
	    		}
	    		if(MovieDetails.get(j).Romance == 1){
	    			Romance++;
	    		}
	    		if(MovieDetails.get(j).SciFi == 1){
	    			SciFi++;
	    		}
	    		if(MovieDetails.get(j).Thriller == 1){
	    			Thriller++;
	    		}
	    		if(MovieDetails.get(j).War == 1){
	    			War++;
	    		}
	    		if(MovieDetails.get(j).Western == 1){
	    			Western++;
	    		}
	    	}
	    	System.out.println("Cluster " + i +" stats: "+ Unknown+ " " + Action + " " + Adventure +" " +  Animation + " " + Children + " " + Comedy+ " " +     Crime +" " + Documentary + " " +     Drama + " " +     Fantasy+ " " +     FilmNoir + " " +     Horror+ " " +     Musical+ " " +     Mystery +" " +     Romance+ " " +     SciFi+ " " +     Thriller+ " " +     War+ " " +     Western);
		
	    }
	    return topNReco2;
    }

}