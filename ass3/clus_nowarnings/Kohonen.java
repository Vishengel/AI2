import java.util.*;
import java.awt.*;

public class Kohonen extends ClusteringAlgorithm
{
	// Size of clustersmap
	private int n;

	// Number of epochs
	private int epochs;
	
	// Dimensionality of the vectors
	private int dim;
	
	// Threshold above which the corresponding html is prefetched
	private double prefetchThreshold;

	private double initialLearningRate; 
	
	// This class represents the clusters, it contains the prototype (the mean of all it's members)
	// and a memberlist with the ID's (Integer objects) of the datapoints that are member of that cluster.  
	private Cluster[][] clusters;

	// Vector which contains the train/test data
	private Vector<float[]> trainData;
	private Vector<float[]> testData;
	
	// Results of test()
	private double hitrate;
	private double accuracy;
	
	static class Cluster
	{
			float[] prototype;

			Set<Integer> currentMembers;

			public Cluster()
			{
				currentMembers = new HashSet<Integer>();
			}
	}
	
	/// Helper class to indicate a cluster in the 2D space.
	static class Coordinates {
		int x;
		int y;
		
		public Coordinates(int x, int y) {
			this.x = x;
			this.y = y;
		}
	}
	
	public Kohonen(int n, int epochs, Vector<float[]> trainData, Vector<float[]> testData, int dim)
	{
		this.n = n;
		this.epochs = epochs;
		prefetchThreshold = 0.5;
		initialLearningRate = 0.8;
		this.trainData = trainData;
		this.testData = testData; 
		this.dim = dim;       
		
		Random rnd = new Random();

		// Here n*n new cluster are initialized
		clusters = new Cluster[n][n];
		for (int i = 0; i < n; i++)  {
			for (int i2 = 0; i2 < n; i2++) {
				clusters[i][i2] = new Cluster();
				clusters[i][i2].prototype = new float[dim];
				
				/// Initialize cluster centre with random floats
				for (int i3=0; i3<dim; i3++) 
				{
					clusters[i][i2].prototype[i3] = rnd.nextFloat();
				}
				
			}
		}
	}

	///Functions to calculate the learning rate and radius.
	public float calculateLearningRate(int t)
	{
		return (float) 0.8*(1 - t/this.epochs);
	}

	public int calculateRadius(int t)
	{
		return (int)((this.n/2)*(1 - t/this.epochs));
	}
	
	///Function to calculate the euclidian distance between a datapoint and the prototype of a cluster
	public double calculateEuclidianDistance(float[] currentClusterPrototype, float[] currentDataPoint) {
		double sumOfSquares = 0.0;
		
		for (int i=0; i<currentClusterPrototype.length; i++)
		{
			sumOfSquares += Math.pow((currentClusterPrototype[i] - currentDataPoint[i]), 2);
		}
		
		return Math.sqrt(sumOfSquares);
	}
	
	/// Function which returns the BMU (cluster node closest to the datapoint)
	public Coordinates findBMU(int currentVector)
	{
		Coordinates closestCluster =  new Coordinates(0,0);
		double currentClusterDistance;
		double closestClusterDistance = Double.POSITIVE_INFINITY;
		
		///looping thourgh all the nodes in the 2D space.
		for (int i = 0; i < n; i++)  {
			for (int j = 0; j < n; j++) {
				currentClusterDistance = calculateEuclidianDistance(clusters[i][j].prototype, trainData.get(currentVector));
				if (currentClusterDistance < closestClusterDistance)
				{
					closestClusterDistance = currentClusterDistance;
					closestCluster.x = i;
					closestCluster.y = j;
				}
			}
		}
		
		
		return closestCluster;
	}
	
	/// Uses the update formula to update the protoype
	public void updatePrototype(float learningRate, int i, Coordinates node)
	{		
		///Looping through the clusters prototype array
		for (int j=0; j<dim; j++) 
		{
			clusters[node.x][node.y].prototype[j] = (1 - learningRate) * clusters[node.x][node.y].prototype[j] + learningRate * this.trainData.get(i)[j];
		}
	}
	
