package com.icodici.universa.node;

import com.icodici.universa.Approvable;
import com.icodici.universa.HashId;
import com.icodici.universa.HashIdentifiable;
import net.sergeych.tools.Do;
import net.sergeych.utils.LogPrinter;
import net.sergeych.utils.Ut;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZonedDateTime;

/**
 * The state of some {@link HashId} - identifiable item (e.g. {@link Approvable} to be sotred in the {@link Ledger}
 * instance. Use ledger to create instances of this class.
 * <p>
 * This class incapsulates part of the business-logic of the universa which is tied to the saved state. This class is
 * primarily used by the Elections, and LocalNode where most of the business-logic is placed. Direct usage
 * of this class should be avoided.
 * <p>
 * See {@link ItemState} for states and state graph description.
 * <p>
 * Created by sergeych on 16/07/2017.
 */
public class StateRecord implements HashIdentifiable, IStateRecord {

    private static LogPrinter log = new LogPrinter("StateRecord");

    private Ledger ledger;
    private boolean dirty;
    private long recordId;
    private long lockedByRecordId;

    public StateRecord(Ledger ledger, ResultSet rs) throws SQLException, IOException {
        this.ledger = ledger;
        initFrom(rs);
    }

    public void initFrom(ResultSet rs) throws SQLException {
        // the processing mught be already fininshed by now:
        if( rs == null || rs.isClosed() )
            throw new SQLException("resultset or connection is closed");
        recordId = rs.getLong("id");
        try {
            id = HashId.withDigest(Do.read(rs.getBinaryStream("hash")));
        } catch (IOException e) {
            throw new SQLException("failed to read hash from the recordset");
        }
        state = ItemState.values()[rs.getInt("state")];
        createdAt = Ut.getTime(rs.getLong("created_at"));
        expiresAt = Ut.getTime(rs.getLong("expires_at"));
        if(expiresAt == null) {
            // todo: what we should do with items without expiresAt?
            expiresAt = createdAt.plusMonths(3);
        }
        lockedByRecordId = rs.getInt("locked_by_id");
    }

    public StateRecord(Ledger ledger) {
        this.ledger = ledger;
        createdAt = ZonedDateTime.now();
    }

    public boolean isDirty() {
        return dirty;
    }

    public StateRecord(HashId id) {
        this.id = id;
    }

    private volatile ItemState state = ItemState.UNDEFINED;
    private HashId id;
    private @NonNull ZonedDateTime expiresAt = ZonedDateTime.now().plusSeconds(300);
    private @NonNull ZonedDateTime createdAt = ZonedDateTime.now();

    public ItemState getState() {
        return state;
    }


    protected void setDirty() {
        dirty = true;
    }

    public final StateRecord setState(ItemState newState) {
        if (state != newState) {
            state = newState;
            setDirty();
        }
        return this;
    }


    @Override
    public final HashId getId() {
        return id;
    }

    public ZonedDateTime getExpiresAt() {
        return this.expiresAt;
    }

    public ZonedDateTime getCreatedAt() {
        return this.createdAt;
    }

    public void destroy() {
        checkLedgerExists();
        ledger.destroy(this);
    }

    public void save() {

        if (dirty && ledger != null) {
            dirty = false;
            ledger.save(this);
        }
    }

    public final boolean isPositive() {
        return state == ItemState.PENDING_POSITIVE;
    }

    public final boolean isApproved() {
        return state.isApproved();
    }

    public final boolean isPending() {
        return state.isPending();
    }

    public final boolean isNegative() {
        return state == ItemState.PENDING_NEGATIVE;
    }

    public final boolean isDeclined() {
        return state == ItemState.DECLINED;
    }

    public final boolean isArchived() {
        return state == ItemState.REVOKED;
    }

    public final boolean isLocked() {
        return state == ItemState.LOCKED;
    }

    public long getRecordId() {
        return recordId;
    }

    public Ledger getLedger() {
        return ledger;
    }

    public void setLedger(Ledger ledger) {
        this.ledger = ledger;
    }

    /**
     * Update recordId. Normally, only {@link Ledger} do it, so avoid calling it. The recordId could be set only if it
     * is not yet set and there is a connected ledger.
     *
     * @param recordId id of record as long
     */
    public void setRecordId(long recordId) {
        checkLedgerExists();
        if (this.recordId != 0 && this.recordId != recordId)
            throw new IllegalStateException("can't change already assigned recordId");
        this.recordId = recordId;
    }

    private void checkLedgerExists() {
        if (ledger == null)
            throw new IllegalStateException("connect to ledger to set recordId");
    }

    public long getLockedByRecordId() {
        return lockedByRecordId;
    }

    public void setLockedByRecordId(long lockedByRecordId) {
        if (lockedByRecordId != this.lockedByRecordId) {
            this.lockedByRecordId = lockedByRecordId;
            dirty = true;
        }
    }

    public void setId(HashId id) {
        if (this.id == null || !this.id.equals(id)) {
            if (this.id != null)
                throw new IllegalStateException("can't change id of StateRecord");
            this.id = id;
            dirty = true;
        }
    }

