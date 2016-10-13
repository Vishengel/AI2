import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.lang.Math;

public class KMeans extends ClusteringAlgorithm
{
	// Number of clusters
	private int k;

	// Dimensionality of the vectors
	private int dim;
	
	// Threshold above which the corresponding html is prefetched
	private double prefetchThreshold;
	
	// Array of k clusters, class cluster is used for easy bookkeeping
	private Cluster[] clusters;
	
	// This class represents the clusters, it contains the prototype (the mean of all it's members)
	// and memberlists with the ID's (which are Integer objects) of the datapoints that are member of that cluster.
	// You also want to remember the previous members so you can check if the clusters are stable.
	static class Cluster
	{
		float[] prototype;

		Set<Integer> currentMembers;
		Set<Integer> previousMembers;
		  
		public Cluster()
		{
			currentMembers = new HashSet<Integer>();
			previousMembers = new HashSet<Integer>();
		}
	}
	// These vectors contains the feature vectors you need; the feature vectors are float arrays.
	// Remember that you have to cast them first, since vectors return objects.
	private Vector<float[]> trainData;
	private Vector<float[]> testData;

	// Results of test()
	private double hitrate;
	private double accuracy;
	
	public KMeans(int k, Vector<float[]> trainData, Vector<float[]> testData, int dim)
	{
		this.k = k;
		this.trainData = trainData;
		this.testData = testData; 
		this.dim = dim;
		prefetchThreshold = 0.5;
		
		// Here k new cluster are initialized
		clusters = new Cluster[k];
		for (int ic = 0; ic < k; ic++)
			clusters[ic] = new Cluster();
	}
	
	public float[] addArrays(float[] sumArray, float[] nextArray)
	{
		for (int i=0; i<sumArray.length; i++)
		{
			sumArray[i] += nextArray[i];
		}
		
		return sumArray;
	}
	
	public float[] divideArray(float[] sumArray, int i) 
	{
		for (int j=0; j<sumArray.length; j++) 
		{
			sumArray[j] /= clusters[i].currentMembers.size();
		}
		
		return sumArray;
	}
	
	public void calculateClusterCenters() 
	{
		for (int i=0; i<clusters.length; i++) {
			// System.out.println(i + "\n ________________________");
			float[] sumArray = new float[200];
			
			for(Iterator<Integer> j = clusters[i].currentMembers.iterator(); j.hasNext() ;) 
			{
				sumArray = addArrays(sumArray, trainData.get(j.next()));
			}
			
			/// Divide the sumArray by the total number of members of the cluster
			/// Assign it to the cluster's prototype
			clusters[i].prototype = divideArray(sumArray, i);
			
		}
	}
	
	public double calculateEuclidianDistance(float[] currentClusterPrototype, float[] currentDataPoint) {
		double sumOfSquares = 0.0;
		
		for (int i=0; i<currentClusterPrototype.length; i++)
		{
			sumOfSquares += Math.pow((currentClusterPrototype[i] - currentDataPoint[i]), 2);
		}
		
		return Math.sqrt(sumOfSquares);
	}
	
	public void makeNewPartition() 
	{	
		/// For each cluster, store the current members hash table in its previous members hash table
		/// Clear the current members hash table
		for (int i=0; i<clusters.length; i++)
		{
			clusters[i].previousMembers = new HashSet<Integer>(clusters[i].currentMembers);
			clusters[i].currentMembers.clear();
		}

		for (int i=0; i<trainData.size(); i++)
		{
			int closestCluster = 0;
			double currentClusterDistance, closestClusterDistance = Double.POSITIVE_INFINITY;
			
			for (int j=0; j<clusters.length; j++)
			{
				currentClusterDistance = calculateEuclidianDistance(clusters[j].prototype, trainData.get(i));
				//System.out.println(currentClusterDistance);
				//System.out.println(closestClusterDistance);
				if (currentClusterDistance < closestClusterDistance)
				{
					closestCluster = j;
					closestClusterDistance = currentClusterDistance;
				}
			}
			/// Add the current data point to the cluster with the closest centre
			clusters[closestCluster].currentMembers.add(i); 
		}
	}
	
