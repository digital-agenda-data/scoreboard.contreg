package eionet.cr.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.log4j.Logger;

import eionet.cr.config.GeneralConfig;
import eionet.cr.config.MigratableCR;
import eionet.cr.dao.readers.DatasetMigrationDTOReader;
import eionet.cr.dto.DatasetMigrationDTO;
import eionet.cr.dto.DatasetMigrationPackageDTO;
import eionet.cr.migration.DatasetMigrationRunner;
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

    /** */
    public static final String GET_MIGRATIONS_SQL = "SELECT * FROM dataset_migration ORDER BY started_time desc";

    /** */
    public static final String CREATE_MIGRATION_SQL = "INSERT INTO dataset_migration ("
            + "source_cr_url, source_package_identifier, target_dataset_uri, pre_purge, user_name, started_time) VALUES (?, ?, ?, ?, ?, ?)";

    /** */
    public static final String GET_MIGRATION_SQL = "SELECT * FROM dataset_migration WHERE id=?";

    /** */
    public static final String SET_MIGRATION_FINISHED_SQL =
            "" + "UPDATE dataset_migration SET finished_time=?, failed=?, messages=COALESCE(?, messages) WHERE id=?";

    /** */
    private static final List<MigratableCR> MIGRATABLE_CRS = initMigratableCrsConf();

    /**
     *
     * @return
     * @throws ServiceException
     */
    public List<DatasetMigrationDTO> listMigrations() throws ServiceException {

        Connection conn = null;
        try {
            conn = SesameUtil.getSQLConnection();

            DatasetMigrationDTOReader reader = new DatasetMigrationDTOReader();
            SQLUtil.executeQuery(GET_MIGRATIONS_SQL, reader, conn);

            List<DatasetMigrationDTO> resultList = reader.getResultList();
            return resultList;
        } catch (Exception e) {
            throw new ServiceException("Failure reading dataset migrations from database", e);
        } finally {
            SQLUtil.close(conn);
        }
    }

    /**
     *
     * @param sourceCrUrl
     * @param sourcePackageIdentifier
     * @param targetDatasetUri
     * @param prePurge
     * @param userName
     * @return
     * @throws ServiceException
     */
    public int startNewMigration(String sourceCrUrl, String sourcePackageIdentifier, String targetDatasetUri, boolean prePurge, String userName)
            throws ServiceException {

        ArrayList<Object> params = new ArrayList<Object>();
        params.add(sourceCrUrl);
        params.add(sourcePackageIdentifier);
        params.add(targetDatasetUri);
        params.add(BooleanUtils.toInteger(prePurge));
        params.add(userName);
        params.add(DateUtils.truncate(new Date(), Calendar.SECOND));

        Connection conn = null;
        try {
            conn = SesameUtil.getSQLConnection();
            int id = SQLUtil.executeUpdateReturnAutoID(CREATE_MIGRATION_SQL, params, conn);

            DatasetMigrationRunner migrationRunner = new DatasetMigrationRunner(id, this);
            migrationRunner.start();
            return id;
        } catch (Exception e) {
            throw new ServiceException("Failure when creating a dataset migration record", e);
        } finally {
            SQLUtil.close(conn);
        }
    }

    /**
     *
     * @param sourceCrUrl
     * @param sourcePackageIdentifier
     * @return
     * @throws ServiceException
     */
    private DatasetMigrationPackageDTO findMigratablePackage(String sourceCrUrl, String sourcePackageIdentifier) throws ServiceException {

        MigratableCR migratableCR = getMigratableCRByUrl(sourceCrUrl);
        if (migratableCR == null) {
            throw new ServiceException("Failed to find source CR by this URL: " + sourceCrUrl);
        }

        for (DatasetMigrationPackageDTO datasetMigrationPackageDTO : DatasetMigrationPackageService.newInstance().listRemote(migratableCR)) {
            if (sourcePackageIdentifier.equals(datasetMigrationPackageDTO.getIdentifier())) {
                return datasetMigrationPackageDTO;
            }
        }

        return null;
    }

    /**
     *
     * @param migrationId
     * @param finishedTime
     * @param failed
     * @param messages
     * @throws ServiceException
     */
    public void setMigrationFinished(int migrationId, Date finishedTime, boolean failed, String messages) throws ServiceException {

        if (finishedTime == null) {
            throw new IllegalArgumentException("Finish-time must not be null!");
        }

        ArrayList<Object> values = new ArrayList<Object>();
        values.add(DateUtils.truncate(finishedTime, Calendar.SECOND));
        values.add(BooleanUtils.toInteger(failed));
        values.add(StringUtils.trim(messages));
        values.add(migrationId);

        Connection conn = null;
        try {
            conn = SesameUtil.getSQLConnection();
            SQLUtil.executeUpdate(SET_MIGRATION_FINISHED_SQL, values, conn);
        } catch (Exception e) {
            throw new ServiceException("Failure when setting migration finished", e);
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
     * @param crUrl
     * @return
     */
    public MigratableCR getMigratableCRByUrl(String crUrl) {

        if (StringUtils.isBlank(crUrl)) {
            throw new IllegalArgumentException("Given URL argument must not be blank!");
        }

        for (MigratableCR migratableCR : DatasetMigrationsService.MIGRATABLE_CRS) {
            if (crUrl.equals(migratableCR.getUrl())) {
                return migratableCR;
            }
        }

        return null;
    }

    /**
     * Create and return a map of available remote migration packages.
     * key = remote CR URL
     * value = list of identifiers of packages in that remote CR
     *
     * @return
     * @throws ServiceException
     */
    public Map<String, List<DatasetMigrationPackageDTO>> getMigratablePackagesMap() throws ServiceException {

        LinkedHashMap<String, List<DatasetMigrationPackageDTO>> resultMap = new LinkedHashMap<String, List<DatasetMigrationPackageDTO>>();

        DatasetMigrationPackageService packagesService = DatasetMigrationPackageService.newInstance();
        for (MigratableCR migratableCR : MIGRATABLE_CRS) {

            String crUrl = migratableCR.getUrl();
            if (StringUtils.isNotBlank(crUrl)) {

                List<DatasetMigrationPackageDTO> packages = packagesService.listRemote(migratableCR);
                resultMap.put(crUrl, packages);
            }
        }

        return resultMap;
    }

    /**
     *
     * @param migrationIds
     * @throws ServiceException
     */
    public void deleteMigrations(List<Integer> migrationIds) throws ServiceException {

        if (CollectionUtils.isEmpty(migrationIds)) {
            return;
        }

        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = SesameUtil.getSQLConnection();
            pstmt = conn.prepareStatement("DELETE FROM dataset_migration where ID=?");

            for (Integer migrationId : migrationIds) {
                pstmt.setInt(1, migrationId);
                pstmt.addBatch();
            }

            pstmt.executeBatch();
        } catch (Exception e) {
            throw new ServiceException("Failure when deleting migrations", e);
        } finally {
            SQLUtil.close(pstmt);
            SQLUtil.close(conn);
        }
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
