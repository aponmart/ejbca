/*
 * Generated by XDoclet - Do not edit!
 */
package se.anatom.ejbca.ca.auth;

/**
 * Local home interface for AuthenticationSession.
 */
public interface IAuthenticationSessionLocalHome
   extends javax.ejb.EJBLocalHome
{
   public static final String COMP_NAME="java:comp/env/ejb/AuthenticationSessionLocal";
   public static final String JNDI_NAME="AuthenticationSessionLocal";

   public se.anatom.ejbca.ca.auth.IAuthenticationSessionLocal create()
      throws javax.ejb.CreateException;

}
