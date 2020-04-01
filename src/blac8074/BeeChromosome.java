package blac8074;

public class BeeChromosome {
    public double translationalKp;
    public double rotationalKp;
    public int lowEnergyThresh;
    public double shootEnemyDist;
    // TODO: more genes?

    /*
    And on the first day God created a bee
     */
    public BeeChromosome(double maxP, double maxD, int maxLowEnergy, double maxShootDist) {
        this.translationalKp = Math.random() * maxP;
        this.rotationalKp = Math.random() * maxD;
        this.lowEnergyThresh = (int)(Math.random() * maxLowEnergy);
        this.shootEnemyDist = Math.random() * maxShootDist;
    }

    /*
    When two bees love each other very much
     */
    public BeeChromosome(BeeChromosome mommy, BeeChromosome daddy) {
        /*
    	if (Math.random() < 0.5) {
            this.pGainVel = mommy.pGainVel;
            this.dGainVel = daddy.dGainVel;
        } else {
            this.pGainVel = daddy.pGainVel;
            this.dGainVel = mommy.dGainVel;
        }
        */
    	this.translationalKp = (mommy.translationalKp + daddy.translationalKp) / 2.0f;
    	this.rotationalKp = (mommy.rotationalKp + daddy.rotationalKp) / 2.0f;
    	this.lowEnergyThresh = (mommy.lowEnergyThresh + daddy.lowEnergyThresh) / 2;
    	this.shootEnemyDist = (mommy.shootEnemyDist + daddy.shootEnemyDist) / 2.0;
    }

    public BeeChromosome() {
    	
	}

	public void mutate(float mutationRate) {
		// TODO: is there a better way to do this?
		// TODO: how are we supposed to mutate each gene?
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
    	// Add more as we add more genes
    	// values += ... + ","
    	return values;
    }
}
