package blac8074;

/**
 * A BeeChromosome stores genes representing characteristics of an individual (a Bee).
 */
public class BeeChromosome {
    public double translationalKp;
    public double rotationalKp;
    public int lowEnergyThresh;
    public double shootEnemyDist;

    /*
     * Creates a random individual given the max values for each gene
     */
    public BeeChromosome(double maxP, double maxD, int maxLowEnergy, double maxShootDist) {
        this.translationalKp = Math.random() * maxP;
        this.rotationalKp = Math.random() * maxD;
        this.lowEnergyThresh = (int)(Math.random() * maxLowEnergy);
        this.shootEnemyDist = Math.random() * maxShootDist;
    }

    /*
     * Creates a child from two parents using crossover
     */
    public BeeChromosome(BeeChromosome mommy, BeeChromosome daddy) {
    	this.translationalKp = (mommy.translationalKp + daddy.translationalKp) / 2.0;
    	this.rotationalKp = (mommy.rotationalKp + daddy.rotationalKp) / 2.0;
    	this.lowEnergyThresh = (mommy.lowEnergyThresh + daddy.lowEnergyThresh) / 2;
    	this.shootEnemyDist = (mommy.shootEnemyDist + daddy.shootEnemyDist) / 2.0;
    }

    public BeeChromosome() {
    	
	}
    
    /*
     * Potentially mutates each gene by setting its value to a random value between 0 and 2x the current value
     */
	public void mutate(float mutationRate) {
		if (Math.random() < mutationRate) {
			this.translationalKp = (2.0 * Math.random()) * translationalKp;
		}
		
		if (Math.random() < mutationRate) {
			this.rotationalKp = (2.0 * Math.random()) * rotationalKp;
		}
		
		if (Math.random() < mutationRate) {
			this.lowEnergyThresh = (int)((2.0 * Math.random()) * lowEnergyThresh);
		}
		
		if (Math.random() < mutationRate) {
			this.shootEnemyDist = (2.0 * Math.random()) * shootEnemyDist;
		}
    }
    
    /*
     * Returns a String containing the values of the genes of the chromosome followed by commas
     */
    public String toString() {
    	String values = "";
    	values += Double.toString(translationalKp) + ",";
    	values += Double.toString(rotationalKp) + ",";
    	values += Integer.toString(lowEnergyThresh) + ",";
    	values += Double.toString(shootEnemyDist) + ",";
    	return values;
    }
}
