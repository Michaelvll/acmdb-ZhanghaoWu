package simpledb;

import java.util.NoSuchElementException;

/**
 * The Aggregation operator that computes an aggregate (e.g., sum, avg, max,
 * min). Note that we only support aggregates over a single column, grouped by a
 * single column.
 */
public class Aggregate extends Operator {

    private static final long serialVersionUID = 1L;
    private DbIterator child;
    private int aField, gField;
    private Aggregator.Op operator;

    private Aggregator aggregator;
    private DbIterator iterator;

    /**
     * Constructor.
     * <p>
     * Implementation hint: depending on the type of afield, you will want to
     * construct an {@link IntegerAggregator} or {@link StringAggregator} to help
     * you with your implementation of readNext().
     *
     * @param child  The DbIterator that is feeding us tuples.
     * @param afield The column over which we are computing an aggregate.
     * @param gfield The column over which we are grouping the result, or -1 if
     *               there is no grouping
     * @param aop    The aggregation operator to use
     */
    public Aggregate(DbIterator child, int afield, int gfield, Aggregator.Op aop) {
        // some code goes here
        this.child = child;
        this.aField = afield;
        this.gField = gfield;
        this.operator = aop;
        Type gType = gfield == Aggregator.NO_GROUPING ? null : child.getTupleDesc().getFieldType(gfield);
        if (child.getTupleDesc().getFieldType(afield).equals(Type.INT_TYPE)) {
            aggregator = new IntegerAggregator(gfield, gType, afield, aop);
        } else {
            aggregator = new StringAggregator(gfield, gType, afield, aop);
        }
    }

    /**
     * @return If this aggregate is accompanied by a groupby, return the groupby
     * field index in the <b>INPUT</b> tuples. If not, return
     * {@link simpledb.Aggregator#NO_GROUPING}
     */
    public int groupField() {
        // some code goes here
        return this.gField;
    }

    /**
     * @return If this aggregate is accompanied by a group by, return the name
     * of the groupby field in the <b>OUTPUT</b> tuples If not, return
     * null;
     */
    public String groupFieldName() {
        // some code goes here
        return child.getTupleDesc().getFieldName(gField);
    }

    /**
     * @return the aggregate field
     */
    public int aggregateField() {
        // some code goes here
        return aField;
    }

    /**
     * @return return the name of the aggregate field in the <b>OUTPUT</b>
     * tuples
     */
    public String aggregateFieldName() {
        // some code goes here
        return child.getTupleDesc().getFieldName(aField);
    }

    /**
     * @return return the aggregate operator
     */
    public Aggregator.Op aggregateOp() {
        // some code goes here
        return operator;
    }

    public static String nameOfAggregatorOp(Aggregator.Op aop) {
        return aop.toString();
    }

    public void open() throws NoSuchElementException, DbException,
            TransactionAbortedException {
        // some code goes here
        child.open();
        while (child.hasNext()) {
            Tuple tup = child.next();
            aggregator.mergeTupleIntoGroup(tup);
        }
        iterator = aggregator.iterator();
        iterator.open();
        super.open();
    }

    /**
     * Returns the next tuple. If there is a group by field, then the first
     * field is the field by which we are grouping, and the second field is the
     * result of computing the aggregate, If there is no group by field, then
     * the result tuple should contain one field representing the result of the
     * aggregate. Should return null if there are no more tuples.
     */
    protected Tuple fetchNext() throws TransactionAbortedException, DbException {
        // some code goes here

        return iterator.hasNext()? iterator.next() : null;
    }

    public void rewind() throws DbException, TransactionAbortedException {
        // some code goes here
        iterator.rewind();
    }

    /**
     * Returns the TupleDesc of this Aggregate. If there is no group by field,
     * this will have one field - the aggregate column. If there is a group by
     * field, the first field will be the group by field, and the second will be
     * the aggregate value column.
     * <p>
     * The name of an aggregate column should be informative. For example:
     * "aggName(aop) (child_td.getFieldName(afield))" where aop and afield are
     * given in the constructor, and child_td is the TupleDesc of the child
     * iterator.
     */
    public TupleDesc getTupleDesc() {
        // some code goes here
        TupleDesc schema;
        if (gField == Aggregator.NO_GROUPING) {
            schema = new TupleDesc(new Type[]{Type.INT_TYPE},
                    new String[]{nameOfAggregatorOp(operator) + " (" + child.getTupleDesc().getFieldName(aField) + ")"});
        } else {
            schema = new TupleDesc(new Type[]{child.getTupleDesc().getFieldType(gField), Type.INT_TYPE},
                    new String[]{child.getTupleDesc().getFieldName(gField),
                            nameOfAggregatorOp(operator) + " (" + child.getTupleDesc().getFieldName(aField) + ")"});

        }
        return schema;
    }

    public void close() {
        // some code goes here
        super.close();
        iterator.close();
        child.close();
    }

    @Override
    public DbIterator[] getChildren() {
        // some code goes here
        return new DbIterator[]{child};
    }

    @Override
    public void setChildren(DbIterator[] children) {
        // some code goes here
        child = children[0];
    }

}
