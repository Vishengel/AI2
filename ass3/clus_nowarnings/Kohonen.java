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
	
	public float calculateLearningRate(int t)
	{
		return (float) 0.8*(1 - t/this.epochs);
	}

	public int calculateRadius(int t)
	{
		return (int)((this.n/2)*(1 - t/this.epochs));
	}
	
	public double calculateEuclidianDistance(float[] currentClusterPrototype, float[] currentDataPoint) {
		double sumOfSquares = 0.0;
		
		for (int i=0; i<currentClusterPrototype.length; i++)
		{
			sumOfSquares += Math.pow((currentClusterPrototype[i] - currentDataPoint[i]), 2);
		}
		
		return Math.sqrt(sumOfSquares);
	}
	
	public Coordinates findBMU(int currentVector)
	{
		Coordinates closestCluster =  new Coordinates(0,0);
		double currentClusterDistance;
		double closestClusterDistance = Double.POSITIVE_INFINITY;
		
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
	
	public void updatePrototype(float learningRate, int i, Coordinates node)
	{		
		for (int j=0; j<dim; j++) 
		{
			clusters[node.x][node.y].prototype[j] = (1 - learningRate) * clusters[node.x][node.y].prototype[j] + learningRate * this.trainData.get(i)[j];
		}
	}
	
	public boolean train()
	{
		// Step 1: initialize map with random vectors (A good place to do this, is in the initialisation of the clusters)
		// Repeat 'epochs' times:
			// Step 2: Calculate the squareSize and the learningRate, these decrease lineary with the number of epochs.
			// Step 3: Every input vector is presented to the map (always in the same order)
			// For each vector its Best Matching Unit is found, and :
				// Step 4: All nodes within the neighbourhood of the BMU are changed, you don't have to use distance relative learning.
		// Since training kohonen maps can take quite a while, presenting the user with a progress bar would be nice
		
		
		int closestClusterI = -1, closestClusterJ = -1, radius, lowerBoundJ = 0, upperBoundJ = n, lowerBoundK = 0, upperBoundK = n;
		float learningRate;
		Coordinates BMU, node = new Coordinates(-1,-1);
		
		for (int t=0; t < this.epochs; t++) /// t is the current epoch
		{	
			radius = calculateRadius(t);
			learningRate = calculateLearningRate(t);
			//System.out.println(t);
			for (int i=0; i<this.trainData.size(); i++)
			{
				BMU = findBMU(i);
				//System.out.print("found BMU");
				//System.out.println("BMU(X,Y): " + BMU.x + ", " + BMU.y);
				lowerBoundJ = ((BMU.x-radius<0) ? 0 : BMU.x-radius);
				upperBoundJ = ((BMU.x+radius>n) ? n : BMU.x+radius);
				
				lowerBoundK = ((BMU.y-radius<0) ? 0 : BMU.y-radius);
				upperBoundK = ((BMU.y+radius>n) ? n : BMU.y+radius);
				
				for (int j=lowerBoundJ; j<upperBoundJ; j++) /// first coordinate
				{
					//System.out.print("j: " + j);
					for (int k=lowerBoundK; k<upperBoundK; k++) ///second coordinate
					{
						node.x=j;
						node.y=k;
						/// We now have 1 neighbourhood node
						updatePrototype(learningRate, i, node);
					}
				}
				//System.out.print("neigbourhood updated");
			}	
			System.out.print("Completed " + t + " / " + this.epochs + " epochs\r");
		}
		
		for (int i=0; i<this.trainData.size(); i++)
		{
			BMU = findBMU(i);
			clusters[BMU.x][BMU.y].currentMembers.add(i);
		}
		
		System.out.println("");
		
		return true;
	}
	
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
		// iterate along all clients
		// for each client find the cluster of which it is a member
		// get the actual testData (the vector) of this client
		// iterate along all dimensions
		// and count prefetched htmls
		// count number of hits
		// count number of requests
		// set the global variables hitrate and accuracy to their appropriate value
		int prefetchCount = 0, requestCount = 0, hitCount = 0;
		Coordinates cluster = null;
		
		for (int i=0; i<testData.size(); i++)
		{
			cluster = getCluster(i);
			
			for (int j=0; j<this.dim; j++)
			{
				if (clusters[cluster.x][cluster.y].prototype[j] >= this.prefetchThreshold && testData.get(i)[j] == 1) 
				{
					prefetchCount++;
					requestCount++;
					hitCount++;
				} else if (clusters[cluster.x][cluster.y].prototype[j] >= this.prefetchThreshold)
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

