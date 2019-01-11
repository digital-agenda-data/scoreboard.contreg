<%@page contentType="text/html;charset=UTF-8"%>

<%@ include file="/pages/common/taglibs.jsp"%>

<stripes:layout-render name="/pages/common/template.jsp" pageTitle="Simple search">

    <stripes:layout-component name="head">
        <script type="text/javascript">
        // <![CDATA[
        ( function($) {
            $(document).ready(function() {
                // Open delete bookmarked queries dialog
                $("#queryDialogLink").click(function() {
                    $('#queryDialog').dialog('open');
                    return false;
                });

                // Dialog setup
                $('#queryDialog').dialog({
                    autoOpen: false,
                    width: 500
                });

                // Close dialog
                $("#closeQueryDialog").click(function() {
                    $('#queryDialog').dialog("close");
                    return true;
                });
            });

        } ) ( jQuery );

        // ]]>
        </script>
    </stripes:layout-component>

    <stripes:layout-component name="contents">

    <c:if test="${not empty actionBean.queryString}">
    <ul id="dropdown-operations">
        <li><a href="#">Operations</a>
            <ul>
                <li>
                    <a href="#" id="queryDialogLink">Search query</a>
                </li>
            </ul>
        </li>
    </ul>
    </c:if>

    <h1>Simple search</h1>
    <p>
    This page enables you to find content by case-insensitive text in any metadata element. For example:
    to search for content that contains words "air" or "soil" or both, enter <span class="searchExprSample">air | soil</span>.
    Entering <span class="searchExprSample">air pollution</span> or <span class="searchExprSample">air &amp; pollution</span>
    will require the content to have both words.<br/>
    Entering <span class="searchExprSample">"air pollution"</span> will require the content to have the exact phrase "air pollution".
    </p>
    <p>
    Wildcards (*) can also be used. Wildcard requires at least four leading characters. Example: <span class="searchExprSample">nois* | water</span>
    </p>
    <p>
    Words shorter than 3 letters are ignored.
    </p>

    <crfn:form action="/simpleSearch.action" method="get">
        <table class="formtable">
            <tr>
                <td>
                <stripes:label for="expressionText" class="question">Expression</stripes:label>
                </td>
                <td colspan="2">
                <stripes:text name="searchExpression" id="expressionText" size="70"/>
                <stripes:text name="dummy" style="visibility:hidden;display:none" disabled="disabled" size="1" title="Hidden dummy input."/>
                <stripes:submit name="search" value="Search" id="searchButton"/>
                </td>
            </tr>
            <tr>
                <td>
                    <stripes:label for="nofield" class="question">What type</stripes:label>
                </td>
                <td>
                    <fieldset style="border:0;padding-left: 0px;">
                        <legend style="visibility: hidden;display: none;">Search type</legend>
                        <stripes:radio id="anyObject" name="simpleFilter" value="anyObject" checked="anyObject" title="Any Object"/>
                        <stripes:label for="anyObject">Any Object</stripes:label>

                        <stripes:radio id="anyFile" name="simpleFilter" value="anyFile"/>
                        <stripes:label for="anyFile">Any File</stripes:label>

                        <stripes:radio id="texts" name="simpleFilter" value="texts"/>
                        <stripes:label for="texts">Texts</stripes:label>

                        <stripes:radio id="datasets" name="simpleFilter" value="datasets"/>
                        <stripes:label for="datasets">Datasets</stripes:label>

                        <stripes:radio id="images" name="simpleFilter" value="images"/>
                        <stripes:label for="images">Images</stripes:label>
                    </fieldset>
                </td>
                </td>
                <td class="exactMatch">
                    <stripes:checkbox name="exactMatch" id="exactMatch"/>
                    <stripes:label for="exactMatch">Exact match</stripes:label>
                </td>
            </tr>
        </table>
    </crfn:form>
    <c:choose>
        <c:when test="${not empty param.search}">
            <c:if test='${empty actionBean.resultList and actionBean.uri and crfn:userHasPermission(pageContext.session, "/registrations", "u")}'>
                <stripes:link href="/admin/registerUrl.action"> Register this URL
                    <stripes:param name="url" value="${actionBean.searchExpression}" />
                </stripes:link>
            </c:if>
            <stripes:layout-render name="/pages/common/subjectsResultList.jsp" tableClass="sortable"/>
        </c:when>
        <c:otherwise>
            <h2>Advanced search operators</h2>
            <dl>
                <dt><code class="literal">http://</code> or <code class="literal">https://</code></dt>
                <dd>
                   Any phrase that starts with a URL prefix is queried as an exact search.
                </dd>
                <dt><code class="literal">&</code></dt>
                <dd>
                    A leading ampersand indicates that this word
                    <span class="emphasis"><em>must</em></span> be present in each row that is returned.
                </dd>
                <dt><code class="literal">|</code></dt>
                <dd>
                    A leading vertical bar indicates that this word is
                    <span class="emphasis"><em>optional</em></span> in each row that is returned.
                </dd>
                <dt><code class="literal">!</code></dt>
                <dd>
                    A leading exclamation mark indicates that this word must
                    <span class="emphasis"><em>not</em></span> be present in any of the rows that
                    are returned.
                </dd>
                <dd>
                    Note: The <code class="literal">!</code> operator acts only to exclude
                    rows that are otherwise matched by other search terms. Thus,
                    a boolean-mode search that contains only terms preceded by
                    <code class="literal">!</code> returns an empty result. It does not
                    return "<span class="quote">all rows except those containing any of the
                    excluded terms.</span>"
                </dd>
                <dt><code class="literal">no operator</code></dt>
                <dd>
                    By default (when neither of the above operators is specified) the word
                    <span class="emphasis"><em>must</em></span> be present in each row that is returned.
                    So by default the search engine uses <code class="literal">&</code> if no operator is specified.
                </dd>
                <dt><code class="literal">( )</code></dt>
                <dd>
                    Parentheses group words into subexpressions. Parenthesized groups can be nested.
                </dd>
                <dt><code class="literal">"</code></dt>
                <dd>
                    A phrase that is enclosed within double quote
                    ("<span class="quote"><code class="literal">"</code></span>") characters matches
                    only rows that contain the phrase <span class="emphasis"><em>literally, as it
                    was typed</em></span>. The search engine splits the phrase
                    into words, performs a search in the
                    <code class="literal">FULLTEXT</code> index for the words. Nonword
                    characters need not be matched exactly: Phrase searching
                    requires only that matches contain exactly the same words as
                    the phrase and in the same order. For example,
                    <code class="literal">"test phrase"</code> matches <code class="literal">"test,
                    phrase"</code>.
                </dd>
                <dd>
                    If the phrase contains no words that are in the index, the
                    result is empty. For example, if all words are either
                    stopwords or shorter than the minimum length of indexed
                    words, the result is empty.
                </dd>
            </dl>
        </c:otherwise>
    </c:choose>
    <div id="queryDialog" title="Search query">
        <c:if test="${not empty actionBean.queryString}">
            <pre><c:out value="${actionBean.queryString}" /></pre>
            <crfn:form action="/sparql" method="get">
                <stripes:hidden name="query" value="${actionBean.queryString}" />
                <br />
                <br />
                <stripes:submit name="noEvent" value="Edit SPARQL query" />
            </crfn:form>
        </c:if>
    </div>
</stripes:layout-component>
</stripes:layout-render>
