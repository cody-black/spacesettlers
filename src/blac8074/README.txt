Bee Beehavior Project 2

GA Learning Setup

To make the learning work, you'll first need to add the BeehaviorTeamClient to a ladder with
some other clients. After that, in your LadderConfig.xml file, numThreads needs to be 1 or else 
the file IO will probably mess up and things will break. In SpaceSettlersConfig.xml, I recommend 
lowering simulationSteps from the default 20000 to something lower like 2500 or else the learning 
will take a very long time. Make sure there is a file in the blac8074 directory called 
individualNumber.txt that contains only the number 0. In BeehaviorTeamClient.java make sure that 
GA_LEARNING is true and that GENERATION_SIZE is set to your desired generation size (I've been 
using 40). 

If everything works correctly, a file called gen0.csv should be created in the blac8074 directory
and be filled with random gene values for GENERATION_SIZE individuals. As the ladder games are 
completed, Spacesettlers scores and fitness scores will be added for each individual. When a score 
has been added for each individual in the generation, a new generation will be created and stored
in gen1.csv. This process will repeat until the ladder finishes all of its games or until you
manually stop the ladder.


Learning Visualization

Since the learning takes place exlusively on a ladder, it's not really possible to watch the learning
in real time. However, after setting GA_LEARNING to false in BeehaviorTeamClient.java, you can specify
gene values in the initialize function and run the agent without a ladder. Setting LEARNING_GRAPHICS
to true will visually depict the shootEnemyDist value as a white circle around the ship. However,
since CirceGraphics are filled by default, I recommend you COMMENT OUT GRAPHICS.FILL(SHAPE) in the 
draw function in CircleGraphics.java which is found in spacesettlers.graphics. If you don't do this, 
a large white circle will obscure much of the screen.