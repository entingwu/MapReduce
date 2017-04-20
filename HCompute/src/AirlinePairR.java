/*
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.WritableComparable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class AirlinePairR implements WritableComparable {

    private Text airlineName;
    private Text month;

    public AirlinePairR() {
        this.airlineName = new Text();
        this.month = new Text();
    }

    public AirlinePairR(Text airLineName, Text month) {
        this.airlineName = airLineName;
        this.month = month;
    }

    public Text getAirlineName() {
        return airlineName;
    }

    public Text getMonth() {
        return month;
    }

    */
/** Compare airlineName and month *//*

    @Override
    public int compareTo(Object o) {
        AirlinePairR ap2 = (AirlinePairR)o;
        // airlineName
        int cmp = this.getAirlineName().compareTo(ap2.getAirlineName());
        if (cmp != 0) {
            return cmp;
        }
        // month increasing order
        int month1 = Integer.parseInt(this.getMonth().toString());
        int month2 = Integer.parseInt(ap2.getMonth().toString());
        return month1 - month2;
    }

    @Override
    public void write(DataOutput dataOutput) throws IOException {
        this.airlineName.write(dataOutput);
        this.month.write(dataOutput);
    }

    @Override
    public void readFields(DataInput dataInput) throws IOException {
        if (this.airlineName == null)
            this.airlineName = new Text();

        if (this.month == null)
            this.month = new Text();

        this.airlineName.readFields(dataInput);
        this.month.readFields(dataInput);
    }
}
*/
