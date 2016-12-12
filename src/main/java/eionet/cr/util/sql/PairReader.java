/*
 * The contents of this file are subject to the Mozilla Public
 * License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * The Original Code is Content Registry 2.0.
 *
 * The Initial Owner of the Original Code is European Environment
 * Agency.  Portions created by Tieto Eesti are Copyright
 * (C) European Environment Agency.  All Rights Reserved.
 *
 * Contributor(s):
 * Aleksandr Ivanov, Tieto Eesti
 */
package eionet.cr.util.sql;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;

import org.openrdf.model.Value;
import org.openrdf.query.BindingSet;

import eionet.cr.dao.readers.ResultSetMixedReader;
import eionet.cr.dao.readers.ResultSetReaderException;
import eionet.cr.util.Pair;

/**
 *
 * @author Jaanus Heinlaid <jaanus.heinlaid@gmail.com>
 * @param <L>
 * @param <R>
 */
public class PairReader<L, R> extends ResultSetMixedReader<Pair<L, R>> {

    /** */
    public enum SortColumn {
        LEFT, RIGHT
    };

    /**
     * Field name for left column in query.
     */
    public static final String LEFTCOL = "LCOL";
    /**
     * Field name for right column in query.
     */
    public static final String RIGHTCOL = "RCOL";

    /** */
    private LinkedHashMap<L, R> resultMap = new LinkedHashMap<L, R>();

    /** */
    private String leftColumn = LEFTCOL;
    private String rightColumn = RIGHTCOL;

    /**
     *
     * Class constructor.
     */
    public PairReader() {
        super();
    }

    /**
     *
     * Class constructor.
     *
     * @param leftColumn
     * @param rightColumn
     */
    public PairReader(String leftColumn, String rightColumn) {
        super();
        this.leftColumn = leftColumn;
        this.rightColumn = rightColumn;
    }

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.readers.ResultSetMixedReader#getResultList()
     */
    @Override
    public List<Pair<L, R>> getResultList() {
        return resultList;
    }

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.util.sql.SQLResultSetReader#readRow(java.sql.ResultSet)
     */
    @Override
    @SuppressWarnings("unchecked")
    public void readRow(ResultSet rs) throws SQLException, ResultSetReaderException {

        L left = (L) rs.getObject(leftColumn);
        R right = (R) rs.getObject(rightColumn);
        resultList.add(new Pair<L, R>(left, right));
        resultMap.put(left, right);
    }

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.util.sesame.SPARQLResultSetReader#readRow(org.openrdf.query.BindingSet)
     */
    @Override
    @SuppressWarnings("unchecked")
    public void readRow(BindingSet bindingSet) {

        if (bindingSet != null && bindingSet.size() > 0) {
            Value lcol = bindingSet.getValue(leftColumn);
            Value rcol = bindingSet.getValue(rightColumn);
            String strLcol = "", strRCol = "";
            if (lcol != null) {
                strLcol = lcol.stringValue();
            }
            if (rcol != null) {
                strRCol = rcol.stringValue();
            }

            L left = (L) strLcol;
            R right = (R) strRCol;
            resultList.add(new Pair<L, R>(left, right));
            resultMap.put(left, right);
        }
    }

    /**
     * @return the resultMap
     */
    public LinkedHashMap<L, R> getResultMap() {
        return resultMap;
    }
}
