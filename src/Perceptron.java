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

public class Perceptron {

    public int maxIter;
    public double learnRate;
    public List<Float> weights;
    public float bias;

    public Perceptron(int maxIter, double learnRate) {
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

            double trainAccuracy = 1 - ((1.0*numMistakes)/dataset.size());
            //System.out.println("Training accuracy: " + trainAccuracy);
            if (numMistakes == 0)
                return;
        }
    }

    public void test(List<HashMap<Integer, Float> > dataset, List<Integer> labels) {
        int i = 0;
        int numMistakes = 0;

        for (HashMap<Integer, Float> sample: dataset) {
            int pred_label = sign(sample);
            int true_label = labels.get(i);
            if (pred_label != true_label) {
                numMistakes += 1;
            }
            i++;
        }

        double testAccuracy = 1 - ((1.0*numMistakes)/i);
        System.out.println("Total examples: " + i);
        System.out.println("Accuracy: " + testAccuracy);
    }


    public List<Integer> predict(List<HashMap<Integer, Float> > dataset)
    {
        List<Integer> predLabels = new ArrayList<>();
        for (HashMap<Integer, Float> sample: dataset) {
            int predLabel = sign(sample);
            predLabels.add(predLabel);
        }
        return predLabels;
    }

}
