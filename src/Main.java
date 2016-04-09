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


        Perceptron classifier = new Perceptron(100, 0.1);
        classifier.train(featMatrixTrain, trainingLabels, fe.numFeatures);

        HashMap<String, List<Instance>> byTopic = DataParser.filterByTopic(testingSet);
        for (String topic: byTopic.keySet()) {
            System.out.println("Testing topic: " + topic);
            List<HashMap<Integer, Float> > featMatrixTest = fe.getFeatureMatrix(byTopic.get(topic));
            List<Integer> labels = DataParser.parseLabels(byTopic.get(topic));
            classifier.test(featMatrixTest, labels);
        }
        System.out.println("Cross-topic:");
        List<HashMap<Integer, Float> > featMatrixTest = fe.getFeatureMatrix(testingSet);
        classifier.test(featMatrixTest, testingLabels);
    }

    public static void runGlobalClassifier(StanfordCoreNLP pipeline,
                                           List<Instance> trainingSet,
                                           List<Integer> trainingLabels,
                                           List<Instance> testingSet,
                                           List<Integer> testingLabels)
    {

        System.out.println("Training local classifier to get initial hints...");
        FeatureExtractor fe0 = new FeatureExtractor(pipeline);
        fe0.extractFeatures(trainingSet);
        fe0.selectFeatures();
        List<HashMap<Integer, Float> > featMatrixTrain = fe0.getFeatureMatrix(trainingSet);
        Perceptron weakClassifier = new Perceptron(100, 0.1);
        weakClassifier.train(featMatrixTrain, trainingLabels, fe0.numFeatures);

        System.out.println("Running and updating initial predictions...");
        List<Integer> predLabels = weakClassifier.predict(featMatrixTrain);

        for (int i = 0; i < trainingSet.size(); i++) {
            trainingSet.get(i).setWeakLabel(predLabels.get(i));
        }

        List<Tree> trainingTrees = DataParser.getAllTrees(trainingSet);
        HashMap<String, List<Instance>> byAuthorTrain = DataParser.getByAuthor(trainingSet);
        EdgeFeatureExtractor fe = new EdgeFeatureExtractor(pipeline);
        fe.extractFeatures(trainingSet);
        fe.selectFeatures();
        StructuredPerceptron classifier = new StructuredPerceptron(100, (float)0.5, fe);
        // pass true to use author-level features
        classifier.train(trainingTrees, byAuthorTrain, true);


        HashMap<String, List<Instance>> byTopic = DataParser.filterByTopic(testingSet);
        for (String topic: byTopic.keySet()) {
            System.out.println("Testing topic: " + topic);
            List<Tree> testingTrees = DataParser.getAllTrees(byTopic.get(topic));
            HashMap<String, List<Instance>> byAuthorTest = DataParser.getByAuthor(byTopic.get(topic));
            // pass true to use author-level features
            classifier.test(testingTrees, byAuthorTest, true);
        }

        List<Tree> testingTrees = DataParser.getAllTrees(testingSet);
        HashMap<String, List<Instance>> byAuthorTest = DataParser.getByAuthor(testingSet);
        // pass true to use author-level features
        System.out.println("Cross-topic:");
        classifier.test(testingTrees, byAuthorTest, true);
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

                System.out.println("\nFold " + i);

                //System.out.println("Local classifier");
                //runLocalClassifier(pipeline, trainingSet, trainingLabels, testingSet, testingLabels);
                runGlobalClassifier(pipeline, trainingSet, trainingLabels, testingSet, testingLabels);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
