package eionet.cr.web.util;

import org.apache.commons.io.FileUtils;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;
import java.io.File;

/**
 * Custom tag for inserting template file
 *
 * @author altnyris
 */
public class TemplateTag extends TagSupport {

    private static final long serialVersionUID = 1L;
    String file;

    public void setFile(String s) {
        file = s;
    }

    @Override
    public int doEndTag() throws JspException {

        System.out.println("\n\n\n");
        System.out.println("_________________________________________________________");
        System.out.println("_________________________________________________________");
        System.out.println("_________________________________________________________");
        System.out.println("\n\n\n");

        try {
            if (file != null && file.length() > 0) {
                String folderPath = pageContext.getServletContext().getInitParameter("templateCacheFolder");
                File f = new File(folderPath, file);
                String content = FileUtils.readFileToString(f, "UTF-8");
                pageContext.getOut().print(content);
            }
        } catch (Exception e) {
            throw new JspException(e.toString());
        }
        return EVAL_PAGE;
    }
}
