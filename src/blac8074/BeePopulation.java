package blac8074;

import java.util.*;

/**
 * A BeePopulation consists of a generation of individuals (Bees).
 */
class Bee {
    BeeChromosome chromosome;
    float score;
}

public class BeePopulation {

    private final float ELITE_RATIO = 0.1f; // Percent of best bees that are copied to the new generation
    private final float MUTATION_RATE = 0.05f; // Percent of genes that are mutated each generation
    private final int TOURNAMENT_SIZE = 2; // Size of tournament in tournament selection

    private int populationSize;
    private Bee[] bees;
    private Random randGen;

    /*
     * Create random population of Bees of the given size
     */
    public BeePopulation(int populationSize) {
        this.populationSize = populationSize;
        bees = new Bee[populationSize];
        randGen = new Random();

        for (int i=0; i<populationSize; i++) {
            Bee bee = new Bee();
            bee.chromosome = new BeeChromosome(50.0, 50.0, 5000, 1000.0);
            bee.score = 0;
            bees[i] = bee;
        }
    }
    
    /*
     * Make a BeePopulation from an array of bees
     */
    public BeePopulation(Bee[] beeArr) {
    	this.populationSize = beeArr.length;
        bees = new Bee[populationSize];
        randGen = new Random();
        
    	for (int i = 0; i < this.populationSize; i++) {
    		this.bees[i] = beeArr[i];
    	}
    }
    
    /*
     * Creates a new population using the current population
     */
    public void createNewGeneration() {
    	// Sort reverse score order
        Arrays.sort(bees, (bee1, bee2) -> (Float.compare(bee2.score, bee1.score)));

        Bee[] newBees = new Bee[populationSize];
        
        // Copy elites to new generation
        for (int i = 0; i < (int)(populationSize * ELITE_RATIO); i++) {
        	newBees[i] = bees[i];
        }
        
        // Create non-elite individuals
        for (int i = (int)(populationSize * ELITE_RATIO); i < populationSize; i++) {
        	// Select parents for new individual
        	Bee bee1 = tournamentSelect();
        	Bee bee2 = tournamentSelect();
        	// Create new individual
        	newBees[i] = new Bee();
        	// Create chromosome for new individual using crossover
        	newBees[i].chromosome = new BeeChromosome(bee1.chromosome, bee2.chromosome);
        	// Mutate chromosome (each gene in chromosome has MUTATION_RATE chance to mutate)
        	newBees[i].chromosome.mutate(MUTATION_RATE);
        }
        
        bees = newBees;
    }
    
    /*
     * Returns the Bee with the highest score out of TOURNAMENT_SIZE randomly selected Bees
     */
    public Bee tournamentSelect() {
    	Bee bestBee = new Bee();
    	bestBee.score = Float.MIN_VALUE;
    	for (int i = 0; i < TOURNAMENT_SIZE; i++) {
    		Bee selectedBee = bees[randGen.nextInt(populationSize)];
    		if (selectedBee.score > bestBee.score) {
    			bestBee = selectedBee;
    		}
    	}
    	return bestBee;
    }
    
    /*
     * Returns the String representation of each BeeChromsome that corresponds to each Bee, followed by a new line
     */
    public String toString() {
    	String beeString = "";
    	for (Bee bee:bees) {
    		beeString += bee.chromosome.toString() + '\n';
    	}
    	return beeString;
    }
}
