package org.recommender101.recommender.extensions.funksvd;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class Clusters {
	static ArrayList<Set<Integer>> cluster;// = new ArrayList<Set<Integer>>();
	
	public void addItem(int itemId,String clusterId)
	{
		int clusId = Integer.parseInt(clusterId);
		if(cluster.get(clusId)==null)
		{
			Set<Integer> s= new HashSet<Integer>();
			cluster.add(clusId,s);
		}
		cluster.get(clusId).add(itemId);
	}
	
	public Clusters(int size)
	{
		cluster = new ArrayList<Set<Integer>>(size);
		for(int i =0;i<size;i++)
		{
			Set<Integer> s= new HashSet<Integer>();
			cluster.add(s);
		}
	}
	public static void printClusters() {
		for(int i =0;i<cluster.size();i++)
		{
			System.out.println(i+" ");
			for(int j : cluster.get(i))
			{
				System.out.print(j+", ");
			}
			System.out.println();
		}
	}

}
