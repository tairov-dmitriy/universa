package com.icodici.universa.contract.jsapi;

import com.icodici.crypto.KeyAddress;
import com.icodici.universa.contract.Contract;

import java.util.ArrayList;
import java.util.List;

/**
 * Implements js-api part for working with contract.
 */
public class JSApiContract {
    private Contract currentContract;

    public JSApiContract(Contract c) {
        this.currentContract = c;
    }

    public String getId() {
        return this.currentContract.getId().toBase64String();
    }

    public int getRevision() {
        return this.currentContract.getState().getRevision();
    }

    public String getOrigin() {
        return this.currentContract.getOrigin().toBase64String();
    }

    public String getParent() {
        return this.currentContract.getParent() == null ? null : this.currentContract.getParent().toBase64String();
    }

    public long getCreatedAt() {
        return this.currentContract.getCreatedAt().toEpochSecond();
    }

    public String getStateDataField(String fieldPath) {
        return this.currentContract.getStateData().getStringOrThrow(fieldPath).toString();
    }

    public void setStateDataField(String fieldPath, String value) {
        this.currentContract.getStateData().set(fieldPath, value);
    }

    public void setStateDataField(String fieldPath, int value) {
        this.currentContract.getStateData().set(fieldPath, value);
    }

    public String getDefinitionDataField(String fieldPath) {
        return this.currentContract.getDefinition().getData().getStringOrThrow(fieldPath);
    }

    public List<String> getIssuer() {
        return this.currentContract.getIssuer().getAllAddresses();
    }

    public List<String> getOwner() {
        return this.currentContract.getOwner().getAllAddresses();
    }

    public List<String> getCreator() {
        return this.currentContract.getCreator().getAllAddresses();
    }

    public void setOwner(List<String> addresses) throws KeyAddress.IllegalAddressException {
        List<KeyAddress> addressesList = new ArrayList<>();
        for (String s : addresses)
            addressesList.add(new KeyAddress(s));
        this.currentContract.setOwnerKeys(addressesList);
    }

    public JSApiContract createRevision() {
        return new JSApiContract(this.currentContract.createRevision());
    }

    /**
     * Extracts instance of {@link Contract} from instance of {@link JSApiContract}.
     */
    public Contract extractContract(JSApiAccessor accessor) throws IllegalArgumentException {
        if (accessor == null)
            throw new ClassCastException("restricted_method");
        return currentContract;
    }
}
