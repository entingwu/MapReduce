import com.opencsv.CSVParser;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.io.WritableComparator;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Partitioner;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;

import java.io.IOException;

public class AverageAirlineMonthlyDelay {
    private static final CSVParser parser = new CSVParser(',', '"');

    public static class AverageAirlineMonthlyDelayMapper extends
            Mapper<Object, Text, AirlinePair, Text> {

        public void map(Object object, Text value, Context context)
                throws IOException, InterruptedException {
            String[] entry = parser.parseLine(value.toString());
            AirlinePair keyPair;
            Text arrDelayMinutes;
            // <[airlineName, month], arrDelayMinutes>
            if (entry.length >= 1 && isValid(entry)) {
                String airlineName = entry[6];
                String month = entry[2];
                String arrDelay = entry[37];
                keyPair = new AirlinePair(new Text(airlineName), new Text(month));
                arrDelayMinutes = new Text(arrDelay);
                context.write(keyPair, arrDelayMinutes);
            }
        }

        private boolean isValid(String[] entry) {
            if (entry == null || entry.length == 0) {
                return false;
            }
            // Missing Info
            if (entry[0].isEmpty() || entry[2].isEmpty() || entry[5].isEmpty() ||
                    entry[6].isEmpty() || entry[37].isEmpty() || entry[41].isEmpty()) {
                return false;
            }
            // Invalid flight
            if (!entry[0].equals("2008") || entry[41].equals("1")) {
                return false;
            }
            return true;
        }
    }

    /**  The Partitioner puts all records for the same airlineName in the same Reduce Task. */
    public static class AirlinePartitioner extends Partitioner<AirlinePair, Text> {
        @Override
        public int getPartition(AirlinePair keyPair, Text value, int numPartitions) {
            return Math.abs(keyPair.getAirlineName().hashCode() * 127) % numPartitions;
        }
    }

    /** KeyComparator sorts the airlineName first and then sorts month for all Reduce calls in the same Reduce Task */
    public static class KeyComparator extends WritableComparator {
        protected KeyComparator() {
            super(AirlinePair.class, true);
        }

        public int compare(WritableComparable w1, WritableComparable w2) {
            AirlinePair ap1 = (AirlinePair) w1;
            AirlinePair ap2 = (AirlinePair) w2;
            // compare airlineName and month
            return ap1.compareTo(ap2);
        }
    }

    /** GroupComparator passes all flight month delay of the given airlineName to the same Reduce call */
    public static class GroupComparator extends WritableComparator {
        protected GroupComparator() {
            super(AirlinePair.class, true);
        }

        public int compare(WritableComparable w1, WritableComparable w2) {
            AirlinePair ap1 = (AirlinePair) w1;
            AirlinePair ap2 = (AirlinePair) w2;
            // compare airlineName
            return ap1.getAirlineName().compareTo(ap2.getAirlineName());
        }
    }

    public static class AverageAirlineMonthlyDelayReducer
            extends Reducer<AirlinePair, Text, Text, Text> {

        /** reduce() will be called every unique airlineName */
        public void reduce(AirlinePair keyPair, Iterable<Text> values, Context context)
                throws IOException, InterruptedException {
            int month = 1;
            int flightNum = 0;
            int avgDelay = 0;
            double totalDelay = 0;

            StringBuilder sb = new StringBuilder();

            // 1. Add AirlineName
            sb.append(keyPair.getAirlineName().toString());

            // 2. Add (month, average delay)
            for (Text value : values) {
                // check if it is next month
                int nextMonth = Integer.parseInt(keyPair.getMonth().toString());
                if (month != nextMonth) {
                    avgDelay = (int)(totalDelay * 1.0 / flightNum);
                    sb.append(",(").append(month).append(",").append(avgDelay).append(")");
                    month = nextMonth;
                    totalDelay = 0;
                    flightNum = 0;
                }
                totalDelay += Double.parseDouble(value.toString());
                flightNum++;
            }

            // 3. Add last Month and average delay
            avgDelay = (int)(totalDelay * 1.0 / flightNum);
            sb.append(",(").append(month).append(",").append(avgDelay).append(")");

            // 4. Missing data
            while (month < 12) {
                month++;
                sb.append(",(").append(month).append(",").append(0).append(")");
            }

            // 5. Every unique airline monthly delay pattern
            context.write(new Text(), new Text(sb.toString()));
        }
    }

    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();
        String[] otherArgs = new GenericOptionsParser(conf, args).getRemainingArgs();
        if (otherArgs.length < 2) {
            System.err.println("Usage: AverageAirlineMonthlyDelay <in> <out>");
            System.exit(2);
        }
        // Define MapReduce job
        Job job = new Job(conf, "Average Airline Monthly Delay");
        job.setJarByClass(AverageAirlineMonthlyDelay.class);

        // Set Mapper and Reducer classes
        job.setMapperClass(AverageAirlineMonthlyDelayMapper.class);
        job.setReducerClass(AverageAirlineMonthlyDelayReducer.class);
        job.setPartitionerClass(AirlinePartitioner.class);
        job.setSortComparatorClass(KeyComparator.class);
        job.setGroupingComparatorClass(GroupComparator.class);

        // Output types
        job.setOutputKeyClass(AirlinePair.class);
        job.setOutputValueClass(Text.class);//delay

        // Submit job
        FileInputFormat.addInputPath(job, new Path(otherArgs[0]));
        FileOutputFormat.setOutputPath(job, new Path(otherArgs[1]));
        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}
