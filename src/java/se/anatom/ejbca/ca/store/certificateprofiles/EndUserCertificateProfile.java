package se.anatom.ejbca.ca.store.certificateprofiles;

import java.util.ArrayList;

/**
 * EndUserCertificateProfile is a class defining the fixed characteristics of an enduser certificate type
 *
* @version $Id: EndUserCertificateProfile.java,v 1.12 2003-10-31 14:41:24 herrvendil Exp $
  */
public class EndUserCertificateProfile extends CertificateProfile{

    // Public Constants

    public final static String CERTIFICATEPROFILENAME =  "ENDUSER";

    // Public Methods
    /** Creates a certificate with the characteristics of an end user. */
    public EndUserCertificateProfile() {

      setCertificateVersion(VERSION_X509V3);
      setValidity(730);

      setUseBasicConstraints(true);
      setBasicConstraintsCritical(true);


      setUseSubjectKeyIdentifier(true);
      setSubjectKeyIdentifierCritical(false);

      setUseAuthorityKeyIdentifier(true);
      setAuthorityKeyIdentifierCritical(false);

      setUseSubjectAlternativeName(true);
      setSubjectAlternativeNameCritical(false);

      setUseCRLDistributionPoint(false);
      setCRLDistributionPointCritical(false);
      setCRLDistributionPointURI("");

      setUseCertificatePolicies(false);
      setCertificatePoliciesCritical(false);
      setCertificatePolicyId("2.5.29.32.0");

      setType(TYPE_ENDENTITY);

      int[] bitlengths = {512,1024,2048,4096};
      setAvailableBitLengths(bitlengths);

      // Standard key usages for end users are: digitalSignature | keyEncipherment or nonRepudiation
      // Default key usage is digitalSignature | keyEncipherment
      // Create an array for KeyUsage acoording to X509Certificate.getKeyUsage()
      setUseKeyUsage(true);
      setKeyUsage(new boolean[9]);
      setKeyUsage(DIGITALSIGNATURE,true);
      setKeyUsage(KEYENCIPHERMENT,true);
      setKeyUsageCritical(true);

      setUseExtendedKeyUsage(true);
      ArrayList eku = new ArrayList();
      eku.add(new Integer(SERVERAUTH));
      eku.add(new Integer(CLIENTAUTH));
      eku.add(new Integer(EMAILPROTECTION));
      eku.add(new Integer(IPSECENDSYSTEM));
      eku.add(new Integer(IPSECUSER));
      setExtendedKeyUsage(eku);
      setExtendedKeyUsageCritical(false);
      
      ArrayList availablecas = new ArrayList();
      availablecas.add(new Integer(ANYCA));
      setAvailableCAs(availablecas);      
      setPublisherList(new ArrayList());
    }

    // Public Methods.
    public void upgrade(){
      if(LATEST_VERSION != getVersion()){
        // New version of the class, upgrade

        super.upgrade();         
      }
    }


    // Private fields.

}
