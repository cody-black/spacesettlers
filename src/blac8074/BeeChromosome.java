package blac8074;

public class BeeChromosome {
    public float pGainVel;
    public float dGainVel;

    /*
    And on the first day God created a bee
     */
    public BeeChromosome(float maxP, float maxD) {
        this.pGainVel = (float) (Math.random() * maxP);
        this.dGainVel = (float) (Math.random() * maxD);
    }

    /*
    When two bees love each other very much
     */
    public BeeChromosome(BeeChromosome mommy, BeeChromosome daddy) {
        this.pGainVel = mommy.pGainVel;
        this.dGainVel = daddy.dGainVel;
    }
}
