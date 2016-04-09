/**
 * @author pachecog@purdue.edu
 * @version 3/27/2016
 */

public class Opinion implements Instance {

    public String topic;
    public String debate;
    public int id;
    public int pid;
    public int stance;
    public String rebuttal;
    public String text;
    public String author;
    public int weakStance;

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Opinion)) {
            return false;
        }

        Opinion other = (Opinion) obj;
        return (other.topic.equals(this.topic) &&
                other.debate.equals(this.debate) &&
                other.id == this.id);
    }

    @Override
    public String toString() {
        String ret = "";
        ret += "Topic=" + this.topic + "\n";
        ret += "Debate=" + this.debate + "\n";
        ret += "ID=" + this.id + "\n";
        ret += "PID=" + this.pid + "\n";
        ret += "Stance=" + this.stance + "\n";
        ret += "Rebuttal=" + this.rebuttal + "\n";
        ret += "Author=" + this.author;

        return ret;
    }

    public void setWeakLabel(int label) {
        this.weakStance = label;
    }

}