	public boolean train()
	{
		int closestClusterI = -1, closestClusterJ = -1, radius, lowerBoundJ = 0, upperBoundJ = n, lowerBoundK = 0, upperBoundK = n;
		float learningRate;
		Coordinates bmu, node = new Coordinates(-1,-1);

		/// Looping until the max amount of epoch ahs been reached	
		for (int t=0; t < this.epochs; t++) /// t is the current epoch
		{	
			/// each epoch, calculate the radius and learning because they change every epoch.
			radius = calculateRadius(t);
			learningRate = calculateLearningRate(t);
			///Looping through the datapoints
			for (int i=0; i<this.trainData.size(); i++)
			{
				bmu = findBMU(i);
				///calculating the neighbourhood for each new found bmu
				///cutting off at the edges of the 2D space.
				lowerBoundJ = ((bmu.x-radius<0) ? 0 : bmu.x-radius);
				upperBoundJ = ((bmu.x+radius>n) ? n : bmu.x+radius);
				
				lowerBoundK = ((bmu.y-radius<0) ? 0 : bmu.y-radius);
				upperBoundK = ((bmu.y+radius>n) ? n : bmu.y+radius);
				
				///looping through the neighbourhood
				for (int j=lowerBoundJ; j<upperBoundJ; j++) /// first coordinate
				{
					for (int k=lowerBoundK; k<upperBoundK; k++) ///second coordinate
					{
						node.x=j;
						node.y=k;
						/// We now have 1 neighbourhood node
						updatePrototype(learningRate, i, node);
					}
				}
			}

			///progressbar	
			System.out.print("Completed " + t + " / " + this.epochs + " epochs\r");
		}
		
		///adding each datapoint to a cluster, by finding its BMU.
		for (int i=0; i<this.trainData.size(); i++)
		{
			bmu = findBMU(i);
			clusters[bmu.x][bmu.y].currentMembers.add(i);
		}
		///Erasing the progressbar
		System.out.println("");
		
		return true;
	}
	
	///Returns the cluster to which a certain datapoint belongs
	public Coordinates getCluster(int i)
	{
		for (int j=0; j<this.n; j++) {
			for (int k=0; k<this.n; k++)
			{
				if (clusters[j][k].currentMembers.contains(i))
				{
					return new Coordinates(j, k);
				}
			}	
		}
		return null;
	}
	
	public boolean test()
	{
		int prefetchCount = 0, requestCount = 0, hitCount = 0;
		Coordinates cluster = null;
		
		///looping over the test data to see for each client how the algorithm has been trained
		for (int i=0; i<testData.size(); i++)
		{
			cluster = getCluster(i);

			///comparing the prototype array to the clients array
			for (int j=0; j<this.dim; j++)
			{
				if (clusters[cluster.x][cluster.y].prototype[j] >= this.prefetchThreshold && testData.get(i)[j] == 1) 
				{
					///if the URL has been prefetched and request, increment all counters.
					prefetchCount++;
					requestCount++;
					hitCount++;
				} else if (clusters[cluster.x][cluster.y].prototype[j] >= this.prefetchThreshold)
				{
					/// if the URL has been prefetched, but not requested, on increment the prefetch counter.
					prefetchCount++;
				} else if (testData.get(i)[j] == 1) 
				{
					/// if the URL has been requested, but not prefetched, on increment the request counter.
					requestCount++;
				}
			}
			
		}
		
		// System.out.println("Prefetch count: " + prefetchCount);
		// System.out.println("Request count: " + requestCount);
		// System.out.println("Hit count: " + hitCount);
		
		this.hitrate = (double) hitCount / (double) requestCount;
		this.accuracy = (double) hitCount / (double) prefetchCount;
		
		return true;
	}


	public void showTest()
	{
		System.out.println("Initial learning Rate=" + initialLearningRate);
		System.out.println("Prefetch threshold=" + prefetchThreshold);
		System.out.println("Hitrate: " + hitrate);
		System.out.println("Accuracy: " + accuracy);
		System.out.println("Hitrate+Accuracy=" + (hitrate + accuracy));
	}
 
 
	public void showMembers()
	{
		for (int i = 0; i < n; i++)
			for (int i2 = 0; i2 < n; i2++)
				System.out.println("\nMembers cluster["+i+"]["+i2+"] :" + clusters[i][i2].currentMembers);
	}

	public void showPrototypes()
	{
		for (int i = 0; i < n; i++) {
			for (int i2 = 0; i2 < n; i2++) {
				System.out.print("\nPrototype cluster["+i+"]["+i2+"] :");
				
				for (int i3 = 0; i3 < dim; i3++)
					System.out.print(" " + clusters[i][i2].prototype[i3]);
				
				System.out.println();
			}
		}
	}

	public void setPrefetchThreshold(double prefetchThreshold)
	{
		this.prefetchThreshold = prefetchThreshold;
	}
}

