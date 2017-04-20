/*
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.filter.CompareFilter.CompareOp;
import org.apache.hadoop.hbase.filter.FilterList;
import org.apache.hadoop.hbase.filter.SingleColumnValueFilter;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableMapReduceUtil;
import org.apache.hadoop.hbase.mapreduce.TableMapper;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.io.WritableComparator;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Partitioner;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;

import java.io.IOException;

public class HComputeR {
    private static final String TABLE_NAME = "AirlineDelay";
    private static final String COLUMN_FAMILY = "Airline";
    private static final String CANCELLED = "cancelled";
    private static final String AIR_DELAY_MINUTES = "airDelayMinutes";
    private static final String YEAR = "year";
    private static final String DIV = "$";
    private static final String YEAR_FILTER = "2008";
    private static final String CANCELLED_FILTER = "0";

    public static class HComputeMapper extends
            TableMapper<AirlinePair, Text> {

        public void map(ImmutableBytesWritable object, Result value, Context context)
                throws IOException, InterruptedException {
            System.out.print("Mapper running .. ");
            String[] rowKey = new String(value.getRow()).split(DIV);
            AirlinePair keyPair;
            Text arrDelayMinutes;

            // <[airlineName, month], arrDelayMinutes>
            String airlineName = rowKey[0];
            String month = rowKey[1];
            keyPair = new AirlinePair(new Text(airlineName), new Text(month));

            String arrDelay = new String(value.getValue(COLUMN_FAMILY.getBytes(), AIR_DELAY_MINUTES.getBytes()));
            System.out.print( "map " +  arrDelay.toLowerCase() + "\n");
            arrDelayMinutes = new Text(arrDelay);
            context.write(keyPair, arrDelayMinutes);
        }
    }

    */
/**  The Partitioner puts all records for the same airlineName in the same Reduce Task. *//*

    public static class HPartitioner extends Partitioner<AirlinePair, Text> {
        @Override
        public int getPartition(AirlinePair keyPair, Text value, int numPartitions) {
            return Math.abs(keyPair.getAirlineName().hashCode() * 127) % numPartitions;
        }
    }

    */
/** KeyComparator sorts the airlineName first and then sorts month for all Reduce calls in the same Reduce Task *//*

    public static class HKeyComparator extends WritableComparator {
        protected HKeyComparator() {
            super(AirlinePair.class, true);
        }

        public int compare(WritableComparable w1, WritableComparable w2) {
            AirlinePair ap1 = (AirlinePair) w1;
            AirlinePair ap2 = (AirlinePair) w2;
            // compare airlineName and month
            return ap1.compareTo(ap2);
        }
    }

    */
/** GroupComparator passes all flight month delay of the given airlineName to the same Reduce call *//*

    public static class HGroupComparator extends WritableComparator {
        protected HGroupComparator() {
            super(AirlinePair.class, true);
        }

        public int compare(WritableComparable w1, WritableComparable w2) {
            AirlinePair ap1 = (AirlinePair) w1;
            AirlinePair ap2 = (AirlinePair) w2;
            // compare airlineName
            return ap1.getAirlineName().compareTo(ap2.getAirlineName());
        }
    }

    public static class HComputeReducer
            extends Reducer<AirlinePair, Text, Text, Text> {

        */
/** reduce() will be called every unique airlineName *//*

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
        if (otherArgs.length < 1) {
            System.err.println("Usage: HCompute <out>");
            System.exit(2);
        }

        // Define MapReduce job
        Job job = new Job(conf, "HCompute");
        job.setJarByClass(HComputeR.class);

        // Set Mapper and Reducer classes
        job.setMapperClass(HComputeMapper.class);
        job.setReducerClass(HComputeReducer.class);
        job.setPartitionerClass(HPartitioner.class);
        job.setSortComparatorClass(HKeyComparator.class);
        job.setGroupingComparatorClass(HGroupComparator.class);

        // Output types
        job.setOutputKeyClass(AirlinePair.class);
        job.setOutputValueClass(Text.class);//delay

        // Submit job
        Scan scan = new Scan();
        scan.setCaching(500);
        scan.setFilter(getFilterList());
        TableMapReduceUtil.initTableMapperJob(TABLE_NAME, scan, HComputeMapper.class, AirlinePair.class, Text.class, job);
        FileOutputFormat.setOutputPath(job, new Path(otherArgs[0]));
        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }

    private static FilterList getFilterList() {
        SingleColumnValueFilter yearFilter = new SingleColumnValueFilter(
                COLUMN_FAMILY.getBytes(), YEAR.getBytes(), CompareOp.EQUAL, YEAR_FILTER.getBytes());
        SingleColumnValueFilter cancelledFilter = new SingleColumnValueFilter(
                COLUMN_FAMILY.getBytes(), CANCELLED.getBytes(), CompareOp.EQUAL, CANCELLED_FILTER.getBytes());
        FilterList filterList = new FilterList(FilterList.Operator.MUST_PASS_ALL);
        filterList.addFilter(yearFilter);
        filterList.addFilter(cancelledFilter);
        return filterList;
    }
}*/
