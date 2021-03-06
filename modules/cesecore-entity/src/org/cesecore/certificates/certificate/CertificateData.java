/*************************************************************************
 *                                                                       *
 *  CESeCore: CE Security Core                                           *
 *                                                                       *
 *  This software is free software; you can redistribute it and/or       *
 *  modify it under the terms of the GNU Lesser General Public           *
 *  License as published by the Free Software Foundation; either         *
 *  version 2.1 of the License, or any later version.                    *
 *                                                                       *
 *  See terms of license at gnu.org.                                     *
 *                                                                       *
 *************************************************************************/
package org.cesecore.certificates.certificate;

import java.io.Serializable;
import java.math.BigInteger;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.persistence.ColumnResult;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.PostLoad;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Query;
import javax.persistence.SqlResultSetMapping;
import javax.persistence.SqlResultSetMappings;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.TypedQuery;

import org.apache.log4j.Logger;
import org.cesecore.certificates.crl.RevokedCertInfo;
import org.cesecore.certificates.endentity.EndEntityConstants;
import org.cesecore.config.CesecoreConfiguration;
import org.cesecore.dbprotection.ProtectedData;
import org.cesecore.dbprotection.ProtectionStringBuilder;
import org.cesecore.keys.util.KeyTools;
import org.cesecore.util.Base64;
import org.cesecore.util.CertTools;
import org.cesecore.util.CompressedCollection;
import org.cesecore.util.QueryResultWrapper;
import org.cesecore.util.StringTools;
import org.cesecore.util.ValueExtractor;

/**
 * Representation of a certificate and related information.
 *
 * @version $Id$
 */
@Entity
@Table(name = "CertificateData")
@SqlResultSetMappings(value = {
        @SqlResultSetMapping(name = "RevokedCertInfoSubset", columns = { @ColumnResult(name = "fingerprint"), @ColumnResult(name = "serialNumber"),
                @ColumnResult(name = "expireDate"), @ColumnResult(name = "revocationDate"), @ColumnResult(name = "revocationReason") }),
        @SqlResultSetMapping(name = "CertificateInfoSubset", columns = { @ColumnResult(name = "issuerDN"), @ColumnResult(name = "subjectDN"),
                @ColumnResult(name = "cAFingerprint"), @ColumnResult(name = "status"), @ColumnResult(name = "type"),
                @ColumnResult(name = "serialNumber"),
                @ColumnResult(name = "notBefore"), @ColumnResult(name = "expireDate"), @ColumnResult(name = "revocationDate"),
                @ColumnResult(name = "revocationReason"), @ColumnResult(name = "username"), @ColumnResult(name = "tag"),
                @ColumnResult(name = "certificateProfileId"), @ColumnResult(name = "endEntityProfileId"), @ColumnResult(name = "updateTime"),
                @ColumnResult(name = "subjectKeyId"), @ColumnResult(name = "subjectAltName") }),
        @SqlResultSetMapping(name = "CertificateInfoSubset2", columns = { @ColumnResult(name = "fingerprint"), @ColumnResult(name = "subjectDN"),
                @ColumnResult(name = "cAFingerprint"), @ColumnResult(name = "status"), @ColumnResult(name = "type"),
                @ColumnResult(name = "notBefore"), @ColumnResult(name = "expireDate"), @ColumnResult(name = "revocationDate"),
                @ColumnResult(name = "revocationReason"), @ColumnResult(name = "username"), @ColumnResult(name = "tag"),
                @ColumnResult(name = "certificateProfileId"), @ColumnResult(name = "endEntityProfileId"), @ColumnResult(name = "updateTime"),
                @ColumnResult(name = "subjectKeyId"), @ColumnResult(name = "subjectAltName") }),
        @SqlResultSetMapping(name = "FingerprintUsernameSubset", columns = { @ColumnResult(name = "fingerprint"), @ColumnResult(name = "username") }) })
public class CertificateData extends ProtectedData implements Serializable {

    private static final long serialVersionUID = -8493105317760641442L;

    private static final Logger log = Logger.getLogger(CertificateData.class);

    private String issuerDN;
    private String subjectDN;
    private String subjectAltName = null;  // @since EJBCA 6.6.0
    private String fingerprint = "";
    private String cAFingerprint;
    private int status = 0;
    private int type = 0;
    private String serialNumber;
    private Long notBefore = null;  // @since EJBCA 6.6.0
    private long expireDate = 0;
    private long revocationDate = 0;
    private int revocationReason = 0;
    private String base64Cert;
    private String username;
    private String tag;
    private Integer certificateProfileId;
    private Integer endEntityProfileId = null;  // @since EJBCA 6.6.0
    private long updateTime = 0;
    private String subjectKeyId;
    private int rowVersion = 0;
    private String rowProtection;

    /**
     * Entity holding info about a certificate. Create by sending in the certificate, which extracts (from the cert) fingerprint (primary key),
     * subjectDN, issuerDN, serial number, expiration date. Status, Type, CAFingerprint, revocationDate and revocationReason are set to default values
     * (CERT_UNASSIGNED, USER_INVALID, null, null and REVOCATION_REASON_UNSPECIFIED) and should be set using the respective set-methods.
     *
     * NOTE! Never use this constructor without considering the useBase64CertTable below!
     *
     * @param certificate the (X509)Certificate to be stored in the database. If the property "database.useSeparateCertificateTable" is true then it should be null.
     * @param enrichedpubkey possibly an EC public key enriched with the full set of parameters, if the public key in the certificate does not have
     *            parameters. Can be null if RSA or certificate public key contains all parameters.
     * @param username the username in UserData to map the certificate to
     * @param cafp CA certificate fingerprint, can be null
     * @param status status of the certificate, active, revoked etcc, i.e. CertificateConstants.CERT_ACTIVE etc
     * @param type the user type the certificate belongs to, i.e. EndEntityTypes.USER_ENDUSER etc
     * @param certprofileid certificate profile id, can be 0 for "no profile"
     * @param endEntityProfileId end entity profile id, can be 0 for "no profile"
     * @param tag a custom tag to map the certificate to any custom defined tag
     * @param updatetime the time the certificate was updated in the database, i.e. System.currentTimeMillis().
     * @param storeCertificate true if the certificate should be stored in this table in the base&4Cert column, false if certificate data isn't to be stored in this table. NOTE: If false and the data should be stored in Base64CertData then the caller must store the certificate in Base64CertData as well.
     * @param storeSubjectAltName true if the subjectAltName column should be populated with the Subject Alternative Name of the certificate
     */
    public CertificateData(Certificate certificate, PublicKey enrichedpubkey, String username, String cafp, int status, int type, int certprofileid, int endEntityProfileId,
            String tag, long updatetime, boolean storeCertificate, boolean storeSubjectAltName) {
        // Extract all fields to store with the certificate.
        try {
            if (storeCertificate ) {
                setBase64Cert(new String(Base64.encode(certificate.getEncoded())));
            }

            String fp = CertTools.getFingerprintAsString(certificate);
            setFingerprint(fp);

            // Make sure names are always looking the same
            setSubjectDN(CertTools.getSubjectDN(certificate));
            setIssuerDN(CertTools.getIssuerDN(certificate));
            if (storeSubjectAltName) {
                setSubjectAltName(CertTools.getSubjectAlternativeName(certificate));
            }
            if (log.isDebugEnabled()) {
                log.debug("Creating CertificateData, subjectDN=" + getSubjectDnNeverNull() + ", subjectAltName=" + getSubjectAltNameNeverNull() + ", issuer=" + getIssuerDN() + ", fingerprint=" + fp+", storeSubjectAltName="+storeSubjectAltName);
            }
            setSerialNumber(CertTools.getSerialNumber(certificate).toString());

            setUsername(username);
            // Values for status and type
            setStatus(status);
            setType(type);
            setCaFingerprint(cafp);
            final Date notBefore = CertTools.getNotBefore(certificate);
            if (notBefore==null) {
                setNotBefore(null);
            } else {
                setNotBefore(notBefore.getTime());
            }
            setExpireDate(CertTools.getNotAfter(certificate));
            setRevocationDate(-1L);
            setRevocationReason(RevokedCertInfo.NOT_REVOKED);
            setUpdateTime(updatetime); // (new Date().getTime());
            setCertificateProfileId(certprofileid);
            setEndEntityProfileId(Integer.valueOf(endEntityProfileId));
            // Create a key identifier
            PublicKey pubk = certificate.getPublicKey();
            if (enrichedpubkey != null) {
                pubk = enrichedpubkey;
            }
            // Creating the KeyId may just throw an exception, we will log this but store the cert and ignore the error
            String keyId = null;
            try {
                keyId = new String(Base64.encode(KeyTools.createSubjectKeyId(pubk).getKeyIdentifier(), false));
            } catch (Exception e) {
                log.warn("Error creating subjectKeyId for certificate with fingerprint '" + fp + ": ", e);
            }
            setSubjectKeyId(keyId);
            setTag(tag);
        } catch (CertificateEncodingException cee) {
            final String msg = "Can't extract DER encoded certificate information.";
            log.error(msg, cee);
            throw new RuntimeException(msg);
        }
    }

