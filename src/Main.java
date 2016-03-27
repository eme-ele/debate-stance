/**
 * @author pachecog@purdue.edu
 * @version 3/27/2016
 */

public class Main {

    public static void main(String[] args) {

        try {
            DataParser.getFolds();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
