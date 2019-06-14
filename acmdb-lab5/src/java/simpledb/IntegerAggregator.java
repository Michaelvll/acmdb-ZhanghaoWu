package simpledb;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Knows how to compute some aggregate over a set of IntFields.
 */
public class IntegerAggregator implements Aggregator {

    private static final long serialVersionUID = 1L;
    private int gbField, aField;
    private Type gbFieldType;
    private Op operator;
    private TupleDesc schema;

    private Map<Field, Integer> aggregates = new HashMap<>();
    private Map<Field, Integer> counts = new HashMap<>();

    /**
     * Aggregate constructor
     *
     * @param gbfield     the 0-based index of the group-by field in the tuple, or
     *                    NO_GROUPING if there is no grouping
     * @param gbfieldtype the type of the group by field (e.g., Type.INT_TYPE), or null
     *                    if there is no grouping
     * @param afield      the 0-based index of the aggregate field in the tuple
     * @param what        the aggregation operator
     */

    public IntegerAggregator(int gbfield, Type gbfieldtype, int afield, Op what) {
        // some code goes here
        this.gbField = gbfield;
        this.gbFieldType = gbfieldtype;
        this.aField = afield;
        this.operator = what;
        if (gbfield == Aggregator.NO_GROUPING) {
            this.schema = new TupleDesc(new Type[] {Type.INT_TYPE}, new String[] {"aggregateValue"});
        } else {
            this.schema = new TupleDesc(new Type[] {gbFieldType, Type.INT_TYPE}, new String[] {"groupValue", "aggregateValue"});
        }
    }

    /**
     * Merge a new tuple into the aggregate, grouping as indicated in the
     * constructor
     *
     * @param tup the Tuple containing an aggregate field and a group-by field
     */
    public void mergeTupleIntoGroup(Tuple tup) {
        // some code goes here
        Field groupField = gbField == Aggregator.NO_GROUPING? null : tup.getField(gbField);
        IntField tField = (IntField) tup.getField(aField);
        Integer tvalue = tField.getValue();
        Integer ovalue = aggregates.get(groupField);

        Integer count = counts.getOrDefault(groupField, 0);
        counts.put(groupField, count + 1);

        Integer newValue = null;
        switch (operator) {
            case MIN:
                newValue = ovalue == null ? tvalue : Integer.min(ovalue, tvalue);
                break;
            case MAX:
                newValue = ovalue == null ? tvalue : Integer.max(ovalue, tvalue);
                break;
            case SUM: case AVG:
                newValue = ovalue == null ? tvalue : ovalue + tvalue;
                break;
            case COUNT:
                newValue = count + 1;
                break;
        }
        aggregates.put(groupField, newValue);

    }

    /**
     * Create a DbIterator over group aggregate results.
     *
     * @return a DbIterator whose tuples are the pair (groupVal, aggregateVal)
     * if using group, or a single (aggregateVal) if no grouping. The
     * aggregateVal is determined by the type of aggregate specified in
     * the constructor.
     */
    public DbIterator iterator() {
        // some code goes here
        ArrayList<Tuple> tuples = new ArrayList<>();
        for (Map.Entry<Field, Integer> entry : aggregates.entrySet()) {
            Field group = entry.getKey();
            Integer value = entry.getValue();
            if (operator == Op.AVG) {
                value /= counts.get(group);
            }
            Tuple tuple = new Tuple(schema);
            if (gbField == Aggregator.NO_GROUPING) {
                tuple.setField(0, new IntField(value));
            } else {
                tuple.setField(0, group);
                tuple.setField(1, new IntField(value));
            }
            tuples.add(tuple);
        }
        return new TupleIterator(schema, tuples);
    }

}