    /**
     * Lock the item with a given it as being revoked by this one. Check this state, looks for the record to lock and
     * also checks its state first.
     * <p>
     * Note that the operation is allowed only to the records in {@link ItemState#PENDING}. If the item is already
     * checked locally and is therefore in PENDING_NEGATIVE or PENDING_POSITIVE state, it can not lock any other items.
     *
     * @param idToRevoke is {@link HashId} for item should be revoked
     * @return locked record id null if it could not be node
     */
    public StateRecord lockToRevoke(HashId idToRevoke) {
        checkLedgerExists();
        if (state != ItemState.PENDING)
            throw new IllegalStateException("only pending records are allowed to lock others. found:  " + state);

        StateRecord lockedRecord = ledger.getRecord(idToRevoke);
        if (lockedRecord == null)
            return null;
        ItemState targetState = ItemState.LOCKED;

        switch (lockedRecord.getState()) {
            case LOCKED:
                return null;
            case APPROVED:
                // it's ok, we can lock it
                break;
            case LOCKED_FOR_CREATION:
                //the only possible situation is that records is locked by us.
                if(lockedRecord.getLockedByRecordId() != recordId)
                    return null;
                targetState = ItemState.LOCKED_FOR_CREATION_REVOKED;
                break;
            default:
                // wrong state, can't lock it
                return null;
        }

        lockedRecord.setLockedByRecordId(recordId);
        lockedRecord.setState(targetState);
        lockedRecord.save();

        return lockedRecord;
    }

    /**
     * Unlock the record if it was in a locked state, does nothing otherwise.
     *
     * @return self {@link StateRecord}
     */
    public StateRecord unlock() {
        switch (state) {
            case LOCKED:
                setState(ItemState.APPROVED);
                setLockedByRecordId(0);
                break;
            case LOCKED_FOR_CREATION:
            case LOCKED_FOR_CREATION_REVOKED:
                destroy();
                break;
            default:
                break;
        }
        return this;
    }

    /**
     * Set the record to the archiver (e.g. revoked) state, if it is allowed, and save. Trows {@link
     * IllegalStateException} if current state does not allow revocation. Note that the normal states graph allows
     * revokation only from {@link ItemState#LOCKED} state!
     */
    public void revoke() {
        if (state == ItemState.LOCKED) {
            setState(ItemState.REVOKED);
            save();
        } else {
            throw new IllegalStateException("can't archive record that is not in the locked state");
        }
    }

    /**
     * Set record to the approved state and saves it. Note that this process does not resolve any dependencies as for
     * now and does not revoke any records locked for revocation. This could be changed though.
     */
    public void approve() {
        checkLedgerExists();
        // check sanity
        if (state.isPending()) {
            setState(ItemState.APPROVED);
            save();
        } else
            throw new IllegalStateException("attempt to approve record that is not pending: " + state);
    }

    /**
     * Create a record, locked for approval, e.g. item that will be creared (and approved) by current item if it will
     * get the positive consensus. Throws {@link IllegalStateException} if this record is not in a proper state to
     * perform this operation, e.g. not is in {@link ItemState#PENDING} - what means, the local check is not yet
     * finished.
     * <p>
     * Note that items that are already locally checked ({@link ItemState#PENDING_NEGATIVE} or {@link
     * ItemState#PENDING_POSITIVE} can not create any output locks.
     *
     * @param id id of the new item to be locked for approval
     * @return the record of the new item locked for creatoin pn success, null it such item already exists and not
     * locked for apporoval by us.
     */
    public StateRecord createOutputLockRecord(HashId id) {
        checkLedgerExists();
        checkHaveRecordId();
        if (state != ItemState.PENDING)
            throw new IllegalStateException("wrong state to createOutputLockRecord: " + state);
        StateRecord newRecord = ledger.getRecord(id);
        if (newRecord != null) {
            return null;
        }
        newRecord = ledger.createOutputLockRecord(recordId, id);
        return newRecord;
    }

    private void checkHaveRecordId() {
        if (recordId == 0)
            throw new IllegalStateException("the record must be created");
    }

    public StateRecord setExpiresAt(@NonNull ZonedDateTime expiresAt) {
        if( !this.expiresAt.equals(expiresAt) ) {
            this.expiresAt = expiresAt;
            dirty = true;
        }
        return this;
    }

    public boolean isExpired() {
        return expiresAt.isBefore(ZonedDateTime.now());
    }

    public StateRecord setCreatedAt(@NonNull ZonedDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public void markTestRecord() {
        if(ledger != null) {
            ledger.markTestRecord(this.getId());
        }
    }

    static public class NotFoundException extends IOException {
        public NotFoundException() {
        }

        public NotFoundException(String message) {
            super(message);
        }

    }

    public StateRecord reload() throws NotFoundException {
        checkLedgerExists();
        if (recordId == 0)
            throw new IllegalStateException("can't reload record without recordId (new?)");
        ledger.reload(this);
        return this;
    }

    @Override
    public String toString() {
        return "State<"+getId()+"/"+getRecordId()+":"+getState()+":"+getCreatedAt()+"/"+getExpiresAt()+">";
    }
}
