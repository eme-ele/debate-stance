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

    public static void runLocalClassifier(StanfordCoreNLP pipeline,
                                          List<Instance> trainingSet,
                                          List<Integer> trainingLabels,
                                          List<Instance> testingSet,
                                          List<Integer> testingLabels)
    {
        FeatureExtractor fe = new FeatureExtractor(pipeline);
        fe.extractFeatures(trainingSet);
        fe.selectFeatures();
        List<HashMap<Integer, Float> > featMatrixTrain = fe.getFeatureMatrix(trainingSet);
        List<HashMap<Integer, Float> > featMatrixTest = fe.getFeatureMatrix(testingSet);

        Perceptron classifier = new Perceptron(100, 0.1);
        classifier.train(featMatrixTrain, trainingLabels, fe.numFeatures);
        classifier.test(featMatrixTest, testingLabels);
    }

    public static void runGlobalClassifier(StanfordCoreNLP pipeline,
                                           List<Instance> trainingSet,
                                           List<Instance> testingSet)
    {
        List<Tree> trainingTrees = DataParser.getAllTrees(trainingSet);
        List<Tree> testingTrees = DataParser.getAllTrees(testingSet);
        // instances by author
        HashMap<String, List<Instance>> byAuthorTrain = DataParser.getByAuthor(trainingSet);
        HashMap<String, List<Instance>> byAuthorTest = DataParser.getByAuthor(trainingSet);

        EdgeFeatureExtractor fe = new EdgeFeatureExtractor(pipeline);
        fe.extractFeatures(trainingSet);
        fe.selectFeatures();
        StructuredPerceptron classifier = new StructuredPerceptron(200, (float)0.1, fe);
        classifier.train(trainingTrees, byAuthorTrain);
        classifier.test(testingTrees, byAuthorTest);
    }

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
                List<Instance> testingSet = new ArrayList<>();
                List<Integer> testingLabels = new ArrayList<>();
                for (int j = 0; j < Config.NFOLDS; j++) {
                    if (j != i) {
                        trainingSet.addAll(folds.get(j).values());
                        trainingLabels.addAll(DataParser.parseLabels(folds.get(j).values()));
                    } else {
                        testingSet.addAll(folds.get(j).values());
                        testingLabels.addAll(DataParser.parseLabels(folds.get(j).values()));
                    }
                }

                System.out.println("Fold " + i);

                System.out.println("Local classifier");
                runLocalClassifier(pipeline, trainingSet, trainingLabels, testingSet, testingLabels);
                System.out.println("Global classifier");
                runGlobalClassifier(pipeline, trainingSet, testingSet);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