	public boolean clustersAreStable() 
	{
		for (int i=0; i<clusters.length; i++)
		{
			/// If the previousMembers set of a certain cluster contains all members of its currentMembers set, and vice versa, the cluster is stable
			/// Return false when there is an unstable cluster
			// if (!(clusters[i].previousMembers.containsAll(clusters[i].currentMembers) && clusters[i].currentMembers.containsAll(clusters[i].previousMembers)) ) 
			if (!clusters[i].previousMembers.equals(clusters[i].currentMembers))
			{
				System.out.println("Unstable");
				return false;
			}
		}
		/// All clusters are stable
		System.out.println("Stable");
		return true;
	}
	
	public void printClusters() 
	{
		for (int i=0; i<clusters.length; i++)
		{
			System.out.println(i + ": \n -------");
			for(Iterator<Integer> j = clusters[i].currentMembers.iterator(); j.hasNext() ;) 
			{
				System.out.println(j.next());
			}
			System.out.println("\n");
			/*
			System.out.println(i + ": \n -------");
			for(Iterator<Integer> j = clusters[i].previousMembers.iterator(); j.hasNext() ;) 
			{
				System.out.println(j.next());
			}
			System.out.println("\n");	
			*/
		}
	}

	public boolean train()
	{
		int rand = 0;
		
		for (int i=0; i<trainData.size(); i++)
		{
			/// Pick a random integer in the range (0, k-1)
			rand = ThreadLocalRandom.current().nextInt(0, clusters.length);
			/// Add the ID of the data point to the randomly picked cluster
			clusters[rand].currentMembers.add(i); 
		}
		
		while (!clustersAreStable()) 
		{
			calculateClusterCenters();		
			makeNewPartition();
		}
		
	 	//implement k-means algorithm here:
		// Step 1: Select an initial random partioning with k clusters
		// Step 2: Generate a new partition by assigning each datapoint to its closest cluster center
		// Step 3: recalculate cluster centers
		// Step 4: repeat until clustermembership stabilizes
		
		return false;
	}
	
	public int getCluster(int i)
	{
		for (int j=0; j<clusters.length; j++) {
			if (clusters[j].currentMembers.contains(i))
			{
				return j;
			}
		}
		return -1;
	}

	public boolean test()
	{
		// iterate along all clients. Assumption: the same clients are in the same order as in the testData
		// for each client find the cluster of which it is a member
		// get the actual testData (the vector) of this client
		// iterate along all dimensions
		// and count prefetched htmls
		// count number of hits
		// count number of requests
		// set the global variables hitrate and accuracy to their appropriate value
		int cluster = -1, prefetchCount = 0, requestCount = 0, hitCount = 0;
		
		for (int i=0; i<testData.size(); i++)
		{
			cluster = getCluster(i);
			
			for (int j=0; j<200; j++)
			{
				if (clusters[cluster].prototype[j] >= this.prefetchThreshold && testData.get(i)[j] == 1) 
				{
					prefetchCount++;
					requestCount++;
					hitCount++;
				} else if (clusters[cluster].prototype[j] >= this.prefetchThreshold)
				{
					prefetchCount++;
				} else if (testData.get(i)[j] == 1) 
				{
					requestCount++;
				}
			}
			
		}
		
		System.out.println("Prefetch count: " + prefetchCount);
		System.out.println("Request count: " + requestCount);
		System.out.println("Hit count: " + hitCount);
		
		this.hitrate = (double) hitCount / (double) requestCount;
		this.accuracy = (double) hitCount / (double) prefetchCount;
		
		return true;
	}


	// The following members are called by RunClustering, in order to present information to the user
	public void showTest()
	{
		System.out.println("Prefetch threshold=" + this.prefetchThreshold);
		System.out.println("Hitrate: " + this.hitrate);
		System.out.println("Accuracy: " + this.accuracy);
		System.out.println("Hitrate+Accuracy=" + (this.hitrate + this.accuracy));
	}
	
	public void showMembers()
	{
		for (int i = 0; i < k; i++)
			System.out.println("\nMembers cluster["+i+"] :" + clusters[i].currentMembers);
	}
	
	public void showPrototypes()
	{
		for (int ic = 0; ic < k; ic++) {
			System.out.print("\nPrototype cluster["+ic+"] :");
			
			for (int ip = 0; ip < dim; ip++)
				System.out.print(clusters[ic].prototype[ip] + " ");
			
			System.out.println();
		 }
	}

	// With this function you can set the prefetch threshold.
	public void setPrefetchThreshold(double prefetchThreshold)
	{
		this.prefetchThreshold = prefetchThreshold;
	}
}
