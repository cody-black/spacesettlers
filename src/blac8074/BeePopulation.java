package blac8074;

import java.util.*;

class Bee {
    BeeChromosome chromosome;
    float score;
}

public class BeePopulation {

    private final float ELITE_RATIO = 0.1f;
    private final float MUTATION_RATE = 0.05f; // Percent of genes that are mutated each generation
    private final int TOURNAMENT_SIZE = 5;

    private int populationSize;
    private Bee[] bees;
    private int currBee = -1;
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
            bee.chromosome = new BeeChromosome(50, 50);
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
     * I don't think this being used?
     */
    public BeeChromosome getNextBee(float score) {
        if (currBee == -1) {
            // Initialization
            currBee = 0;
        } else {
            bees[currBee].score = score;
            if (currBee == bees.length - 1) {
                // End of generation
                currBee = 0;
                this.createNewGeneration();
            } else {
                currBee++;
            }
        }

        return bees[currBee].chromosome;
    }  
    
    /*
     * Creates a new population using the current population
     */
    public void createNewGeneration() {
    	// Sort reverse score order
        Arrays.sort(bees, (bee1, bee2) -> (Float.compare(bee2.score, bee1.score)));

        //List<Bee> beeList = Arrays.asList(bees);

        System.out.println("Best Score " + bees[0].score);
        System.out.println("P: " + bees[0].chromosome.pGainVel + ", D: " + bees[0].chromosome.dGainVel);

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
        /*
        // Selection (Tournament)
        Bee[] newBees = new Bee[populationSize];

        for (int i=0; i < populationSize * ELITE_RATIO; i++) {
            Collections.shuffle(beeList);

            Bee bestBee = beeList.get(0);
            for (int j=1; j<TOURNAMENT_SIZE; j++) {
                if (beeList.get(j).score > bestBee.score) {
                    bestBee = beeList.get(j);
                }
            }

            newBees[i] = bestBee;
        }

        // Crossover
        for (int i=(int)(populationSize * ELITE_RATIO); i < populationSize; i++) {
            // TODO: bees can self breed??
            Bee bee1 = newBees[(int)(Math.random() * ELITE_RATIO * populationSize)];
            Bee bee2 = newBees[(int)(Math.random() * ELITE_RATIO * populationSize)];

            newBees[i] = new Bee();
            newBees[i].chromosome = new BeeChromosome(bee1.chromosome, bee2.chromosome);
            if (Math.random() < MUTATION_RATE) {
                newBees[i].chromosome.mutate();
            }
        }

        // Reset scores
        for (Bee bee : newBees) {
            bee.score = 0;
        }
        
        bees = newBees;
        */
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
