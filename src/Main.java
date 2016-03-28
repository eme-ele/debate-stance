import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Properties;

import edu.stanford.nlp.pipeline.StanfordCoreNLP;

/**
 * @author pachecog@purdue.edu
 * @version 3/27/2016
 */

public class Main {

    public static void main(String[] args) {

        Properties props =  new Properties();
        props.put("annotators", "tokenize, ssplit");
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

        try {
            List<HashMap<String, Instance> > folds = DataParser.parseFolds();

            // Go through each fold
            for (int i = 0; i < Config.NFOLDS; i++) {
                List<Instance> trainingSet = new ArrayList<>();
                List<Integer> trainingLabels = new ArrayList<>();
                for (int j = 0; j < Config.NFOLDS; j++) {
                    if (j != i) {
                        trainingSet.addAll(folds.get(j).values());
                        trainingLabels.addAll(DataParser.parseLabels(folds.get(j).values()));
                    }
                }

                FeatureExtractor fe = new FeatureExtractor(pipeline);
                fe.extractFeatures(trainingSet);
                fe.selectFeatures();
                List<HashMap<Integer, Float> > feats = fe.getFeatureVector(trainingSet);

                SenseClassifier classifier = new SenseClassifier(1000, 0.1);
                classifier.train(feats, trainingLabels, fe.numFeatures);
                System.exit(1);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
