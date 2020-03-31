package blac8074;

public class BeeChromosome {
    public float pGainVel;
    public float dGainVel;
    public int lowEnergyThresh;
    public double shootEnemyDist;
    // TODO: more genes?

    /*
    And on the first day God created a bee
     */
    public BeeChromosome(float maxP, float maxD, int maxLowEnergy, double maxShootDist) {
        this.pGainVel = (float) (Math.random() * maxP);
        this.dGainVel = (float) (Math.random() * maxD);
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
    	this.pGainVel = (mommy.pGainVel + daddy.pGainVel) / 2.0f;
    	this.dGainVel = (mommy.dGainVel + daddy.dGainVel) / 2.0f;
    	this.lowEnergyThresh = (mommy.lowEnergyThresh + daddy.lowEnergyThresh) / 2;
    	this.shootEnemyDist = (mommy.shootEnemyDist + daddy.shootEnemyDist) / 2.0;
    }

    public BeeChromosome() {
    	
	}

	public void mutate(float mutationRate) {
		// TODO: is there a better way to do this?
		// TODO: how are we supposed to mutate each gene?
		if (Math.random() < mutationRate) {
			this.pGainVel += (Math.random() - 0.5) * pGainVel;
		}
		
		if (Math.random() < mutationRate) {
			this.dGainVel += (Math.random() - 0.5) * dGainVel;
		}
		
		if (Math.random() < mutationRate) {
			this.lowEnergyThresh += (Math.random() - 0.5) * lowEnergyThresh;
		}
		
		if (Math.random() < mutationRate) {
			this.shootEnemyDist += (Math.random() - 0.5) * shootEnemyDist;
		}
    }
    
    /*
     * Returns a String containing the values of the genes of the chromosome followed by commas
     */
    public String toString() {
    	String values = "";
    	values += Float.toString(pGainVel) + ",";
    	values += Float.toString(dGainVel) + ",";
    	values += Integer.toString(lowEnergyThresh) + ",";
    	values += Double.toString(shootEnemyDist) + ",";
    	// Add more as we add more genes
    	// values += ... + ","
    	return values;
    }
}
