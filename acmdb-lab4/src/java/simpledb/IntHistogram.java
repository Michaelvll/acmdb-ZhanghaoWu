package simpledb;

/** A class to represent a fixed-width histogram over a single integer-based field.
 */
public class IntHistogram {

    private int bucketNum, min, max, width;
    private int histogram[];
    private int totalTuples;

    /**
     * Create a new IntHistogram.
     * 
     * This IntHistogram should maintain a histogram of integer values that it receives.
     * It should split the histogram into "histogram" histogram.
     * 
     * The values that are being histogrammed will be provided one-at-a-time through the "addValue()" function.
     * 
     * Your implementation should use space and have execution time that are both
     * constant with respect to the number of values being histogrammed.  For example, you shouldn't 
     * simply store every value that you see in a sorted list.
     * 
     * @param buckets The number of histogram to split the input value into.
     * @param min The minimum integer value that will ever be passed to this class for histogramming
     * @param max The maximum integer value that will ever be passed to this class for histogramming
     */
    public IntHistogram(int buckets, int min, int max) {
    	// some code goes here
        this.bucketNum = buckets;
        this.min = min;
        this.max = max;
        this.width = (max - min + 1 + buckets - 1) / buckets;
        this.histogram = new int[bucketNum];
        this.totalTuples = 0;
    }

    /**
     * Add a value to the set of values that you are keeping a histogram of.
     * @param v Value to add to the histogram
     */
    public void addValue(int v) {
    	// some code goes here
        ++histogram[getBucketId(v)];
        ++totalTuples;
    }

    /**
     * Estimate the selectivity of a particular predicate and operand on this table.
     * 
     * For example, if "op" is "GREATER_THAN" and "v" is 5, 
     * return your estimate of the fraction of elements that are greater than 5.
     * 
     * @param op Operator
     * @param v Value
     * @return Predicted selectivity of this particular operator and value
     */
    public double estimateSelectivity(Predicate.Op op, int v) {

    	// some code goes here
        int bucketId = getBucketId(v);
        double tuples = 0.0;
        double percentage = 0.0;

        switch (op) {
            case EQUALS:
                if (v < min || v > max) return 0.0;
                tuples = histogram[bucketId] * 1.0 / width;
                break;
            case NOT_EQUALS:
                return 1 - estimateSelectivity(Predicate.Op.EQUALS, v);
            case LESS_THAN:
                if (v <= min) return 0.0;
                if (v > max) return 1.0;
                for (int i = 0; i < bucketId; ++i) {
                    tuples += histogram[i];
                }
                percentage = (v - leftBound(bucketId)) * 1.0 / width;
                tuples += histogram[bucketId] * percentage;
                break;
            case LESS_THAN_OR_EQ:
                return estimateSelectivity(Predicate.Op.LESS_THAN, v + 1);
            case GREATER_THAN:
                if (v < min) return 1.0;
                if (v >= max) return 0.0;
                for (int i = bucketId + 1; i < bucketNum; ++i) {
                    tuples += histogram[i];
                }
                tuples += histogram[bucketId] * (rightBound(bucketId) - v - 1) * 1.0 / width;
                break;
            case GREATER_THAN_OR_EQ:
                return estimateSelectivity(Predicate.Op.GREATER_THAN, v - 1);
            default:
                throw new RuntimeException("Unknown op"+op.toString());

        }
        return tuples / totalTuples;
    }
    
    /**
     * @return
     *     the average selectivity of this histogram.
     *     
     *     This is not an indispensable method to implement the basic
     *     join optimization. It may be needed if you want to
     *     implement a more efficient optimization
     * */
    public double avgSelectivity()
    {
        // some code goes here
        return 1.0;
    }

    private int leftBound(int i) {
        return min + i * width;
    }
    private int rightBound(int i) {
        return min + (i+1) * width;
    }
    private int getBucketId(int v) {
        return v == max ? bucketNum - 1 : (v - min) / width;
    }
    
    /**
     * @return A string describing this histogram, for debugging purposes
     */
    public String toString() {
        // some code goes here
        return null;
    }
}
