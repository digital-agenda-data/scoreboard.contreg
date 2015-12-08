<%@page contentType="text/html;charset=UTF-8"%>

<%@ include file="/pages/common/taglibs.jsp"%>


<%@page import="net.sourceforge.stripes.action.ActionBean"%><stripes:layout-render name="/pages/common/template.jsp" pageTitle="Delete observations">

    <stripes:layout-component name="contents">

        <h1>Delete observations matching below criteria</h1>

        <p>
            This is an administrators page for deleting observations of a particular indicator from a particular dataset.<br/>
            The observations time period is also one possible input criteria.<br/>
            You have to select the target dataset, and supply the URIs of indicators and time periods as a white-space separated list.<br/>
            Specifying at least one indicator is mandatory, specifying a time period is not.<br/>
            The logical operator between the list of indicators and the list of time periods is AND.
        </p>

        <div style="margin-top:1em">
            <crfn:form beanclass="${actionBean['class'].name}" method="post">
                <div>
                    <stripes:label for="datasetSelect" class="question required">Target dataset:</stripes:label><br/>
                    <stripes:select id="datasetSelect" name="datasetUri" value="${actionBean.datasetUri}">
                        <c:forEach items="${actionBean.availableDatasetUris}" var="dst">
                            <stripes:option value="${dst}" label="${dst}" title="${dst}"/>
                        </c:forEach>
                    </stripes:select>
                </div>
                <div style="margin-top:0.8em">
                    <stripes:label for="indicatorsText" class="question required">Indicator URIs:</stripes:label><br/>
                    <stripes:textarea id="indicatorsText" name="indicatorUris" cols="70" rows="4"/>
                </div>
                <div style="margin-top:0.8em">
                    <stripes:label for="timePeriodsText" class="question">Time period URIs:</stripes:label><br/>
                    <stripes:textarea id="timePeriodsText" name="timePeriodUris" cols="70" rows="4"/><br/>
                    <stripes:submit name="delete" value="Delete"/>
                </div>
            </crfn:form>
        </div>

        <div id="executed_sparql" style="display:none">
	        <pre>
${actionBean.executedSparql}
	        </pre>
        </div>

    </stripes:layout-component>

</stripes:layout-render>
