<%@page contentType="text/html;charset=UTF-8"%>

<%@ include file="/pages/common/taglibs.jsp"%>

<stripes:layout-render name="/pages/common/template.jsp" pageTitle="Add a staging database">

    <stripes:layout-component name="contents">

        <%-- The page's heading --%>

        <h1>Create a new staging database</h1>

        <div style="margin-top:20px">
            This page enables you to create a new staging database from the file noted below.<br/>
            Please enter the new database's properties below and click 'Submit'. Mandatory properties are marked with <img src="${pageContext.request.contextPath}/images/mandatory.gif"/>.
        </div>

        <%-- The form --%>

        <div style="padding-top:20px">
            <crfn:form id="newDbForm" beanclass="${actionBean['class'].name}" method="post">
                <table>
                    <tr>
                        <td class="question" style="text-align:right">
                           From file:
                        </td>
                        <td>
                           <c:out value="${actionBean.fileName}"/>&nbsp;
                           <c:if test="${actionBean.fileSize < 1000}">
                               (<c:out value="${actionBean.fileSize}"/> bytes)
                           </c:if>
                           <c:if test="${actionBean.fileSize >= 1000}">
                               (<fmt:formatNumber value="${actionBean.fileSize / 1000}" maxFractionDigits="0"/>&nbsp;KB)
                           </c:if>
                        </td>
                    </tr>
                    <tr>
                        <td style="text-align:right;vertical-align:top">
                           <stripes:label for="txtName" class="question required">Name:</stripes:label>
                        </td>
                        <td>
                           <stripes:text name="dbName" id="txtName" size="80" value="${actionBean.suggestedDbName}"/>
                        </td>
                    </tr>
                    <tr>
                        <td style="text-align:right;vertical-align:top">
                           <stripes:label for="txtDescription" class="question">Description:</stripes:label>
                        </td>
                        <td>
                           <stripes:textarea id="txtDescription" name="dbDescription" cols="80" rows="5"/>
                        </td>
                    </tr>
                    <tr>
                        <td>&nbsp;</td>
                        <td>
                            <stripes:submit name="add" value="Submit"/>
                            <stripes:submit name="toAvailableFilesList" value="Cancel"/>
                        </td>
                    </tr>
                </table>
                <div id="hiddenInputs" style="display:none">
                    <stripes:hidden name="fileName"/>
                </div>
            </crfn:form>
        </div>

    </stripes:layout-component>
</stripes:layout-render>