    /**
     * Copy Constructor
     */
    public CertificateData(final CertificateData copy) {
        setBase64Cert(copy.getBase64Cert());
        setFingerprint(copy.getFingerprint());
        setSubjectDN(copy.getSubjectDN());
        setIssuerDN(copy.getIssuerDN());
        setSubjectAltName(copy.getSubjectAltName());
        setSerialNumber(copy.getSerialNumber());
        setUsername(copy.getUsername());
        setStatus(copy.getStatus());
        setType(copy.getType());
        setCaFingerprint(copy.getCaFingerprint());
        setNotBefore(copy.getNotBefore());
        setExpireDate(copy.getExpireDate());
        setRevocationDate(copy.getRevocationDate());
        setRevocationReason(copy.getRevocationReason());
        setUpdateTime(copy.getUpdateTime());
        setCertificateProfileId(copy.getCertificateProfileId());
        setEndEntityProfileId(copy.getEndEntityProfileId());
        setSubjectKeyId(copy.getSubjectKeyId());
        setTag(copy.getTag());
        setRowVersion(copy.getRowVersion());
        setRowProtection(copy.getRowProtection());
    }

    public CertificateData() {
    }

    /**
     * Fingerprint of certificate
     *
     * @return fingerprint
     */
    // @Id @Column
    public String getFingerprint() {
        return fingerprint;
    }

    /**
     * Fingerprint of certificate
     *
     * @param fingerprint fingerprint
     */
    public void setFingerprint(String fingerprint) {
        this.fingerprint = fingerprint;
    }

    /**
     * DN of issuer of certificate
     *
     * @return issuer dn
     */
    // @Column
    public String getIssuerDN() {
        return issuerDN;
    }

    /**
     * Use setIssuer instead
     *
     * @param issuerDN issuer dn
     * @see #setIssuer(String)
     */
    public void setIssuerDN(String issuerDN) {
        this.issuerDN = issuerDN;
    }

    /**
     * DN of subject in certificate
     *
     * @return subject dn, never null
     */
    @Transient
    public String getSubjectDnNeverNull() {
        final String subjectDn = getSubjectDN();
        return subjectDn == null ? "" : subjectDn;
    }

    /**
     * Use getSubjectDnNeverNull() for consistent access, since Oracle will treat empty Strings as NULL.
     *
     * @return value as it is stored in the database
     */
    // @Column(length=400)
    public String getSubjectDN() {
        return subjectDN;
    }

    /**
     * Use setSubject instead
     *
     * @param subjectDN subject dn
     * @see #setSubject(String)
     */
    public void setSubjectDN(String subjectDN) {
        this.subjectDN = subjectDN;
    }

    /** @return Subject Alternative Name from the certificate if it was saved at the time of issuance. */
    @Transient
    public String getSubjectAltNameNeverNull() {
        final String subjectAltName = getSubjectAltName();
        return subjectAltName == null ? "" : subjectAltName;
    }

    /**
     * Use getSubjectAltNameNeverNull() for consistent access, since Oracle will treat empty Strings as null.
     *
     * @return value as it is stored in the database
     */
    //@Column(length=2000)
    public String getSubjectAltName() {
        return subjectAltName;
    }
    public void setSubjectAltName(final String subjectAltName) {
        this.subjectAltName = subjectAltName;
    }

    /**
     * Fingerprint of CA certificate
     *
     * @return fingerprint
     */
    // @Column
    public String getCaFingerprint() {
        return cAFingerprint;
    }

    /**
     * Fingerprint of CA certificate
     *
     * @param cAFingerprint fingerprint
     */
    public void setCaFingerprint(String cAFingerprint) {
        this.cAFingerprint = cAFingerprint;
    }

    /**
     * status of certificate, ex CertificateConstants.CERT_ACTIVE
     *
     * @see CertificateConstants#CERT_ACTIVE etc
     *
     * @return status
     */
    // @Column
    public int getStatus() {
        return status;
    }

    /**
     * status of certificate, ex CertificateConstants.CERT_ACTIVE
     *
     * @param status status
     */
    public void setStatus(int status) {
        this.status = status;
    }

    /**
     * What type of user the certificate belongs to, ex CertificateConstants.CERTTYPE_ENDENTITY
     *
     * @return user type
     */
    // @Column
    public int getType() {
        return type;
    }

    /**
     * What type of user the certificate belongs to, ex CertificateConstants.CERTTYPE_ENDENTITY
     *
     * @param type type
     */
    public void setType(int type) {
        this.type = type;
    }

    /**
     * Serialnumber formated as BigInteger.toString()
     *
     * @return serial number
     */
    // @Column
    public String getSerialNumber() {
        return serialNumber;
    }

    /**
     * Serialnumber formated as BigInteger.toString(16).toUpperCase(), or just as it is in DB if not encodable to hex.
     *
     * @return serial number in hex format
     */
    @Transient
    public String getSerialNumberHex() throws NumberFormatException {
        try {
            return new BigInteger(serialNumber, 10).toString(16).toUpperCase();
        } catch (NumberFormatException e) {
            return serialNumber;
        }
    }

    /**
     * Sets serial number (formated as BigInteger.toString())
     *
     * @param serialNumber serial number formated as BigInteger.toString()
     */
    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    /** @returns the number of milliseconds since 1970-01-01 00:00:00 GMT until the certificate was issued or null if the information is not known. */
    // @Column
    public Long getNotBefore() {
        return notBefore;
    }
    public void setNotBefore(final Long notBefore) {
        this.notBefore = notBefore;
    }

    /** @returns the number of milliseconds since 1970-01-01 00:00:00 GMT until the certificate expires. */
    // @Column
    public long getExpireDate() {
        return expireDate;
    }

