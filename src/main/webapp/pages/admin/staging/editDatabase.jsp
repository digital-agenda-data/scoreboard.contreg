<%@page contentType="text/html;charset=UTF-8"%>

<%@ include file="/pages/common/taglibs.jsp"%>

<stripes:layout-render name="/pages/common/template.jsp" pageTitle="Staging database edit page">

    <stripes:layout-component name="contents">

        <c:if test="${actionBean.dbDTO != null}">

            <%-- The page's heading --%>

            <h1>Edit staging database metadata</h1>

            <div style="margin-top:20px">
                This page enables you to edit the metadata of staging database <stripes:link beanclass="${actionBean['class'].name}"><c:out value="${actionBean.dbName}"/><stripes:param name="dbName" value="${actionBean.dbName}"/></stripes:link>.
            </div>

            <%-- The table with the database's metadata. --%>

            <div style="padding-top:20px">
                <crfn:form id="editForm" beanclass="${actionBean['class'].name}" method="post">
                    <table class="datatable" style="width:90%">
                        <colgroup>
                            <col width="25%">
                            <col width="75%">
                        </colgroup>
                        <tr>
                            <th class="question" style="text-align:right" title="The database's description">Description:</th>
                            <td>
                                <stripes:textarea id="txtDescription" name="dbDescription" cols="80" rows="5"/>
                            </td>
                        </tr>
                        <tr>
                            <th class="question" style="text-align:right" title="The database's default RDF export query">Default export query:</th>
                            <td>
                                <stripes:textarea id="txtQuery" name="defaultQuery" cols="80" rows="8"/>
                            </td>
                        </tr>
                        <tr>
                            <td>&nbsp;</td>
                            <td>
                                <stripes:submit name="save" value="Save"/>
                                <stripes:submit name="saveAndClose" value="Save & close"/>
                                <stripes:submit name="cancelEdit" value="Cancel"/>
                            </td>
                        </tr>
                    </table>
                    <div style="display:none">
                        <input type="hidden" name="dbId" value="${actionBean.dbDTO.id}"/>
                    </div>
                </crfn:form>

                <c:if test="${not empty actionBean.tablesColumns}">
                    <display:table name="${actionBean.tablesColumns}" id="tableColumn" class="datatable" style="width:100%;margin-top:30px">
                       <display:caption style="text-align:left;margin-bottom:10px;">Tables and columns in this database:</display:caption>
                       <display:column property="table" title="Table" style="width:34%"/>
                       <display:column property="column" title="Column" style="width:33%"/>
                       <display:column property="dataType" title="Data type" style="width:33%"/>
                    </display:table>
                </c:if>

                <c:if test="${empty actionBean.tablesColumns}">
                    <div class="note-msg" style="width:75%;margin-top:30px">
                        <strong>Note</strong>
                        <p>Found no tables in this database!</p>
                    </div>
                </c:if>

            </div>
        </c:if>

        <c:if test="${actionBean.dbDTO == null}">
            <div class="warning-msg">
                <strong>Warning</strong>
                <p>Found no staging database by this criteria!</p>
            </div>
        </c:if>

    </stripes:layout-component>
</stripes:layout-render>
