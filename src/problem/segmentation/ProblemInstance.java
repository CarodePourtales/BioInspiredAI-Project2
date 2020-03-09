package problem.segmentation;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import ga.segmentation.Individual.Direction;
import problem.IProblemInstance;
import utils.ImageUtils;
import utils.WeightedGraph;

/**
 * Represents an image segmentation problem instance.
 * @author Kelian Baert & Caroline de Pourtales
 */
public class ProblemInstance implements IProblemInstance {
	// The problem instance's image
	private BufferedImage image;
	
	// Name of the problem instance
	private String name;
	
	// Original size of the image, prior to scaling
	private int originalWidth, originalHeight;
	
	// Scaling factor
	private float imageScaling;
	
	// Store the RGB of each pixel as a matrix of [r, g, b] integer arrays
	private int[][][] rgb;
	
	// Store the HSB (hue, saturation, brightness) of each pixel as a matrix of [h, s, b] integer arrays
	private float[][][] hsb;
	
	// A graph in which each pixel is connected to its cardinal neighbors with weights equal to the euclidean distances in HSB space
	private WeightedGraph euclideanDistanceGraph;
	
	/**
	 * Create a new problem instance
	 * @param name - The name of this problem instance
	 * @param originalImage - An image
	 * @param imageScaling - A ratio by which to scale the input image
	 */
	public ProblemInstance(String name, BufferedImage originalImage, float imageScaling) {
		this.name = name;
		this.originalWidth = originalImage.getWidth();
		this.originalHeight = originalImage.getHeight();
		this.image = imageScaling == 1 ? originalImage : ImageUtils.scaleImage(originalImage, imageScaling);
		this.imageScaling = imageScaling;
		
		int w = image.getWidth(), h = image.getHeight();
		
		// Store the RGB and HSB of each pixel in three-dimensional arrays of shape (width, height, 3)
		rgb = new int[w][h][3];
		hsb = new float[w][h][3];
		for(int x = 0; x < w; x++) {
			for(int y = 0; y < h; y++) {
				Color c = new Color(getImage().getRGB(x, y));
				rgb[x][y][0] = c.getRed();
				rgb[x][y][1] = c.getGreen();
				rgb[x][y][2] = c.getBlue();
				hsb[x][y] = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
			}	
		}
				
		// Create a graph in which each pixel is connected to its 4 cardinal neighbours.
		// The weight of the edges are given by the euclidean distance in HSB color space
		euclideanDistanceGraph = new WeightedGraph(image.getWidth()*image.getHeight());
		for(int x = 0; x < w; x++) {
			for(int y = 0; y < h; y++) {
				List<int[]> cardinalNeighbours = new ArrayList<int[]>();
				if(x > 0)   cardinalNeighbours.add(new int[] {x-1, y});
				if(x < w-1) cardinalNeighbours.add(new int[] {x+1, y});
				if(y > 0)   cardinalNeighbours.add(new int[] {x, y-1});
				if(y < h-1) cardinalNeighbours.add(new int[] {x, y+1});
					
				for(int i = 0; i < cardinalNeighbours.size(); i++) {
					int x2 = cardinalNeighbours.get(i)[0], y2 = cardinalNeighbours.get(i)[1];
					euclideanDistanceGraph.addConnection(
						y*w+x, // position of current pixel in flattened coordinates
						y2*w+x2, // position of neighbour in flattened coordinates
						//euclideanDistance(getRGB(x, y), getRGB(x2, y2))
						euclideanDistance(getHSB(x, y), getHSB(x2, y2))
					);
				}
			}	
		}
	}
	
	/**
	 * Get the image of this problem instance.
	 * @return the image
	 */
	public BufferedImage getImage() {
		return image;
	}
	
	/**
	 * Get the RGB at a given position of the image.
	 * @param x - A horizontal position
	 * @param y - A vertical position
	 * @return the rgb at position (x, y), as a [r, g, b] float array
	 */
	private int[] getRGB(int x, int y) {
		return rgb[x][y];
	}
	
	/**
	 * Get the HSB value at a given position of the image.
	 * @param x - A horizontal position
	 * @param y - A vertical position
	 * @return the hsb at position (x, y), as a [h, s, b] float array
	 */
	private float[] getHSB(int x, int y) {
		return hsb[x][y];
	}
	
	/**
	 * Get the RGB of a given pixel index in the image.
	 * @param i - A pixel index (between 0 and width*height)
	 * @return the rgb at pixel index i, as a [r, g, b] float array
	 */
	public int[] getRGB(int i) {
		int[] pos = pixelIndexToPos(i);
		return getRGB(pos[0], pos[1]);
	}
	
