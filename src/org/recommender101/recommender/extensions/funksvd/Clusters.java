package org.recommender101.recommender.extensions.funksvd;

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
    
    public void topNReco(int numItems, int N, double[][] itemMatrix_SVD) {
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
  		System.out.println("Recommendation Using Clustering: ");
  		for (int i = 0; i < numItems; i++) {
  			System.out.print(i);
  			System.out.print(":");
  			System.out.print(" ");
  			for (int j = 0; j < N; j++) {
  				System.out.print(topNReco2.get(i).get(j));
  				System.out.print(" ");
  			}
  			System.out.println(" ");
  		}
    }

}