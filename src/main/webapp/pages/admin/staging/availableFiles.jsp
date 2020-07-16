<%@page contentType="text/html;charset=UTF-8"%>

<%@ include file="/pages/common/taglibs.jsp"%>

<stripes:layout-render name="/pages/common/template.jsp" pageTitle="Staging databases: available files">

    <stripes:layout-component name="head">
        <script type="text/javascript">
        // <![CDATA[
            ( function($) {
                $(document).ready(
                    function(){

                        $("#uploadLink").click(function() {
                            $('#uploadDialog').dialog('option','width', 800);
                            $('#uploadDialog').dialog('open');
                            return false;
                        });

                        $('#uploadDialog').dialog({
                            autoOpen: false,
                            width: 500
                        });

                        $("#closeUploadDialog").click(function() {
                            $('#uploadDialog').dialog("close");
                            return true;
                        });

                        ////////////////////////////////////////////

                        $("#downloadLink").click(function() {
                            $('#downloadDialog').dialog('option','width', 800);
                            $('#downloadDialog').dialog('open');
                            return false;
                        });

                        $('#downloadDialog').dialog({
                            autoOpen: false,
                            width: 500
                        });

                        $("#closeDownloadDialog").click(function() {
                            $('#downloadDialog').dialog("close");
                            return true;
                        });

                        ////////////////////////////////////////////

                        $('#compressedFileDialog').dialog({
                            autoOpen: ${actionBean.context.eventName=='select' && actionBean.compressedFile ? 'true' : 'false'},
                            height: 250,
                            width: 700,
                            maxHeight: 400,
                            maxWidth: 800,
                            modal: true,
                            closeOnEscape: true
                        });

                        $("#closeCompressedFileDialog").click(function() {
                            $('#compressedFileDialog').dialog("close");
                            return true;
                        });

                        ////////////////////////////////////////////

                        $('#archiveFileDialog').dialog({
                            autoOpen: ${actionBean.context.eventName=='select' && actionBean.archiveFile ? 'true' : 'false'},
                            height: 500,
                            width: 700,
                            maxHeight: 800,
                            maxWidth: 800,
                            modal: true,
                            closeOnEscape: true
                        });

                        $("#closeArchiveFileDialog").click(function() {
                            $('#archiveFileDialog').dialog("close");
                            return true;
                        });
                    });
            } ) ( jQuery );
        // ]]>
        </script>
    </stripes:layout-component>

    <stripes:layout-component name="contents">

        <%-- Drop-down operations --%>

        <ul id="dropdown-operations">
            <li><a href="#">Operations</a>
                <ul>
                    <li><a href="#" id="uploadLink" title="Upload a new file">Upload</a></li>
                    <li><a href="#" id="downloadLink" title="Download a new file">Download</a></li>
                    <li><stripes:link beanclass="${actionBean.stagingDatabasesActionBeanClass.name}">Staging databases</stripes:link></li>
                </ul>
            </li>
        </ul>

        <%-- The page's heading --%>

        <h1>Files available for creating staging databases.</h1>

        <%-- The section that displays the files list. --%>

        <c:if test="${not empty actionBean.availableFiles}">

            <div style="margin-top:20px">
                <p>
                    Although staging databases can be created from <strong>Microsoft Access</strong> files only, the system allows you to upload or download any type of files here.<br/>
                    If they are compressed (e.g. GZIP) or archive files (e.g. ZIP archive), the system allows you to uncompress/extract them after upload/download.<br/>
                    <br/>
                    If a file's name is suffixed with "${actionBean.downloadingSuffix}", it means it is being downloaded, and no operations can be performed on it yet.<br/>
                    If no such suffix is present, the file is complete and ready for operations with it.<br/>
                    <br/>
                    To create a staging database from a listed file, click on the file's name. If the file happens to be compressed or is an archive file, you will be<br/>
                    prompted with a pop-up that allows you to uncompress/extract it before it can be used. If the file is not compressed or archive, and it is also<br/>
                    not a <em>Microsoft Access</em> file, you will receive a warning message. In all other cases you will be taken straight to the database creation page.
                </p>
            </div>

            <div style="width:75%;padding-top:10px">
                <stripes:form id="filesForm" method="post" beanclass="${actionBean['class'].name}">

                    <display:table name="${actionBean.availableFiles}" id="file" class="sortable" sort="list" requestURI="${actionBean.urlBinding}" style="width:100%">
                        <display:column>
                            <stripes:checkbox name="fileNames" value="${file.name}" />
                        </display:column>
                        <display:column title="Name" sortable="true" sortProperty="name" style="width:30%">
                            <c:choose>
                                <c:when test="${fn:endsWith(file.name, actionBean.downloadingSuffix)}">
                                    <c:out value="${file.name}"/>
                                </c:when>
                                <c:otherwise>
                                    <stripes:link beanclass="${actionBean['class'].name}">
                                        <c:out value="${file.name}"/>
                                        <stripes:param name="select" value="${file.name}"/>
                                    </stripes:link>
                                </c:otherwise>
                            </c:choose>
                        </display:column>
                        <display:column title="Size" sortable="true" sortProperty="size" style="width:30%">
                            <c:if test="${file.size < 1000}">
                                <c:out value="${file.size}"/> bytes
                            </c:if>
                            <c:if test="${file.size >= 1000}">
                                <fmt:formatNumber value="${file.size / 1000}" maxFractionDigits="0"/>&nbsp;KB
                            </c:if>
                        </display:column>
                        <display:column title="Last modified" sortable="true" sortProperty="lastModified" style="width:30%">
                            <fmt:formatDate value="${file.lastModified}" pattern="yyyy-MM-dd HH:mm:ss" />
                        </display:column>
                    </display:table>

                    <stripes:submit name="delete" value="Delete" />
                    <input type="button" onclick="toggleSelectAll('filesForm');return false" value="Select all" name="selectAll">

                </stripes:form>
            </div>
        </c:if>

        <%-- Message if no databases found. --%>

        <c:if test="${empty actionBean.availableFiles}">
            <div class="system-msg">
                No files found! Use operations menu to upload or download one.<br/>
                Although staging databases can be created from Microsoft Access files only, the system allows you to upload<br/>
                or download files of any type. If they are zipped, you will later get a possibility to unzip them.
            </div>
        </c:if>

        <%-- The upload dialog. Hidden unless activated. --%>

        <div id="uploadDialog" title="Upload file">
            <stripes:form beanclass="${actionBean['class'].name}" method="post">

                <table>
                    <tr>
                        <td>&nbsp;</td>
                        <td style="font-weight:bold;font-size:0.75em">NB! Maximum allowed size for the uploaded files is <fmt:formatNumber value="${actionBean.maxFilePostSize / 1000}" maxFractionDigits="0"/>&nbsp;KB.</td>
                    </tr>
                    <tr>
                        <td><stripes:label for="fileInput" class="question required">File:</stripes:label></td>
                        <td><stripes:file name="uploadFile" id="fileInput" size="80"/></td>
                    </tr>
                    <tr>
                        <td>&nbsp;</td>
                        <td style="padding-top:10px">
                            <stripes:submit name="upload" value="Upload"/>
                            <input type="button" id="closeUploadDialog" value="Cancel"/>
                        </td>
                    </tr>
                </table>

            </stripes:form>
        </div>

        <%-- The download dialog. Hidden unless activated. --%>

        <div id="downloadDialog" title="Download file">
            <stripes:form beanclass="${actionBean['class'].name}" method="post">

                <table>
                    <tr>
                        <td><stripes:label for="txtUrl" class="question required">URL:</stripes:label></td>
                        <td><stripes:text name="downloadUrl" id="txtUrl" size="80"/></td>
                    </tr>
                    <tr>
                        <td><stripes:label for="txtName" class="question">Name:</stripes:label></td>
                        <td>
                            <stripes:text name="newFileName" id="txtName" size="50"/><br/>
                            <span style="font-size:0.75em;color:#666666">
                                Name you wish to give to the file. If not given, the system will attempt to detect the downloaded file's name.
                                If a while with such a name already exists, the new file's name will prepended with a number,  e.g. (1).
                            </span>
                        </td>
                    </tr>
                    <tr>
                        <td>&nbsp;</td>
                        <td style="padding-top:10px">
                            <stripes:submit name="download" value="Download"/>
                            <input type="button" id="closeDownloadDialog" value="Cancel"/>
                        </td>
                    </tr>
                </table>

            </stripes:form>
        </div>

        <%-- The compressed file dialog. Hidden unless activated. --%>

        <div id="compressedFileDialog" title="Compressed file">

            <div class="note-msg">
                <strong>Note</strong>
                <p>
                    This is a compressed file that needs to be uncompressed before it can be used.<br/>
                    Click "Uncompress" if you want to proceed, otherwise click "Cancel".<br/>
                    The uncompressed file's name will be: <strong><c:out value="${actionBean.uncompressedFileName}"/></strong>.
                </p>

                <crfn:form beanclass="${actionBean['class'].name}" method="post" style="margin-top:20px">
                    <input type="submit" value="Uncompress">
                    <input type="button" id="closeCompressedFileDialog" value="Cancel"/>

                    <fieldset style="display:none">
                        <stripes:hidden name="uncompress" value="${actionBean.selectedFileName}"/>
                    </fieldset>
                </crfn:form>
               </div>

        </div>

        <%-- The archive file dialog. Hidden unless activated. --%>

        <div id="archiveFileDialog" title="Archive file">

            <div class="note-msg">
                <strong>Note</strong>
                <p>
                    This is an archive file that contains the below entries.<br/>
                    These need to be extracted before they can be used. Select the ones you<br/>
                    wish to extract, give them other names if you wish (optional), and click "Extract".<br/>
                    Otherwise click "Cancel".
                </p>

                <crfn:form beanclass="${actionBean['class'].name}" method="post" style="margin-top:20px">

                    <display:table name="${actionBean.archiveEntries}" id="entry" class="datatable" requestURI="${actionBean.urlBinding}" style="padding-bottom:20px">

                        <display:setProperty name="css.tr.even" value="${crfn:md5Hex('none')}"/>
                        <display:setProperty name="css.tr.odd" value="${crfn:md5Hex('none')}"/>

                        <display:column style="text-align:center;width:4%">
                            <input type="checkbox" name="extractEntries" value="${entry.name}" title="Select this entry" id="entryChk${entry_rowNum}"/>
                        </display:column>
                        <display:column title='<span title="The file name of this entry">Entry</span>' style="text-align:left;width:48%">
                            <label for="entryChk${entry_rowNum}"><strong><c:out value="${entry.name}"/></strong></label>
                        </display:column>
                        <display:column title='<span title="The name to give to this entry after extraction">Rename to</span>' style="text-align:left;width:48%">
                            <input type="text" name="${crfn:md5Hex(entry.name)}_newName" size="20"/>
                        </display:column>

                    </display:table>

                    <input type="submit" value="Extract">
                    <input type="button" id="closeArchiveFileDialog" value="Cancel"/>

                    <fieldset style="display:none">
                        <stripes:hidden name="extract" value="${actionBean.selectedFileName}"/>
                    </fieldset>

                </crfn:form>
            </div>

        </div>

    </stripes:layout-component>
</stripes:layout-render>
