<%               
  BasePublisher publisherdata = publisherhelper.publisherdata;
   

  TreeMap authorizedcas = ejbcawebbean.getInformationMemory().getCANames();  
  HashMap caidtonamemap = ejbcawebbean.getInformationMemory().getCAIdToNameMap();
    
  int[] publishertypes = {LdapPublisher.TYPE_LDAPPUBLISHER, 
                          ActiveDirectoryPublisher.TYPE_ADPUBLISHER,
                          CustomPublisherContainer.TYPE_CUSTOMPUBLISHERCONTAINER};
  String[] publishertypetexts = {"LDAPPUBLISHER", "ACTIVEDIRECTORYPUBLISHER", "CUSTOMPUBLISHER"};

  int row = 0;
%>
<SCRIPT language="JavaScript">
<!--  

function checkallfields(){
    var illegalfields = 0;

 <% if( publisherdata instanceof LdapPublisher){ %>

 
    if(!checkfieldfordecimalnumbers("document.editpublisher.<%=EditPublisherJSPHelper.TEXTFIELD_LDAPPORT%>","<%= ejbcawebbean.getText("INVALIDPORTNUMBER") %>"))
      illegalfields++;
    
    if(document.editpublisher.<%=EditPublisherJSPHelper.PASSWORD_LDAPLOGINPASSWORD%>.value != document.editpublisher.<%=EditPublisherJSPHelper.PASSWORD_LDAPCONFIRMLOGINPWD%>.value ){
      alert("<%= ejbcawebbean.getText("PASSWORDSDOESNTMATCH") %>");
      illegalfields++; 
    }

    if(document.editpublisher.<%=EditPublisherJSPHelper.SELECT_LDAPUSEFIELDINLDAPDN%>.options.selectedIndex == -1){
      alert("<%=  ejbcawebbean.getText("MUSTSELECTATLEASTONEFIELD") %>");
      illegalfields++;
    }

    if(document.editpublisher.<%=EditPublisherJSPHelper.TEXTFIELD_LDAPHOSTNAME%>.value == ""){
      alert("<%= ejbcawebbean.getText("YOUAREREQUIRED") + " " + ejbcawebbean.getText("HOSTNAME")%>");
      illegalfields++;
    }

    if(document.editpublisher.<%=EditPublisherJSPHelper.TEXTFIELD_LDAPPORT%>.value == ""){
      alert("<%= ejbcawebbean.getText("YOUAREREQUIRED") + " " + ejbcawebbean.getText("PORT")%>");
      illegalfields++;
    }

    if(document.editpublisher.<%=EditPublisherJSPHelper.TEXTFIELD_LDAPBASEDN%>.value == ""){
      alert("<%= ejbcawebbean.getText("YOUAREREQUIRED") + " " + ejbcawebbean.getText("BASEDN")%>");
      illegalfields++;
    }

    if(document.editpublisher.<%=EditPublisherJSPHelper.TEXTFIELD_LDAPLOGINDN%>.value == ""){
      alert("<%= ejbcawebbean.getText("YOUAREREQUIRED") + " " + ejbcawebbean.getText("LOGINDN")%>");
      illegalfields++;
    }

    if(document.editpublisher.<%=EditPublisherJSPHelper.TEXTFIELD_LDAPUSEROBJECTCLASS%>.value == ""){
      alert("<%= ejbcawebbean.getText("YOUAREREQUIRED") + " " + ejbcawebbean.getText("USEROBJECTCLASS")%>");
      illegalfields++;
    }

    if(document.editpublisher.<%=EditPublisherJSPHelper.TEXTFIELD_LDAPCAOBJECTCLASS%>.value == ""){
      alert("<%= ejbcawebbean.getText("YOUAREREQUIRED") + " " + ejbcawebbean.getText("CAOBJECTCLASS")%>");
      illegalfields++;
    }

    if(document.editpublisher.<%=EditPublisherJSPHelper.TEXTFIELD_LDAPUSERCERTATTRIBUTE%>.value == ""){
      alert("<%= ejbcawebbean.getText("YOUAREREQUIRED") + " " + ejbcawebbean.getText("USERCERTIFICATEATTR")%>");
      illegalfields++;
    }

    if(document.editpublisher.<%=EditPublisherJSPHelper.TEXTFIELD_LDAPCACERTATTRIBUTE%>.value == ""){
      alert("<%= ejbcawebbean.getText("YOUAREREQUIRED") + " " + ejbcawebbean.getText("CACERTIFICATEATTR")%>");
      illegalfields++;
    }
    if(document.editpublisher.<%=EditPublisherJSPHelper.TEXTFIELD_LDAPCRLATTRIBUTE%>.value == ""){
      alert("<%= ejbcawebbean.getText("YOUAREREQUIRED") + " " + ejbcawebbean.getText("CRLATTRIBUTE")%>");
      illegalfields++;
    }
    if(document.editpublisher.<%=EditPublisherJSPHelper.TEXTFIELD_LDAPARLATTRIBUTE%>.value == ""){
      alert("<%= ejbcawebbean.getText("YOUAREREQUIRED") + " " + ejbcawebbean.getText("ARLATTRIBUTE")%>");
      illegalfields++;
    }
    if(document.editpublisher.<%=EditPublisherJSPHelper.PASSWORD_LDAPLOGINPASSWORD%>.value == ""){
      alert("<%= ejbcawebbean.getText("YOUAREREQUIRED") + " " + ejbcawebbean.getText("LOGINPWD")%>");
      illegalfields++;
    }

 <% } %>
 <% if( publisherdata instanceof CustomPublisherContainer){ %>
    if(document.editpublisher.<%=EditPublisherJSPHelper.TEXTFIELD_CUSTOMCLASSPATH%>.value == ""){
      alert("<%= ejbcawebbean.getText("YOUAREREQUIRED") + " " + ejbcawebbean.getText("CLASSPATH")%>");
      illegalfields++;
    }
 <% } %>
     return illegalfields == 0;  
   } 

 <% if( publisherdata instanceof LdapPublisher){ %>
 function setUseSSLPort(){
   if(document.editpublisher.<%=EditPublisherJSPHelper.CHECKBOX_LDAPUSESSL%>.checked){
     document.editpublisher.<%=EditPublisherJSPHelper.TEXTFIELD_LDAPPORT%>.value= <%= LdapPublisher.DEFAULT_SSLPORT%>;
   }else{
     document.editpublisher.<%=EditPublisherJSPHelper.TEXTFIELD_LDAPPORT%>.value= <%= LdapPublisher.DEFAULT_PORT%>;
   }
  }
 <% } %>