    /**
     * Date formated as milliseconds since 1970 (== Date.getTime())
     *
     * @param expireDate expire date
     */
    public void setExpireDate(long expireDate) {
        this.expireDate = expireDate;
    }

    /**
     * Set to date when revocation occured if status == CERT_REVOKED. Format == Date.getTime()
     *
     * @return revocation date
     */
    // @Column
    public long getRevocationDate() {
        return revocationDate;
    }

    /**
     * Set to date when revocation occurred if status == CERT_REVOKED. Format == Date.getTime()
     *
     * @param revocationDate revocation date
     */
    public void setRevocationDate(long revocationDate) {
        this.revocationDate = revocationDate;
    }

    /**
     * Set to revocation reason if status == CERT_REVOKED
     *
     * @return revocation reason
     */
    // @Column
    public int getRevocationReason() {
        return revocationReason;
    }

    /**
     * Set to revocation reason if status == CERT_REVOKED
     *
     * @param revocationReason revocation reason
     */
    public void setRevocationReason(int revocationReason) {
        this.revocationReason = revocationReason;
    }

    /**
     * The certificate itself
     *
     * @return base64 encoded certificate
     */
    // @Column @Lob
    public String getBase64Cert() {
        return this.getZzzBase64Cert();
    }

    /**
     * The certificate itself
     *
     * @param base64Cert base64 encoded certificate
     */
    public void setBase64Cert(String base64Cert) {
        this.setZzzBase64Cert(base64Cert);
    }

    /**
     * Horrible work-around due to the fact that Oracle needs to have (LONG and) CLOB values last in order to avoid ORA-24816.
     *
     * Since Hibernate sorts columns by the property names, naming this Z-something will apparently ensure that this column is used last.
     * @deprecated Use {@link #getBase64Cert()} instead
     */
    @Deprecated
    public String getZzzBase64Cert() {
        return base64Cert;
    }
    /** @deprecated Use {@link #setBase64Cert(String)} instead */
    @Deprecated
    public void setZzzBase64Cert(final String zzzBase64Cert) {
        this.base64Cert = zzzBase64Cert;
    }

    /**
     * username in database
     *
     * @return username
     */
    // @Column
    public String getUsername() {
        return username;
    }

    /**
     * username in database
     *
     * @param username username
     */
    public void setUsername(String username) {
        this.username = StringTools.stripUsername(username);
    }

    /**
     * tag in database. This field was added for the 3.9.0 release, but is not used yet.
     *
     * @return tag
     */
    // @Column
    public String getTag() {
        return tag;
    }

    /**
     * tag in database. This field was added for the 3.9.0 release, but is not used yet.
     *
     * @param username tag
     */
    public void setTag(String tag) {
        this.tag = tag;
    }

    /**
     * Certificate Profile Id that was used to issue this certificate.
     *
     * @return certificateProfileId
     */
    // @Column
    public Integer getCertificateProfileId() {
        return certificateProfileId;
    }

    /**
     * Certificate Profile Id that was used to issue this certificate.
     *
     * @param certificateProfileId certificateProfileId
     */
    public void setCertificateProfileId(Integer certificateProfileId) {
        this.certificateProfileId = certificateProfileId;
    }

    /**
     * The time this row was last updated.
     *
     * @return updateTime
     */
    // @Column
    public Long getUpdateTime() {
        return updateTime;
    }

    /**
     * The time this row was last updated.
     */
    // Hibernate + Oracle ignores nullable=false so we can expect null-objects as input after upgrade. TODO: Verify if still true!
    public void setUpdateTime(Long updateTime) {
        this.updateTime = (updateTime == null ? this.updateTime : updateTime);
    }

    /**
     * The ID of the public key of the certificate
     */
    // @Column
    public String getSubjectKeyId() {
        return subjectKeyId;
    }

    /**
     * The ID of the public key of the certificate
     */
    public void setSubjectKeyId(String subjectKeyId) {
        this.subjectKeyId = subjectKeyId;
    }

    // @Version @Column
    public int getRowVersion() {
        return rowVersion;
    }

    public void setRowVersion(int rowVersion) {
        this.rowVersion = rowVersion;
    }

    // @Column @Lob
    @Override
    public String getRowProtection() {
        return this.getZzzRowProtection();
    }

    @Override
    public void setRowProtection(String rowProtection) {
        this.setZzzRowProtection(rowProtection);
    }

    /**
     * Horrible work-around due to the fact that Oracle needs to have (LONG and) CLOB values last in order to avoid ORA-24816.
     *
     * Since Hibernate sorts columns by the property names, naming this Z-something will apparently ensure that this column is used last.
     * @deprecated Use {@link #getRowProtection()} instead
     */
    @Deprecated
    public String getZzzRowProtection() {
        return rowProtection;
    }
    /** @deprecated Use {@link #setRowProtection(String)} instead */
    @Deprecated
    public void setZzzRowProtection(final String zzzRowProtection) {
        this.rowProtection = zzzRowProtection;
    }

    //
    // Public business methods used to help us manage certificates
    //

    /**
     * Return the certificate . From this table if contained here. From {link Base64CertData} if contained there.
     * @param entityManager To be used if the cert is in the {@link Base64CertData} table.
     * @return The certificate
     */
    @Transient
    public String getBase64Cert(EntityManager entityManager) {
        if ( this.base64Cert!=null && this.base64Cert.length()>0 ) {
            return this.base64Cert; // the cert was in this table.
        }
        // try the other table.
        final Base64CertData res = Base64CertData.findByFingerprint(entityManager, this.fingerprint);
        if ( res==null ) {
            log.info("No certificate found with fingerprint "+this.fingerprint+" for '"+this.subjectDN+"' issued by '"+this.issuerDN+"'.");
            return null;
        }
        // it was in the other table.
        return res.getBase64Cert();
    }
    /**
     * Returns the certificate as an object.
     *
     * @return The certificate or null if it doesn't exist or is blank/null in the database
     */
    @Transient
    public Certificate getCertificate(EntityManager entityManager) {
        try {
            String certEncoded = getBase64Cert(entityManager);
            if (certEncoded == null || certEncoded.isEmpty()) {
                if (log.isDebugEnabled()) {
                    log.debug("Certificate data was null or empty. Fingerprint of certificate: " + this.fingerprint);
                }
                return null;
            }
            return CertTools.getCertfromByteArray(Base64.decode(certEncoded.getBytes()), Certificate.class);
        } catch (CertificateException ce) {
            log.error("Can't decode certificate.", ce);
            return null;
        }
    }
    /**
     * Returns the certificate as an object.
     *
     * @return The certificate or null if it doesn't exist or is blank/null in the database
     */
    @Transient
    public Certificate getCertificate(final Base64CertData base64CertData) {
        try {
            String certEncoded = null;
            if (base64Cert!=null && base64Cert.length()>0 ) {
                certEncoded = base64Cert;
            } else if (base64CertData!=null) {
                certEncoded = base64CertData.getBase64Cert();
            }
            if (certEncoded==null || certEncoded.isEmpty()) {
                if (log.isDebugEnabled()) {
                    log.debug("Certificate data was null or empty. Fingerprint of certificate: " + fingerprint);
                }
                return null;
            }
            return CertTools.getCertfromByteArray(Base64.decode(certEncoded.getBytes()), Certificate.class);
        } catch (CertificateException ce) {
            log.error("Can't decode certificate.", ce);
            return null;
        }
    }

