package eionet.cr.dao.readers;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.commons.lang.BooleanUtils;

import eionet.cr.dto.DatasetMigrationDTO;
import eionet.cr.util.sql.SQLResultSetBaseReader;

/**
 * @author Jaanus Heinlaid <jaanus.heinlaid@gmail.com>
 */
public class DatasetMigrationDTOReader extends SQLResultSetBaseReader<DatasetMigrationDTO> {

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.util.sql.SQLResultSetReader#readRow(java.sql.ResultSet)
     */
    @Override
    public void readRow(ResultSet rs) throws SQLException, ResultSetReaderException {

        DatasetMigrationDTO dto = new DatasetMigrationDTO();
        dto.setId(rs.getInt("ID"));
        dto.setSourceCrUrl(rs.getString("SOURCE_CR_URL"));
        dto.setSourcePackageName(rs.getString("SOURCE_PACKAGE_NAME"));
        dto.setTargetDatasetUri(rs.getString("TARGET_DATASET_URI"));
        dto.setPrePurge(BooleanUtils.toBoolean(rs.getInt("PRE_PURGE")));
        dto.setUserName(rs.getString("USER_NAME"));
        dto.setStartedTime(rs.getTimestamp("STARTED_TIME"));
        dto.setFinishedTime(rs.getTimestamp("FINISHED_TIME"));
        dto.setFailed(BooleanUtils.toBoolean(rs.getInt("FAILED")));
        dto.setMessages(rs.getString("MESSAGES"));
        resultList.add(dto);
    }
}