	/**
	 * Get the HSB value of a given pixel index in the image.
	 * @param i - A pixel index (between 0 and width*height)
	 * @return the hsb at pixel index i, as a [h, s, b] float array
	 */
	public float[] getHSB(int i) {
		int[] pos = pixelIndexToPos(i);
		return getHSB(pos[0], pos[1]);
	}
	
	/**
	 * Get the scaling factor used for the image
	 * @return the scaling factor
	 */
	public float getImageScaling() {
		return imageScaling;
	}
	
	/**
	 * Get the original width of the image (prior to scaling)
	 * @return the original width
	 */
	public int getOriginalWidth() {
		return originalWidth;
	}
	
	/**
	 * Get the original height of the image (prior to scaling)
	 * @return the original height
	 */
	public int getOriginalHeight() {
		return originalHeight;
	}
	
	/**
	 * Project a 1D pixel index into a 2D position
	 * @param index - A pixel index
	 * @return the corresponding position, as a [x, y] int array
	 */
	public int[] pixelIndexToPos(int index) {
		int x = index % getImage().getWidth();
		int y = (index  - x) / getImage().getWidth();
		return new int[] {x, y};
	}
	
	/** Get the direction to go from a pixel to a given adjacent one.
	 * @param i - The source pixel index
	 * @param j - The destination pixel index
	 * @return the direction to go from a pixel to a given adjacent one (e.g. from i=0 to j=1 direction is RIGHT), 
	 * or Direction.NONE if the given pixels aren't adjacent
	 */
	public Direction getDirection(int i, int j) {
		int[] pos1 = pixelIndexToPos(i);
		int[] pos2 = pixelIndexToPos(j);
		return getDirection(pos1[0], pos1[1], pos2[0], pos2[1]);
	}
	
	/**
	 * Get the direction to go from a pixel to a given adjacent one.
	 * @param xfrom - The source x position
	 * @param yfrom - The source y position
	 * @param xto - The destination x position
	 * @param yto - The destination y position
	 * @return the direction to go from a pixel to a given adjacent one (e.g. from x=0,y=0 to x=1,y=0, direction is RIGHT),
	 * or Direction.NONE if given pixels aren't adjacent
	 */
	public Direction getDirection(int xfrom, int yfrom, int xto, int yto) {
		if(yfrom == yto) {
			if(xto == xfrom + 1)
				return Direction.RIGHT;
			else if(xto == xfrom - 1)
				return Direction.LEFT;
		}
		else if(xfrom == xto) {
			if(yto == yfrom + 1)
				return Direction.DOWN;
			else if(yto == yfrom - 1)
				return Direction.UP;
		}
		return Direction.NONE;
	}
	
	/**
	 * Calculates the euclidean distance between two one-dimensional float arrays of same length.
	 * @param arr1 - An array of floats
	 * @param arr2 - Another array of floats
	 * @return the euclidean distance between the two arrays.
	 */
	public static float euclideanDistance(float[] arr1, float[] arr2) {
		float sumOfSquares = 0.0f;
		for(int i = 0; i < arr1.length; i++)
			sumOfSquares += Math.pow(arr1[i] - arr2[i], 2);
		return (float) Math.sqrt(sumOfSquares);
	}
	
	/**
	 * Calculates the euclidean distance between two one-dimensional integer arrays of same length.
	 * @param arr1 - An array of integers
	 * @param arr2 - Another array of integers
	 * @return the euclidean distance between the two arrays.
	 */
	public static float euclideanDistance(int[] arr1, int[] arr2) {
		float sumOfSquares = 0.0f;
		for(int i = 0; i < arr1.length; i++)
			sumOfSquares += Math.pow(arr1[i] - arr2[i], 2);
		return (float) Math.sqrt(sumOfSquares);
	}
	
	/**
	 * Get the euclidean distance graph for this problem instance.
	 * @return a graph in which each pixel is connected to its cardinal neighbors with weights equal to the euclidean distances in HSB space
	 */
	public WeightedGraph getEuclideanDistanceGraph() {
		return euclideanDistanceGraph;
	}

	/**
	 * Get the euclidean distance between two pixels, in HSB color space.
	 * @param i - A pixel index
	 * @param j - Another pixel index
	 * @return the euclidean distance
	 */
	public float getEuclideanDistance(int i, int j) {
		return euclideanDistance(getHSB(i), getHSB(j));
	}

	@Override
	public String getName() {
		return name;
	}
}
