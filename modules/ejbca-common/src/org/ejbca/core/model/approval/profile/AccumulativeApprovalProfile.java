/*************************************************************************
 *                                                                       *
 *  EJBCA Community: The OpenSource Certificate Authority                *
 *                                                                       *
 *  This software is free software; you can redistribute it and/or       *
 *  modify it under the terms of the GNU Lesser General Public           *
 *  License as published by the Free Software Foundation; either         *
 *  version 2.1 of the License, or any later version.                    *
 *                                                                       *
 *  See terms of license at gnu.org.                                     *
 *                                                                       *
 *************************************************************************/
package org.ejbca.core.model.approval.profile;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.cesecore.authentication.AuthenticationFailedException;
import org.cesecore.authentication.tokens.AuthenticationToken;
import org.cesecore.internal.InternalResources;
import org.cesecore.util.ui.DynamicUiProperty;
import org.ejbca.core.model.approval.Approval;
import org.ejbca.core.model.approval.ApprovalException;
import org.ejbca.core.model.profiles.Profile;

/**
 * This approval archetype represents the legacy method of approvals, i.e where a fixed number of administrators need to approve a request for it to 
 * pass.
 * 
 * @version $Id$
 */
public class AccumulativeApprovalProfile extends ApprovalProfileBase {

    public static final int FIXED_STEP_ID = 0; //Contains only a single sequence
    
    private static final long serialVersionUID = 6432620040542676563L;
    
    private static final InternalResources intres = InternalResources.getInstance();

    /**
     * Key for the data value marking the number of approvals required. 
     */
    private static final String PROPERTY_NUMBER_OF_REQUIRED_APPROVALS = "number_of_required_approvals";

    /**
     * Note: do not change, may cause problems in deployed installations.
     */
    private static final String TYPE_IDENTIFIER = "ACCUMULATIVE_APPROVAL";
        
    {
        //Workaround, since this profile normally doesn't allow adding sequences. 
        if (getSteps().isEmpty()) {
            getSteps().put(FIXED_STEP_ID, new ApprovalStep(FIXED_STEP_ID));
            addPartition(FIXED_STEP_ID);
            setFirstStep(FIXED_STEP_ID);
        }
    }

    public AccumulativeApprovalProfile() {
        //Public constructor needed deserialization 
        super();
    }

    public AccumulativeApprovalProfile(final String name) {
        super(name);
    }

    @Override
    public String getApprovalProfileIdentifier() {
        return TYPE_IDENTIFIER;
    }

    @Override
    public String getApprovalProfileLabel() {
        return intres.getLocalizedMessage("approval.profile.implementation.accumulative.approval.name");
    }

    public void setNumberOfApprovalsRequired(int approvalsRequired) {
        int partitionIdentifier = getSteps().get(FIXED_STEP_ID).getPartitions().values().iterator().next().getPartitionIdentifier();
        DynamicUiProperty<? extends Serializable> approvalsRequiredProperty = getSteps().get(FIXED_STEP_ID).getPartition(partitionIdentifier)
                .getProperty(PROPERTY_NUMBER_OF_REQUIRED_APPROVALS);
        approvalsRequiredProperty.setValueGeneric(Integer.valueOf(approvalsRequired));
        addPropertyToPartition(FIXED_STEP_ID, partitionIdentifier, approvalsRequiredProperty);
        saveTransientObjects();
    }

    public int getNumberOfApprovalsRequired() {
        return (Integer) getSteps().get(FIXED_STEP_ID).getPartitions().values().iterator().next().getProperty(PROPERTY_NUMBER_OF_REQUIRED_APPROVALS)
                .getValue();
    }

    @Override
    public boolean isApprovalRequired() {
        return ((Integer) getSteps().get(FIXED_STEP_ID).getPartitions().values().iterator().next().getProperty(PROPERTY_NUMBER_OF_REQUIRED_APPROVALS).getValue()) > 0;
    }

    @Override
    public boolean canApprovalExecute(final Collection<Approval> approvalsPerformed) throws ApprovalException {
        //Verify that at least one of the approvals performed covers the single sequence in this implementation (Though it would be odd if they didn't)
        boolean sequenceAndPartitionFound = false;
        for(Approval approval : approvalsPerformed) {
            if(approval.getStepId() == FIXED_STEP_ID) {
                sequenceAndPartitionFound = true;
                break;
            }
        }
        if(!sequenceAndPartitionFound) {
            return false;
        } else {
            int numberofapprovalsleft = getRemainingApprovals(approvalsPerformed);
            if (numberofapprovalsleft < 0) {
                throw new ApprovalException("Too many approvals have been performed on this request.");
            }
            return numberofapprovalsleft == 0;
        }

    }

    @Override
    public int getRemainingApprovals(Collection<Approval> approvalsPerformed) {
        int approvalsRequired = getNumberOfApprovalsRequired();
        return approvalsRequired - approvalsPerformed.size();
    }

    @Override
    public boolean isStepSizeFixed() {
        // Accumulative Approval Profiles can only have a single sequence
        return true;
    }

    @Override
    protected Class<? extends Profile> getImplementationClass() {
        return  AccumulativeApprovalProfile.class;
    }
    
    @Override
    protected ApprovalPartition addConstantProperties(ApprovalPartition approvalPartition) {
        approvalPartition.addProperty(new DynamicUiProperty<Integer>(PROPERTY_NUMBER_OF_REQUIRED_APPROVALS, 1));
        return approvalPartition;
    }

    @Override
    public boolean canApprovePartition(final AuthenticationToken authenticationToken, final ApprovalPartition approvalPartition) throws AuthenticationFailedException {
        // We all good here, homie. 
        return true;
    }
    
    @Override
    public boolean canViewPartition(AuthenticationToken authenticationToken, ApprovalPartition approvalPartition)
            throws AuthenticationFailedException {
        return canApprovePartition(authenticationToken, approvalPartition);
    }

    @Override
    public int getOrdinalOfStepBeingEvaluated(Collection<Approval> approvalsPerformed) {
        return 1;
    }

    @Override
    public ApprovalStep getStepBeingEvaluated(Collection<Approval> approvalsPerformed) {
        return getStep(FIXED_STEP_ID);
    }

    @Override
    public Set<String> getHiddenProperties() {
        return new HashSet<>(Arrays.asList(PROPERTY_NUMBER_OF_REQUIRED_APPROVALS));
    }


}