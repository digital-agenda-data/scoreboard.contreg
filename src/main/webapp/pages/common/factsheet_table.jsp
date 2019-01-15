<%@ include file="/pages/common/taglibs.jsp"%>

<stripes:layout-definition>

<c:if test="${actionBean.subject.predicates!=null && fn:length(actionBean.subject.predicates)>0}">

    <c:set var="isRaw" value="${param.raw!=null}"/>

    <table class="datatable" width="100%" cellspacing="0" summary="">

        <col style="width:23%;"/>
        <col/>
        <col/>
        <col style="width:10%;"/>

        <thead>
            <th scope="col" class="scope-col">Property</th>
            <th scope="col" class="scope-col">&nbsp;</th>
            <th scope="col" class="scope-col">Value</th>
            <c:if test="${!enableEdit}">
                <th scope="col" class="scope-col">Source</th>
            </c:if>
            <c:if test="${enableEdit}">
                <th scope="col" class="scope-col">Tools</th>
            </c:if>
        </thead>
        <tbody>
            <c:forEach var="predicate" items="${actionBean.subject.sortedPredicates}" varStatus="predLoop">

                <c:set var="predicateLabelDisplayed" value="${false}"/>
                <c:set var="predicateObjectsCount" value="${actionBean.subject.predicateObjectCounts[predicate.key]}"/>

                <c:forEach items="${predicate.value}" var="object" varStatus="objLoop">

                    <tr>
                        <th scope="row" class="scope-row" style="white-space:nowrap">
                            <c:choose>
                                <c:when test="${not predicateLabelDisplayed}">
                                    <c:out value="${actionBean.subject.predicateLabels[predicate.key]}"/>
                                    <c:set var="predicateLabelDisplayed" value="${true}"/>
                                    <stripes:link  href="/factsheet.action" title="${predicate.key}">
                                        <stripes:param name="uri" value="${predicate.key}"/>
                                        <img src="${pageContext.request.contextPath}/images/view2.gif" alt="Definition."/>
                                    </stripes:link>
                                </c:when>
                                <c:otherwise>&nbsp;</c:otherwise>
                            </c:choose>
                        </th>
                        <td>
                            <c:choose>
                                <c:when test="${objLoop.index==0 && predicateObjectsCount>1}">
                                    <c:choose>
                                        <c:when test="${empty actionBean.predicatePageNumbers[predicate.key]}">
                                            <stripes:link href="${crfn:predicateExpandLink(actionBean,predicate.key,1)}">
                                                <img src="${pageContext.request.contextPath}/images/expand.png" title="${predicateObjectsCount} values" alt="Browse ${predicateObjectsCount} values"/>
                                            </stripes:link>
                                        </c:when>
                                        <c:otherwise>
                                            <stripes:link href="${crfn:predicateCollapseLink(actionBean,predicate.key)}">
                                                <img src="${pageContext.request.contextPath}/images/collapse.png" title="Collapse" alt="Collapse"/>
                                            </stripes:link>
                                        </c:otherwise>
                                    </c:choose>
                                </c:when>
                                <c:otherwise>&nbsp;</c:otherwise>
                            </c:choose>
                        </td>
                        <td>
                            <c:choose>
                                <c:when test="${!object.literal}">
                                    <c:choose>
                                        <c:when test="${!object.anonymous}">
                                            <stripes:link class="infolink" href="/factsheet.action" title="${object.displayValue==object.value ? object.displayValue : object.value}"><c:out value="${object.displayValue}"/>
                                                <stripes:param name="uri" value="${object.value}"/>
                                            </stripes:link>
                                        </c:when>
                                        <c:otherwise>
                                            <stripes:link class="infolink" href="/factsheet.action"><c:out value="${object.displayValue}"/>
                                                <stripes:param name="uri" value="${object.value}"/>
                                            </stripes:link>
                                        </c:otherwise>
                                    </c:choose>
                                </c:when>
                                <c:otherwise>
                                    <span title="[Datatype: ${object.dataTypeLabel}]" style="white-space:pre-wrap"><c:out value="${object.value}"/></span><c:if test="${object.exceedsMaxLength}">&nbsp;<stripes:link id="predObjValueLink_${predLoop.index+objLoop.index}" href="${actionBean.urlBinding}" event="openPredObjValue" title="Open full text of this value">
                                            <strong>[...]</strong>
                                            <stripes:param name="uri" value="${actionBean.uri}"/>
                                            <stripes:param name="predicateUri" value="${predicate.key}"/>
                                            <stripes:param name="objectMD5" value="${object.objectMD5}"/>
                                            <stripes:param name="graphUri" value="${object.sourceSmart}"/>
                                        </stripes:link></c:if>
                                </c:otherwise>
                            </c:choose>
                            <c:if test="${not empty object.language}">
                                <span class="langcode" title="Language code of this text is '${object.language}'"><c:out value="${object.language}"/></span>
                            </c:if>
                        </td>
                        <td class="center">
                            <crfn:form id="deleteForm_${objLoop.index}" action="/factsheet.action" method="post">
                                <div>
                                    <c:if test="${enableEdit}">
                                        <c:if test="${actionBean.allEditable || (fn:contains(object.sourceSmart, 'home/') && fn:endsWith(object.sourceSmart, '/registrations'))}">
                                            <a class="edit-property-opener"
                                               data-property-uri="${predicate.key}"
                                               data-property-label="${actionBean.subject.predicateLabels[predicate.key]} (${predicate.key})"
                                               data-property-value="${object.displayValue}"
                                               data-property-value-md5="${object.objectMD5}"
                                               data-source-uri="${object.sourceSmart}"
                                               href="#" title="Edit this property value."><img src="${pageContext.request.contextPath}/images/edit.gif" alt="Edit"/></a>
                                            <input type="image" name="delete" src="${pageContext.request.contextPath}/images/delete.gif" title="Remove this property value." alt="Remove this property value." onclick="return confirm('Are you sure you want to delete this property value? Press OK to confirm, otherwise press Cancel.');"/>
                                        </c:if>
                                    </c:if>
                                    <c:if test="${object.sourceSmart!=null}">
                                        <stripes:link href="/factsheet.action" target="_blank">
                                            <img src="${pageContext.request.contextPath}/images/harvest_source.png" title="${fn:escapeXml(object.sourceSmart)}" alt="${fn:escapeXml(object.sourceSmart)}"/>
                                            <stripes:param name="uri" value="${object.sourceSmart}"/>
                                        </stripes:link>
                                    </c:if>
                                </div>
                                <fieldset style="display: none;">

                                    <stripes:hidden name="uri" value="${subjectUri}"/>
                                    <stripes:hidden name="anonymous" value="${actionBean.subject.anonymous}"/>
                                    <stripes:hidden name="propertyUri" value="${predicate.key}" />
                                    <stripes:hidden name="propertyValue" value="${object.value}"/>
                                    <stripes:hidden name="propertyValueMd5" value="${object.objectMD5}"/>
                                    <stripes:hidden name="sourceUri" value="${object.sourceUri}"/>

                                </fieldset>
                            </crfn:form>
                        </td>
                    </tr>

                    <c:set var="predicateNumberOfPages" value="${crfn:numberOfPages(predicateObjectsCount, actionBean.predicatePageSize)}"/>

                    <c:if test="${predicateNumberOfPages>1 && fn:length(predicate.value)>1 && objLoop.index==fn:length(predicate.value)-1}">
                        <c:set var="predicatePageNumber" value="${actionBean.predicatePageNumbers[predicate.key]}"/>
                        <tr>
                            <c:if test="${enableEdit}">
                                <th>&nbsp;</th>
                            </c:if>
                            <th>&nbsp;</th>
                            <td>&nbsp;</td>
                            <td class="factsheetValueBrowse">
                                <c:if test="${predicatePageNumber>1}">
                                    <c:choose>
                                        <c:when test="${predicatePageNumber==2}">
                                            <stripes:link href="${crfn:predicateExpandLink(actionBean,predicate.key,1)}" class="factsheetValueBrowse">First ${actionBean.predicatePageSize} values ...</stripes:link>
                                        </c:when>
                                        <c:otherwise>
                                            <stripes:link href="${crfn:predicateExpandLink(actionBean,predicate.key,predicatePageNumber-1)}" class="factsheetValueBrowse">Previous ${actionBean.predicatePageSize} values ...</stripes:link>
                                        </c:otherwise>
                                    </c:choose>
                                </c:if>
                                <c:if test="${predicatePageNumber != predicateNumberOfPages}">
                                    <c:choose>
                                        <c:when test="${predicatePageNumber == predicateNumberOfPages-1}">
                                            <c:if test="${predicatePageNumber>1}">&nbsp;|&nbsp;</c:if><stripes:link href="${crfn:predicateExpandLink(actionBean,predicate.key,predicateNumberOfPages)}" class="factsheetValueBrowse">Last ${predicateObjectsCount-(predicatePageNumber*actionBean.predicatePageSize)} values ...</stripes:link>
                                        </c:when>
                                        <c:otherwise>
                                            <c:if test="${predicatePageNumber>1}">&nbsp;|&nbsp;</c:if><stripes:link href="${crfn:predicateExpandLink(actionBean,predicate.key,predicatePageNumber+1)}" class="factsheetValueBrowse">Next ${actionBean.predicatePageSize} values ...</stripes:link>
                                        </c:otherwise>
                                    </c:choose>
                                </c:if>
                            </td>
                            <td>&nbsp;</td>
                        </tr>
                    </c:if>

                </c:forEach>
            </c:forEach>
        </tbody>
       </table>

    <div id="editDialog" title="Edit property value">
        <stripes:form beanclass="${actionBean['class'].name}" method="post">

            <div class="advice-msg" style="margin-top: 10px;margin-bottom: 20px;"><em>Edit the below property value and click 'Save' to submit the change.</em></div>

            <table>
                <tr>
                    <td class="question">Property:</td>
                    <td id="property-label">topConceptOf (http://www.w3.org/2004/02/skos/core#topConceptOf)</td>
                </tr>
                <tr>
                    <td style="vertical-align: top; padding-top: 10px;"><stripes:label for="txtValue" class="question">Value:</stripes:label></td>
                    <td style="padding-top: 10px;">
                        <stripes:textarea id="property-value" name="propertyValue" cols="60" rows="5"/>
                    </td>
                </tr>
                <tr>
                    <td>&nbsp;</td>
                    <td style="padding-top:10px">
                        <stripes:submit name="save" value="Save"/>
                        <input type="button" id="closeEditDialog" value="Cancel"/>
                    </td>
                </tr>
            </table>

            <fieldset style="display: none">
                <stripes:hidden name="uri" value="${subjectUri}"/>
                <stripes:hidden name="anonymous" value="${actionBean.subject.anonymous}"/>
                <stripes:hidden id="input-predicate-uri" name="propertyUri" value="" />
                <stripes:hidden id="input-source-uri" name="sourceUri" value=""/>
                <stripes:hidden id="input-old-property-value-md5" name="oldPropertyValueMd5" value=""/>
            </fieldset>

        </stripes:form>
    </div>

</c:if>
</stripes:layout-definition>
