<%
  TreeMap publishernames = ejbcawebbean.getInformationMemory().getAuthorizedPublisherNames(); 
  
%>


<div align="center">
  <p><H1><%= ejbcawebbean.getText("EDITPUBLISHERS") %></H1></p>
 <!-- <div align="right"><A  onclick='displayHelpWindow("<%= ejbcawebbean.getHelpfileInfix("ca_help.html") + "#publishers"%>")'>
    <u><%= ejbcawebbean.getText("HELP") %></u> </A> -->
  </div>
  <form name="editpublishers" method="post"  action="<%= THIS_FILENAME%>">
    <input type="hidden" name='<%= publisherhelper.ACTION %>' value='<%=publisherhelper.ACTION_EDIT_PUBLISHERS %>'>
    <table width="100%" border="0" cellspacing="0" cellpadding="0">
    <% if(publisherhelper.publisherexists){ 
          publisherhelper.publisherexists= false;%> 
      <tr> 
        <td width="5%"></td>
        <td width="60%"><H4 id="alert"><%= ejbcawebbean.getText("PUBLISHERALREADY") %></H4></td>
        <td width="35%"></td>
      </tr>
    <% } %>
    <% if(publisherhelper.publisherdeletefailed){
          publisherhelper.publisherdeletefailed = false; 
          %> 
      <tr> 
        <td width="5%"></td>
        <td width="60%"><H4 id="alert"><%= ejbcawebbean.getText("COULDNTDELETEPUBLISHER") %></H4></td>
        <td width="35%"></td>
      </tr>
    <% } %>
      <tr> 
        <td width="5%"></td>
        <td width="60%"><H3><%= ejbcawebbean.getText("CURRENTPUBLISHERS") %></H3></td>
        <td width="35%"></td>
      </tr>
      <tr> 
        <td width="5%"></td>
        <td width="60%">
          <select name="<%=EditPublisherJSPHelper.SELECT_PUBLISHER%>" size="15"  >
            <% Iterator iter = publishernames.keySet().iterator();
               while(iter.hasNext()){
                 String publishername = (String) iter.next(); %>
                 
              <option value="<%=publishername%>"> 
                  <%= publishername %>
               </option>
            <%}%>
              <option value="">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</option>
          </select>
          </td>
      </tr>
      <tr> 
        <td width="5%"></td>
        <td width="60%"> 
          <table width="100%" border="0" cellspacing="0" cellpadding="0">
            <tr>
              <td>
                <input type="submit" name="<%= publisherhelper.BUTTON_EDIT_PUBLISHER %>" value="<%= ejbcawebbean.getText("EDITPUBLISHER") %>">
              </td>
              <td>
             &nbsp; 
              </td>
              <td>
                <input class=buttonstyle type="submit" onClick="return confirm('<%= ejbcawebbean.getText("AREYOUSURE") %>');" name="<%= publisherhelper.BUTTON_DELETE_PUBLISHER %>" value="<%= ejbcawebbean.getText("DELETEPUBLISHER") %>">
              </td>
            </tr>
          </table> 
        </td>
        <td width="35%"> </td>
      </tr>
    </table>
   
  <p align="left"> </p>
    <table width="100%" border="0" cellspacing="0" cellpadding="0">
      <tr> 
        <td width="5%"></td>
        <td width="95%"><H3><%= ejbcawebbean.getText("ADD") %></H3></td>
      </tr>
      <tr> 
        <td width="5%"></td>
        <td width="95%"> 
          <input type="text" name="<%= publisherhelper.TEXTFIELD_PUBLISHERNAME%>" size="40" maxlength="255">   
          <input type="submit" name="<%= publisherhelper.BUTTON_ADD_PUBLISHER%>" onClick='return checkfieldforlegalchars("document.editpublishers.<%=publisherhelper.TEXTFIELD_PUBLISHERNAME%>","<%= ejbcawebbean.getText("ONLYCHARACTERS") %>")' value="<%= ejbcawebbean.getText("ADD") %>">&nbsp;&nbsp;&nbsp;
          <input type="submit" name="<%= publisherhelper.BUTTON_RENAME_PUBLISHER%>" onClick='return checkfieldforlegalchars("document.editpublishers.<%=publisherhelper.TEXTFIELD_PUBLISHERNAME%>","<%= ejbcawebbean.getText("ONLYCHARACTERS") %>")' value="<%= ejbcawebbean.getText("RENAMESELECTED") %>">&nbsp;&nbsp;&nbsp;
          <input type="submit" name="<%= publisherhelper.BUTTON_CLONE_PUBLISHER%>" onClick='return checkfieldforlegalchars("document.editpublishers.<%=publisherhelper.TEXTFIELD_PUBLISHERNAME%>","<%= ejbcawebbean.getText("ONLYCHARACTERS") %>")' value="<%= ejbcawebbean.getText("USESELECTEDASTEMPLATE") %>">
        </td>
      </tr>
      <tr> 
        <td width="5%">&nbsp; </td>
        <td width="95%">&nbsp;</td>
      </tr>
    </table>
  </form>
  <p align="center">&nbsp;</p>
  <p>&nbsp;</p>
</div>

