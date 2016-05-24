package eionet.cr.service;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import eionet.cr.config.GeneralConfig;
import eionet.cr.config.MigratableCR;
import eionet.cr.dao.readers.DatasetMigrationDTOReader;
import eionet.cr.dto.DatasetMigrationDTO;
import eionet.cr.util.sesame.SesameUtil;
import eionet.cr.util.sql.SQLUtil;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import net.sf.json.JsonConfig;

/**
 * Service for operations with dataset migrations.
 *
 * @author Jaanus Heinlaid <jaanus.heinlaid@gmail.com>
 */
public class DatasetMigrationsService {

    /** */
    private static final Logger LOGGER = Logger.getLogger(DatasetMigrationsService.class);

    // "id" INTEGER IDENTITY,
    // "source_cr_url" VARCHAR(255),
    // "source_package_name" VARCHAR(255),
    // "target_dataset_uri" VARCHAR(255),
    // "pre_purge" SMALLINT,
    // "user_name" VARCHAR(80),
    // "started_time" DATETIME,
    // "finished_time" DATETIME,
    // "failed" SMALLINT,
    // "messages" LONG VARCHAR,

    /** */
    public static final String CREATE_MIGRATION_SQL = "INSERT INTO dataset_migrations ("
            + "source_cr_url, source_package_name, target_dataset_uri, pre_purge, user_name, started_time) VALUES (?, ?, ?, ?, ?, now())";

    /** */
    public static final String GET_MIGRATION_SQL = "SELECT * FROM dataset_migrations WHERE id=?";

    /** */
    private static final List<MigratableCR> MIGRATABLE_CRS = initMigratableCrsConf();

    /**
     *
     * @param sourceCrUrl
     * @param sourcePackageName
     * @param targetDatasetUri
     * @param prePurge
     * @param userName
     * @return
     * @throws ServiceException
     */
    public int startNewMigration(String sourceCrUrl, String sourcePackageName, String targetDatasetUri, boolean prePurge, String userName)
            throws ServiceException {

        ArrayList<Object> params = new ArrayList<Object>();
        params.add(sourceCrUrl);
        params.add(sourcePackageName);
        params.add(targetDatasetUri);
        params.add(BooleanUtils.toInteger(prePurge));
        params.add(userName);

        Connection conn = null;
        try {
            conn = SesameUtil.getSQLConnection();
            int id = SQLUtil.executeUpdateReturnAutoID(CREATE_MIGRATION_SQL, params, conn);
            return id;
        } catch (Exception e) {
            throw new ServiceException("Failure when creating a dataset migration record", e);
        } finally {
            SQLUtil.close(conn);
        }
    }

    /**
     *
     * @param migrationId
     * @return
     * @throws ServiceException
     */
    public DatasetMigrationDTO findById(int migrationId) throws ServiceException {

        Connection conn = null;
        try {
            conn = SesameUtil.getSQLConnection();

            DatasetMigrationDTOReader reader = new DatasetMigrationDTOReader();
            SQLUtil.executeQuery(GET_MIGRATION_SQL, Collections.singletonList(migrationId), reader, conn);

            List<DatasetMigrationDTO> resultList = reader.getResultList();
            return resultList != null && !resultList.isEmpty() ? resultList.iterator().next() : null;
        } catch (Exception e) {
            throw new ServiceException("Failure when creating a dataset migration record", e);
        } finally {
            SQLUtil.close(conn);
        }
    }

    /**
     *
     * @return
     */
    public List<MigratableCR> listMigratableCRInstances() {
        return DatasetMigrationsService.MIGRATABLE_CRS;
    }

    /**
     *
     * @return
     */
    public static DatasetMigrationsService newInstance() {
        return new DatasetMigrationsService();
    }

    /**
     *
     * @return
     */
    private static List<MigratableCR> initMigratableCrsConf() {

        ArrayList<MigratableCR> resultList = new ArrayList<MigratableCR>();
        String jsonArrayString = GeneralConfig.getProperty(GeneralConfig.MIGRATABLE_CR_INSTANCES);
        if (StringUtils.isNotBlank(jsonArrayString)) {

            JsonConfig jsonConfig = new JsonConfig();
            jsonConfig.setRootClass(MigratableCR.class);

            try {
                JSONArray jsonArray = JSONArray.fromObject(jsonArrayString);
                for (Object object : jsonArray) {
                    MigratableCR migratableCR = (MigratableCR) JSONSerializer.toJava((JSONObject) object, jsonConfig);
                    resultList.add(migratableCR);
                }
            } catch (Exception e) {
                LOGGER.warn(
                        String.format("Failed to parse '%s' property, assuming no migratable CRs available", GeneralConfig.MIGRATABLE_CR_INSTANCES),
                        e);
            }
        }
        return resultList;
    }
}
