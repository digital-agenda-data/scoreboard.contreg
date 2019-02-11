package eionet.cr.common;

import eionet.cr.config.GeneralConfig;
import eionet.cr.web.security.CRUser;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateFormatUtils;

import java.io.File;
import java.util.Collection;
import java.util.Date;

/**
 *
 */
public class UserUploadFilePath {

    public static final File UPLOAD_DIR = new File(GeneralConfig.getRequiredProperty(GeneralConfig.UPLOADS_DIR));

    /**
     *
     * @param user
     * @param fileNameSuffix
     * @return
     */
    public static File get(CRUser user, String fileNameSuffix) {

        if (user == null) {
            throw new IllegalArgumentException("Given user must not be null!");
        }

        if (StringUtils.isBlank(fileNameSuffix)) {
            throw new IllegalArgumentException("Given user file anme suffix must not be blank!");
        }

        File userDir = new File(UPLOAD_DIR, user.getUserName());
        if (!userDir.exists()) {
            userDir.mkdirs();
        }

        String dateStr = DateFormatUtils.format(new Date(), "yyyy-MM-dd HHmmss");
        String fileName = String.format("%s %s %s", dateStr, user.getUserName(), fileNameSuffix);
        return new File(userDir, fileName);
    }
}