-->

</SCRIPT>
<div align="center"> 
  <h2><%= ejbcawebbean.getText("EDITPUBLISHER") %><br>
  </h2>
  <h3><%= ejbcawebbean.getText("PUBLISHER")+ " : " + publisherhelper.publishername %> </h3>
<% if(publisherhelper.connectionmessage){
     publisherhelper.connectionmessage = false;
     if(publisherhelper.connectionsuccessful){
        publisherhelper.connectionsuccessful = false;
        out.write("<H3>" + ejbcawebbean.getText("CONTESTEDSUCESSFULLY") +"</H3>");
     }else{
       out.write("<H3>" + ejbcawebbean.getText("ERRORCONNECTINGTOPUB") + " : " + publisherhelper.connectionerrormessage +"</H3>");
     }
   } %>
</div>
  <table width="100%" border="0" cellspacing="3" cellpadding="3">
    <tr id="Row<%=row++%2%>"> 
      <td width="50%" valign="top"> 
        <div align="left"> 
          <h3>&nbsp;</h3>
        </div>
      </td>
      <td width="50%" valign="top"> 
        <div align="right">
        <A href="<%=THIS_FILENAME %>"><u><%= ejbcawebbean.getText("BACKTOPUBLISHERS") %></u></A>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
   <!--     <A  onclick='displayHelpWindow("<%= ejbcawebbean.getHelpfileInfix("ca_help.html") + "#publishers"%>")'>
        <u><%= ejbcawebbean.getText("HELP") %></u> </A></div> -->
      </td>
    </tr>
    <tr  id="Row<%=row++%2%>"> 
      <td width="50%"  align="right"> 
        <%= ejbcawebbean.getText("NAME") %>
      </td>
      <td width="50%"> 
        <%= publisherhelper.publishername%>
      </td>
    </tr>
    <form name="publishertype" method="post" action="<%=THIS_FILENAME %>">
      <input type="hidden" name='<%= EditPublisherJSPHelper.ACTION %>' value='<%=EditPublisherJSPHelper.ACTION_CHANGE_PUBLISHERTYPE %>'>
      <input type="hidden" name='<%= EditPublisherJSPHelper.HIDDEN_PUBLISHERNAME %>' value='<%=publisherhelper.publishername %>'>
    <tr  id="Row<%=row++%2%>"> 
      <td width="50%"  align="right"> 
        <%= ejbcawebbean.getText("PUBLISHERTYPE") %>
      </td>
      <td width="50%">
        <select name="<%=EditPublisherJSPHelper.SELECT_PUBLISHERTYPE%>" size="1" onchange='document.publishertype.submit()'>
           <%  int currenttype = publisherhelper.getPublisherType(); 
               for(int i=0; i<publishertypes.length;i++){ %>
           <option  value="<%= publishertypes[i] %>" 
              <% if(publishertypes[i] == currenttype) out.write(" selected "); %>> 
              <%= ejbcawebbean.getText(publishertypetexts[i]) %>
           </option>
           <%   } %> 
        </select>
      </td>
    </tr>
   </form>
   </table>
   <table width="100%" border="0" cellspacing="3" cellpadding="3">
   <form name="editpublisher" method="post" action="<%=THIS_FILENAME %>">
    <input type="hidden" name='<%= EditPublisherJSPHelper.ACTION %>' value='<%=EditPublisherJSPHelper.ACTION_EDIT_PUBLISHER %>'>
    <input type="hidden" name='<%= EditPublisherJSPHelper.HIDDEN_PUBLISHERNAME %>' value='<%=publisherhelper.publishername %>'>
    <input type="hidden" name='<%= EditPublisherJSPHelper.HIDDEN_PUBLISHERTYPE %>' value='<%=publisherhelper.getPublisherType() %>'>
    <tr  id="Row<%=row++%2%>"> 
      <td width="50%"  align="right">         
         &nbsp;
      </td>
      <td width="50%">
         &nbsp;  
      </td>
    </tr>
    <% if(publisherhelper.publisherdata instanceof LdapPublisher){%>
         <%@ include file="ldappublisherpage.jsp" %> 
    <% }
      if(publisherhelper.publisherdata instanceof ActiveDirectoryPublisher){%>
         <%@ include file="adpublisherpage.jsp" %> 
    <% }
       if(publisherhelper.publisherdata instanceof CustomPublisherContainer){%>
         <%@ include file="custompublisherpage.jsp" %> 
    <% } %>
    <tr  id="Row<%=row++%2%>"> 
      <td width="50%"  align="right">         
         &nbsp;
      </td>
      <td width="50%">
         &nbsp;  
      </td>
    </tr>
    <tr  id="Row<%=row++%2%>"> 
      <td width="50%"  align="right">         
         <%= ejbcawebbean.getText("GENERALSETTINGS") %>:
      </td>
      <td width="50%">
         &nbsp;  
      </td>
    </tr>
    <tr  id="Row<%=row++%2%>"> 
      <td width="50%"  align="right"> 
         <%= ejbcawebbean.getText("DESCRIPTION") %> 
      </td>
      <td width="50%">        
        <textarea name="<%=EditPublisherJSPHelper.TEXTAREA_DESCRIPTION%>" cols=40 rows=6><% out.write(publisherdata.getDescription());%></textarea>
      </td>
    </tr>
    <tr  id="Row<%=row++%2%>"> 
      <td width="49%" valign="top">&nbsp;</td>
      <td width="51%" valign="top"> 
        <input type="submit" name="<%= EditPublisherJSPHelper.BUTTON_TESTCONNECTION %>" onClick='return checkallfields()' value="<%= ejbcawebbean.getText("SAVEANDTESTCONNECTION") %>">
        <input type="submit" name="<%= EditPublisherJSPHelper.BUTTON_SAVE %>" onClick='return checkallfields()' value="<%= ejbcawebbean.getText("SAVE") %>">
        <input type="submit" name="<%= EditPublisherJSPHelper.BUTTON_CANCEL %>" value="<%= ejbcawebbean.getText("CANCEL") %>">
      </td>
    </tr>
  </table>
 </form>