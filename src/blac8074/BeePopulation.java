package blac8074;

import java.util.Arrays;
import java.util.Comparator;

class Bee {
    BeeChromosome chromosome;
    float score;
}

public class BeePopulation {

    private final float ELITE_RATIO = 0.3f;
    private final float MUTATION_RATE = 0.05f; // Percent of chromosomes that are mutated each generation

    private int populationSize;
    private Bee[] bees;
    private int currBee = -1;

    public BeePopulation(int populationSize) {
        this.populationSize = populationSize;
        bees = new Bee[populationSize];

        for (int i=0; i<populationSize; i++) {
            Bee bee = new Bee();
            bee.chromosome = new BeeChromosome(50, 50);
            bee.score = 0;
            bees[i] = bee;
        }
    }

    public BeeChromosome getNextBee(float score) {
        if (currBee == -1) {
            // Initialization
            currBee = 0;
        } else {
            bees[currBee].score = score;
            if (currBee == bees.length - 1) {
                // End of generation
                currBee = 0;

                // Sort reverse score order
                Arrays.sort(bees, (bee1, bee2) -> (Float.compare(bee2.score, bee1.score)));

                System.out.println("Best Score " + bees[0].score);
                System.out.println("P: " + bees[0].chromosome.pGainVel + ", D: " + bees[0].chromosome.dGainVel);


                // Crossover (By replacing bottom 1-ELITE_RATIO percentile bees)

                for (int i=(int)(populationSize * ELITE_RATIO); i < populationSize; i++) {
                    // TODO: bees can self breed??
                    Bee bee1 = bees[(int)(Math.random() * ELITE_RATIO * populationSize)];
                    Bee bee2 = bees[(int)(Math.random() * ELITE_RATIO * populationSize)];

                    bees[i] = new Bee();
                    bees[i].chromosome = new BeeChromosome(bee1.chromosome, bee2.chromosome);
                    if (Math.random() < MUTATION_RATE) {
                        bees[i].chromosome.mutate();
                    }
                    bees[i].score = 0;
                }

                // Reset scores
                for (Bee bee : bees) {
                    bee.score = 0;
                }
            } else {
                currBee++;
            }
        }

        return bees[currBee].chromosome;
    }
}