    /**
     * DN of issuer of certificate
     *
     * @param dn issuer dn
     */
    public void setIssuer(String dn) {
        setIssuerDN(CertTools.stringToBCDNString(dn));
    }

    /**
     * DN of subject in certificate
     *
     * @param dn subject dn
     */
    public void setSubject(String dn) {
        setSubjectDN(CertTools.stringToBCDNString(dn));
    }

    /**
     * expire date of certificate
     *
     * @param expireDate expire date
     */
    public void setExpireDate(Date expireDate) {
        if (expireDate == null) {
            setExpireDate(-1L);
        } else {
            setExpireDate(expireDate.getTime());
        }
    }

    /**
     * date the certificate was revoked
     *
     * @param revocationDate revocation date
     */
    public void setRevocationDate(Date revocationDate) {
        if (revocationDate == null) {
            setRevocationDate(-1L);
        } else {
            setRevocationDate(revocationDate.getTime());
        }
    }

    public void setEndEntityProfileId(final Integer endEntityProfileId) {
        this.endEntityProfileId = endEntityProfileId;
    }
    // @Column
    /** @return the end entity profile this certificate was issued under or null if the information is not available. */
    public Integer getEndEntityProfileId() {
        return endEntityProfileId;
    }
    /** @return the end entity profile this certificate was issued under or 0 if the information is not available. */
    @Transient
    public int getEndEntityProfileIdOrZero() {
        return endEntityProfileId==null ? EndEntityConstants.NO_END_ENTITY_PROFILE : endEntityProfileId;
    }

