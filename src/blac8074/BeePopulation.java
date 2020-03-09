package blac8074;

public class BeePopulation {

    private BeeChromosome[] bees;
    private int currBee = 0;

    public BeePopulation(int populationSize) {
        bees = new BeeChromosome[populationSize];

        for (int i=0; i < bees.length; i++) {
            bees[i] = new BeeChromosome(50, 50);
        }
    }

    public BeeChromosome getNextBee() {
        if (currBee == bees.length - 1) {
            currBee = 0;

            // Crossover

            // Mutate
        } else {
            currBee++;
        }

        System.out.println("Bee #" + currBee + " coming in HOT!");

        return bees[currBee];
    }
}
