package simpledb;

import java.io.Serializable;
import java.util.*;

/**
 * Tuple maintains information about the contents of a tuple. Tuples have a
 * specified schema specified by a TupleDesc object and contain Field objects
 * with the data for each field.
 */
public class Tuple implements Serializable {

    private static final long serialVersionUID = 1L;

    private TupleDesc tupleDesc;
    private RecordId recordId = null;
    private List<Field> fieldList;

    /**
     * Create a new tuple with the specified schema (type).
     *
     * @param td
     *            the schema of this tuple. It must be a valid TupleDesc
     *            instance with at least one field.
     */
    public Tuple(TupleDesc td) {
        // some code goes here
        tupleDesc = td;
        fieldList = Arrays.asList(new Field[td.numFields()]);
    }

    /**
     * @return The TupleDesc representing the schema of this tuple.
     */
    public TupleDesc getTupleDesc() {
        // some code goes here
        return tupleDesc;
    }

    /**
     * @return The RecordId representing the location of this tuple on disk. May
     *         be null.
     */
    public RecordId getRecordId() {
        // some code goes here
        return recordId;
    }

    /**
     * Set the RecordId information for this tuple.
     *
     * @param rid
     *            the new RecordId for this tuple.
     */
    public void setRecordId(RecordId rid) {
        // some code goes here
        recordId = rid;
    }

    /**
     * Change the value of the ith field of this tuple.
     *
     * @param i
     *            index of the field to change. It must be a valid index.
     * @param f
     *            new value for the field.
     */
    public void setField(int i, Field f) {
        // some code goes here
        fieldList.set(i, f);
    }

    /**
     * @return the value of the ith field, or null if it has not been set.
     *
     * @param i
     *            field index to return. Must be a valid index.
     */
    public Field getField(int i) {
        // some code goes here
        return fieldList.get(i);
    }

    /**
     * Returns the contents of this Tuple as a string. Note that to pass the
     * system tests, the format needs to be as follows:
     *
     * column1\tcolumn2\tcolumn3\t...\tcolumnN
     *
     * where \t is any whitespace (except a newline)
     */
    public String toString() {
        // some code goes here
//        throw new UnsupportedOperationException("Implement this");
        StringJoiner fieldsString = new StringJoiner("\t");
        fieldList.forEach(field -> fieldsString.add(field.toString()));
        return fieldsString.toString();
    }

    /**
     * @return
     *        An iterator which iterates over all the fields of this tuple
     * */
    public Iterator<Field> fields()
    {
        // some code goes here
        return fieldList.iterator();
    }

    /**
     * reset the TupleDesc of this tuple
     * */
    public void resetTupleDesc(TupleDesc td)
    {
        // some code goes here
        tupleDesc = td;
        fieldList = Arrays.asList(new Field[td.numFields()]);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Tuple tuple = (Tuple) o;

        if (!tupleDesc.equals(tuple.tupleDesc)) return false;
        if (!recordId.equals(tuple.recordId)) return false;
        return fieldList.equals(tuple.fieldList);
    }


    static public Tuple merge(Tuple tuple1, Tuple tuple2) {
        TupleDesc schema1 = tuple1.getTupleDesc(), schema2 = tuple2.getTupleDesc();
        TupleDesc joinSchema = TupleDesc.merge(schema1, schema2);
        Tuple mergedTuple =new Tuple(joinSchema);

        for (int i = 0; i < schema1.numFields(); ++i) {
            mergedTuple.setField(i, tuple1.getField(i));
        }

        for (int i = 0; i < schema2.numFields(); ++i){
            mergedTuple.setField(i+ schema1.numFields(), tuple2.getField(i));
        }
        return mergedTuple;
    }
}
