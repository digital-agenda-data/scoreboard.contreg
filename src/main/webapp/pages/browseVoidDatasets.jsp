<%@page contentType="text/html;charset=UTF-8"%>

<%@ include file="/pages/common/taglibs.jsp"%>

<stripes:layout-render name="/pages/common/template.jsp" pageTitle="Browse VoID datasets">

     <stripes:layout-component name="head">
        <script type="text/javascript">
        // <![CDATA[
        function submitFiltersForm(){
            document.getElementById('loadingMessage').style.visibility='visible';
            document.getElementById('filtersForm').submit();
            return true;
        }

        function clearFilters(filterName){

            var i;
            var formElems = document.getElementById('filtersForm').elements;
            var formElemsLen = formElems.length;

            for (i=0; i<formElemsLen; i++){
                if (formElems[i].name == filterName){
                    if (formElems[i].type == 'checkbox'){
                        if (formElems[i].checked == true){
                            formElems[i].checked = false;
                        }
                    }
                }
            }

            return submitFiltersForm();
        }

        function clearerChanged(clearer, filterName){

            if (clearer.checked == true){
                clearer.checked = false;
                return false;
            }
            else{
                return clearFilters(filterName);
            }
        }

        function toggleCheckbox(id){
            var chkbox = document.getElementById(id);
            chkbox.checked = !chkbox.checked;
        }

        // ]]>
        </script>
    </stripes:layout-component>

    <stripes:layout-component name="contents">

        <h1>Browse VoID datasets</h1>

        <crfn:form id="filtersForm" beanclass="${actionBean['class'].name}" method="post">

            <fieldset>
                <legend>Filters</legend>
                <div style="float:left;width:49%">
                    <div style="width:100%;">
                        <table style="background-color:#CCCCCC;color:#FFFFFF;font-weight:bold;width:100%;">
                            <tr>
                                <td style="padding-left:5px;">Creator</td>
                                <td style="width:7%;">
                                    <input type="checkbox" name="creatorsClearer" title="Clear this filter" onchange="clearerChanged(this,'creator');" ${empty actionBean.selectedCreators ? '' : 'checked="checked"'}}/>
                                </td>
                            </tr>
                        </table>
                    </div>
                    <div style="height:130px;overflow:auto;background-color:#F2F2F2;">
                        <c:if test="${not empty actionBean.availableCreators}">
                            <table style="width:100%;height:100%;background-color:#F2F2F2;">
                                <c:forEach items="${actionBean.availableCreators}" var="availableCreator" varStatus="creatorsLoop">
                                    <c:if test="${not empty actionBean.selectedCreators[availableCreator]}">
                                        <c:set var="isCreatorChecked" value='checked="checked"'/>
                                    </c:if>
                                    <c:if test="${empty actionBean.selectedCreators[availableCreator]}">
                                        <c:set var="isCreatorChecked" value=""/>
                                    </c:if>
                                    <tr>
                                        <td style="padding-left:5px;font-size:0.8em;" onclick="toggleCheckbox('creator_${creatorsLoop.index}');return submitFiltersForm();">
                                            <label for="creator_${creatorsLoop.index}" title="Select this creator" style="color:#2A6495"><c:out value="${availableCreator}"/></label>
                                        </td>
                                        <td style="width:7%" onclick="toggleCheckbox('creator_${creatorsLoop.index}');return submitFiltersForm();">
                                            <input type="checkbox" id="creator_${creatorsLoop.index}" name="creator" value="${availableCreator}" onchange="return submitFiltersForm();" title="Select this creator" ${isCreatorChecked}/>
                                        </td>
                                    </tr>
                                </c:forEach>
                            </table>
                        </c:if>
                        <c:if test="${empty actionBean.availableCreators}">
                            <c:out value="None found!"/>
                        </c:if>
                    </div>
                </div>
                <div style="float:right;width:49%">
                    <div style="width:100%;">
                        <table style="background-color:#CCCCCC;color:#FFFFFF;font-weight:bold;width:100%;">
                            <tr>
                                <td style="padding-left:5px;">Subject</td>
                                <td style="width:7%;">
                                    <input type="checkbox" name="subjectsClearer" title="Clear this filter" onchange="clearerChanged(this,'subject');" ${empty actionBean.selectedSubjects ? '' : 'checked="checked"'}}/>
                                </td>
                            </tr>
                        </table>
                    </div>
                    <div style="height:130px;overflow:auto;background-color:#F2F2F2;">
                        <c:if test="${not empty actionBean.availableSubjects}">
                            <table style="width:100%;height:100%;background-color:#F2F2F2;">
                                <c:forEach items="${actionBean.availableSubjects}" var="availableSubject" varStatus="subjectsLoop">
                                    <c:if test="${not empty actionBean.selectedSubjects[availableSubject]}">
                                        <c:set var="isSubjectChecked" value='checked="checked"'/>
                                    </c:if>
                                    <c:if test="${empty actionBean.selectedSubjects[availableSubject]}">
                                        <c:set var="isSubjectChecked" value=""/>
                                    </c:if>
                                    <tr>
                                        <td style="padding-left:5px;font-size:0.8em;" onclick="toggleCheckbox('subject_${subjectsLoop.index}');return submitFiltersForm();">
                                            <label for="subject_${subjectsLoop.index}" title="Select this subject" style="color:#2A6495"><c:out value="${availableSubject}"/></label>
                                        </td>
                                        <td style="width:7%" onclick="toggleCheckbox('subject_${subjectsLoop.index}');return submitFiltersForm();">
                                            <input type="checkbox" id="subject_${subjectsLoop.index}" name="subject" value="${availableSubject}" onchange="return submitFiltersForm();" title="Select this subject" ${isSubjectChecked}/>
                                        </td>
                                    </tr>
                                </c:forEach>
                            </table>
                        </c:if>
                        <c:if test="${empty actionBean.availableSubjects}">
                            <c:out value="None found!"/>
                        </c:if>
                    </div>
                </div>
                <stripes:hidden name="prevCrtSize" value="${actionBean.prevCrtSize}"/>
                <stripes:hidden name="prevCrtSize" value="${actionBean.prevCrtSize}"/>
            </fieldset>

        </crfn:form>
        <br/>
        <display:table name="${actionBean.datasets}" class="sortable" id="dataset" sort="list" pagesize="20" requestURI="${actionBean.urlBinding}" style="width:98%;">

            <display:setProperty name="paging.banner.item_name" value="dataset"/>
            <display:setProperty name="paging.banner.items_name" value="datasets"/>
            <display:setProperty name="paging.banner.all_items_found" value='<span class="pagebanner">{0} {1} found.</span>'/>
            <display:setProperty name="paging.banner.onepage" value=""/>

            <display:column title='<span title="Title or label of the dataset, either supplied or derived from dataset URI.">Title</span>' sortable="true" sortProperty="label" style="width:50%;">
                <stripes:link beanclass="${actionBean.factsheetActionBeanClass.name}" title="Go to the dataset factsheet">
                    <c:out value="${dataset.label}"/>
                    <stripes:param name="uri" value="${dataset.uri}"/>
                </stripes:link>
            </display:column>
            <display:column style="width:25%;text-align:center" title='<span title="The dataset creator(s)">Creator</span>' sortable="true" sortProperty="creator">
                <c:out value="${dataset.creator}"/>
            </display:column>
            <display:column style="width:25%;text-align:center" title='<span title="The dataset subject(s)">Subject</span>' sortable="true" sortProperty="subjects">
                <c:out value="${dataset.subjects}"/>
            </display:column>

        </display:table>

        <div id="loadingMessage" style="padding:20px; border:1px solid; background-color: #cccccc; width: 200px; height: 50px; position:absolute; z-index:100000; left: 300px; top: 300px; visibility:hidden;">
            <img src="${pageContext.request.contextPath}/images/wait.gif" alt="Waiting clock ..."/><span style="padding-left:20px;">Loading...</span>
        </div>

    </stripes:layout-component>
</stripes:layout-render>
