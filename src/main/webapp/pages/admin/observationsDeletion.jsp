<%@page contentType="text/html;charset=UTF-8"%>

<%@ include file="/pages/common/taglibs.jsp"%>


<%@page import="net.sourceforge.stripes.action.ActionBean"%><stripes:layout-render name="/pages/common/template.jsp" pageTitle="Delete observations">

    <stripes:layout-component name="contents">
    
        <script type="text/javascript" xml:space="preserve">
            ( function($) {
                $(document).ready(function(){
                	
                	$('#submitForm').submit(function() {
                	    $('#loader').show();
                	    return true;
                	});
                	
                });
            } ) ( jQuery );
        </script>

        <h1>Delete observations matching below criteria</h1>

        <p>
            This is an administrators page for deleting observations of particular indicators and time periods from particular datasets.<br/>
            All values are to be provided as lists (separated by new line) of URIs.<br/>
            The logical operator between the lists of datasets, indicators and time periods is AND.<br/>
            At least one dataset has to be provided!<br/>
            If no indicators and time periods are provided, then all observations of the given dataset(s) will be deleted!.<br/>
            
        </p>

        <div style="margin-top:1em">
            <crfn:form id="submitForm" beanclass="${actionBean['class'].name}" method="post">
                <div style="margin-top:0.8em">
                    <stripes:label for="datasetsText" class="question required">Dataset URIs:</stripes:label><br/>
                    <stripes:textarea id="datasetsText" name="datasetUris" cols="70" rows="4"/>
                </div>
                <div style="margin-top:0.8em">
                    <stripes:label for="indicatorsText" class="question">Indicator URIs:</stripes:label><br/>
                    <stripes:textarea id="indicatorsText" name="indicatorUris" cols="70" rows="4"/>
                </div>
                <div style="margin-top:0.8em">
                    <stripes:label for="timePeriodsText" class="question">Time period URIs:</stripes:label><br/>
                    <stripes:textarea id="timePeriodsText" name="timePeriodUris" cols="70" rows="4"/><br/>
                    
                    <stripes:submit name="delete" value="Delete" onclick="return this.form.elements['indicatorsText'].value.trim()=='' && this.form.elements['timePeriodsText'].value.trim()=='' ? confirm('You have not specified any indicators or time periods. This will delete ALL observations in your desired dataset(s). Click OK to confirm, otherwise click Cancel.') : true;"/>
                    
                    <label><img id="loader" src="http://dev.cloudcell.co.uk/bin/loading.gif" style="display:none"/></label>
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
