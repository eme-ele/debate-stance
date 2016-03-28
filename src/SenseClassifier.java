import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

/**
 * @author pachecog@purdue.edu
 * @version 3/27/2016
 */

public class SenseClassifier {

    public int maxIter;
    public double learnRate;
    public List<Float> weights;
    public float bias;

    public SenseClassifier(int maxIter, double learnRate) {
        this.maxIter = maxIter;
        this.learnRate = learnRate;
    }

    /**
     * Initialize weights to 0
     * @param numFeats number of features
     * */
    public void initWeights(int numFeats) {
        this.weights = new ArrayList<>();
        for (int i = 0; i < numFeats; i++) {
            this.weights.add((float)0.0);
        }
        this.bias = (float)0.0;
    }

    public int sign(HashMap<Integer, Float> sample) {
        float dotProduct = (float)0.0;
        for (Map.Entry<Integer, Float> entry: sample.entrySet()) {
            dotProduct += entry.getValue() * this.weights.get(entry.getKey());
        }
        dotProduct += this.bias;
        return (dotProduct > 0) ? 1 : -1;
    }

    public void updateWeights(HashMap<Integer, Float> sample, Integer label) {
        this.bias += this.learnRate * label;
        for (Map.Entry<Integer, Float> entry: sample.entrySet()) {
            float w = this.weights.get(entry.getKey());
            this.weights.set(entry.getKey(), w + (float)this.learnRate * label);
        }
    }

    public void train(List<HashMap<Integer, Float> > dataset, List<Integer> labels, int numFeats) {
        // indexes for shuffling input
        ArrayList<Integer> indexList = new ArrayList<>();
        for (int i = 0; i < dataset.size(); i++) {
            indexList.add(i);
        }

        initWeights(numFeats);
        for (int i = 0; i < this.maxIter; i++) {
            int numMistakes = 0;

            // shuffle here
            long seed = System.nanoTime();
            Collections.shuffle(indexList, new Random(seed));

            for (int j = 0; j < indexList.size(); j++) {
                int index = indexList.get(j);
                Integer y_gold = labels.get(index);
                int y_pred = sign(dataset.get(index));

                if (y_gold != y_pred) {
                    numMistakes += 1;
                    updateWeights(dataset.get(index), y_gold);
                }
            }

            System.out.println(1 - ((1.0*numMistakes)/dataset.size()));
            if (numMistakes == 0)
                return;
        }
    }


}