    // Comparators

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof CertificateData)) {
            return false;
        }
        return equals((CertificateData) obj, true);
    }

    public boolean equals(CertificateData certificateData, boolean mode, boolean strictStatus) {
        if (mode) {
            return equalsNonSensitive(certificateData, strictStatus);
        }
        return equals(certificateData, strictStatus);
    }

    private boolean equals(CertificateData certificateData, boolean strictStatus) {
        if (!equalsNonSensitive(certificateData, strictStatus)) {
            return false;
        }
        if ( this.base64Cert==null && certificateData.base64Cert==null ) {
            return true; // test before shows that fingerprint is equal and then both objects must refer to same row in Base64CertData
        }
        if ( this.base64Cert==null || certificateData.base64Cert==null ) {
            return false; // one is null and the other not null
        }
        if (!this.base64Cert.equals(certificateData.base64Cert)) {
            return false;
        }
        return true;
    }

    private boolean equalsNonSensitive(CertificateData certificateData, boolean strictStatus) {
        if (!issuerDN.equals(certificateData.issuerDN)) {
            return false;
        }
        if (!subjectDN.equals(certificateData.subjectDN)) {
            return false;
        }
        if (!fingerprint.equals(certificateData.fingerprint)) {
            return false;
        }
        if (!cAFingerprint.equals(certificateData.cAFingerprint)) {
            return false;
        }
        if (!equalsStatus(certificateData, strictStatus)) {
            return false;
        }
        if (type != certificateData.type) {
            return false;
        }
        if (!serialNumber.equals(certificateData.serialNumber)) {
            return false;
        }
        if (notBefore==null) {
            if (certificateData.notBefore!=null) {
                return false;
            }
        } else {
            if (notBefore.equals(certificateData.notBefore)) {
                return false;
            }
        }
        if (expireDate != certificateData.expireDate) {
            return false;
        }
        if (revocationDate != certificateData.revocationDate) {
            return false;
        }
        if (revocationReason != certificateData.revocationReason) {
            return false;
        }
        if (!username.equals(certificateData.username)) {
            return false;
        }
        if (tag == null && certificateData.tag != null) {
            return false;
        }
        if (tag != null && !tag.equals(certificateData.tag)) {
            return false;
        }
        if (certificateProfileId == null && certificateData.certificateProfileId != null) {
            return false;
        }
        if (certificateProfileId != null && !certificateProfileId.equals(certificateData.certificateProfileId)) {
            return false;
        }
        if (endEntityProfileId==null) {
            if (certificateData.endEntityProfileId!=null) {
                return false;
            }
        } else {
            if (endEntityProfileId.equals(certificateData.endEntityProfileId)) {
                return false;
            }
        }
        if (updateTime != certificateData.updateTime) {
            return false;
        }
        if (subjectAltName==null) {
            if (certificateData.subjectAltName!=null) {
                return false;
            }
        } else {
            if (!subjectAltName.equals(certificateData.subjectAltName)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Compare the status field of this and another CertificateData object.
     *
     * @param strict will treat NOTIFIED as ACTIVE and ARCHIVED as REVOKED if set to false
     */
    public boolean equalsStatus(CertificateData certificateData, boolean strict) {
        if (strict) {
            return status == certificateData.status;
        }
        if (status == certificateData.status) {
            return true;
        }
        if ((status == CertificateConstants.CERT_ACTIVE || status == CertificateConstants.CERT_NOTIFIEDABOUTEXPIRATION)
                && (certificateData.status == CertificateConstants.CERT_ACTIVE || certificateData.status == CertificateConstants.CERT_NOTIFIEDABOUTEXPIRATION)) {
            return true;
        }
        if ((status == CertificateConstants.CERT_REVOKED || status == CertificateConstants.CERT_ARCHIVED)
                && (certificateData.status == CertificateConstants.CERT_REVOKED || certificateData.status == CertificateConstants.CERT_ARCHIVED)) {
            return true;
        }
        return false;
    }

    public void updateWith(CertificateData certificateData, boolean inclusionMode) {
        issuerDN = certificateData.issuerDN;
        subjectDN = certificateData.subjectDN;
        fingerprint = certificateData.fingerprint;
        cAFingerprint = certificateData.cAFingerprint;
        status = certificateData.status;
        type = certificateData.type;
        serialNumber = certificateData.serialNumber;
        expireDate = certificateData.expireDate;
        revocationDate = certificateData.revocationDate;
        revocationReason = certificateData.revocationReason;
        setUsername(certificateData.username);
        tag = certificateData.tag;
        certificateProfileId = certificateData.certificateProfileId;
        updateTime = certificateData.updateTime;
        base64Cert = inclusionMode ? null : certificateData.base64Cert;
    }

    //
    // Search functions.
    //

    /** @return the found entity instance or null if the entity does not exist */
    public static CertificateData findByFingerprint(EntityManager entityManager, String fingerprint) {
        return entityManager.find(CertificateData.class, fingerprint);
    }

    /** @return return the query results as a Set. */
    @SuppressWarnings("unchecked")
    public static Set<String> findUsernamesBySubjectDNAndIssuerDN(EntityManager entityManager, String subjectDN, String issuerDN) {
            final Query query = entityManager.createQuery("SELECT a.username FROM CertificateData a WHERE a.subjectDN=:subjectDN AND a.issuerDN=:issuerDN");
            query.setParameter("subjectDN", subjectDN);
            query.setParameter("issuerDN", issuerDN);
            return new HashSet<String>(query.getResultList());
    }

    /** @return return the query results as a List. */
    public static List<CertificateData> findBySubjectDN(EntityManager entityManager, String subjectDN) {
        final TypedQuery<CertificateData> query = entityManager.createQuery("SELECT a FROM CertificateData a WHERE a.subjectDN=:subjectDN", CertificateData.class);
        query.setParameter("subjectDN", subjectDN);
        return query.getResultList();
    }

    /** @return return the query results as a List. */
    public static List<CertificateData> findBySerialNumber(EntityManager entityManager, String serialNumber) {
        final TypedQuery<CertificateData> query = entityManager.createQuery("SELECT a FROM CertificateData a WHERE a.serialNumber=:serialNumber", CertificateData.class);
        query.setParameter("serialNumber", serialNumber);
        return query.getResultList();
    }

    /** @return return the query results as a List. */
    public static List<CertificateData> findByIssuerDNSerialNumber(EntityManager entityManager, String issuerDN, String serialNumber) {
        final TypedQuery<CertificateData> query = entityManager.createQuery("SELECT a FROM CertificateData a WHERE a.issuerDN=:issuerDN AND a.serialNumber=:serialNumber", CertificateData.class);
        query.setParameter("issuerDN", issuerDN);
        query.setParameter("serialNumber", serialNumber);
        return query.getResultList();
    }

    /** @return return the query results as a List. */
    public static CertificateInfo findFirstCertificateInfo(EntityManager entityManager, String issuerDN, String serialNumber) {
        CertificateInfo ret = null;
        final Query query = entityManager
                .createNativeQuery(
                        "SELECT a.fingerprint, a.subjectDN, a.cAFingerprint, a.status, a.type, a.serialNumber, a.notBefore, a.expireDate, a.revocationDate, a.revocationReason, "
                                + "a.username, a.tag, a.certificateProfileId, a.endEntityProfileId, a.updateTime, a.subjectKeyId, a.subjectAltName FROM CertificateData a WHERE a.issuerDN=:issuerDN AND a.serialNumber=:serialNumber",
                        "CertificateInfoSubset2");
        query.setParameter("issuerDN", issuerDN);
        query.setParameter("serialNumber", serialNumber);
        query.setMaxResults(1);
        @SuppressWarnings("unchecked")
        final List<Object[]> resultList = query.getResultList();
        if (!resultList.isEmpty()) {
            final Object[] fields = resultList.get(0);
            // The order of the results are defined by the SqlResultSetMapping annotation
            final String fingerprint = (String) fields[0];
            final String subjectDN = (String) fields[1];
            final String cafp = (String) fields[2];
            final int status = ValueExtractor.extractIntValue(fields[3]);
            final int type = ValueExtractor.extractIntValue(fields[4]);
            final Long notBefore;
            if (fields[5] == null) {
                notBefore = null;
            } else {
                notBefore = ValueExtractor.extractLongValue(fields[5]);
            }
            final long expireDate = ValueExtractor.extractLongValue(fields[6]);
            final long revocationDate = ValueExtractor.extractLongValue(fields[7]);
            final int revocationReason = ValueExtractor.extractIntValue(fields[8]);
            final String username = (String) fields[9];
            final String tag = (String) fields[10];
            final int certificateProfileId = ValueExtractor.extractIntValue(fields[11]);
            final Integer endEntityProfileId;
            if (fields[12] == null) {
                endEntityProfileId = null;
            } else {
                endEntityProfileId = ValueExtractor.extractIntValue(fields[12]);
            }
            final long updateTime;
            if (fields[13] == null) {
                updateTime = 0; // Might be null in an upgraded installation
            } else {
                updateTime = ValueExtractor.extractLongValue(fields[13]);
            }
            final String subjectKeyId = (String)fields[14];
            final String subjectAltName = (String)fields[15];
            ret = new CertificateInfo(fingerprint, cafp, serialNumber, issuerDN, subjectDN, status, type, notBefore, expireDate, revocationDate,
                    revocationReason, username, tag, certificateProfileId, endEntityProfileId, updateTime, subjectKeyId, subjectAltName);
        }
        return ret;
    }

    /** @return the last found username or null if none was found */
    public static String findLastUsernameByIssuerDNSerialNumber(EntityManager entityManager, String issuerDN, String serialNumber) {
        final Query query = entityManager
                .createQuery("SELECT a.username FROM CertificateData a WHERE a.issuerDN=:issuerDN AND a.serialNumber=:serialNumber");
        query.setParameter("issuerDN", issuerDN);
        query.setParameter("serialNumber", serialNumber);
        // Since no ordering is done this seems a bit strange, but this is what it was like in previous versions..
        return QueryResultWrapper.getLastResult(query);
    }

    /** @return return the query results as a List. */
    @SuppressWarnings("unchecked")
    public static List<CertificateData> findByUsernameOrdered(EntityManager entityManager, String username) {
        final Query query = entityManager
                .createQuery("SELECT a FROM CertificateData a WHERE a.username=:username ORDER BY a.expireDate DESC, a.serialNumber DESC");
        query.setParameter("username", username);
        return query.getResultList();
    }

    /** @return return the query results as a List. */
    @SuppressWarnings("unchecked")
    public static List<CertificateData> findByUsernameAndStatus(EntityManager entityManager, String username, int status) {
        final Query query = entityManager
                .createQuery("SELECT a FROM CertificateData a WHERE a.username=:username AND a.status=:status ORDER BY a.expireDate DESC, a.serialNumber DESC");
        query.setParameter("username", username);
        query.setParameter("status", status);
        return query.getResultList();
    }

    /** @return return the query results as a List. */
    @SuppressWarnings("unchecked")
    public static List<CertificateData> findByUsernameAndStatusAfterExpireDate(EntityManager entityManager, String username, int status, long afterExpireDate) {
        final Query query = entityManager
                .createQuery("SELECT a FROM CertificateData a WHERE a.username=:username AND a.status=:status AND a.expireDate>=:afterExpireDate ORDER BY a.expireDate DESC, a.serialNumber DESC");
        query.setParameter("username", username);
        query.setParameter("status", status);
        query.setParameter("afterExpireDate", afterExpireDate);
        return query.getResultList();
    }

    /** @return return the query results as a List. */
    // TODO: When only JPA is used, check if we can refactor this method to SELECT DISTINCT a.username FROM ...
    @SuppressWarnings("unchecked")
    public static Set<String> findUsernamesByIssuerDNAndSubjectKeyId(EntityManager entityManager, String issuerDN, String subjectKeyId) {
        final Query query = entityManager.createQuery("SELECT a.username FROM CertificateData a WHERE a.issuerDN=:issuerDN AND a.subjectKeyId=:subjectKeyId");
        query.setParameter("issuerDN", issuerDN);
        query.setParameter("subjectKeyId", subjectKeyId);
        return new HashSet<String>(query.getResultList());
    }

    public static final String findUsernameByIssuerDnAndSerialNumber(EntityManager entityManager, String issuerDn, String serialNumber) {
        final Query query = entityManager.createQuery("SELECT a.username FROM CertificateData a WHERE a.issuerDN=:issuerDN AND a.serialNumber=:serialNumber");
        query.setParameter("issuerDN", issuerDn);
        query.setParameter("serialNumber", serialNumber);
        return (String) query.getSingleResult();
    }

    /** @return return the query results as a List. */
    @SuppressWarnings("unchecked")
    public static Set<String> findUsernamesBySubjectKeyIdOrDnAndIssuer(EntityManager entityManager, String issuerDN, String subjectKeyId, String subjectDN) {
        final Query query = entityManager.createQuery("SELECT a.username FROM CertificateData a WHERE (a.subjectKeyId=:subjectKeyId OR a.subjectDN=:subjectDN) AND a.issuerDN=:issuerDN");
        query.setParameter("issuerDN", issuerDN);
        query.setParameter("subjectKeyId", subjectKeyId);
        query.setParameter("subjectDN", subjectDN);
        return new HashSet<String>(query.getResultList());
    }

    /** @return return the query results as a List<String>. */
    @SuppressWarnings("unchecked")
    public static List<String> findFingerprintsByIssuerDN(EntityManager entityManager, String issuerDN) {
        final Query query = entityManager.createQuery("SELECT a.fingerprint FROM CertificateData a WHERE a.issuerDN=:issuerDN");
        query.setParameter("issuerDN", issuerDN);
        return query.getResultList();
    }

    /**
     * Get next batchSize row ordered by fingerprint
     *
     * @param entityManager
     * @param certificateProfileId
     * @param currentFingerprint
     * @param batchSize
     * @return
     */
    @SuppressWarnings("unchecked")
    public static List<CertificateData> getNextBatch(EntityManager entityManager, int certificateProfileId, String currentFingerprint, int batchSize) {
        final Query query = entityManager
                .createQuery("SELECT a FROM CertificateData a WHERE a.fingerprint>:currentFingerprint AND a.certificateProfileId=:certificateProfileId ORDER BY a.fingerprint ASC");
        query.setParameter("certificateProfileId", certificateProfileId);
        query.setParameter("currentFingerprint", currentFingerprint);
        query.setMaxResults(batchSize);
        return query.getResultList();
    }

    /**
     * Get next batchSize row ordered by fingerprint
     *
     * @param entityManager
     * @param certificateProfileId
     * @param currentFingerprint
     * @param batchSize
     * @return
     */
    @SuppressWarnings("unchecked")
    public static List<CertificateData> getNextBatch(EntityManager entityManager, String currentFingerprint, int batchSize) {
        final Query query = entityManager
                .createQuery("SELECT a FROM CertificateData a WHERE a.fingerprint>:currentFingerprint ORDER BY a.fingerprint ASC");
        query.setParameter("currentFingerprint", currentFingerprint);
        query.setMaxResults(batchSize);
        return query.getResultList();
    }

    /** @return the number of entries with the given parameter */
    public static long getCount(EntityManager entityManager, int certificateProfileId) {
        final Query countQuery = entityManager
                .createQuery("SELECT COUNT(a) FROM CertificateData a WHERE a.certificateProfileId=:certificateProfileId");
        countQuery.setParameter("certificateProfileId", certificateProfileId);
        return ((Long) countQuery.getSingleResult()).longValue(); // Always returns a result
    }

    /** @return the number of entries with the given parameter */
    public static long getCount(EntityManager entityManager) {
        final Query countQuery = entityManager.createQuery("SELECT COUNT(a) FROM CertificateData a");
        return ((Long) countQuery.getSingleResult()).longValue(); // Always returns a result
    }

    /** @return return the query results as a List. */
    @SuppressWarnings("unchecked")
    public static List<Integer> getUsedCertificateProfileIds(EntityManager entityManager) {
        final Query query = entityManager.createQuery("SELECT DISTINCT a.certificateProfileId FROM CertificateData a ORDER BY a.certificateProfileId");
        return query.getResultList();
    }

    /** @return return the query results as a Collection<RevokedCertInfo>. */
    public static Collection<RevokedCertInfo> getRevokedCertInfos(EntityManager entityManager, String issuerDN, long lastbasecrldate) {
        Query query;
        if (lastbasecrldate > 0) {
            query = entityManager.createNativeQuery(
                    "SELECT a.fingerprint as fingerprint, a.serialNumber as serialNumber, a.expireDate as expireDate, a.revocationDate as revocationDate, a.revocationReason as revocationReason FROM CertificateData a WHERE "
                            + "a.issuerDN=:issuerDN AND a.revocationDate>:revocationDate AND (a.status=:status1 OR a.status=:status2 OR a.status=:status3)",
                    "RevokedCertInfoSubset");
            query.setParameter("issuerDN", issuerDN);
            query.setParameter("revocationDate", lastbasecrldate);
            query.setParameter("status1", CertificateConstants.CERT_REVOKED);
            query.setParameter("status2", CertificateConstants.CERT_ACTIVE); // in case the certificate has been changed from on hold, we need to include it as "removeFromCRL" in the Delta CRL
            query.setParameter("status3", CertificateConstants.CERT_NOTIFIEDABOUTEXPIRATION); // could happen if a cert is re-activated just before expiration
        } else {
            query = entityManager.createNativeQuery(
                    "SELECT a.fingerprint as fingerprint, a.serialNumber as serialNumber, a.expireDate as expireDate, a.revocationDate as revocationDate, a.revocationReason as revocationReason FROM CertificateData a WHERE "
                            + "a.issuerDN=:issuerDN AND a.status=:status",
                    "RevokedCertInfoSubset");
            query.setParameter("issuerDN", issuerDN);
            query.setParameter("status", CertificateConstants.CERT_REVOKED);
        }
        final int maxResults = CesecoreConfiguration.getDatabaseRevokedCertInfoFetchSize();
        query.setMaxResults(maxResults);
        int firstResult = 0;
        final CompressedCollection<RevokedCertInfo> revokedCertInfos = new CompressedCollection<RevokedCertInfo>();
        while (true) {
            query.setFirstResult(firstResult);
            @SuppressWarnings("unchecked")
            final List<Object[]> incompleteCertificateDatas = query.getResultList();
            if (incompleteCertificateDatas.size()==0) {
                break;
            }
            if (log.isDebugEnabled()) {
                log.debug("Read batch of " + incompleteCertificateDatas.size() + " RevokedCertInfo.");
            }
            for (final Object[] current : incompleteCertificateDatas) {
                // The order of the results are defined by the SqlResultSetMapping annotation
                final byte[] fingerprint = ((String)current[0]).getBytes();
                final byte[] serialNumber = new BigInteger((String)current[1]).toByteArray();
                final long expireDate = ValueExtractor.extractLongValue(current[2]);
                final long revocationDate = ValueExtractor.extractLongValue(current[3]);
                int revocationReason = ValueExtractor.extractIntValue(current[4]);
                if (revocationReason == -1) {
                    revocationReason = RevokedCertInfo.REVOCATION_REASON_REMOVEFROMCRL;
                }
                revokedCertInfos.add(new RevokedCertInfo(fingerprint, serialNumber, revocationDate, revocationReason, expireDate));
            }
            firstResult += maxResults;
        }
        revokedCertInfos.closeForWrite();
        return revokedCertInfos;
    }

    /** @return return the query results as a List. */
    @SuppressWarnings("unchecked")
    public static List<CertificateData> findByExpireDateWithLimit(EntityManager entityManager, long expireDate, int maxNumberOfResults) {
        final Query query = entityManager
                .createQuery("SELECT a FROM CertificateData a WHERE a.expireDate<:expireDate AND (a.status=:status1 OR a.status=:status2)");
        query.setParameter("expireDate", expireDate);
        query.setParameter("status1", CertificateConstants.CERT_ACTIVE);
        query.setParameter("status2", CertificateConstants.CERT_NOTIFIEDABOUTEXPIRATION);
        query.setMaxResults(maxNumberOfResults);
        return query.getResultList();
    }

    /** @return return the query results as a List. */
    @SuppressWarnings("unchecked")
    public static List<CertificateData> findByExpireDateAndIssuerWithLimit(EntityManager entityManager, long expireDate, String issuerDN, int maxNumberOfResults) {
        final Query query = entityManager
                .createQuery("SELECT a FROM CertificateData a WHERE a.expireDate<:expireDate AND (a.status=:status1 OR a.status=:status2) AND issuerDN=:issuerDN");
        query.setParameter("expireDate", expireDate);
        query.setParameter("status1", CertificateConstants.CERT_ACTIVE);
        query.setParameter("status2", CertificateConstants.CERT_NOTIFIEDABOUTEXPIRATION);
        query.setParameter("issuerDN", issuerDN);
        query.setMaxResults(maxNumberOfResults);
        return query.getResultList();
    }

    /** @return return the query results as a List. */
    @SuppressWarnings("unchecked")
    public static List<CertificateData> findByExpireDateAndTypeWithLimit(EntityManager entityManager, long expireDate, int certificateType, int maxNumberOfResults) {
        final Query query = entityManager
                .createQuery("SELECT a FROM CertificateData a WHERE a.expireDate<:expireDate AND (a.status=:status1 OR a.status=:status2) AND a.type=:ctype");
        query.setParameter("expireDate", expireDate);
        query.setParameter("status1", CertificateConstants.CERT_ACTIVE);
        query.setParameter("status2", CertificateConstants.CERT_NOTIFIEDABOUTEXPIRATION);
        query.setParameter("ctype", certificateType);
        query.setMaxResults(maxNumberOfResults);
        return query.getResultList();
    }

    @SuppressWarnings("unchecked")
    public static List<String> findUsernamesByExpireTimeWithLimit(EntityManager entityManager, long minExpireTime, long maxExpireTime, int maxResults) {
        // TODO: Would it be more effective to drop the NOT NULL of this query and remove it from the result?
        final Query query = entityManager
                .createQuery("SELECT DISTINCT a.username FROM CertificateData a WHERE a.expireDate>=:minExpireTime AND a.expireDate<:maxExpireTime AND (a.status=:status1 OR a.status=:status2) AND a.username IS NOT NULL");
        query.setParameter("minExpireTime", minExpireTime);
        query.setParameter("maxExpireTime", maxExpireTime);
        query.setParameter("status1", CertificateConstants.CERT_ACTIVE);
        query.setParameter("status2", CertificateConstants.CERT_NOTIFIEDABOUTEXPIRATION);
        query.setMaxResults(maxResults);
        return query.getResultList();
    }

    /**
     * Get a list of {@link Certificate} from a list of list of {@link CertificateData}.
     * @param cdl
     * @param entityManager
     * @return The resulting list.
     */
    public static List<Certificate> getCertificateList(List<CertificateData> cdl, EntityManager entityManager) {
        final List<Certificate> cl = new LinkedList<Certificate>();
        for ( CertificateData cd : cdl) {
            final Certificate cert = cd.getCertificate(entityManager);
            if ( cert==null ) {
                continue;
            }
            cl.add(cert);
        }
        return cl;
    }
    @SuppressWarnings("unchecked")
    public static List<Certificate> findCertificatesByIssuerDnAndSerialNumbers(EntityManager entityManager, String issuerDN,
            Collection<BigInteger> serialNumbers) {
        final StringBuilder sb = new StringBuilder();
        for(final BigInteger serno : serialNumbers) {
            sb.append(", '");
            sb.append(serno.toString());
            sb.append("'");
        }
        // to save the repeating if-statement in the above closure not to add ', ' as the first characters in the StringBuilder we remove the two chars
        // here :)
        sb.delete(0, ", ".length());
        // Derby: Columns of type 'LONG VARCHAR' may not be used in CREATE INDEX, ORDER BY, GROUP BY, UNION, INTERSECT, EXCEPT or DISTINCT statements
        // because comparisons are not supported for that type.
        // Since two certificates in the database should never be the same, "SELECT DISTINCT ..." was changed to "SELECT ..." here.
        final Query query = entityManager.createQuery("SELECT a FROM CertificateData a WHERE a.issuerDN=:issuerDN AND a.serialNumber IN ("
                + sb.toString() + ")");
        query.setParameter("issuerDN", issuerDN);
        return getCertificateList(query.getResultList(), entityManager);
    }

    /** @return the CertificateInfo representation (all fields except the actual cert) or null if no such fingerprint exists. */
    public static CertificateInfo getCertificateInfo(EntityManager entityManager, String fingerprint) {
        CertificateInfo ret = null;
        final Query query = entityManager.createNativeQuery(
                "SELECT a.issuerDN as issuerDN, a.subjectDN as subjectDN, a.cAFingerprint as cAFingerprint, a.status as status, a.type as type, a.serialNumber as serialNumber, "
                        + "a.notBefore as notBefore, a.expireDate as expireDate, a.revocationDate as revocationDate, a.revocationReason as revocationReason, "
                        + "a.username as username, a.tag as tag, a.certificateProfileId as certificateProfileId, a.endEntityProfileId as endEntityProfileId, a.updateTime as updateTime, "
                        + "a.subjectKeyId as subjectKeyId, a.subjectAltName as subjectAltName FROM CertificateData a WHERE a.fingerprint=:fingerprint",
                "CertificateInfoSubset");
        query.setParameter("fingerprint", fingerprint);
        @SuppressWarnings("unchecked")
        final List<Object[]> resultList = query.getResultList();
        if (!resultList.isEmpty()) {
            final Object[] fields = resultList.get(0);
            // The order of the results are defined by the SqlResultSetMapping annotation
            final String issuerDN = (String) fields[0];
            final String subjectDN = (String) fields[1];
            final String cafp = (String) fields[2];
            final int status = ValueExtractor.extractIntValue(fields[3]);
            final int type = ValueExtractor.extractIntValue(fields[4]);
            final String serno = (String) fields[5];
            final Long notBefore;
            if (fields[6] == null) {
                notBefore = null;
            } else {
                notBefore = ValueExtractor.extractLongValue(fields[6]);
            }
            final long expireDate = ValueExtractor.extractLongValue(fields[7]);
            final long revocationDate = ValueExtractor.extractLongValue(fields[8]);
            final int revocationReason = ValueExtractor.extractIntValue(fields[9]);
            final String username = (String) fields[10];
            final String tag = (String) fields[11];
            final int certificateProfileId = ValueExtractor.extractIntValue(fields[12]);
            final Integer endEntityProfileId;
            if (fields[13] == null) {
                endEntityProfileId = null;
            } else {
                endEntityProfileId = ValueExtractor.extractIntValue(fields[13]);
            }
            final long updateTime;
            if (fields[14] == null) {
                updateTime = 0; // Might be null in an upgraded installation
            } else {
                updateTime = ValueExtractor.extractLongValue(fields[14]);
            }
            final String subjectKeyId = (String)fields[15];
            final String subjectAltName = (String)fields[16];
            ret = new CertificateInfo(fingerprint, cafp, serno, issuerDN, subjectDN, status, type, notBefore, expireDate, revocationDate, revocationReason,
                    username, tag, certificateProfileId, endEntityProfileId, updateTime, subjectKeyId, subjectAltName);
        }
        return ret;
    }
    /**
     * @return a List<Certificate> of SecConst.CERT_ACTIVE and CERT_NOTIFIEDABOUTEXPIRATION certs that have one of the specified types. */
    @SuppressWarnings("unchecked")
    public static List<Certificate> findActiveCertificatesByType(EntityManager entityManager, Collection<Integer> certificateTypes) {
        // Derby: Columns of type 'LONG VARCHAR' may not be used in CREATE INDEX, ORDER BY, GROUP BY, UNION, INTERSECT, EXCEPT or DISTINCT statements
        // because comparisons are not supported for that type.
        // Since two certificates in the database should never be the same, "SELECT DISTINCT ..." was changed to "SELECT ..." here.
        final Query query = entityManager
                .createQuery("SELECT a FROM CertificateData a WHERE (a.status=:status1 or a.status=:status2) AND a.type IN (:ctypes)");
        query.setParameter("status1", CertificateConstants.CERT_ACTIVE);
        query.setParameter("status2", CertificateConstants.CERT_NOTIFIEDABOUTEXPIRATION);
        query.setParameter("ctypes", certificateTypes);
        return getCertificateList( query.getResultList(), entityManager );
    }

    /**
     * @return a List<Certificate> of SecConst.CERT_ACTIVE and CERT_NOTIFIEDABOUTEXPIRATION certs that have one of the specified types for the given
     *         issuer.
     */
    @SuppressWarnings("unchecked")
    public static List<Certificate> findActiveCertificatesByTypeAndIssuer(EntityManager entityManager, final Collection<Integer> certificateTypes, String issuerDN) {
        // Derby: Columns of type 'LONG VARCHAR' may not be used in CREATE INDEX, ORDER BY, GROUP BY, UNION, INTERSECT, EXCEPT or DISTINCT statements
        // because comparisons are not supported for that type.
        // Since two certificates in the database should never be the same, "SELECT DISTINCT ..." was changed to "SELECT ..." here.
        final Query query = entityManager
                .createQuery("SELECT a FROM CertificateData a WHERE (a.status=:status1 or a.status=:status2) AND a.type IN (:ctypes) AND a.issuerDN=:issuerDN");
        query.setParameter("ctypes", certificateTypes);
        query.setParameter("status1", CertificateConstants.CERT_ACTIVE);
        query.setParameter("status2", CertificateConstants.CERT_NOTIFIEDABOUTEXPIRATION);
        query.setParameter("issuerDN", issuerDN);
        return getCertificateList(query.getResultList(), entityManager);
    }

    /**
     * Fetch a List of all certificate fingerprints and corresponding username
     *
     * We want to accomplish two things:
     *
     * 1. Notify for expirations within the service window
     * 2. Notify _once_ for expirations that occurred before the service window like flagging certificates that have a shorter
     * life-span than the threshold (pathologic test-case...)
     *
     * The first is checked by:
     *
     * notify = currRunTimestamp + thresHold <= ExpireDate < nextRunTimestamp + thresHold
     *          AND (status = ACTIVE OR status = NOTIFIEDABOUTEXPIRATION)
     *
     * The second can be checked by:
     *
     * notify = currRunTimestamp + thresHold > ExpireDate AND status = ACTIVE
     *
     * @param cas A list of CAs that the sought certificates should be issued from
     * @param certificateProfiles A list if certificateprofiles to sort from. Will be ignored if left empty.
     * @param activeNotifiedExpireDateMin The minimal date for expiration notification
     * @param activeNotifiedExpireDateMax The maxmimal date for expiration notification
     * @param activeExpireDateMin the current rune timestamp + the threshold
     *
     * @return [0] = (String) fingerprint, [1] = (String) username
     */
    @SuppressWarnings("unchecked")
    public static List<Object[]> findExpirationInfo(EntityManager entityManager, Collection<String> cas, Collection<Integer> certificateProfiles,
            long activeNotifiedExpireDateMin, long activeNotifiedExpireDateMax, long activeExpireDateMin) {
        // We don't select the base64 certificate data here, because it may be a LONG data type which we can't simply select, or we don't want to read all the data.
        final Query query = entityManager.createNativeQuery("SELECT DISTINCT fingerprint as fingerprint, username as username"
                + " FROM CertificateData WHERE "
                + "issuerDN IN (:cas) AND "
                // If the list of certificate profiles is empty, ignore it as a parameter
                + (!certificateProfiles.isEmpty() ? "certificateProfileId IN (:certificateProfiles) AND" : "")
                + "(expireDate>:activeNotifiedExpireDateMin) AND " + "(expireDate<:activeNotifiedExpireDateMax) AND (status=:status1"
                + " OR status=:status2) AND (expireDate>=:activeExpireDateMin OR " + "status=:status3)", "FingerprintUsernameSubset");
        query.setParameter("cas", cas);
        if(!certificateProfiles.isEmpty()) {
            query.setParameter("certificateProfiles", certificateProfiles);
        }
        query.setParameter("activeNotifiedExpireDateMin", activeNotifiedExpireDateMin);
        query.setParameter("activeNotifiedExpireDateMax", activeNotifiedExpireDateMax);
        query.setParameter("status1", CertificateConstants.CERT_ACTIVE);
        query.setParameter("status2", CertificateConstants.CERT_NOTIFIEDABOUTEXPIRATION);
        query.setParameter("activeExpireDateMin", activeExpireDateMin);
        query.setParameter("status3", CertificateConstants.CERT_ACTIVE);
        // How to debug log the SQL query:
        // log.debug("findExpirationInfo: "+query.unwrap(org.hibernate.Query.class).getQueryString());
        return query.getResultList();
    }

    //
    // Start Database integrity protection methods
    //

    @Transient
    @Override
    protected String getProtectString(final int version) {
    	final ProtectionStringBuilder build = new ProtectionStringBuilder(3000);
        // What is important to protect here is the data that we define, id, name and certificate profile data
        // rowVersion is automatically updated by JPA, so it's not important, it is only used for optimistic locking
        build.append(getFingerprint()).append(getIssuerDN());
        if (version>=3) {
            // From version 3 for EJBCA 6.7 we always use empty String here to allow future migration between databases when this value is unset
            build.append(getSubjectDnNeverNull());
        } else {
            build.append(getSubjectDN());
        }
        build.append(getCaFingerprint()).append(getStatus()).append(getType())
                .append(getSerialNumber()).append(getExpireDate()).append(getRevocationDate()).append(getRevocationReason()).append(getBase64Cert())
                .append(getUsername()).append(getTag()).append(getCertificateProfileId()).append(getUpdateTime()).append(getSubjectKeyId());
        if (version>=2) {
            // In version 2 for EJBCA 6.6 the following columns where added
            build.append(String.valueOf(getNotBefore()));
            build.append(String.valueOf(getEndEntityProfileId()));
            if (version>=3) {
                // From version 3 for EJBCA 6.7 we always use empty String here to allow future migration between databases when this value is unset
                build.append(getSubjectAltNameNeverNull());
            } else {
                build.append(String.valueOf(getSubjectAltName()));
            }
        }
        if (log.isDebugEnabled()) {
            // Some profiling
            if (build.length() > 3000) {
                log.debug("CertificateData.getProtectString gives size: " + build.length());
            }
        }
        return build.toString();
    }

    @Transient
    @Override
    protected int getProtectVersion() {
        return 3;
    }

    @PrePersist
    @PreUpdate
    @Override
    protected void protectData() {
        super.protectData();
    }

    @PostLoad
    @Override
    protected void verifyData() {
        super.verifyData();
    }

    @Override
    @Transient
    protected String getRowId() {
        return getFingerprint();
    }
    //
    // End Database integrity protection methods
    //
}
