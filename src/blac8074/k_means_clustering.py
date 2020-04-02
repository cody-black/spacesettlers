import numpy as np
import random
import matplotlib.pyplot as plt
import matplotlib.cm as cm

# k-means clustering k-value
k = 10

# column index to cluster on
cluster_index = 5

# Load generation 0 data (all random)
data = np.genfromtxt("gen0.csv", delimiter=',')

# Choose random unique, sorted, clusters
cluster_set = list(set([data[i][cluster_index] for i in range(len(data))]))
random.shuffle(cluster_set)
cluster_means = cluster_set[0:k]
cluster_means = sorted(cluster_means)
new_clusters = [0] * k

# View initial cluster
print(np.array(cluster_means))

# Perform K-means clustering
while True:
    # Dictionary of closest cluster to each pt
    closest_to_index = {}

    # Populate closest_to_index
    for i in range(len(data)):
        closest_index = 0
        closest_dist = abs(cluster_means[0] - data[i][cluster_index])
        for j in range(1, k):
            dist = abs(cluster_means[j] - data[i][cluster_index])
            if dist < closest_dist:
                closest_dist = dist
                closest_index = j

        closest_to_index[i] = closest_index

    # Perform k-means clustering cluster shifting
    # Very slow solution, but it works
    for j in range(k):
        summ = 0
        num = 0

        for i in range(len(data)):
            if closest_to_index[i] == j:
                summ += data[i][cluster_index]
                num += 1

        new_clusters[j] = summ / num if num != 0 else cluster_means[j]

    # View new clusters
    print(np.array(new_clusters))

    # When our clusters stop changing, we have converged
    if new_clusters == cluster_means:
        print("Done!")

        # Create pretty plot
        fig, ax = plt.subplots()

        outdata = []

        for j in range(k):
            for i in range(len(data)):
                if closest_to_index[i] == j:
                    outdata.append(list(data[i]) + [j])

        outdata = np.array(outdata)

        cmap = cm.get_cmap('PiYG', k)
        plt.scatter(outdata[:, 2], outdata[:, 3], label=outdata[:, 6], c=outdata[:, 6], cmap=cmap)
        plt.colorbar()
        ax.set_xlabel('Max Lower Energy')
        ax.set_ylabel('Max Shoot Distance')

        plt.show()

        # Optionally output data to csv
        # np.savetxt("out.csv", outdata, delimiter=',')
        break

    # Use new clusters as clusters for next iteration
    cluster_means = new_clusters
